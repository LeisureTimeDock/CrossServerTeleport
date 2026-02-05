package com.leisuretimedock.crossmod.reset;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.FriendlyByteBuf;
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
        com.leisuretimedock.crossmod.network.toClient.ResetPacket.handler(ctxSupplier, log);

    }

}

