// 客户端网络处理类（CrossMod 端）
package com.leisuretimedock.crossmod;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * NetworkHandler 用于客户端向服务端发送插件消息。
 * 目前只用 plugin message 方式进行通信。
 */
public class NetworkHandler {

    // 自定义插件消息通道标识
    public static final ResourceLocation TELEPORT_ID = new ResourceLocation(CrossTeleportMod.MOD_ID, "teleport");
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation(CrossTeleportMod.MOD_ID, "channel");

    public static void register() {
        // TODO: 未来支持双端注册，以便服务器端也能处理相关命令
        // 当前仅客户端发送 PluginMessage，无需额外注册
    }

    /**
     * 发送自定义插件消息
     * @param subChannel 子通道标识
     * @param payload   负载数据（字节数组）
     */
    public static void sendPluginMessage(ResourceLocation subChannel, byte[] payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
//        buf.writeUtf(subChannel.getPath());  // 写入子通道字符串
        buf.writeBytes(payload);              // 写入负载字节

        // 获取当前连接并发送自定义负载包
        Objects.requireNonNull(Minecraft.getInstance().getConnection())
                .send(new ServerboundCustomPayloadPacket(subChannel, buf));
    }

    /**
     * 发送客户端已准备好消息（示例方法，调用具体实现）
     */
    public static void sendClientReady() {
        PluginMessageListener.sendClientReady();
    }

    /**
     * 发送传送请求到代理服务器
     * @param serverName 目标服务器名
     */
    public static void sendTeleportRequest(String serverName) {
        PluginMessageListener.sendTeleport(serverName);
    }
}
