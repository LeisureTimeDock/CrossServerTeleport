package com.leisuretimedock.crossplugin.handler;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PluginChannelHandler {

    public static final MinecraftChannelIdentifier CHANNEL_ID =
            MinecraftChannelIdentifier.create(Static.MOD_ID, "channel");

    private final ProxyServer proxy;
    private final Logger logger;
    private final ConfigManager configManager;
    private final ServerManager serverManager;
    private final Set<Player> waitingForReady = Collections.synchronizedSet(new HashSet<>());
    public PluginChannelHandler(ProxyServer proxy, Logger logger, ConfigManager configManager) {
        this.proxy = proxy;
        this.logger = logger;
        this.configManager = configManager;
        this.serverManager = new ServerManager(proxy);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL_ID)) return;
        if (!(event.getSource() instanceof Player player)) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String command = in.readUTF();
            logger.debug("Received plugin message from {}: {}", player.getUsername(), command);

            if (command.startsWith("teleport:")) {
                String targetServer = command.substring("teleport:".length());
                proxy.getServer(targetServer).ifPresentOrElse(server -> {
                    player.createConnectionRequest(server).fireAndForget();
                    logger.debug("Teleporting {} to {}", player.getUsername(), targetServer);
                }, () -> {
                    player.sendMessage(I18n.translatable(I18nKeyEnum.SERVER_NOT_FOUND, NamedTextColor.RED, Component.text(targetServer)));
                });
            } else if ("client_ready".equals(command)) {
                // 收到客户端准备消息
                if (waitingForReady.remove(player)) {
                    logger.debug("[CrossTeleportMod] {} is ready, sending overlay and server list", player.getUsername());
                    OverlayManager.showOverlay(player);
                    //TODO：未来计划使对应客户端mod可加载来自插件的自定义服务器列表
//                    OverlayManager.sendServerList(player, serverManager.getAvailableServers());
                } else {
                    logger.debug("[CrossTeleportMod] Received client_ready from {}, but was not waiting", player.getUsername());
                }
            } else {
                logger.warn("[CrossTeleportMod] Unknown plugin command from {}: {}", player.getUsername(), command);
            }
        } catch (IOException e) {
            logger.error("[CrossTeleportMod] Error parsing plugin message", e);
        }
    }

    @Subscribe
    public void onPlayerJoin(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String currentServer = event.getServer().getServerInfo().getName();

        logger.debug("[CrossTeleportMod] Player {} joined server {}", player.getUsername(), currentServer);

        if (configManager.getOverlayServers().contains(currentServer)) {
            // 标记此玩家等待客户端准备确认
            waitingForReady.add(player);
            logger.debug("[CrossTeleportMod] Added {} to waitingForReady set", player.getUsername());
        } else {
            // 不是 lobby，隐藏 overlay
            OverlayManager.hideOverlay(player);
            logger.debug("[CrossTeleportMod] Hide overlay for player {}", player.getUsername());
        }
    }
}
