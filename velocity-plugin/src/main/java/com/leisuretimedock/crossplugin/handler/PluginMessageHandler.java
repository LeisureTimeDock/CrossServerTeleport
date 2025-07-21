package com.leisuretimedock.crossplugin.handler;

import com.leisuretimedock.crossplugin.CrossPlugin;
import com.leisuretimedock.crossplugin.Static;
import com.leisuretimedock.crossplugin.manager.ConfigManager;
import com.leisuretimedock.crossplugin.messages.I18n;
import com.leisuretimedock.crossplugin.messages.I18nKeyEnum;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;

public class PluginMessageHandler {
    public static final MinecraftChannelIdentifier CHANNEL_ID =
            MinecraftChannelIdentifier.create(Static.MOD_ID, "teleport");
    private static final String PERMISSION_HEAD = Static.MOD_ID + ".goto.";

    private final ProxyServer server;
    private final Logger logger;
    private final ConfigManager config;

    public PluginMessageHandler(ProxyServer server, Logger logger, ConfigManager config) {
        this.server = server;
        this.logger = logger;
        this.config = config;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSource() instanceof Player player)) return;
        if (!event.getIdentifier().equals(CHANNEL_ID)) return;

        byte[] data = event.getData();
        String raw = new String(data, 1, data.length - 1, StandardCharsets.UTF_8);

        logger.info("Received plugin message from {}: {}", player.getUsername(), raw);

        // 处理 connect:key 模式
        if (raw.startsWith("connect:")) {
            String key = raw.substring("connect:".length());
            String targetServerName = config.resolveServerName(key);

            if (isAlreadyOnServer(player, targetServerName)) {
                player.sendMessage(I18n.translatable(I18nKeyEnum.ALREADY_ON_SERVER, NamedTextColor.RED));
                return;
            }

            server.getServer(targetServerName).ifPresentOrElse(
                    srv -> player.createConnectionRequest(srv).fireAndForget(),
                    () -> player.sendMessage(I18n.translatable(I18nKeyEnum.SERVER_NOT_FOUND, NamedTextColor.RED, Component.text(targetServerName)))
            );
            return;
        }

        // 普通 serverName 模式
        String permissionNode = PERMISSION_HEAD + raw;
        //这个权限是 "ltdcrossteleport.goto.<xx服务器名>"
        if (CrossPlugin.isLuckPermsEnabled && !player.hasPermission(permissionNode)) {
            player.sendMessage(I18n.translatable(I18nKeyEnum.NO_PERMISSION_TO_TRANS_THIS_SERVER, NamedTextColor.RED, Component.text(raw)));
            return;
        }

        if (isAlreadyOnServer(player, raw)) {
            player.sendMessage(I18n.translatable(I18nKeyEnum.ALREADY_ON_SERVER, NamedTextColor.RED));
            return;
        }

        server.getServer(raw).ifPresentOrElse(
                srv -> player.createConnectionRequest(srv).fireAndForget(),
                () -> player.sendMessage(I18n.translatable(I18nKeyEnum.SERVER_NOT_FOUND, NamedTextColor.RED, Component.text(raw)))
        );
    }

    private boolean isAlreadyOnServer(Player player, String serverName) {
        return player.getCurrentServer()
                .map(current -> current.getServerInfo().getName().equalsIgnoreCase(serverName))
                .orElse(false);
    }
}
