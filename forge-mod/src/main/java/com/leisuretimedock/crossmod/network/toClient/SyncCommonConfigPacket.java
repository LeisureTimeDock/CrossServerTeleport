package com.leisuretimedock.crossmod.network.toClient;

import com.leisuretimedock.crossmod.config.CrossServerConfigManager;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The type Sync common config packet.
 */
@Slf4j
public record SyncCommonConfigPacket(CompoundTag config, int hash) {
    /**
     * Encode.
     *
     * @param msg the msg
     * @param buf the buf
     */
    public static void encode(SyncCommonConfigPacket msg, FriendlyByteBuf buf) {
       buf.writeNbt(msg.config);
       buf.writeInt(msg.hash);
    }

    /**
     * Decode packet eternal potato remove packet.
     *
     * @param buf the buf
     * @return the packet eternal potato remove packet
     */
    public static SyncCommonConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncCommonConfigPacket(buf.readNbt(), buf.readInt());
    }

    /**
     * Handle.
     *
     * @param msg the msg
     * @param ctx the ctx
     */
    public static void handle(SyncCommonConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            CompoundTag old = CrossServerConfigManager.INSTANCE.serializeToNBT();
            CrossServerConfigManager.INSTANCE.deserializeFromNBT(msg.config);
            int newHashCode = CrossServerConfigManager.INSTANCE.calculateConfigHash();
            if (newHashCode != msg.hash) { //BACK
                log.error("Hash mismatch! Except:{}, Actual:{}", msg.hash, newHashCode);
                CrossServerConfigManager.INSTANCE.deserializeFromNBT(old);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
