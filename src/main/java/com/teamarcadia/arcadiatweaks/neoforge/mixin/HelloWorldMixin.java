package com.teamarcadia.arcadiatweaks.neoforge.mixin;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class HelloWorldMixin {

    @Unique
    private static boolean arcadia$pipelineLogged = false;

    @Inject(method = "runServer", at = @At("HEAD"))
    private void arcadia$logPipelineOk(CallbackInfo ci) {
        if (!arcadia$pipelineLogged) {
            arcadia$pipelineLogged = true;
            ArcadiaTweaks.LOGGER.info("[Mixin] Pipeline OK - HelloWorldMixin attached to MinecraftServer.runServer.");
        }
    }
}
