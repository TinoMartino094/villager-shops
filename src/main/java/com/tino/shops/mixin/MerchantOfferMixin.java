package com.tino.shops.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    // This mixin ensures that if `maxUses` has been dynamically set to 0 by our 
    // MerchantMenu updates (because stock ran out), the physical trade completely 
    // fails even if the UI hasn't updated on the client visually.
    
    @Inject(method = "take", at = @At("HEAD"), cancellable = true)
    private void onTake(ItemStack buyA, ItemStack buyB, CallbackInfoReturnable<Boolean> cir) {
        MerchantOffer self = (MerchantOffer) (Object) this;
        
        // If the shop recalculation marked this offer as out of stock (Uses >= Max Uses),
        // completely reject the native take execution before any items are deleted or given.
        if (self.isOutOfStock()) {
            cir.setReturnValue(false);
            return;
        }

        // If this offer is our custom Cash Out trade (identified by a negative price multiplier),
        // we intercept the execution to prevent the Admin Key from being destroyed/consumed natively.
        if (self.getPriceMultiplier() < 0.0f) {
            if (!self.satisfiedBy(buyA, buyB)) {
                cir.setReturnValue(false);
                return;
            }
            // By returning true without shrinking buyA or buyB, the trade mathematically succeeds,
            // the Villager pays out the currency, but the Admin Key stays perfectly intact in the input slot!
            cir.setReturnValue(true);
        }
    }
}
