package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismBackoffState;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.DynamicNetwork;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicNetwork.class)
public abstract class DynamicNetworkMixin {

    @Inject(method = "acceptorChanged", at = @At("HEAD"))
    private void arcadia$resetBackoffOnAcceptorChange(Transmitter<?, ?, ?> transmitter, Direction side, CallbackInfo ci) {
        if ((Object) this instanceof ArcadiaMekanismBackoffState state) {
            state.arcadia$resetBackoff();
        }
    }
}
