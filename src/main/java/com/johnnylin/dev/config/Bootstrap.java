package com.johnnylin.dev.config;

import com.johnnylin.dev.domain.*;
import com.johnnylin.dev.mapper.*;
import com.johnnylin.dev.service.CareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/**
 * 系统冷启动数据初始化引导组件。
 *
 * <p>在 Spring 容器启动完成后最先执行（{@code @Order(0)}），负责检测并初始化基础系统数据：
 * <ul>
 *   <li>默认系统管理员账号（需通过 {@code APP_ADMIN_PASSWORD} 环境变量或参数配置密码）</li>
 *   <li>演示基础网格片区数据</li>
 *   <li>健康体征监测的基础阈值规则（收缩压、舒张压、心率、血氧、体温）</li>
 * </ul>
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class Bootstrap implements ApplicationRunner {

    private final UserMapper users;
    private final RegionMapper regions;
    private final HealthRuleMapper rules;
    private final PasswordEncoder passwords;

    /** 初始管理员密码，由配置项 app.bootstrap-password 注入 */
    @Value("${app.bootstrap-password:}")
    private String password;

    /**
     * 容器启动后执行数据自检与初始化。
     *
     * <p>若各表记录为空，则依次执行系统管理员入库、默认片区预置与初始健康规则写入。
     *
     * @param args 命令行启动参数
     * @throws IllegalStateException 若未设置有效的初始管理员密码则抛出异常阻止启动
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 1. 初始化系统初始超级管理员账号
        if (users.selectCount(null) == 0) {
            if (password.isBlank()) {
                throw new IllegalStateException("首次启动必须设置 APP_ADMIN_PASSWORD，至少 12 字符且不超过 72 字节");
            }
            CareService.password(Map.of("password", password));
            var u = new User();
            u.setUsername("admin");
            u.setDisplayName("系统管理员");
            u.setRole("ADMIN");
            u.setEnabled(true);
            u.setAuthVersion(0);
            u.setPasswordHash(passwords.encode(password));
            users.insert(u);
        }

        // 2. 初始化演示网格片区数据
        if (regions.selectCount(null) == 0) {
            for (int i = 0; i < 4; i++) {
                var r = new Region();
                r.setCode("DEMO-" + (i + 1));
                r.setName(List.of("滨江片区", "文苑片区", "湖畔片区", "城北片区").get(i));
                regions.insert(r);
            }
        }

        // 3. 初始化默认健康体征监测阈值规则
        if (rules.selectCount(null) == 0) {
            String[] metrics = {"systolic", "diastolic", "heartRate", "oxygen", "temperature"};
            double[] lo = {90, 60, 60, 95, 36};
            double[] hi = {139, 89, 100, 100, 37.3};
            String[] units = {"mmHg", "mmHg", "次/分", "%", "℃"};
            for (int i = 0; i < metrics.length; i++) {
                var r = new HealthRule();
                r.setMetric(metrics[i]);
                r.setLowerBound(lo[i]);
                r.setUpperBound(hi[i]);
                r.setUnit(units[i]);
                r.setVersion(1);
                r.setEnabled(true);
                rules.insert(r);
            }
        }
    }
}
