package com.johnnylin.dev.web;
import com.johnnylin.dev.common.*;
import com.johnnylin.dev.service.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@Profile("demo")
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoController {
    private final GeoService geo;
    @PostMapping("/locations") Api<?> location(@RequestBody Map<String,Object>b){return Api.ok(geo.ingest(b));}
    @PostMapping("/locations/batch") Api<?> locations(@RequestBody Map<String,Object>b){return Api.ok(geo.ingestBatch(b));}
    @PostMapping("/heartbeats") Api<?> heartbeat(@RequestBody Map<String,Object>b){return Api.ok(geo.heartbeat(b));}
}
