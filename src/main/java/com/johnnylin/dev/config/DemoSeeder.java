package com.johnnylin.dev.config;
import com.johnnylin.dev.domain.*;
import com.johnnylin.dev.mapper.*;
import com.johnnylin.dev.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static com.johnnylin.dev.common.Input.*;

@Component
@Profile("demo")
@ConditionalOnProperty(name="app.seed-demo",havingValue="true")
@Order(1)
@RequiredArgsConstructor
public class DemoSeeder implements ApplicationRunner {
    private final ElderMapper elders;private final DoctorMapper doctors;private final RegionMapper regions;
    private final DeviceMapper devices;private final BindingMapper bindings;private final CareService care;private final GeoService geo;
    @Value("${app.bootstrap-password:}") private String password;
    @Override @Transactional public void run(ApplicationArguments args){
        if(elders.selectCount(null)>0)return;
        CareService.password(Map.of("password",password));
        for(var role:List.of("OPERATOR","ANALYST"))care.saveUser(null,Map.of("username",role.toLowerCase(),"displayName",role.equals("OPERATOR")?"业务演示员":"数据分析员","role",role,"enabled",true,"password",password));
        var regionList=regions.selectList(null);List<Elder> people=new ArrayList<>();List<Doctor> medics=new ArrayList<>();var random=new Random(20260907);
        for(int i=0;i<6;i++){var d=new Doctor();d.setCode("DOC-"+(i+1));d.setName("演示医生"+(i+1));d.setDepartment(i%2==0?"全科医学":"老年健康");d.setEnabled(true);d.setVersion(0);doctors.insert(d);medics.add(d);}
        for(int i=0;i<120;i++){
            var e=new Elder();e.setCode(String.format("EL-%04d",i+1));e.setName(String.format("演示老人%03d",i+1));e.setGender(i%11==0?"UNKNOWN":i%2==0?"MALE":"FEMALE");
            e.setBirthDate(i%17==0?null:LocalDate.now(ZONE).minusYears(58+random.nextInt(40)).minusDays(random.nextInt(300)));e.setRegionId(regionList.get(i%regionList.size()).getId());e.setStatus("ACTIVE");e.setVersion(0);elders.insert(e);people.add(e);
            for(int j=0;j<9;j++){var body=new HashMap<String,Object>();body.put("elderId",e.getId());body.put("measuredAt",now().minusDays(j*3).minusMinutes(i+10).atOffset(ZoneOffset.ofHours(8)).toString());body.put("systolic",i%7==0?152:110+random.nextInt(25));body.put("diastolic",65+random.nextInt(20));body.put("heartRate",62+random.nextInt(30));body.put("oxygen",i%13==0?93:97);body.put("temperature",36.0+random.nextInt(10)/10.0);body.put("eventId","demo-health-"+i+"-"+j);care.addHealth(body);}
            var plan=care.addPlan(Map.of("elderId",e.getId(),"doctorId",medics.get(i%6).getId(),"dueAt",now().minusDays(i%20).minusHours(2).atOffset(ZoneOffset.ofHours(8)).toString()));
            if(i%4!=0)care.complete(plan.getId(),Map.of("completedAt",now().minusDays(i%20).minusHours(i%3==0?1:3).atOffset(ZoneOffset.ofHours(8)).toString(),"content","教学演示：记录日常健康情况和服务需求。","result","已完成常规随访"));
        }
        var fence=geo.saveFence(null,Map.of("name","滨江服务中心活动范围","centerLon",120.16,"centerLat",30.25,"radiusM",650,"enabled",true));
        geo.setMembers(fence.getId(),people.subList(0,18).stream().map(Elder::getId).toList());
        for(int i=0;i<18;i++){
            var d=geo.saveDevice(null,Map.of("serialNo",String.format("WATCH-%04d",i+1),"model","教学模拟腕表","enabled",true));
            var binding=new Binding();binding.setDeviceId(d.getId());binding.setElderId(people.get(i).getId());binding.setBoundAt(now().minusDays(2));binding.setActiveDeviceId(d.getId());binding.setActiveElderId(people.get(i).getId());bindings.insert(binding);
            for(int j=0;j<36;j++){double r=(i%3==0&&j>14&&j<27)?0.009:0.0025;double angle=(j*10+i*7)*Math.PI/180;
                geo.ingest(Map.of("deviceId",d.getId(),"eventId","demo-position-"+i+"-"+j,"longitude",120.16+r*Math.cos(angle),"latitude",30.25+r*Math.sin(angle),"recordedAt",now().minusMinutes((36-j)*5L).atOffset(ZoneOffset.ofHours(8)).toString()));}
            geo.heartbeat(Map.of("deviceId",d.getId(),"eventId","demo-heartbeat-"+i,"recordedAt",now().minusMinutes(i%5==0?30:1).atOffset(ZoneOffset.ofHours(8)).toString()));
        }
    }
}
