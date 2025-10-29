package net.shiroha233.roadweaver.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import net.shiroha233.roadweaver.generation.InitialGenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void roadweaver$renderProgress(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!InitialGenManager.isActive()) return;
        int total = InitialGenManager.getTotal();
        int done = InitialGenManager.getDone();
        int planned = InitialGenManager.getPlanned();
        int generating = InitialGenManager.getGenerating();
        int failed = InitialGenManager.getFailed();
        int percent = (total <= 0) ? 0 : (int) Math.round(100.0 * done / Math.max(1, total));

        Minecraft mc = Minecraft.getInstance();
        var font = mc.font;
        int sh = mc.getWindow().getGuiScaledHeight();
        int x = 10;
        int y = sh - 60;

        graphics.drawString(font, Component.translatable("gui.roadweaver.initgen.title"), x, y, 0xFFFFFF, false);
        y += 12;
        graphics.drawString(font, Component.translatable("gui.roadweaver.initgen.summary", total, planned, generating, done, failed), x, y, 0xA0A0A0, false);
        y += 12;
        graphics.drawString(font, Component.translatable("gui.roadweaver.initgen.progress", done, total, percent), x, y, 0xA0FFA0, false);
    }
}
