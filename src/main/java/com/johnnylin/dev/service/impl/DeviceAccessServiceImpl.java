package com.johnnylin.dev.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.johnnylin.dev.common.Api;
import com.johnnylin.dev.domain.DeviceCredential;
import com.johnnylin.dev.mapper.DeviceCredentialMapper;
import com.johnnylin.dev.mapper.DeviceMapper;
import com.johnnylin.dev.service.DeviceAccessService;
import com.johnnylin.dev.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;

import static com.johnnylin.dev.common.Input.now;
import static com.johnnylin.dev.service.impl.CareServiceImpl.required;

/**
 * 智能设备边缘接入凭据管理与无状态认证服务实现类。
 */
@Service
@RequiredArgsConstructor
public class DeviceAccessServiceImpl implements DeviceAccessService {

    private final DeviceCredentialMapper credentials;
    private final DeviceMapper devices;
    private final GeoService geo;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public Map<String, Object> rotate(Long deviceId) {
        var device = required(devices.selectById(deviceId));
        if (!device.getEnabled()) {
            throw Api.conflict("设备已停用，不能生成接入密钥");
        }

        // 生成 32 字节密码学安全随机密钥
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = "scd_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        var current = credentials.selectById(deviceId);
        LocalDateTime stamp = now();
        boolean creating = current == null;
        if (creating) {
            current = new DeviceCredential();
            current.setDeviceId(deviceId);
            current.setCreatedAt(stamp);
        } else {
            current.setRotatedAt(stamp);
        }

        // 保存 SHA-256 单向哈希
        current.setKeyHash(hash(secret));
        if (creating) {
            credentials.insert(current);
        } else {
            credentials.updateById(current);
        }

        geo.auditDeviceAction("ROTATE_CREDENTIAL", deviceId);
        return new LinkedHashMap<>(Map.of(
                "deviceId", deviceId,
                "serialNo", device.getSerialNo(),
                "apiKey", secret,
                "issuedAt", stamp
        ));
    }

    @Override
    public void authenticate(Long deviceId, String secret) {
        if (secret == null || secret.length() < 20 || secret.length() > 200) {
            throw unauthorized();
        }
        var credential = credentials.selectOne(new QueryWrapper<DeviceCredential>().eq("device_id", deviceId));
        if (credential == null || !MessageDigest.isEqual(
                credential.getKeyHash().getBytes(StandardCharsets.US_ASCII),
                hash(secret).getBytes(StandardCharsets.US_ASCII))) {
            throw unauthorized();
        }
        var device = devices.selectById(deviceId);
        if (device == null || !device.getEnabled()) {
            throw unauthorized();
        }
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Api.Failure unauthorized() {
        return new Api.Failure(401, "设备接入凭据无效");
    }
}
