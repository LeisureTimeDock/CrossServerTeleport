package com.leisuretimedock.crossplugin.manager;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
//TODO： 未来计划
public class ServerManager {
    private final ProxyServer proxy;
    private final Map<String, ServerInfo> serverMap = new HashMap<>();

    public ServerManager(ProxyServer proxy) {
        this.proxy = proxy;

        // 示例：静态初始化可跳转服务器
        registerServer("lobby", "大厅服务器");
        registerServer("survival", "生存服务器");
    }

    private void registerServer(String id, String motd) {
        Optional<RegisteredServer> server = proxy.getServer(id);
        server.ifPresent(s -> serverMap.put(id, new ServerInfo(id, motd)));
    }

    public Collection<ServerInfo> getAvailableServers() {
        return serverMap.values();
    }

    public Optional<RegisteredServer> getServerById(String id) {
        return proxy.getServer(id);
    }

    public record ServerInfo(String id, String motd) {}
}
