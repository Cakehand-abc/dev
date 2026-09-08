package com.johnnylin.dev.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnylin.dev.common.*;
import com.johnnylin.dev.domain.*;
import com.johnnylin.dev.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.*;
import java.util.*;
import static com.johnnylin.dev.common.Input.*;
import static com.johnnylin.dev.service.CareService.required;

@Service
@RequiredArgsConstructor
public class GeoService {
    private final DeviceMapper devices; private final BindingMapper bindings; private final ElderMapper elders;
    private final LocationMapper locations; private final FenceMapper fences; private final FenceMemberMapper members; private final UserMapper users;
    private final HeartbeatEventMapper heartbeatEvents;
    private final FenceStateMapper states; private final AlertMapper alerts; private final AuditMapper audits; private final ObjectMapper json;
    @Value("${app.online-minutes:5}") private int onlineMinutes;
    @Value("${app.clock-skew-seconds:60}") private int skewSeconds;
    // A deterministic fence -> device -> elder lock order serializes monitoring edits with ingestion.
    // Suitable for the single-unit teaching deployment; partition locks before high-throughput hardware ingestion.
    private void lockMonitoring(){users.selectList(new QueryWrapper<User>().orderByAsc("id").last("LIMIT 1 FOR UPDATE"));fences.selectList(new QueryWrapper<Fence>().orderByAsc("id").last("FOR UPDATE"));}
    private void audit(String action,String type,Long id){var a=new Audit();a.setUserId(CareService.actor());a.setAction(action);a.setTargetType(type);a.setTargetId(id);a.setOccurredAt(now());a.setResult("SUCCESS");audits.insert(a);}
    public void auditDeviceAction(String action,Long id){audit(action,"watch_device",id);}
    public long bindingCountForElder(Long elderId){return bindings.selectCount(new QueryWrapper<Binding>().eq("elder_id",elderId));}
    public long locationCountForElder(Long elderId){return locations.selectCount(new QueryWrapper<Location>().eq("elder_id",elderId));}
    public long fenceMembershipCountForElder(Long elderId){return members.selectCount(new QueryWrapper<FenceMember>().eq("elder_id",elderId));}
    private Elder active(Long id){var e=required(elders.lock(id));if(!"ACTIVE".equals(e.getStatus()))throw Api.conflict("老人已归档");return e;}
    private static double coordinate(Map<String,Object>b,String name,double low,double high){return BigDecimal.valueOf(number(b,name,low,high)).setScale(7,RoundingMode.HALF_UP).doubleValue();}
    public static double distance(double lon1,double lat1,double lon2,double lat2){
        double a=Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2),2);
        return 6371008.8*2*Math.asin(Math.sqrt(Math.min(1,Math.max(0,a))));
    }
    @Transactional
    public Device saveDevice(Long id,Map<String,Object>b){
        keys(b,"serialNo","model","enabled","version");lockMonitoring();var d=id==null?new Device():required(devices.lock(id));
        if(id!=null&&version(b)!=d.getVersion())throw Api.conflict("数据已变化，请刷新");
        boolean enabled=bool(b,"enabled");if(!enabled&&id!=null&&bindings.selectCount(new QueryWrapper<Binding>().eq("active_device_id",id))>0)throw Api.conflict("请先解绑设备");
        d.setSerialNo(text(b,"serialNo",80));d.setModel(text(b,"model",80));d.setEnabled(enabled);
        if(id==null){d.setVersion(0);devices.insert(d);}else{d.setVersion(d.getVersion()+1);devices.updateById(d);}audit(id==null?"CREATE":"UPDATE","watch_device",d.getId());return d;
    }
    @Transactional
    public Binding bind(Long deviceId,Long elderId){
        lockMonitoring();var d=required(devices.lock(deviceId));if(!d.getEnabled())throw Api.conflict("设备已停用");active(elderId);
        if(bindings.selectCount(new QueryWrapper<Binding>().eq("active_device_id",deviceId).or().eq("active_elder_id",elderId))>0)throw Api.conflict("设备或老人已有有效绑定，请先解绑");
        var b=new Binding();b.setDeviceId(deviceId);b.setElderId(elderId);b.setBoundAt(now());b.setActiveDeviceId(deviceId);b.setActiveElderId(elderId);bindings.insert(b);audit("BIND","watch_device",deviceId);return b;
    }
    @Transactional
    public void unbind(Long id){lockMonitoring();required(devices.lock(id));var b=bindings.selectOne(new QueryWrapper<Binding>().eq("active_device_id",id));if(b==null)throw Api.conflict("设备未绑定");closeBinding(b);audit("UNBIND","watch_device",id);}
    private void closeBinding(Binding b){
        bindings.update(null,new UpdateWrapper<Binding>().eq("id",b.getId()).set("unbound_at",now()).set("active_device_id",null).set("active_elder_id",null));
        for(var m:members.selectList(new QueryWrapper<FenceMember>().eq("elder_id",b.getElderId())))reset(m,"UNBOUND");
    }
    @Transactional
    public void releaseElder(Long elderId){
        lockMonitoring();var b=bindings.selectOne(new QueryWrapper<Binding>().eq("active_elder_id",elderId));
        if(b!=null){required(devices.lock(b.getDeviceId()));closeBinding(b);}
        required(elders.lock(elderId));
        for(var m:members.selectList(new QueryWrapper<FenceMember>().eq("elder_id",elderId))){reset(m,"ARCHIVED");m.setEnabled(false);members.updateById(m);}
    }
    public List<Map<String,Object>> distribution(Long regionId,String status){
        List<Map<String,Object>> out=new ArrayList<>();
        for(var d:devices.selectList(new QueryWrapper<Device>().orderByAsc("id"))){
            var b=bindings.selectOne(new QueryWrapper<Binding>().eq("active_device_id",d.getId()));Elder e=b==null?null:elders.selectById(b.getElderId());
            if(regionId!=null&&(e==null||!regionId.equals(e.getRegionId())))continue;
            String state=!d.getEnabled()?"DISABLED":d.getLastSeenAt()==null?"UNKNOWN":d.getLastSeenAt().isBefore(now().minusMinutes(onlineMinutes))?"OFFLINE":"ONLINE";
            if(status!=null&&!status.equals(state))continue;
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",d.getId());row.put("serialNo",d.getSerialNo());row.put("model",d.getModel());row.put("enabled",d.getEnabled());row.put("version",d.getVersion());row.put("status",state);row.put("lastSeenAt",d.getLastSeenAt());
            row.put("elderId",e==null?null:e.getId());row.put("elderName",e==null?null:e.getName());
            Location point=b==null?null:locations.selectOne(new QueryWrapper<Location>().eq("binding_id",b.getId()).orderByDesc("recorded_at","id").last("LIMIT 1"));row.put("location",point);out.add(row);
        }return out;
    }
    public Map<String,Object> devices(int page,int size){return CareService.page(devices,new QueryWrapper<>(),page,size);}
    public List<Map<String,Object>> fences(){
        List<Map<String,Object>> out=new ArrayList<>();for(var f:fences.selectList(new QueryWrapper<Fence>().orderByAsc("id"))){
            Map<String,Object> row=new LinkedHashMap<>();row.put("id",f.getId());row.put("name",f.getName());row.put("centerLon",f.getCenterLon());row.put("centerLat",f.getCenterLat());row.put("radiusM",f.getRadiusM());row.put("enabled",f.getEnabled());row.put("version",f.getVersion());
            row.put("elderIds",members.selectList(new QueryWrapper<FenceMember>().eq("fence_id",f.getId()).eq("enabled",true)).stream().map(FenceMember::getElderId).toList());out.add(row);
        }return out;
    }
    @Transactional
    public Fence saveFence(Long id,Map<String,Object>b){
        keys(b,"name","centerLon","centerLat","radiusM","enabled","version");lockMonitoring();var f=id==null?new Fence():required(fences.lock(id));
        if(id!=null&&version(b)!=f.getVersion())throw Api.conflict("围栏已变化，请刷新");
        f.setName(text(b,"name",80));f.setCenterLon(coordinate(b,"centerLon",-180,180));f.setCenterLat(coordinate(b,"centerLat",-90,90));f.setRadiusM(number(b,"radiusM",50,5000));f.setEnabled(bool(b,"enabled"));
        if(id==null){f.setVersion(0);fences.insert(f);}else{f.setVersion(f.getVersion()+1);fences.updateById(f);for(var m:members.selectList(new QueryWrapper<FenceMember>().eq("fence_id",id)))reset(m,f.getEnabled()?"RECONFIGURED":"DISABLED");}
        audit(id==null?"CREATE":"UPDATE","geofence",f.getId());return f;
    }
    @Transactional
    public void setMembers(Long id,List<Long> elderIds){
        if(elderIds==null||elderIds.size()>1000||elderIds.stream().anyMatch(Objects::isNull))throw Api.bad("围栏成员列表无效");lockMonitoring();required(fences.lock(id));
        Set<Long> desired=new TreeSet<>(elderIds);for(Long eid:desired)active(eid);
        for(var m:members.selectList(new QueryWrapper<FenceMember>().eq("fence_id",id))){
            boolean enabled=desired.remove(m.getElderId());if(enabled!=m.getEnabled()){reset(m,enabled?"RECONFIGURED":"REMOVED");m.setEnabled(enabled);members.updateById(m);}
        }
        for(Long eid:desired){var m=new FenceMember();m.setFenceId(id);m.setElderId(eid);m.setEnabled(true);members.insert(m);var s=new FenceState();s.setMemberId(m.getId());states.insert(s);}audit("SET_MEMBERS","geofence",id);
    }
    private void reset(FenceMember m,String reason){
        var s=states.selectOne(new QueryWrapper<FenceState>().eq("member_id",m.getId()));if(s==null)return;
        if(s.getActiveAlertId()!=null){var a=required(alerts.selectById(s.getActiveAlertId()));a.setClosedAt(now());a.setCloseReason(reason);a.setVersion(a.getVersion()+1);alerts.updateById(a);}
        states.update(null,new UpdateWrapper<FenceState>().eq("id",s.getId()).set("inside",null).set("active_alert_id",null).set("last_recorded_at",null).set("last_point_id",null));
    }
    @Transactional
    public Location ingest(Map<String,Object>b){
        keys(b,"deviceId","eventId","longitude","latitude","recordedAt");Long deviceId=id(b,"deviceId");String eventId=text(b,"eventId",100);var recorded=time(b,"recordedAt");
        if(recorded.isAfter(now().plusSeconds(skewSeconds)))throw Api.bad("定位时间超过允许时钟偏差");double lon=coordinate(b,"longitude",-180,180),lat=coordinate(b,"latitude",-90,90);
        lockMonitoring();var device=required(devices.lock(deviceId));
        Location duplicate=locations.selectOne(new QueryWrapper<Location>().eq("device_id",deviceId).eq("event_id",eventId));
        if(duplicate!=null){if(duplicate.getRecordedAt().equals(recorded)&&duplicate.getLongitude()==lon&&duplicate.getLatitude()==lat)return duplicate;throw Api.conflict("同一事件编号的内容不一致");}
        if(!device.getEnabled())throw Api.conflict("设备已停用");
        var binding=bindings.selectOne(new QueryWrapper<Binding>().eq("device_id",deviceId).le("bound_at",recorded).and(q->q.isNull("unbound_at").or().gt("unbound_at",recorded)));
        if(binding==null)throw Api.conflict("定位时刻没有有效绑定");
        var point=new Location();point.setDeviceId(deviceId);point.setBindingId(binding.getId());point.setElderId(binding.getElderId());point.setEventId(eventId);point.setLongitude(lon);point.setLatitude(lat);point.setRecordedAt(recorded);point.setReceivedAt(now());locations.insert(point);
        // Historical events remain queryable but must not reactivate monitoring after unbinding.
        if(binding.getUnboundAt()!=null)return point;
        for(var member:members.selectList(new QueryWrapper<FenceMember>().eq("elder_id",binding.getElderId()).eq("enabled",true))){
            var fence=required(fences.selectById(member.getFenceId()));if(!fence.getEnabled())continue;
            var state=states.selectOne(new QueryWrapper<FenceState>().eq("member_id",member.getId()));
            if(state.getLastRecordedAt()!=null&&recorded.isBefore(state.getLastRecordedAt()))continue;
            boolean inside=distance(lon,lat,fence.getCenterLon(),fence.getCenterLat())<=fence.getRadiusM()+0.000001;
            if(!inside&&state.getActiveAlertId()==null){
                var a=new Alert();a.setMemberId(member.getId());a.setExitPointId(point.getId());a.setTriggeredAt(recorded);a.setVersion(0);
                try{a.setGeometrySnapshot(json.writeValueAsString(Map.of("longitude",fence.getCenterLon(),"latitude",fence.getCenterLat(),"radiusM",fence.getRadiusM(),"version",fence.getVersion())));}catch(Exception e){throw new IllegalStateException(e);}
                alerts.insert(a);state.setActiveAlertId(a.getId());
            }else if(inside&&state.getActiveAlertId()!=null){var a=required(alerts.selectById(state.getActiveAlertId()));a.setReturnedAt(recorded);a.setClosedAt(recorded);a.setCloseReason("RETURNED");a.setVersion(a.getVersion()+1);alerts.updateById(a);state.setActiveAlertId(null);}
            states.update(null,new UpdateWrapper<FenceState>().eq("id",state.getId()).set("last_point_id",point.getId()).set("last_recorded_at",recorded).set("inside",inside).set("active_alert_id",state.getActiveAlertId()));
        }return point;
    }
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String,Object> ingestBatch(Map<String,Object>b){
        keys(b,"deviceId","points");Long deviceId=id(b,"deviceId");Object raw=b.get("points");
        if(!(raw instanceof List<?> list)||list.isEmpty()||list.size()>500)throw Api.bad("points 必须包含 1 至 500 个定位点");
        List<Map<String,Object>> validated=new ArrayList<>(list.size());
        for(Object item:list){
            if(!(item instanceof Map<?,?> source))throw Api.bad("定位点格式不正确");
            Map<String,Object> point=new LinkedHashMap<>();source.forEach((k,v)->point.put(String.valueOf(k),v));
            keys(point,"eventId","longitude","latitude","recordedAt");text(point,"eventId",100);coordinate(point,"longitude",-180,180);coordinate(point,"latitude",-90,90);
            if(time(point,"recordedAt").isAfter(now().plusSeconds(skewSeconds)))throw Api.bad("定位时间超过允许时钟偏差");
            point.put("deviceId",deviceId);validated.add(point);
        }
        int accepted=0,duplicates=0;LocalDateTime first=null,last=null;
        for(Map<String,Object> point:validated){
            String eventId=text(point,"eventId",100);
            boolean exists=locations.selectCount(new QueryWrapper<Location>().eq("device_id",deviceId).eq("event_id",eventId))>0;
            Location saved=ingest(point);if(exists)duplicates++;else accepted++;
            if(first==null||saved.getRecordedAt().isBefore(first))first=saved.getRecordedAt();
            if(last==null||saved.getRecordedAt().isAfter(last))last=saved.getRecordedAt();
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("received",list.size());result.put("accepted",accepted);result.put("duplicates",duplicates);result.put("firstRecordedAt",first);result.put("lastRecordedAt",last);return result;
    }
    @Transactional
    public Device heartbeat(Map<String,Object>b){keys(b,"deviceId","eventId","recordedAt");Long deviceId=id(b,"deviceId");String eventId=text(b,"eventId",100);var stamp=time(b,"recordedAt");if(stamp.isAfter(now().plusSeconds(skewSeconds)))throw Api.bad("心跳时间超过允许偏差");
        var d=required(devices.lock(deviceId));if(!d.getEnabled())throw Api.conflict("设备已停用");
        var duplicate=heartbeatEvents.selectOne(new QueryWrapper<HeartbeatEvent>().eq("device_id",deviceId).eq("event_id",eventId));
        if(duplicate!=null){if(!duplicate.getRecordedAt().equals(stamp))throw Api.conflict("同一心跳事件编号的内容不一致");return d;}
        var event=new HeartbeatEvent();event.setDeviceId(deviceId);event.setEventId(eventId);event.setRecordedAt(stamp);event.setReceivedAt(now());heartbeatEvents.insert(event);
        if(d.getLastSeenAt()==null||stamp.isAfter(d.getLastSeenAt())){d.setLastSeenAt(stamp);devices.updateById(d);}return d;}
    public List<Location> trajectory(Long id,String start,String end){
        required(elders.selectById(id));if(start==null||end==null)throw Api.bad("请选择轨迹起止时间");var p=filters(null,start,end,id,null);
        if(Duration.between((LocalDateTime)p.get("start"),(LocalDateTime)p.get("end")).compareTo(Duration.ofHours(24))>0)throw Api.bad("单次轨迹查询不能超过 24 小时");
        var q=new QueryWrapper<Location>().eq("elder_id",id).ge("recorded_at",p.get("start")).lt("recorded_at",p.get("end"));
        if(locations.selectCount(q)>5000)throw Api.bad("轨迹超过 5000 点，请缩短时间范围");return locations.selectList(q.orderByAsc("recorded_at","id"));
    }
    @SuppressWarnings("unchecked")
    public Map<String,Object> alerts(Long elderId,Boolean handled,String start,String end,int page,int size){
        var p=filters(null,start,end,elderId,null);var q=new QueryWrapper<Alert>().ge("triggered_at",p.get("start")).lt("triggered_at",p.get("end"));
        if(elderId!=null)q.inSql("member_id","SELECT id FROM geofence_member WHERE elder_id = "+elderId);
        if(handled!=null){if(handled)q.isNotNull("handled_at");else q.isNull("handled_at");}
        var result=CareService.page(alerts,q,page,size);List<Map<String,Object>> rows=new ArrayList<>();
        for(var a:(List<Alert>)result.get("records")){Map<String,Object> row=json.convertValue(a,Map.class);var m=members.selectById(a.getMemberId());row.put("elderId",m.getElderId());row.put("elderName",elders.selectById(m.getElderId()).getName());row.put("fenceName",fences.selectById(m.getFenceId()).getName());rows.add(row);}result.put("records",rows);return result;
    }
    @Transactional
    public void handle(Long id,Map<String,Object>b){
        keys(b,"note","version");var a=required(alerts.lock(id));if(a.getHandledAt()!=null)throw Api.conflict("该事件已处理");if(version(b)!=a.getVersion())throw Api.conflict("事件状态已变化，请刷新");
        a.setHandledBy(CareService.actor());a.setHandledAt(now());a.setHandlingNote(text(b,"note",1000));a.setVersion(a.getVersion()+1);alerts.updateById(a);audit("HANDLE","geofence_alert",id);
    }
}
