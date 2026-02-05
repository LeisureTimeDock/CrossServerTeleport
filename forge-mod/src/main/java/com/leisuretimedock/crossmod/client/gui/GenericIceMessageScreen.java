package com.leisuretimedock.crossmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

public class GenericIceMessageScreen extends GenericDirtMessageScreen {
    public static final ResourceLocation ICE = new ResourceLocation("textures/block/ice.png");
    //todo: tip ?
    private int tick = 0;
    public GenericIceMessageScreen(Component title) {
        super(title);
    }

    @Override
    public void tick() {
        super.tick();
        tick++;
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        if (minecraft != null && minecraft.player != null){
            InventoryScreen.renderEntityInInventory(width / 2, height / 2, 30, (float) width / 2 - mouseX, (float) height / 2 - mouseY, minecraft.player);
        }
    }

    @Override
    public void renderDirtBackground(int vOffset) {
        renderIceBackground(vOffset,200, 200, 200, 255);
    }

    public void renderIceBackground(int vOffset, int r, int g, int b, int a) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ICE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float f = 32.0F;
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.vertex(0.0, this.height, 0.0).uv(0.0F, (float)this.height / f + (float)vOffset).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(this.width, this.height, 0.0).uv((float)this.width / f, (float)this.height / f + (float)vOffset).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(this.width, 0.0, 0.0).uv((float)this.width / f, (float)vOffset).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(0.0, 0.0, 0.0).uv(0.0F, (float)vOffset).color(r, g, b, a).endVertex();
        tesselator.end();
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundDrawnEvent(this, new PoseStack()));
    }

}
