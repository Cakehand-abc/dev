package com.johnnylin.dev;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.common.*;
import com.johnnylin.dev.domain.*;
import com.johnnylin.dev.mapper.*;
import com.johnnylin.dev.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static com.johnnylin.dev.common.Input.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BusinessTests {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired CareService care;@Autowired GeoService geo;@Autowired StatisticsService stats;@Autowired DeviceAccessService deviceAccess;
    @Autowired ElderMapper elders;@Autowired UserMapper users;@Autowired BindingMapper bindings;@Autowired AlertMapper alerts;@Autowired LocationMapper locations;@Autowired DeviceMapper devices;
    @Autowired HeartbeatEventMapper heartbeatEvents;
    Elder person; Doctor doctor; Device device; AccountPrincipal admin; String t;
    @BeforeEach void fixture(){
        admin=AccountPrincipal.of(users.selectOne(new QueryWrapper<User>().eq("username","admin")));
        person=care.saveElder(null,new HashMap<>(Map.of("code","TEST-E","name","测试老人","gender","UNKNOWN","regionId",care.regions().get(0).getId())));
        doctor=care.saveDoctor(null,Map.of("code","TEST-D","name","测试医生","department","全科","enabled",true));
        device=geo.saveDevice(null,Map.of("serialNo","TEST-W","model","测试设备","enabled",true));
        var binding=geo.bind(device.getId(),person.getId());bindings.update(null,new UpdateWrapper<Binding>().eq("id",binding.getId()).set("bound_at",now().minusDays(3)));
        t=now().minusMinutes(30).atOffset(ZoneOffset.ofHours(8)).toString();
    }
    Map<String,Object> point(String event,double lon,LocalDateTime at){return Map.of("deviceId",device.getId(),"eventId",event,"longitude",lon,"latitude",30.25,"recordedAt",at.atOffset(ZoneOffset.ofHours(8)).toString());}
    Fence fence(){var f=geo.saveFence(null,Map.of("name","测试围栏","centerLon",120.16,"centerLat",30.25,"radiusM",500,"enabled",true));geo.setMembers(f.getId(),List.of(person.getId()));return f;}
    @Test void anonymousCannotReadData()throws Exception{mvc.perform(get("/api/v1/elders")).andExpect(status().isUnauthorized());}
    @Test void realLoginAndLogoutInvalidateSession()throws Exception{
        var tokenResult=mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token=json.readTree(tokenResult.getResponse().getContentAsString()).get("data");var session=(MockHttpSession)tokenResult.getRequest().getSession();
        mvc.perform(post("/api/v1/auth/login").session(session).header(token.get("headerName").asText(),token.get("token").asText()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"TestAdmin!2026\"}"))
           .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN")).andExpect(jsonPath("$.data.password").doesNotExist());
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isOk());assertThat(session.isInvalid()).isTrue();
    }
    @Test void invalidPasswordIsRejected()throws Exception{mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"incorrect\"}")).andExpect(status().isUnauthorized());}
    @Test void csrfAndReadOnlyRoleAreEnforced()throws Exception{
        mvc.perform(post("/api/v1/elders").with(user(admin)).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        var analyst=care.saveUser(null,Map.of("username","readonly","displayName","分析","role","ANALYST","enabled",true,"password","ReadOnly!2026"));
        mvc.perform(post("/api/v1/elders").with(user(AccountPrincipal.of(analyst))).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/elders").with(user(AccountPrincipal.of(analyst)))).andExpect(status().isOk());
    }
    @Test void roleChangeInvalidatesExistingAuthentication()throws Exception{
        var u=care.saveUser(null,Map.of("username","op","displayName","业务","role","OPERATOR","enabled",true,"password","Operator!2026"));var old=AccountPrincipal.of(u);
        care.saveUser(u.getId(),Map.of("displayName","业务","role","ANALYST","enabled",true));
        mvc.perform(get("/api/v1/elders").with(user(old))).andExpect(status().isUnauthorized());
    }
    @Test void lastAdministratorCannotBeDisabled(){assertThatThrownBy(()->care.saveUser(admin.id(),Map.of("displayName","管理员","role","ADMIN","enabled",false))).isInstanceOf(Api.Failure.class).hasMessageContaining("最后");}
    @Test void paginationAndMassAssignmentAreRejected()throws Exception{
        mvc.perform(get("/api/v1/elders?size=101").with(user(admin))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/elders").with(user(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"id\":99}")).andExpect(status().isBadRequest());
    }
    @Test void populationIncludesUnknownAndExcludesArchived(){
        var population=stats.population(null);assertThat(((Number)population.get("total")).longValue()).isEqualTo(1);
        assertThat(population.get("age").toString()).contains("年龄未知");care.archive(person.getId());assertThat(((Number)stats.population(null).get("total")).longValue()).isZero();
    }
    @Test void staleElderUpdateIsRejected(){assertThatThrownBy(()->care.saveElder(person.getId(),Map.of("version",4))).isInstanceOf(Api.Failure.class).hasMessageContaining("刷新");}
    @Test void healthCountsPeopleAndRecordsSeparately(){
        care.addHealth(Map.of("elderId",person.getId(),"measuredAt",t,"systolic",160));care.addHealth(Map.of("elderId",person.getId(),"measuredAt",t,"heartRate",70));
        @SuppressWarnings("unchecked") var s=(Map<String,Object>)stats.health(null,null,null).get("summary");assertThat(((Number)s.get("measurements")).longValue()).isEqualTo(2);assertThat(((Number)s.get("people")).longValue()).isEqualTo(1);assertThat(((Number)s.get("abnormalPeople")).longValue()).isEqualTo(1);
    }
    @Test void healthMissingMetricsAndFutureDataAreRejected(){
        assertThatThrownBy(()->care.addHealth(Map.of("elderId",person.getId(),"measuredAt",t))).isInstanceOf(Api.Failure.class);
        assertThatThrownBy(()->care.addHealth(Map.of("elderId",person.getId(),"measuredAt",now().plusDays(1).atOffset(ZoneOffset.ofHours(8)).toString(),"oxygen",99))).isInstanceOf(Api.Failure.class);
    }
    @Test void completionIsAtomicAndCannotBeRepeated(){
        var p=care.addPlan(Map.of("elderId",person.getId(),"doctorId",doctor.getId(),"dueAt",t));var b=Map.<String,Object>of("completedAt",t,"content","测试随访","result","已完成");care.complete(p.getId(),b);
        assertThatThrownBy(()->care.complete(p.getId(),b)).isInstanceOf(Api.Failure.class);
        @SuppressWarnings("unchecked") var s=(Map<String,Object>)stats.followups(null,null,null,null).get("summary");assertThat(s.get("completionRate")).isEqualTo(100.0);
    }
    @Test void noPlansHaveNoArtificialCompletionRate(){@SuppressWarnings("unchecked")var s=(Map<String,Object>)stats.followups(null,null,null,null).get("summary");assertThat(s.get("completionRate")).isNull();}
    @Test void duplicateBindingIsRejected(){assertThatThrownBy(()->geo.bind(device.getId(),person.getId())).isInstanceOf(Api.Failure.class);}
    @Test void persistentOutsideOnlyCreatesOneAlertAndReturnAllowsAnother(){
        fence();var base=now().minusHours(1);geo.ingest(point("a",120.17,base));geo.ingest(point("b",120.171,base.plusMinutes(1)));assertThat(alerts.selectCount(null)).isEqualTo(1);
        geo.ingest(point("c",120.16,base.plusMinutes(2)));assertThat(alerts.selectList(null).get(0).getReturnedAt()).isNotNull();geo.ingest(point("d",120.17,base.plusMinutes(3)));assertThat(alerts.selectCount(null)).isEqualTo(2);
    }
    @Test void duplicateAndOutOfOrderPointsDoNotCorruptMonitoring(){
        fence();var base=now().minusHours(1);var b=point("new",120.17,base.plusMinutes(2));geo.ingest(b);geo.ingest(b);geo.ingest(point("old",120.16,base));assertThat(locations.selectCount(null)).isEqualTo(2);assertThat(alerts.selectList(null).get(0).getReturnedAt()).isNull();
        assertThatThrownBy(()->geo.ingest(point("new",120.16,base.plusMinutes(2)))).isInstanceOf(Api.Failure.class);
    }
    @Test void trajectoryIsSortedAndRestricted(){
        var base=now().minusHours(1);geo.ingest(point("late",120.16,base.plusMinutes(2)));geo.ingest(point("early",120.16,base));
        var list=geo.trajectory(person.getId(),base.minusSeconds(1).atOffset(ZoneOffset.ofHours(8)).toString(),base.plusHours(1).atOffset(ZoneOffset.ofHours(8)).toString());assertThat(list).extracting(Location::getEventId).containsExactly("early","late");
        assertThatThrownBy(()->geo.trajectory(person.getId(),now().minusDays(2).atOffset(ZoneOffset.ofHours(8)).toString(),now().atOffset(ZoneOffset.ofHours(8)).toString())).isInstanceOf(Api.Failure.class);
    }
    @Test void rebindingPreservesHistoricalOwner(){
        var old=now().minusHours(1);geo.unbind(device.getId());var second=care.saveElder(null,Map.of("code","SECOND","name","另一老人","gender","FEMALE","regionId",person.getRegionId()));geo.bind(device.getId(),second.getId());
        assertThat(geo.ingest(point("historical",120.16,old)).getElderId()).isEqualTo(person.getId());assertThat(geo.distribution(null,null).get(0).get("location")).isNull();
    }
    @Test void heartbeatDoesNotMoveBackwards(){geo.heartbeat(Map.of("deviceId",device.getId(),"eventId","h1","recordedAt",t));geo.heartbeat(Map.of("deviceId",device.getId(),"eventId","h0","recordedAt",now().minusHours(2).atOffset(ZoneOffset.ofHours(8)).toString()));assertThat(devices.selectById(device.getId()).getLastSeenAt()).isEqualTo(time(t));}
    @Test void heartbeatEventsAreDurablyIdempotent(){var body=Map.<String,Object>of("deviceId",device.getId(),"eventId","same-heartbeat","recordedAt",t);geo.heartbeat(body);geo.heartbeat(body);assertThat(heartbeatEvents.selectCount(null)).isEqualTo(1);assertThatThrownBy(()->geo.heartbeat(Map.of("deviceId",device.getId(),"eventId","same-heartbeat","recordedAt",now().minusHours(2).atOffset(ZoneOffset.ofHours(8)).toString()))).isInstanceOf(Api.Failure.class);}
    @Test void batchLocationIngestionAcceptsMultiplePoints(){var base=now().minusHours(1);var body=Map.<String,Object>of("deviceId",device.getId(),"points",List.of(Map.of("eventId","batch-1","longitude",120.16,"latitude",30.25,"recordedAt",base.atOffset(ZoneOffset.ofHours(8)).toString()),Map.of("eventId","batch-2","longitude",120.161,"latitude",30.251,"recordedAt",base.plusMinutes(1).atOffset(ZoneOffset.ofHours(8)).toString())));var result=geo.ingestBatch(body);assertThat(result.get("accepted")).isEqualTo(2);assertThat(locations.selectCount(null)).isEqualTo(2);var repeated=geo.ingestBatch(body);assertThat(repeated.get("duplicates")).isEqualTo(2);var invalid=Map.<String,Object>of("deviceId",device.getId(),"points",List.of(Map.of("eventId","batch-3","longitude",120.162,"latitude",30.252,"recordedAt",base.plusMinutes(2).atOffset(ZoneOffset.ofHours(8)).toString()),Map.of("eventId","batch-invalid","longitude",999,"latitude",30.25,"recordedAt",base.plusMinutes(3).atOffset(ZoneOffset.ofHours(8)).toString())));assertThatThrownBy(()->geo.ingestBatch(invalid)).isInstanceOf(Api.Failure.class);assertThat(locations.selectCount(null)).isEqualTo(2);}
    @Test void restrictedDeleteOnlyRemovesUnusedRecords(){var unused=care.saveElder(null,Map.of("code","UNUSED","name","误录老人","gender","UNKNOWN","regionId",person.getRegionId()));care.deleteElder(unused.getId());assertThat(elders.selectById(unused.getId())).isNull();assertThatThrownBy(()->care.deleteElder(person.getId())).isInstanceOf(Api.Failure.class).hasMessageContaining("只能归档");}
    @Test void onlyAdministratorCanDeleteElder()throws Exception{var unused=care.saveElder(null,Map.of("code","DELETE-AUTH","name","权限测试","gender","UNKNOWN","regionId",person.getRegionId()));var operator=care.saveUser(null,Map.of("username","delete-op","displayName","业务","role","OPERATOR","enabled",true,"password","DeleteTest!2026"));mvc.perform(delete("/api/v1/elders/"+unused.getId()).with(user(AccountPrincipal.of(operator))).with(csrf())).andExpect(status().isForbidden());mvc.perform(delete("/api/v1/elders/"+unused.getId()).with(user(admin)).with(csrf())).andExpect(status().isOk());}
    @Test void authenticatedDeviceEndpointAcceptsKeyAndRejectsMissingKey()throws Exception{var credential=deviceAccess.rotate(device.getId());var base=now().minusHours(1);String body=json.writeValueAsString(Map.of("deviceId",device.getId(),"points",List.of(Map.of("eventId","real-1","longitude",120.16,"latitude",30.25,"recordedAt",base.atOffset(ZoneOffset.ofHours(8)).toString()))));mvc.perform(post("/api/device/v1/locations").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());mvc.perform(post("/api/device/v1/locations").header("X-Device-Key",credential.get("apiKey")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.data.accepted").value(1));var rotated=deviceAccess.rotate(device.getId());String next=json.writeValueAsString(Map.of("deviceId",device.getId(),"points",List.of(Map.of("eventId","real-2","longitude",120.161,"latitude",30.251,"recordedAt",base.plusMinutes(1).atOffset(ZoneOffset.ofHours(8)).toString()))));mvc.perform(post("/api/device/v1/locations").header("X-Device-Key",credential.get("apiKey")).contentType(MediaType.APPLICATION_JSON).content(next)).andExpect(status().isUnauthorized());mvc.perform(post("/api/device/v1/locations").header("X-Device-Key",rotated.get("apiKey")).contentType(MediaType.APPLICATION_JSON).content(next)).andExpect(status().isOk());}
    @Test void productionDoesNotExposeDemoIngestion()throws Exception{mvc.perform(post("/api/v1/demo/locations").with(user(admin)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isNotFound());}
    @Test void dashboardAndStatisticsContractsWork()throws Exception{
        for(String path:List.of("/dashboard","/statistics/population","/statistics/health","/statistics/followups"))mvc.perform(get("/api/v1"+path).with(user(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
    }
    @Test void distanceHasCorrectScale(){assertThat(GeoService.distance(120.16,30.25,120.16,30.25)).isZero();assertThat(GeoService.distance(0,0,0,1)).isBetween(111000.0,111300.0);}
}
