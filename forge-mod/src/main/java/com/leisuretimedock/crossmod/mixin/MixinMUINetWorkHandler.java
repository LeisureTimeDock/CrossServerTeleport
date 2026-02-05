package com.leisuretimedock.crossmod.mixin;

import icyllis.modernui.mc.forge.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = NetworkHandler.class, remap = false)
public class MixinMUINetWorkHandler {

    @ModifyVariable(
            method = "<init>",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private static ResourceLocation modifyNameParameter(ResourceLocation name) {
        return name.getPath().isEmpty() ? name : new ResourceLocation(name.getNamespace(), "default");
    }

}
