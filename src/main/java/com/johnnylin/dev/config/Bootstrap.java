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

@Component
@Order(0)
@RequiredArgsConstructor
public class Bootstrap implements ApplicationRunner {
    private final UserMapper users;private final RegionMapper regions;private final HealthRuleMapper rules;private final PasswordEncoder passwords;
    @Value("${app.bootstrap-password:}") private String password;
    @Override @Transactional public void run(ApplicationArguments args){
        if(users.selectCount(null)==0){
            if(password.isBlank())throw new IllegalStateException("首次启动必须设置 APP_ADMIN_PASSWORD，至少 12 字符且不超过 72 字节");
            CareService.password(Map.of("password",password));var u=new User();u.setUsername("admin");u.setDisplayName("系统管理员");u.setRole("ADMIN");u.setEnabled(true);u.setAuthVersion(0);u.setPasswordHash(passwords.encode(password));users.insert(u);
        }
        if(regions.selectCount(null)==0){for(int i=0;i<4;i++){var r=new Region();r.setCode("DEMO-"+(i+1));r.setName(List.of("滨江片区","文苑片区","湖畔片区","城北片区").get(i));regions.insert(r);}}
        if(rules.selectCount(null)==0){
            String[] metrics={"systolic","diastolic","heartRate","oxygen","temperature"};double[] lo={90,60,60,95,36};double[] hi={139,89,100,100,37.3};String[] units={"mmHg","mmHg","次/分","%","℃"};
            for(int i=0;i<metrics.length;i++){var r=new HealthRule();r.setMetric(metrics[i]);r.setLowerBound(lo[i]);r.setUpperBound(hi[i]);r.setUnit(units[i]);r.setVersion(1);r.setEnabled(true);rules.insert(r);}
        }
    }
}
