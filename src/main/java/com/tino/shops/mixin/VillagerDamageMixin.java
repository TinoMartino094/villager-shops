package com.tino.shops.mixin;

import com.tino.shops.api.IShopVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class VillagerDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtServer(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof IShopVillager shop) {
            if (shop.isShop()) {
                // If it falls in the void or uses a generic kill command, let it die to avoid permanent stuck entities
                if (!source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD) && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
                    cir.setReturnValue(false); // Cancel all other damage, including attacks, explosions, and suffocation
                }
            }
        }
    }

    @Inject(method = "isImmobile", at = @At("HEAD"), cancellable = true)
    private void injectIsImmobile(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof IShopVillager shop && shop.isShop()) {
            cir.setReturnValue(true);
        }
    }

}
