package com.johnnylin.dev.mapper;
import org.apache.ibatis.annotations.Mapper;
import java.util.*;
@Mapper
public interface StatisticsMapper {
    List<Map<String,Object>> populationAge(Map<String,Object> p);
    List<Map<String,Object>> populationGender(Map<String,Object> p);
    List<Map<String,Object>> populationRegion(Map<String,Object> p);
    Map<String,Object> healthSummary(Map<String,Object> p);
    List<Map<String,Object>> healthTrend(Map<String,Object> p);
    Map<String,Object> followupSummary(Map<String,Object> p);
    List<Map<String,Object>> followupDoctors(Map<String,Object> p);
}
