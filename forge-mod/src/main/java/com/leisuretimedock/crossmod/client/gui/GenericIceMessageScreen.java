package com.leisuretimedock.crossmod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (minecraft != null && minecraft.player != null){
            InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, width / 2, height / 2, 30, (float) width / 2 - mouseX, (float) height / 2 - mouseY, minecraft.player);
        }
    }

    @Override
    public void renderDirtBackground(@NotNull GuiGraphics guiGraphics) {
        renderIceBackground(guiGraphics,1.0F, 1.0F, 1.0F, 1.0F);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void renderIceBackground(GuiGraphics guiGraphics, float r, float g, float b, float a) {
        guiGraphics.setColor(0.65F, 0.65F, 0.65F, 1.0F);
        guiGraphics.blit(ICE, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
        guiGraphics.setColor(r, g, b, a);
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(this, guiGraphics));
    }

}
