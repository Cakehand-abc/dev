package com.johnnylin.dev.service;
import com.johnnylin.dev.mapper.StatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.johnnylin.dev.common.Input.*;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final StatisticsMapper queries;private final GeoService geo;
    public Map<String,Object> population(Long region){
        var p=new HashMap<String,Object>();p.put("regionId",region);var age=queries.populationAge(p);
        long total=age.stream().mapToLong(r->((Number)r.get("value")).longValue()).sum();
        return Map.of("total",total,"age",age,"gender",queries.populationGender(p),"region",queries.populationRegion(p),"asOf",java.time.LocalDate.now(ZONE));
    }
    public Map<String,Object> health(Long region,String start,String end){var p=filters(region,start,end,null,null);return Map.of("summary",queries.healthSummary(p),"trend",queries.healthTrend(p));}
    private Map<String,Object> rate(Map<String,Object> m){long count=((Number)m.get("planned")).longValue();m.put("completionRate",count==0?null:Math.round(((Number)m.get("completed")).doubleValue()/count*1000)/10.0);return m;}
    public Map<String,Object> followups(Long region,Long doctor,String start,String end){var p=filters(region,start,end,null,doctor);return Map.of("summary",rate(queries.followupSummary(p)),"doctors",queries.followupDoctors(p).stream().map(this::rate).toList());}
    public Map<String,Object> dashboard(Long region,String start,String end){
        var devices=geo.distribution(region,null);var counts=new HashMap<String,Long>();for(var row:devices)counts.merge((String)row.get("status"),1L,Long::sum);
        return Map.of("population",population(region),"health",health(region,start,end),"followups",followups(region,null,start,end),"devices",counts,"generatedAt",now());
    }
}
