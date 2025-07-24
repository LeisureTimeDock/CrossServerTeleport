package com.leisuretimedock.crossplugin.manager;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static com.leisuretimedock.crossplugin.CrossPlugin.CROSS_TELEPORT_MOD;

@Slf4j
public class PingManager {
    private final ProxyServer server;
    private final Map<String, Long> serverPingResults = new HashMap<>();
    private final ConfigManager configManager;

    public PingManager(ProxyServer server, ConfigManager configManager) {
        this.server = server;
        this.configManager = configManager;
    }

    /**
     * 测量所有后端服务器的Ping值
     */
    public void measureAllBackendPing() {
        server.getAllServers().forEach(serverInfo  -> _measureBackendPing(serverInfo, System.currentTimeMillis()));
    }

    /**
     * 测量指定后端服务器的Ping值
     * @param backendName 后端服务器名称
     */
    public void measureBackendPing(String backendName) {
        server.getAllServers().forEach(serverInfo  -> {
            if (serverInfo.getServerInfo().getName().equals(backendName)) {
                _measureBackendPing(serverInfo, System.currentTimeMillis());
            }
        });
    }

    /**
     * 测量单个后端服务器的Ping值
     * @param serverInfo 后端服务器信息
     * @param startTime  开始时间
     */
    private void _measureBackendPing(RegisteredServer serverInfo, long startTime) {
        serverInfo.ping().whenComplete((serverPing, throwable) -> {
            long endTime = System.currentTimeMillis();
            long ping = endTime - startTime;
            String name = serverInfo.getServerInfo().getName();
            if (throwable == null) {
                serverPingResults.put(name, ping);
                if(configManager.isEnablePingLog()) log.debug(CROSS_TELEPORT_MOD, "Ping to server {}: {}ms", name, ping);
            } else {
                if(configManager.isEnablePingLog()) log.warn(CROSS_TELEPORT_MOD, "Failed to ping server {}", name, throwable);
            }
        });
    }
}