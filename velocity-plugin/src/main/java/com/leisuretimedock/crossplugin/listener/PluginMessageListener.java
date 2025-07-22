// 代理端插件消息监听器（Velocity Proxy 端）
package com.leisuretimedock.crossplugin.listener;

import com.leisuretimedock.crossplugin.Static;
import com.leisuretimedock.crossplugin.manager.ConfigManager;
import com.leisuretimedock.crossplugin.manager.OverlayManager;
import com.leisuretimedock.crossplugin.manager.ServerManager;
import com.leisuretimedock.crossplugin.messages.I18n;
import com.leisuretimedock.crossplugin.messages.I18nKeyEnum;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 插件消息监听器，负责接收客户端发来的插件消息并处理跨服传送、Overlay显示等逻辑。
 */
public class PluginMessageListener {

    // 插件消息通道标识（与客户端保持一致）
    public static final MinecraftChannelIdentifier TELEPORT_ID =
            MinecraftChannelIdentifier.create(Static.MOD_ID, "teleport");
    public static final MinecraftChannelIdentifier CHANNEL_ID =
            MinecraftChannelIdentifier.create(Static.MOD_ID, "channel");

    private static final String PERMISSION_HEAD = Static.MOD_ID + ".goto.";

    private final ProxyServer proxy;
    private final Logger logger;
    private final ConfigManager configManager;
    @SuppressWarnings("ALL")
    private final ServerManager serverManager;

    /**
     * 维护等待客户端发送 "client_ready" 的玩家集合。
     */
    private final Set<Player> waitingForReady = Collections.synchronizedSet(new HashSet<>());

    public PluginMessageListener(ProxyServer proxy, Logger logger, ConfigManager configManager) {
        this.proxy = proxy;
        this.logger = logger;
        this.configManager = configManager;
        this.serverManager = new ServerManager(proxy);
    }

    /**
     * 监听插件消息事件
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof Player player)) return;

        MinecraftChannelIdentifier id = (MinecraftChannelIdentifier) event.getIdentifier();

        if (id.equals(TELEPORT_ID)) {
            handleTeleportChannel(player, event.getData());
        } else if (id.equals(CHANNEL_ID)) {
            handlePluginChannel(player, event.getData());
        }
    }

    /**
     * 处理“teleport”子通道，旧协议兼容纯字符串形式
     * @param player 玩家对象
     * @param data   消息字节数组
     */
    private void handleTeleportChannel(Player player, byte[] data) {
        // 跳过第一个 byte（长度信息），后面是 UTF-8 字符串
        String raw = new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
        logger.debug("[CrossTeleportMod] Received teleport msg from {}: {}", player.getUsername(), raw);

        if (raw.startsWith("connect:")) {
            // 兼容旧的 connect: 方式，映射别名到真实服务器名
            String key = raw.substring("connect:".length());
            String targetServerName = configManager.resolveServerName(key);
            tryTeleport(player, targetServerName, false);
        } else {
            tryTeleport(player, raw, true);
        }
    }

    /**
     * 处理“channel”子通道，支持多命令格式
     * @param player 玩家对象
     * @param data   消息字节数组
     */
    private void handlePluginChannel(Player player, byte[] data) {
        // 简单日志，打印字节长度和十六进制，便于调试
        System.out.println("Received plugin message on channel 'channel' from player " + player.getUsername());
        System.out.println("Data length: " + data.length);
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        System.out.println("Data hex: " + sb);
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String command = in.readUTF();
            logger.debug("[CrossTeleportMod] Received plugin command from {}: {}", player.getUsername(), command);

            if ("client_ready".equals(command)) {
                if (waitingForReady.remove(player)) {
                    logger.debug("[CrossTeleportMod] {} is ready, sending overlay", player.getUsername());
                    OverlayManager.showOverlay(player);
                    // TODO: 支持发送自定义服务器列表
                } else {
                    logger.debug("[CrossTeleportMod] Received client_ready from {}, but not in waiting set", player.getUsername());
                }
            } else if (command.startsWith("teleport:")) {
                String server = command.substring("teleport:".length());
                tryTeleport(player, server, true);
            } else {
                logger.warn("[CrossTeleportMod] Unknown command: {}", command);
            }

        } catch (IOException e) {
            logger.error("[CrossTeleportMod] Failed to parse plugin message from {}", player.getUsername(), e);
        }
    }

    /**
     * 尝试传送玩家到目标服务器，包含权限与当前所在服务器判断
     * @param player           玩家对象
     * @param targetServer     目标服务器名
     * @param checkPermission  是否检查权限
     */
    private void tryTeleport(Player player, String targetServer, boolean checkPermission) {
        if (checkPermission && !player.hasPermission(PERMISSION_HEAD + targetServer)) {
            player.sendMessage(I18n.translatable(I18nKeyEnum.NO_PERMISSION_TO_TRANS_THIS_SERVER,
                    NamedTextColor.RED, Component.text(targetServer)));
            return;
        }

        if (isAlreadyOnServer(player, targetServer)) {
            player.sendMessage(I18n.translatable(I18nKeyEnum.ALREADY_ON_SERVER, NamedTextColor.RED));
            return;
        }

        proxy.getServer(targetServer).ifPresentOrElse(server -> {
            player.createConnectionRequest(server).fireAndForget();
            logger.info("[CrossTeleportMod] Sent {} to {}", player.getUsername(), targetServer);
        }, () -> {
            player.sendMessage(I18n.translatable(I18nKeyEnum.SERVER_NOT_FOUND,
                    NamedTextColor.RED, Component.text(targetServer)));
        });
    }

    /**
     * 监听玩家服务器连接事件，维护是否显示 Overlay 状态
     */
    @Subscribe
    public void onPlayerJoin(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String currentServer = event.getServer().getServerInfo().getName();

        logger.debug("[CrossTeleportMod] Player {} joined server {}", player.getUsername(), currentServer);

        if (configManager.getOverlayServers().contains(currentServer)) {
            waitingForReady.add(player);
            logger.debug("[CrossTeleportMod] Added {} to waitingForReady set", player.getUsername());
        } else {
            OverlayManager.hideOverlay(player);
            logger.debug("[CrossTeleportMod] Hiding overlay for {}", player.getUsername());
        }
    }

    /**
     * 判断玩家是否已经在目标服务器
     * @param player 玩家对象
     * @param serverName 目标服务器名
     * @return 是否已在该服务器
     */
    private boolean isAlreadyOnServer(Player player, String serverName) {
        return player.getCurrentServer()
                .map(s -> s.getServerInfo().getName().equalsIgnoreCase(serverName))
                .orElse(false);
    }
}
