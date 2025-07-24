package com.leisuretimedock.crossmod.mixin;

import net.minecraftforge.network.HandshakeMessages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = HandshakeMessages.C2SModListReply.class, remap = false)
public class ModListSpoofMixin {
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void injectFakeModList(CallbackInfo ci) {
        HandshakeMessages.C2SModListReply self = HandshakeMessages.C2SModListReply.class.cast(this);
        List<String> mods = self.getModList();
        if (!mods.contains("clientresetpacket")) {
            // "[Mixin] 模拟添加 clientresetpacket 模组到 modlist" ,以启用跳转功能
            mods.add("clientresetpacket");

        }
    }
}