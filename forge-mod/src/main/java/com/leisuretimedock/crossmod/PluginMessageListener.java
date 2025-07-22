package com.leisuretimedock.crossmod;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 客户端插件消息发送工具类，负责向服务器发送自定义插件消息。
 */
@Slf4j
public class PluginMessageListener {

    /**
     * 发送客户端已准备好消息给服务器（CHANNEL_ID通道）
     */
    public static void sendClientReady() {
        if (Minecraft.getInstance().player == null) return;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeUTF("client_ready");  // 命令字符串
            dos.flush();
            byte[] payload = baos.toByteArray();
            NetworkHandler.sendPluginMessage(NetworkHandler.CHANNEL_ID, payload);
            log.debug("Sent client_ready message with payload length: {}", payload.length);
        } catch (IOException e) {
            log.error("Failed to send client ready", e);
        }
    }

    /**
     * 发送传送请求给服务器（TELEPORT_ID通道）
     * @param serverName 目标服务器名
     */
    public static void sendTeleport(String serverName) {
        if (Minecraft.getInstance().player == null) return;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            // 旧协议：写一个UTF字符串 “teleport:目标服务器名”
            // 代理端代码是识别 "teleport:" 开头的字符串的
            dos.writeUTF("teleport:" + serverName);
            dos.flush();

            NetworkHandler.sendPluginMessage(NetworkHandler.CHANNEL_ID, baos.toByteArray());
        } catch (IOException e) {
            log.error("Failed to send teleport", e);
        }
    }
}
