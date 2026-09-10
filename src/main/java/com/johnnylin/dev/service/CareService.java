package com.johnnylin.dev.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.auth.AccountPrincipal;
import com.johnnylin.dev.common.Api;
import com.johnnylin.dev.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.johnnylin.dev.common.Input.text;

/**
 * 养老医护综合业务服务接口。
 *
 * <p>定义长者档案、责任医生、健康监测、随访计划、管理用户与审计日志的核心业务契约。
 */
public interface CareService {

    /**
     * 校验对象非空断言，若为空则抛出 404 记录不存在异常。
     *
     * @param <T> 对象泛型
     * @param value 待检查对象
     * @return 校验通过的非空对象
     * @throws Api.Failure 当 value 为 null 时抛出 404
     */
    static <T> T required(T value) {
        if (value == null) {
            throw Api.missing();
        }
        return value;
    }

    /**
     * 从当前安全上下文中解析出当前操作员的用户主键 ID。
     *
     * @return 当前登录操作员用户 ID；若未登录或主体类型不匹配则返回 null
     */
    static Long actor() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof AccountPrincipal p ? p.id() : null;
    }

    /**
     * 基于 MyBatis-Plus 的通用分页查询辅助方法。
     *
     * @param <T> 实体泛型类型
     * @param mapper 实体持久层 Mapper
     * @param query 查询条件包裹器
     * @param page 页码（从 1 开始，最大 1,000,000）
     * @param size 每页大小（1 至 100）
     * @return 包含 records, total, page, size 的分页结果 Map
     */
    static <T> Map<String, Object> page(BaseMapper<T> mapper, QueryWrapper<T> query, int page, int size) {
        if (page < 1 || page > 1000000 || size < 1 || size > 100) {
            throw Api.bad("分页范围不正确");
        }
        long total = mapper.selectCount(query);
        query.orderByDesc("id").last("LIMIT " + size + " OFFSET " + ((long) (page - 1) * size));
        return new LinkedHashMap<>(Map.of("records", mapper.selectList(query), "total", total, "page", page, "size", size));
    }

    /**
     * 密码强度与长度校验辅助工具方法。
     *
     * @param b 包含 password 字段的 Map
     * @return 校验通过的密码明文字符串
     */
    static String password(Map<String, ?> b) {
        @SuppressWarnings("unchecked")
        Map<String, Object> cast = (Map<String, Object>) b;
        String s = text(cast, "password", 72);
        if (s.length() < 12 || s.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw Api.bad("密码需至少 12 字符且不超过 72 字节");
        }
        return s;
    }

    /**
     * 记录一条业务操作审计日志。
     *
     * @param action 操作动作标识
     * @param type 目标业务实体类型标识
     * @param id 目标业务对象主键 ID
     */
    void audit(String action, String type, Long id);

    /**
     * 查询全量片区网格列表。
     *
     * @return 片区列表
     */
    List<Region> regions();

    /**
     * 分页多条件检索长者档案列表。
     *
     * @param regionId 片区ID
     * @param keyword 模糊搜索关键字
     * @param status 状态（ACTIVE/ARCHIVED）
     * @param page 页码
     * @param size 每页大小
     * @return 分页长者档案及脱敏视图
     */
    Map<String, Object> elders(Long regionId, String keyword, String status, int page, int size);

    /**
     * 根据主键 ID 查询长者档案实体（含电话脱敏）。
     *
     * @param id 长者主键 ID
     * @return 长者实体
     */
    Elder elder(Long id);

    /**
     * 新增或编辑更新长者档案。
     *
     * @param id 长者主键 ID（新增时为 null）
     * @param b 请求参数 Map
     * @return 保存后的长者实体
     */
    Elder saveElder(Long id, Map<String, Object> b);

    /**
     * 将长者档案状态变更为归档（软迁出）。
     *
     * @param id 长者主键 ID
     */
    void archive(Long id);

    /**
     * 安全删除无任何关联历史流水数据的长者空档案。
     *
     * @param id 长者主键 ID
     */
    void deleteElder(Long id);

    /**
     * 分页查询社区责任医生列表。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页医生列表
     */
    Map<String, Object> doctors(int page, int size);

    /**
     * 新增或编辑更新签约医生资料。
     *
     * @param id 医生主键 ID（新增时为 null）
     * @param b 请求参数 Map
     * @return 保存后的医生实体
     */
    Doctor saveDoctor(Long id, Map<String, Object> b);

    /**
     * 多维度分页筛选健康体征监测记录。
     *
     * @param regionId 片区ID
     * @param elderId 长者ID
     * @param start 起始时间
     * @param end 截止时间
     * @param abnormal 是否仅异常
     * @param page 页码
     * @param size 每页大小
     * @return 分页体征记录
     */
    Map<String, Object> health(Long regionId, Long elderId, String start, String end, Boolean abnormal, int page, int size);

    /**
     * 新增上报单次生理健康体征检测数据。
     *
     * @param b 请求载荷 Map
     * @return 持久化后的体征记录实体
     */
    HealthRecord addHealth(Map<String, Object> b);

    /**
     * 查询指定长者在历史时段内的单项生理体征趋势历史记录。
     *
     * @param id 长者主键 ID
     * @param metric 指标项
     * @param start 起始时间
     * @param end 截止时间
     * @return 历史体征记录列表
     */
    List<HealthRecord> trend(Long id, String metric, String start, String end);

    /**
     * 分页筛选随访任务计划列表。
     *
     * @param regionId 片区ID
     * @param elderId 长者ID
     * @param doctorId 责任医生ID
     * @param start 起始日期
     * @param end 截止日期
     * @param status 状态
     * @param page 页码
     * @param size 每页大小
     * @return 分页随访计划列表
     */
    Map<String, Object> plans(Long regionId, Long elderId, Long doctorId, String start, String end, String status, int page, int size);

    /**
     * 新建制定一条随访服务计划。
     *
     * @param b 请求载荷 Map
     * @return 新增的随访计划实体
     */
    FollowupPlan addPlan(Map<String, Object> b);

    /**
     * 完成随访计划并录入履约明细记录。
     *
     * @param id 随访计划 ID
     * @param b 请求载荷 Map
     * @return 履约记录实体
     */
    FollowupRecord complete(Long id, Map<String, Object> b);

    /**
     * 取消随访计划并记录取消原因。
     *
     * @param id 随访计划 ID
     * @param b 请求载荷 Map
     */
    void cancel(Long id, Map<String, Object> b);

    /**
     * 分页查询系统运营管理人员列表。
     *
     * @param page 页码
     * @param size 每页大小
     * @return 分页脱敏用户列表
     */
    Map<String, Object> users(int page, int size);

    /**
     * 新增或更新运营管理人员资料。
     *
     * @param id 用户主键 ID（新增时为 null）
     * @param b 请求载荷 Map
     * @return 保存后的用户实体
     */
    User saveUser(Long id, Map<String, Object> b);

    /**
     * 强制重置指定管理用户的登录密码。
     *
     * @param id 目标用户 ID
     * @param b 包含 newPassword 的请求载荷 Map
     */
    void resetPassword(Long id, Map<String, Object> b);
}
