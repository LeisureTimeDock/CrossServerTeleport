package com.leisuretimedock.crossplugin.listener;

import com.leisuretimedock.crossplugin.Static;
import com.leisuretimedock.crossplugin.manager.ConfigManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static com.leisuretimedock.crossplugin.CrossPlugin.CROSS_TELEPORT_MOD;
//TODO:
// 1.模组C <-> 模组S
// 2.代理V <-> 模组S
// 3.模组C <-> 代理V
@Slf4j
public class PingMessageListener {
    public static final MinecraftChannelIdentifier PING_ID =
            MinecraftChannelIdentifier.create(Static.MOD_ID, "ping");
    public static final MinecraftChannelIdentifier PONG_ID =
            MinecraftChannelIdentifier.create(Static.MOD_ID, "pong");
    private final ConfigManager configManager;

    public PingMessageListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Subscribe
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(PING_ID)) return;
        Player player = (Player) event.getTarget();
        handlePingChannel(player, event.getData());
    }
    /**
     * 处理“ping”子通道
     * @param player 玩家对象
     * @param data   消息字节数组
     */ //模组C <-> 代理V
    private void handlePingChannel(Player player, byte[] data) {
        if(configManager.isEnablePingLog()) log.debug(CROSS_TELEPORT_MOD, "Received ping msg from {}: {}", player.getUsername(), data);
        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            long clientTime = in.readLong();
            long velocityReceiveTime = System.currentTimeMillis();

            // 准备响应
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeLong(clientTime); // 客户端原始时间戳
            out.writeLong(velocityReceiveTime); // Velocity接收时间
            out.writeLong(System.currentTimeMillis()); // Velocity发送时间

            byte[] response = baos.toByteArray();
            player.sendPluginMessage(
                    PONG_ID,
                    response
            );
        } catch (Exception e) {
            if(configManager.isEnableErrorLog())log.error(CROSS_TELEPORT_MOD, "failed to handle ping msg from {}: {}", player.getUsername(), e);
        }
    }


}
