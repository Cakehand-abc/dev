package com.johnnylin.dev.common;

import java.time.*;
import java.util.*;

/**
 * HTTP 请求入参校验与类型安全转换工具类。
 *
 * <p>提供面向请求 Body/Params 的安全提取与防御性校验方法，统一使用 Asia/Shanghai（UTC+8）
 * 时区进行时间归一化截断，并防御非受信任字段的 Mass-Assignment 注入攻击。
 */
public final class Input {

    /** 系统统一业务时区：Asia/Shanghai (UTC+8) */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private Input() {}

    /**
     * 获取当前系统时间，截断至微秒精度并对齐统一业务时区。
     *
     * @return 当前时区的 LocalDateTime 对象（精度截断至微秒）
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    /**
     * 校验并提取必填文本字段。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @param max 允许的最大字符长度
     * @return 去除首尾空白后的非空字符串
     * @throws Api.Failure 当字段缺失、非字符串、空白或超出长度限制时抛出 400
     */
    public static String text(Map<String, Object> b, String key, int max) {
        Object raw = b.get(key);
        if (!(raw instanceof String)) {
            throw Api.bad(key + " 必须为文本");
        }
        String v = ((String) raw).trim();
        if (v.isEmpty() || v.length() > max) {
            throw Api.bad(key + " 长度不正确");
        }
        return v;
    }

    /**
     * 校验并提取选填文本字段。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @param max 允许的最大字符长度
     * @return 去除首尾空白的字符串；若字段为 null 或空串则返回 null
     * @throws Api.Failure 当超出长度限制或类型非文本时抛出 400
     */
    public static String optional(Map<String, Object> b, String key, int max) {
        return b.get(key) == null || "".equals(b.get(key)) ? null : text(b, key, max);
    }

    /**
     * 校验并提取必填正整数主键 ID。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @return 正整数 Long 类型 ID（>= 1）
     * @throws Api.Failure 当非正整数或数值格式不合法时抛出 400
     */
    public static Long id(Map<String, Object> b, String key) {
        try {
            long n = Long.parseLong(String.valueOf(b.get(key)));
            if (n < 1) {
                throw new NumberFormatException();
            }
            return n;
        } catch (Exception e) {
            throw Api.bad(key + " 必须为正整数");
        }
    }

    /**
     * 校验并提取乐观锁版本号。
     *
     * @param b 请求数据 Map 载荷
     * @return 非负整数版本号（>= 0）
     * @throws Api.Failure 当非合法非负整数时抛出 400
     */
    public static int version(Map<String, Object> b) {
        try {
            int n = Integer.parseInt(String.valueOf(b.get("version")));
            if (n < 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (Exception e) {
            throw Api.bad("version 必须为非负整数");
        }
    }

    /**
     * 校验并提取指定闭区间内的浮点数值（如经度、纬度、健康监测数值）。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @param min 允许的最小值（包含）
     * @param max 允许的最大值（包含）
     * @return 校验通过的 double 数值
     * @throws Api.Failure 当值非有限数或超出 [min, max] 范围时抛出 400
     */
    public static double number(Map<String, Object> b, String key, double min, double max) {
        try {
            double n = Double.parseDouble(String.valueOf(b.get(key)));
            if (!Double.isFinite(n) || n < min || n > max) {
                throw new NumberFormatException();
            }
            return n;
        } catch (Exception e) {
            throw Api.bad(key + " 超出允许范围");
        }
    }

    /**
     * 校验并提取布尔值字段。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @return 布尔值
     * @throws Api.Failure 当字段缺失或类型非 Boolean 时抛出 400
     */
    public static boolean bool(Map<String, Object> b, String key) {
        if (!(b.get(key) instanceof Boolean)) {
            throw Api.bad(key + " 必须为布尔值");
        }
        return (Boolean) b.get(key);
    }

    /**
     * 校验字符串值是否属于给定的候选集合（枚举校验）。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @param values 允许的合法取值枚举列表
     * @return 匹配的字符串取值
     * @throws Api.Failure 当取值不在合法候选集中时抛出 400
     */
    public static String choice(Map<String, Object> b, String key, String... values) {
        String v = text(b, key, 30);
        if (!Arrays.asList(values).contains(v)) {
            throw Api.bad(key + " 无效");
        }
        return v;
    }

    /**
     * 解析带有时区偏移的 ISO-8601 格式时间字符串，并对齐至统一业务时区。
     *
     * @param raw ISO-8601 原始时间文本（例如 "2026-09-07T12:00:00+08:00"）
     * @return 统一时区下的本地时间 LocalDateTime
     * @throws Api.Failure 当时间解析失败或未携带时区信息时抛出 400
     */
    public static LocalDateTime time(String raw) {
        try {
            return OffsetDateTime.parse(raw).atZoneSameInstant(ZONE).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        } catch (Exception e) {
            throw Api.bad("时间必须包含时区，例如 2026-09-07T12:00:00+08:00");
        }
    }

    /**
     * 从请求 Map 载荷中提取并解析带时区的时间字段。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @return 统一时区下的本地时间 LocalDateTime
     */
    public static LocalDateTime time(Map<String, Object> b, String key) {
        return time(text(b, key, 50));
    }

    /**
     * 校验并提取出生日期，限制在 1900 年至今天之间。
     *
     * @param b 请求数据 Map 载荷
     * @param key 字段键名
     * @return 解析后的 LocalDate，若字段为空则返回 null
     * @throws Api.Failure 当日期早于 1900 年或晚于今天时抛出 400
     */
    public static LocalDate date(Map<String, Object> b, String key) {
        String s = optional(b, key, 20);
        if (s == null) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(s);
            if (d.isAfter(LocalDate.now(ZONE)) || d.isBefore(LocalDate.of(1900, 1, 1))) {
                throw new IllegalArgumentException();
            }
            return d;
        } catch (Exception e) {
            throw Api.bad("出生日期应在 1900 年至今天之间");
        }
    }

    /**
     * 构建统计与列表查询的标准时间范围及通用过滤条件。
     *
     * <p>若未指定 start/end，默认拉取最近 30 天至明天凌晨的时间窗口。同时计算 {@code cutoff}
     * 作为未来随访履约统计的统计截止线。
     *
     * @param regionId 过滤片区 ID（可为 null）
     * @param start 起始时间文本（带时区，可为 null）
     * @param end 截止时间文本（带时区，可为 null）
     * @param elderId 过滤长者 ID（可为 null）
     * @param doctorId 过滤医生 ID（可为 null）
     * @return 包含 start、end、cutoff、now、regionId 等查询参数的 Map
     * @throws Api.Failure 当仅提供 start 或 end 其一，或起始时间不早于结束时间时抛出 400
     */
    public static Map<String, Object> filters(Long regionId, String start, String end, Long elderId, Long doctorId) {
        if ((start == null) != (end == null)) {
            throw Api.bad("start 和 end 需同时提供");
        }
        var from = start == null ? LocalDate.now(ZONE).minusDays(29).atStartOfDay() : time(start);
        var to = end == null ? LocalDate.now(ZONE).plusDays(1).atStartOfDay() : time(end);
        if (!from.isBefore(to)) {
            throw Api.bad("开始时间必须早于结束时间");
        }
        Map<String, Object> p = new HashMap<>();
        p.put("regionId", regionId);
        p.put("start", from);
        p.put("end", to);
        p.put("cutoff", to.isAfter(now()) ? now() : to);
        p.put("now", now());
        p.put("elderId", elderId);
        p.put("doctorId", doctorId);
        return p;
    }

    /**
     * 请求 Body 键名白名单防御校验（防御 Mass-Assignment 批量赋值攻击）。
     *
     * @param b 请求入参 Map
     * @param keys 允许传递的合法键名集合
     * @throws Api.Failure 当存在未在白名单中的多余字段时抛出 400
     */
    public static void keys(Map<String, Object> b, String... keys) {
        var allowed = Set.of(keys);
        for (String k : b.keySet()) {
            if (!allowed.contains(k)) {
                throw Api.bad("不支持字段 " + k);
            }
        }
    }
}
