package com.johnnylin.dev.common;

import java.time.*;
import java.util.*;

public final class Input {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private Input() {}
    public static LocalDateTime now() { return LocalDateTime.now(ZONE).truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
    public static String text(Map<String,Object> b,String key,int max) {
        Object raw=b.get(key); if (!(raw instanceof String)) throw Api.bad(key+" 必须为文本");
        String v=((String)raw).trim(); if(v.isEmpty()||v.length()>max) throw Api.bad(key+" 长度不正确"); return v;
    }
    public static String optional(Map<String,Object>b,String key,int max) {
        return b.get(key)==null || "".equals(b.get(key)) ? null : text(b,key,max);
    }
    public static Long id(Map<String,Object>b,String key) {
        try { long n=Long.parseLong(String.valueOf(b.get(key))); if(n<1) throw new NumberFormatException();return n; }
        catch(Exception e) {throw Api.bad(key+" 必须为正整数");}
    }
    public static int version(Map<String,Object>b) {
        try { int n=Integer.parseInt(String.valueOf(b.get("version")));if(n<0)throw new NumberFormatException();return n; }
        catch(Exception e) {throw Api.bad("version 必须为非负整数");}
    }
    public static double number(Map<String,Object>b,String key,double min,double max) {
        try {double n=Double.parseDouble(String.valueOf(b.get(key))); if(!Double.isFinite(n)||n<min||n>max)throw new NumberFormatException();return n;}
        catch(Exception e){throw Api.bad(key+" 超出允许范围");}
    }
    public static boolean bool(Map<String,Object>b,String key) {
        if(!(b.get(key) instanceof Boolean))throw Api.bad(key+" 必须为布尔值");return (Boolean)b.get(key);
    }
    public static String choice(Map<String,Object>b,String key,String... values) {
        String v=text(b,key,30);if(!Arrays.asList(values).contains(v))throw Api.bad(key+" 无效");return v;
    }
    public static LocalDateTime time(String raw) {
        try{return OffsetDateTime.parse(raw).atZoneSameInstant(ZONE).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MICROS);}
        catch(Exception e){throw Api.bad("时间必须包含时区，例如 2026-09-07T12:00:00+08:00");}
    }
    public static LocalDateTime time(Map<String,Object>b,String key) {return time(text(b,key,50));}
    public static LocalDate date(Map<String,Object>b,String key) {
        String s=optional(b,key,20);if(s==null)return null;
        try {LocalDate d=LocalDate.parse(s);if(d.isAfter(LocalDate.now(ZONE))||d.isBefore(LocalDate.of(1900,1,1)))throw new IllegalArgumentException();return d;}
        catch(Exception e){throw Api.bad("出生日期应在 1900 年至今天之间");}
    }
    public static Map<String,Object> filters(Long regionId,String start,String end,Long elderId,Long doctorId) {
        if((start==null)!=(end==null))throw Api.bad("start 和 end 需同时提供");
        var from=start==null?LocalDate.now(ZONE).minusDays(29).atStartOfDay():time(start);
        var to=end==null?LocalDate.now(ZONE).plusDays(1).atStartOfDay():time(end);
        if(!from.isBefore(to))throw Api.bad("开始时间必须早于结束时间");
        Map<String,Object> p=new HashMap<>();p.put("regionId",regionId);p.put("start",from);p.put("end",to);
        p.put("cutoff",to.isAfter(now())?now():to);p.put("now",now());p.put("elderId",elderId);p.put("doctorId",doctorId);return p;
    }
    public static void keys(Map<String,Object>b,String... keys) {
        var allowed=Set.of(keys);for(String k:b.keySet())if(!allowed.contains(k))throw Api.bad("不支持字段 "+k);
    }
}
