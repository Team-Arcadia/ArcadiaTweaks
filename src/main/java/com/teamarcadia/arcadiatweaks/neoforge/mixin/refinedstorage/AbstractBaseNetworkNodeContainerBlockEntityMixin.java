package com.teamarcadia.arcadiatweaks.neoforge.mixin.refinedstorage;

import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.teamarcadia.arcadiatweaks.neoforge.refinedstorage.ArcadiaRefinedStorageNodeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractBaseNetworkNodeContainerBlockEntity.class)
public abstract class AbstractBaseNetworkNodeContainerBlockEntityMixin implements ArcadiaRefinedStorageNodeState {

    @Unique private int arcadia$activenessCheckCooldown;
    @Unique private int arcadia$transmitterStateCheckCooldown;

    @Override
    @Unique
    public int arcadia$getActivenessCheckCooldown() {
        return arcadia$activenessCheckCooldown;
    }

    @Override
    @Unique
    public void arcadia$setActivenessCheckCooldown(int value) {
        arcadia$activenessCheckCooldown = value;
    }

    @Override
    @Unique
    public int arcadia$getTransmitterStateCheckCooldown() {
        return arcadia$transmitterStateCheckCooldown;
    }

    @Override
    @Unique
    public void arcadia$setTransmitterStateCheckCooldown(int value) {
        arcadia$transmitterStateCheckCooldown = value;
    }
}
