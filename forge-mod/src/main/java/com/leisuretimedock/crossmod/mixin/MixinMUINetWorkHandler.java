package com.leisuretimedock.crossmod.mixin;

import icyllis.modernui.mc.forge.NetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(value = NetworkHandler.class, remap = false)
public class MixinMUINetWorkHandler {
    /**
    * 修补构造 ResourceLocation("modernui", id) 时，若 id 是空字符串，则替换为 "default"
    */
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;<init>(Ljava/lang/String;Ljava/lang/String;)V"
            ),
            index = 1 // 修改 id 参数
    )
    private String fixEmptyId(String id) {
        return id == null || id.isEmpty() ? "default" : id;
    }
}
