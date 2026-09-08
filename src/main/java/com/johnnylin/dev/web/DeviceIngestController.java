package com.johnnylin.dev.web;

import com.johnnylin.dev.common.*;
import com.johnnylin.dev.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/device/v1")
@RequiredArgsConstructor
public class DeviceIngestController {
    private final DeviceAccessService access;
    private final GeoService geo;

    @PostMapping("/locations")
    Api<?> locations(@RequestHeader(value="X-Device-Key",required=false)String key,@RequestBody Map<String,Object> body){
        Long deviceId=Input.id(body,"deviceId");access.authenticate(deviceId,key);return Api.ok(geo.ingestBatch(body));
    }
    @PostMapping("/heartbeats")
    Api<?> heartbeat(@RequestHeader(value="X-Device-Key",required=false)String key,@RequestBody Map<String,Object> body){
        Long deviceId=Input.id(body,"deviceId");access.authenticate(deviceId,key);return Api.ok(geo.heartbeat(body));
    }
}
