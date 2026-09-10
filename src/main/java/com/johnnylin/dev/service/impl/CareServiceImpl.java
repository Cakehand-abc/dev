package com.johnnylin.dev.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.common.*;
import com.johnnylin.dev.domain.*;
import com.johnnylin.dev.mapper.*;
import com.johnnylin.dev.service.CareService;
import com.johnnylin.dev.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.johnnylin.dev.common.Input.*;

/**
 * 养老医护综合业务服务实现类。
 */
@Service
@RequiredArgsConstructor
public class CareServiceImpl implements CareService {
    private final ElderMapper elders; private final RegionMapper regions; private final DoctorMapper doctors;
    private final HealthRecordMapper health; private final HealthRuleMapper rules;
    private final FollowupPlanMapper plans; private final FollowupRecordMapper records;
    private final UserMapper users; private final AuditMapper audits; private final PasswordEncoder passwords;
    private final ObjectMapper json; private final GeoService geo;

    public static <T> T required(T value){if(value==null)throw Api.missing();return value;}
    public static Long actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.getPrincipal() instanceof AccountPrincipal p?p.id():null;}
    public void audit(String action,String type,Long id){var a=new Audit();a.setUserId(actor());a.setAction(action);a.setTargetType(type);a.setTargetId(id);a.setOccurredAt(now());a.setResult("SUCCESS");audits.insert(a);}
    public static <T> Map<String,Object> page(BaseMapper<T> mapper,QueryWrapper<T> query,int page,int size){
        if(page<1||page>1000000||size<1||size>100)throw Api.bad("分页范围不正确");
        long total=mapper.selectCount(query);query.orderByDesc("id").last("LIMIT "+size+" OFFSET "+((long)(page-1)*size));
        return new LinkedHashMap<>(Map.of("records",mapper.selectList(query),"total",total,"page",page,"size",size));
    }
    public List<Region> regions(){return regions.selectList(new QueryWrapper<Region>().orderByAsc("id"));}
    public static String mask(String s){return s==null||s.length()<7?s:s.substring(0,3)+"****"+s.substring(s.length()-4);}
    private Elder masked(Elder e){e.setPhone(mask(e.getPhone()));return e;}
    private Doctor masked(Doctor d){d.setPhone(mask(d.getPhone()));return d;}
    public Elder elder(Long id){return masked(required(elders.selectById(id)));}
    @SuppressWarnings("unchecked")
    public Map<String,Object> elders(Long region,String keyword,String status,int page,int size){
        var q=new QueryWrapper<Elder>().eq(region!=null,"region_id",region).eq(status!=null,"status",status);
        if(keyword!=null&&!keyword.isBlank())q.and(w->w.like("name",keyword).or().like("code",keyword));
        var out=page(elders,q,page,size);((List<Elder>)out.get("records")).forEach(this::masked);return out;
    }
    @Transactional
    public Elder saveElder(Long id,Map<String,Object>b){
        keys(b,"code","name","gender","birthDate","regionId","phone","version");
        Elder e=id==null?new Elder():required(elders.lock(id));
        if(id!=null&&(!"ACTIVE".equals(e.getStatus())))throw Api.conflict("已归档老人不可编辑");
        if(id!=null&&version(b)!=e.getVersion())throw Api.conflict("数据已变化，请刷新");
        e.setCode(text(b,"code",40));e.setName(text(b,"name",80));e.setGender(choice(b,"gender","MALE","FEMALE","UNKNOWN"));
        e.setBirthDate(date(b,"birthDate"));e.setRegionId(id(b,"regionId"));required(regions.selectById(e.getRegionId()));
        String phone=optional(b,"phone",30);if(phone!=null&&phone.contains("*")){if(id==null)throw Api.bad("电话不能包含脱敏符号");phone=e.getPhone();}
        e.setPhone(phone);
        if(id==null){e.setStatus("ACTIVE");e.setVersion(0);elders.insert(e);}
        else {e.setVersion(e.getVersion()+1);elders.update(e,new UpdateWrapper<Elder>().eq("id",id).set("phone",phone).set("birth_date",e.getBirthDate()));}
        audit(id==null?"CREATE":"UPDATE","elder",e.getId());return masked(e);
    }
    @Transactional
    public void archive(Long id){
        // Lock ordering is device then elder, matching bind/unbind ingestion transactions.
        geo.releaseElder(id);
        Elder e=required(elders.lock(id));if("ARCHIVED".equals(e.getStatus()))throw Api.conflict("已归档");
        e.setStatus("ARCHIVED");e.setVersion(e.getVersion()+1);elders.updateById(e);
        plans.update(null,new UpdateWrapper<FollowupPlan>().eq("elder_id",id).eq("status","PENDING").set("status","CANCELED").set("canceled_at",now()).set("cancel_reason","老人归档").setSql("version = version + 1"));
        audit("ARCHIVE","elder",id);
    }
    @Transactional
    public void deleteElder(Long id){
        Elder elder=required(elders.lock(id));
        long healthCount=health.selectCount(new QueryWrapper<HealthRecord>().eq("elder_id",id));
        long planCount=plans.selectCount(new QueryWrapper<FollowupPlan>().eq("elder_id",id));
        long bindingCount=geo.bindingCountForElder(id);
        long locationCount=geo.locationCountForElder(id);
        long fenceCount=geo.fenceMembershipCountForElder(id);
        long related=healthCount+planCount+bindingCount+locationCount+fenceCount;
        if(related>0)throw Api.conflict("该档案已有健康、随访、设备、定位或围栏业务记录，只能归档，不能删除");
        if(elders.deleteById(id)!=1)throw Api.conflict("档案删除失败，请刷新后重试");
        audit("DELETE","elder",elder.getId());
    }
    public Elder activeElder(Long id){Elder e=required(elders.selectById(id));if(!"ACTIVE".equals(e.getStatus()))throw Api.conflict("老人已归档");return e;}
    @SuppressWarnings("unchecked")
    public Map<String,Object> doctors(int page,int size){var out=page(doctors,new QueryWrapper<>(),page,size);((List<Doctor>)out.get("records")).forEach(this::masked);return out;}
    @Transactional
    public Doctor saveDoctor(Long id,Map<String,Object>b){
        keys(b,"code","name","department","phone","enabled","version");
        var d=id==null?new Doctor():required(doctors.lock(id));if(id!=null&&version(b)!=d.getVersion())throw Api.conflict("数据已变化，请刷新");
        d.setCode(text(b,"code",40));d.setName(text(b,"name",80));d.setDepartment(text(b,"department",100));d.setEnabled(bool(b,"enabled"));
        String phone=optional(b,"phone",30);if(phone!=null&&phone.contains("*")){if(id==null)throw Api.bad("电话不能包含脱敏符号");phone=d.getPhone();}d.setPhone(phone);
        if(id==null){d.setVersion(0);doctors.insert(d);}else{d.setVersion(d.getVersion()+1);doctors.update(d,new UpdateWrapper<Doctor>().eq("id",id).set("phone",phone));}
        audit(id==null?"CREATE":"UPDATE","doctor",d.getId());return masked(d);
    }
    @Transactional
    public HealthRecord addHealth(Map<String,Object>b){
        keys(b,"elderId","measuredAt","systolic","diastolic","heartRate","oxygen","temperature","eventId");
        var h=new HealthRecord();h.setElderId(id(b,"elderId"));activeElder(h.getElderId());h.setMeasuredAt(time(b,"measuredAt"));
        if(h.getMeasuredAt().isAfter(now()))throw Api.bad("检测时间不能晚于当前时间");
        h.setSystolic(optionalMetric(b,"systolic",20,300));h.setDiastolic(optionalMetric(b,"diastolic",10,200));
        h.setHeartRate(optionalMetric(b,"heartRate",10,300));h.setOxygen(optionalMetric(b,"oxygen",0,100));h.setTemperature(optionalMetric(b,"temperature",25,45));
        if(h.getSystolic()!=null&&h.getDiastolic()!=null&&h.getSystolic()<=h.getDiastolic())throw Api.bad("收缩压必须大于舒张压");
        Map<String,Double> values=new LinkedHashMap<>();values.put("systolic",h.getSystolic());values.put("diastolic",h.getDiastolic());values.put("heartRate",h.getHeartRate());values.put("oxygen",h.getOxygen());values.put("temperature",h.getTemperature());
        if(values.values().stream().allMatch(Objects::isNull))throw Api.bad("至少填写一项检测指标");
        boolean abnormal=false;List<Map<String,Object>> snapshot=new ArrayList<>();
        for(var entry:values.entrySet())if(entry.getValue()!=null){
            var rule=rules.selectOne(new QueryWrapper<HealthRule>().eq("metric",entry.getKey()).eq("enabled",true).orderByDesc("version").last("LIMIT 1"));
            if(rule==null)throw Api.conflict("缺少演示阈值规则");
            boolean outside=entry.getValue()<rule.getLowerBound()||entry.getValue()>rule.getUpperBound();abnormal|=outside;
            snapshot.add(Map.of("metric",entry.getKey(),"value",entry.getValue(),"lower",rule.getLowerBound(),"upper",rule.getUpperBound(),"version",rule.getVersion(),"abnormal",outside,"purpose","教学演示"));
        }
        h.setAbnormal(abnormal);try{h.setRuleSnapshot(json.writeValueAsString(snapshot));}catch(Exception e){throw new IllegalStateException(e);}
        h.setSource("MANUAL");h.setEventId(b.get("eventId")==null?UUID.randomUUID().toString():text(b,"eventId",100));health.insert(h);audit("CREATE","health_record",h.getId());return h;
    }
    private Double optionalMetric(Map<String,Object>b,String key,double low,double high){return b.get(key)==null?null:number(b,key,low,high);}
    public Map<String,Object> health(Long region,Long elderId,String start,String end,Boolean abnormal,int page,int size){
        var p=filters(region,start,end,elderId,null);var q=new QueryWrapper<HealthRecord>().eq(elderId!=null,"elder_id",elderId)
            .ge("measured_at",p.get("start")).lt("measured_at",p.get("end")).eq(abnormal!=null,"abnormal",abnormal);
        if(region!=null)q.inSql("elder_id","SELECT id FROM elder WHERE region_id = "+region);
        var out=page(health,q,page,size);return enrich(out,"health");
    }
    public List<HealthRecord> trend(Long elderId,String metric,String start,String end){
        required(elders.selectById(elderId));if(!Set.of("systolic","diastolic","heartRate","oxygen","temperature").contains(metric))throw Api.bad("指标无效");
        var p=filters(null,start,end,elderId,null);var q=new QueryWrapper<HealthRecord>().eq("elder_id",elderId).ge("measured_at",p.get("start")).lt("measured_at",p.get("end"));
        if(health.selectCount(q)>5000)throw Api.bad("记录超过 5000 条，请缩短时间范围");return health.selectList(q.orderByAsc("measured_at","id"));
    }
    @Transactional
    public FollowupPlan addPlan(Map<String,Object>b){
        keys(b,"elderId","doctorId","dueAt");var p=new FollowupPlan();p.setElderId(id(b,"elderId"));activeElder(p.getElderId());p.setDoctorId(id(b,"doctorId"));
        if(!required(doctors.selectById(p.getDoctorId())).getEnabled())throw Api.conflict("医生已停用");
        p.setDueAt(time(b,"dueAt"));p.setStatus("PENDING");p.setVersion(0);plans.insert(p);audit("CREATE","followup_plan",p.getId());return p;
    }
    @Transactional
    public FollowupRecord complete(Long id,Map<String,Object>b){
        keys(b,"completedAt","content","result");var plan=required(plans.lock(id));if(!"PENDING".equals(plan.getStatus()))throw Api.conflict("计划已完成或取消");
        var r=new FollowupRecord();r.setPlanId(id);r.setCompletedAt(time(b,"completedAt"));if(r.getCompletedAt().isAfter(now()))throw Api.bad("完成时间不能晚于当前时间");
        r.setContent(text(b,"content",2000));r.setResult(text(b,"result",500));records.insert(r);plan.setStatus("COMPLETED");plan.setVersion(plan.getVersion()+1);plans.updateById(plan);audit("COMPLETE","followup_plan",id);return r;
    }
    @Transactional
    public void cancel(Long id,Map<String,Object>b){
        keys(b,"reason");var p=required(plans.lock(id));if(!"PENDING".equals(p.getStatus()))throw Api.conflict("计划已完成或取消");p.setStatus("CANCELED");p.setCanceledAt(now());p.setCancelReason(text(b,"reason",500));p.setVersion(p.getVersion()+1);plans.updateById(p);audit("CANCEL","followup_plan",id);
    }
    public Map<String,Object> plans(Long region,Long elderId,Long doctorId,String start,String end,String status,int page,int size){
        var p=filters(region,start,end,elderId,doctorId);var q=new QueryWrapper<FollowupPlan>().eq(elderId!=null,"elder_id",elderId).eq(doctorId!=null,"doctor_id",doctorId)
            .ge("due_at",p.get("start")).lt("due_at",p.get("end")).eq(status!=null,"status",status);
        if(region!=null)q.inSql("elder_id","SELECT id FROM elder WHERE region_id = "+region);
        return enrich(page(plans,q,page,size),"plan");
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> enrich(Map<String,Object> page,String kind){
        var list=(List<?>)page.get("records");List<Map<String,Object>> output=new ArrayList<>();
        for(Object row:list){Map<String,Object> value=json.convertValue(row,Map.class);
            Long elderId=row instanceof HealthRecord h?h.getElderId():((FollowupPlan)row).getElderId();
            Elder e=elders.selectById(elderId);value.put("elderName",e==null?"已归档":e.getName());
            if(row instanceof FollowupPlan p){Doctor d=doctors.selectById(p.getDoctorId());value.put("doctorName",d==null?"未知":d.getName());
                var r=records.selectOne(new QueryWrapper<FollowupRecord>().eq("plan_id",p.getId()));value.put("record",r);
                value.put("overdue","PENDING".equals(p.getStatus())&&p.getDueAt().isBefore(now()));
                value.put("late",r!=null&&r.getCompletedAt().isAfter(p.getDueAt()));}
            output.add(value);
        }
        page.put("records",output);return page;
    }
    public Map<String,Object> users(int page,int size){return page(users,new QueryWrapper<>(),page,size);}
    public static String password(Map<String,Object>b){String s=text(b,"password",72);if(s.length()<12||s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw Api.bad("密码需至少 12 字符且不超过 72 字节");return s;}
    @Transactional
    public User saveUser(Long id,Map<String,Object>b){
        keys(b,"username","displayName","role","enabled","password");
        // Lock the administrator set in ID order so concurrent demotions cannot remove the last admin.
        var admins=users.selectList(new QueryWrapper<User>().eq("role","ADMIN").eq("enabled",true).orderByAsc("id").last("FOR UPDATE"));
        var u=id==null?new User():required(users.lock(id));String role=choice(b,"role","ADMIN","OPERATOR","ANALYST");boolean enabled=bool(b,"enabled");
        if(id!=null&&"ADMIN".equals(u.getRole())&&u.getEnabled()&&admins.size()==1&&(!enabled||!"ADMIN".equals(role)))throw Api.conflict("不能停用或降权最后一名管理员");
        if(id==null){u.setUsername(text(b,"username",64));u.setPasswordHash(passwords.encode(password(b)));u.setAuthVersion(0);}
        else{if(b.containsKey("password"))throw Api.bad("请使用密码重置接口");if(b.containsKey("username")&&!u.getUsername().equals(b.get("username")))throw Api.bad("用户名不可修改");u.setAuthVersion(u.getAuthVersion()+1);}
        u.setDisplayName(text(b,"displayName",80));u.setRole(role);u.setEnabled(enabled);if(id==null)users.insert(u);else users.updateById(u);audit(id==null?"CREATE":"UPDATE","sys_user",u.getId());return u;
    }
    @Transactional
    public void resetPassword(Long id,Map<String,Object>b){keys(b,"password");var u=required(users.lock(id));u.setPasswordHash(passwords.encode(password(b)));u.setAuthVersion(u.getAuthVersion()+1);users.updateById(u);audit("RESET_PASSWORD","sys_user",id);}
}
