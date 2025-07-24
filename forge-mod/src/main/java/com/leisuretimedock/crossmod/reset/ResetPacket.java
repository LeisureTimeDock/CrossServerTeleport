package com.leisuretimedock.crossmod.reset;

import com.leisuretimedock.crossmod.mixin.AccessorMinecraft;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.network.*;

import java.util.function.Supplier;

@Slf4j
@Setter
@Getter
public class ResetPacket extends HandshakeMessages.C2SAcknowledge {
    private int loginIndex;
    public ResetPacket() {
        super();
    }
    public static ResetPacket decode(FriendlyByteBuf buf) {
        return new ResetPacket();
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static void handler(HandshakeHandler handler , ResetPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ClientResetManager.isNegotiating.set(true);
        Connection conn = ctx.getNetworkManager();
        if (ctx.getDirection() != NetworkDirection.LOGIN_TO_CLIENT && ctx.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
           conn.disconnect(new TranslatableComponent("ltd.mod.client.invalid_packet"));
           return;
        }
        if (ResetHelper.clearClient(ctx)) {
           NetworkHooks.registerClientLoginChannel(conn);
           conn.setProtocol(ConnectionProtocol.LOGIN);
           conn.setListener(new ClientHandshakePacketListenerImpl(
                   conn, Minecraft.getInstance(), null, s -> {}
           ));

           ((AccessorMinecraft) Minecraft.getInstance()).setPendingConnection(conn);

           try {
               ClientResetManager.handshakeChannel.reply(
                       new HandshakeMessages.C2SAcknowledge(),
                       ClientResetManager.contextConstructor.newInstance(conn, NetworkDirection.LOGIN_TO_CLIENT, 98)
               );
           } catch (Exception e) {
               log.error("Failed to send acknowledgment", e);
               conn.disconnect(new TranslatableComponent("ltd.mod.client.error.handshake"));
           }
        }
       ctx.setPacketHandled(true);

    }

}

