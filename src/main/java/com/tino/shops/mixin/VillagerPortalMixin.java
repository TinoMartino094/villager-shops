package com.tino.shops.mixin;

import com.tino.shops.api.IShopVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerPortalMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void injectTickPortalCheck(CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;

        // Only apply to shop villagers on the server
        if (!(villager instanceof IShopVillager shopVillager)) return;
        if (!shopVillager.isShop()) return;
        if (villager.level().isClientSide()) return;

        // Shop villagers are pinned 1 block above their container, so their hitbox
        // never physically intersects the End Portal block below them. We manually
        // call entityInside each tick so the portal processor triggers correctly.
        BlockPos posBelow = villager.blockPosition().below();
        BlockState stateBelow = villager.level().getBlockState(posBelow);

        if (stateBelow.is(Blocks.END_PORTAL)) {
            stateBelow.entityInside(villager.level(), posBelow, villager, InsideBlockEffectApplier.NOOP, false);
        }
    }
}
