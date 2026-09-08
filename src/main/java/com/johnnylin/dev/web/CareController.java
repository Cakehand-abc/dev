package com.johnnylin.dev.web;
import com.johnnylin.dev.common.*;
import com.johnnylin.dev.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CareController {
    private final CareService care;private final GeoService geo;private final StatisticsService stats;private final DeviceAccessService deviceAccess;
    @GetMapping("/regions") Api<?> regions(){return Api.ok(care.regions());}
    @GetMapping("/elders") Api<?> elders(@RequestParam(required=false)Long regionId,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return Api.ok(care.elders(regionId,keyword,status,page,size));}
    @GetMapping("/elders/{id}") Api<?> elder(@PathVariable Long id){return Api.ok(care.elder(id));}
    @PostMapping("/elders") Api<?> elder(@RequestBody Map<String,Object>b){return Api.ok(care.saveElder(null,b));}
    @PutMapping("/elders/{id}") Api<?> elder(@PathVariable Long id,@RequestBody Map<String,Object>b){return Api.ok(care.saveElder(id,b));}
    @PostMapping("/elders/{id}/archive") Api<?> archive(@PathVariable Long id){care.archive(id);return Api.ok(null);}
    @DeleteMapping("/elders/{id}") Api<?> deleteElder(@PathVariable Long id){care.deleteElder(id);return Api.ok(null);}
    @GetMapping("/doctors") Api<?> doctors(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return Api.ok(care.doctors(page,size));}
    @PostMapping("/doctors") Api<?> doctor(@RequestBody Map<String,Object>b){return Api.ok(care.saveDoctor(null,b));}
    @PutMapping("/doctors/{id}") Api<?> doctor(@PathVariable Long id,@RequestBody Map<String,Object>b){return Api.ok(care.saveDoctor(id,b));}
    @GetMapping("/health-records") Api<?> health(@RequestParam(required=false)Long regionId,@RequestParam(required=false)Long elderId,@RequestParam(required=false)String start,@RequestParam(required=false)String end,@RequestParam(required=false)Boolean abnormal,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return Api.ok(care.health(regionId,elderId,start,end,abnormal,page,size));}
    @PostMapping("/health-records") Api<?> health(@RequestBody Map<String,Object>b){return Api.ok(care.addHealth(b));}
    @GetMapping("/elders/{id}/health-trend") Api<?> trend(@PathVariable Long id,@RequestParam String metric,@RequestParam String start,@RequestParam String end){return Api.ok(care.trend(id,metric,start,end));}
    @GetMapping("/followup-plans") Api<?> plans(@RequestParam(required=false)Long regionId,@RequestParam(required=false)Long elderId,@RequestParam(required=false)Long doctorId,@RequestParam(required=false)String start,@RequestParam(required=false)String end,@RequestParam(required=false)String status,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return Api.ok(care.plans(regionId,elderId,doctorId,start,end,status,page,size));}
    @PostMapping("/followup-plans") Api<?> plan(@RequestBody Map<String,Object>b){return Api.ok(care.addPlan(b));}
    @PostMapping("/followup-plans/{id}/complete") Api<?> complete(@PathVariable Long id,@RequestBody Map<String,Object>b){return Api.ok(care.complete(id,b));}
    @PostMapping("/followup-plans/{id}/cancel") Api<?> cancel(@PathVariable Long id,@RequestBody Map<String,Object>b){care.cancel(id,b);return Api.ok(null);}
    @GetMapping("/statistics/population") Api<?> population(@RequestParam(required=false)Long regionId){return Api.ok(stats.population(regionId));}
    @GetMapping("/statistics/health") Api<?> healthStats(@RequestParam(required=false)Long regionId,@RequestParam(required=false)String start,@RequestParam(required=false)String end){return Api.ok(stats.health(regionId,start,end));}
    @GetMapping("/statistics/followups") Api<?> followStats(@RequestParam(required=false)Long regionId,@RequestParam(required=false)Long doctorId,@RequestParam(required=false)String start,@RequestParam(required=false)String end){return Api.ok(stats.followups(regionId,doctorId,start,end));}
    @GetMapping("/dashboard") Api<?> dashboard(@RequestParam(required=false)Long regionId,@RequestParam(required=false)String start,@RequestParam(required=false)String end){return Api.ok(stats.dashboard(regionId,start,end));}
    @GetMapping("/devices") Api<?> devices(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return Api.ok(geo.devices(page,size));}
    @PostMapping("/devices") Api<?> device(@RequestBody Map<String,Object>b){return Api.ok(geo.saveDevice(null,b));}
    @PutMapping("/devices/{id}") Api<?> device(@PathVariable Long id,@RequestBody Map<String,Object>b){return Api.ok(geo.saveDevice(id,b));}
    @PostMapping("/devices/{id}/bind") Api<?> bind(@PathVariable Long id,@RequestBody Map<String,Object>b){Input.keys(b,"elderId");return Api.ok(geo.bind(id,Input.id(b,"elderId")));}
    @PostMapping("/devices/{id}/unbind") Api<?> unbind(@PathVariable Long id){geo.unbind(id);return Api.ok(null);}
    @PostMapping("/devices/{id}/credential") Api<?> credential(@PathVariable Long id){return Api.ok(deviceAccess.rotate(id));}
    @GetMapping("/devices/distribution") Api<?> distribution(@RequestParam(required=false)Long regionId,@RequestParam(required=false)String status){return Api.ok(geo.distribution(regionId,status));}
    @GetMapping("/geofences") Api<?> fences(){return Api.ok(geo.fences());}
    @PostMapping("/geofences") Api<?> fence(@RequestBody Map<String,Object>b){return Api.ok(geo.saveFence(null,b));}
    @PutMapping("/geofences/{id}") Api<?> fence(@PathVariable Long id,@RequestBody Map<String,Object>b){return Api.ok(geo.saveFence(id,b));}
    public record MemberRequest(List<Long> elderIds){}
    @PutMapping("/geofences/{id}/members") Api<?> members(@PathVariable Long id,@RequestBody MemberRequest b){geo.setMembers(id,b.elderIds());return Api.ok(null);}
    @GetMapping("/geofence-alerts") Api<?> alerts(@RequestParam(required=false)Long elderId,@RequestParam(required=false)Boolean handled,@RequestParam(required=false)String start,@RequestParam(required=false)String end,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return Api.ok(geo.alerts(elderId,handled,start,end,page,size));}
    @PostMapping("/geofence-alerts/{id}/handle") Api<?> handle(@PathVariable Long id,@RequestBody Map<String,Object>b){geo.handle(id,b);return Api.ok(null);}
    @GetMapping("/elders/{id}/trajectory") Api<?> trajectory(@PathVariable Long id,@RequestParam String start,@RequestParam String end){return Api.ok(geo.trajectory(id,start,end));}
    @GetMapping("/users") Api<?> users(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return Api.ok(care.users(page,size));}
    @PostMapping("/users") Api<?> user(@RequestBody Map<String,Object>b){return Api.ok(care.saveUser(null,b));}
    @PutMapping("/users/{id}") Api<?> user(@PathVariable Long id,@RequestBody Map<String,Object>b){return Api.ok(care.saveUser(id,b));}
    @PostMapping("/users/{id}/reset-password") Api<?> reset(@PathVariable Long id,@RequestBody Map<String,Object>b){care.resetPassword(id,b);return Api.ok(null);}
}
