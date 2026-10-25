package com.xproject.device;

import com.xproject.device.Models.*;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
import java.util.List;

/** REST 查詢介面(即時推送請見 /ws) */
@RestController
@RequestMapping("/api")
public class DeviceController {
    private final DeviceGatewayHelper helper;
    private final DeviceMessageConnector connector;
    private final MqttServerDeviceGateway gateway;

    public DeviceController(DeviceGatewayHelper h, DeviceMessageConnector c, MqttServerDeviceGateway g) { helper = h; connector = c; gateway = g; }

    @GetMapping("/devices")  public Collection<Device> devices() { return helper.devices.values(); }
    @GetMapping("/sessions") public Collection<DeviceSession> sessions() { return helper.sessions.values(); }
    @GetMapping("/messages") public List<DeviceMessage> messages(@RequestParam(defaultValue = "50") int limit) { return connector.latest(limit); }
    @PostMapping("/devices/{id}/send") public String send(@PathVariable String id, @RequestBody String body) { gateway.send(id, body); return "ok"; }
}
