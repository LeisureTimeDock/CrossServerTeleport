package com.leisuretimedock.crossmod;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import static com.leisuretimedock.crossmod.client.PluginChannelClient.CHANNEL_ID;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel CHANNEL;

    public static void register() {
        //TODO: 以后会做出双端版本，以让游戏服务器端可以允运行代理命令简化些流程
        // 不需要注册普通 packet，因为我们只用 plugin message
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(CrossTeleportMod.MOD_ID, "teleport"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
    }

    public static void sendTeleportMessage(String serverName) {
        // 构建 raw plugin message
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(serverName);

        Objects.requireNonNull(Minecraft.getInstance().getConnection()).send(
            new ServerboundCustomPayloadPacket(
                CrossTeleportMod.CHANNEL, buf
            )
        );
    }
    public static void sendClientReady() {
        if (Minecraft.getInstance().player == null) return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeUTF("client_ready");
            dos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] bytes = baos.toByteArray();
        Objects.requireNonNull(Minecraft.getInstance().getConnection())
                .send(new ServerboundCustomPayloadPacket(CHANNEL_ID, new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes))));


    }

}

