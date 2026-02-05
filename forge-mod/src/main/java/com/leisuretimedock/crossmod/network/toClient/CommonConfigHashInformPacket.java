package com.leisuretimedock.crossmod.network.toClient;

import com.leisuretimedock.crossmod.config.CrossServerConfigManager;
import com.leisuretimedock.crossmod.network.NetworkHandler;
import com.leisuretimedock.crossmod.network.toServer.SyncCommonConfigRequestPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The type Common config hash inform packet.
 */
public record CommonConfigHashInformPacket(int hash) {
    /**
     * Encode.
     *
     * @param packet the packet
     * @param buffer the buffer
     */
    public static void encode(CommonConfigHashInformPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.hash());
    }


    /**
     * Decode common config hash inform packet.
     *
     * @param buffer the buffer
     * @return the common config hash inform packet
     */
    public static CommonConfigHashInformPacket decode(FriendlyByteBuf buffer) {
        return new CommonConfigHashInformPacket(buffer.readInt());
    }

    /**
     * Handle.
     *
     * @param packet the packet
     * @param ctx    the ctx
     */
    public static void handle(CommonConfigHashInformPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            int hash = CrossServerConfigManager.INSTANCE.calculateConfigHash();
            if (hash != packet.hash()) {
                      NetworkHandler.CHANNEL.sendToServer(new SyncCommonConfigRequestPacket(hash));
                  }
            }
        );
        context.setPacketHandled(true);
    }

}
