package com.tino.shops.mixin;

import com.tino.shops.api.IShopVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {

    @Shadow @Final private Merchant trader;
    @Shadow public abstract void setOffers(MerchantOffers offers);

    @Unique
    private Player shopPlayer;

    @Unique
    private int lastStockCount = -1;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/trading/Merchant;)V", at = @At("TAIL"))
    private void onMenuInit(int i, Inventory inventory, Merchant merchant, CallbackInfo ci) {
        this.shopPlayer = inventory.player;
        if (!this.shopPlayer.level().isClientSide() && merchant instanceof IShopVillager shop) {
            if (shop.isShop()) {
                this.lastStockCount = -1; // Force initial update
                this.refreshShopOffers(shop);
            }
        }
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void onSlotsChanged(Container container, CallbackInfo ci) {
        if (this.shopPlayer != null && !this.shopPlayer.level().isClientSide() && this.trader instanceof IShopVillager shop) {
            if (shop.isShop()) {
                this.refreshShopOffers(shop);
            }
        }
    }

    @Inject(method = "setSelectionHint", at = @At("HEAD"))
    private void onSelectionHint(int hint, CallbackInfo ci) {
        if (this.shopPlayer != null && !this.shopPlayer.level().isClientSide() && this.trader instanceof IShopVillager shop) {
            if (shop.isShop()) {
                this.refreshShopOffers(shop);
            }
        }
    }

    @Inject(method = "stillValid", at = @At("HEAD"))
    private void onStillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (this.shopPlayer != null && !this.shopPlayer.level().isClientSide() && this.trader instanceof IShopVillager shop) {
            if (shop.isShop()) {
                this.refreshShopOffers(shop);
            }
        }
    }

    @Unique
    private void refreshShopOffers(IShopVillager shop) {
        Level level = this.shopPlayer.level();
        int currentStock = calculateStock(shop, level, this.shopPlayer);
        
        // Prevent redundant UI packet spam
        if (currentStock == this.lastStockCount) {
            return; 
        }
        
        this.lastStockCount = currentStock;
        
        MerchantOffers customOffers = new MerchantOffers();
        int maxUses1 = 0;
        
        ItemStack itemSold = shop.getItemSold();
        ItemStack costItem = shop.getCostItem();
        ItemStack adminKey = shop.getAdminKey();

        if (!itemSold.isEmpty() && !costItem.isEmpty()) {
            int batchSize = itemSold.getCount();
            if (batchSize > 0) {
                maxUses1 = currentStock / batchSize;
            }
            
            // Trade 1: Customer Trade
            MerchantOffer customerTrade = new MerchantOffer(
                new ItemCost(costItem.typeHolder(), costItem.getCount(), net.minecraft.core.component.DataComponentExactPredicate.EMPTY),
                Optional.empty(),
                itemSold.copy(),
                0,
                maxUses1,
                0,
                0.0F,
                0
            );
            customOffers.add(customerTrade);
        }

        if (!adminKey.isEmpty() && !costItem.isEmpty()) {
            // Trade 2: Cash Out Trade — always shown, maxUses=0 = "out of stock" when no currency
            int storedCurrency = shop.getStoredCurrency();
            ItemStack currencyResult = costItem.copy();
            // clamp count: at least 1 (empty stack not allowed on wire), at most maxStack
            int payoutCount = storedCurrency > 0
                ? Math.min(currencyResult.getMaxStackSize(), storedCurrency)
                : 1;
            currencyResult.setCount(payoutCount);

            int maxCashOutUses = storedCurrency > 0
                ? storedCurrency / payoutCount
                : 0;

            MerchantOffer cashOutTrade = new MerchantOffer(
                new ItemCost(adminKey.typeHolder(), 1, net.minecraft.core.component.DataComponentExactPredicate.EMPTY),
                Optional.empty(),
                currencyResult,
                0,
                maxCashOutUses,
                0,
                -1.0F,  // negative identifier for cash out trade
                0
            );
            customOffers.add(cashOutTrade);
        }

        // Apply Offers
        this.setOffers(customOffers);
        if (this.trader instanceof net.minecraft.world.entity.npc.villager.Villager villager) {
            villager.setOffers(customOffers); 
        }

        // Force Client screen packet update
        if (this.shopPlayer instanceof ServerPlayer sp) {
            sp.sendMerchantOffers(
                ((MerchantMenu)(Object)this).containerId, 
                customOffers, 
                this.trader.getVillagerXp(), // For our purposes, traderlevel
                this.trader.getVillagerXp(), 
                this.trader.showProgressBar(), 
                this.trader.canRestock()
            );
        }
    }

    @Unique
    private int calculateStock(IShopVillager shop, Level level, Player player) {
        BlockPos pos = new BlockPos(shop.getShopX(), shop.getShopY(), shop.getShopZ());
        
        ServerLevel targetLevel = null;
        if (player.level().getServer() != null) {
            for (ServerLevel sl : player.level().getServer().getAllLevels()) {
                if (sl.dimension().identifier().toString().equals(shop.getShopDimension())) {
                    targetLevel = sl;
                    break;
                }
            }
        }

        if (targetLevel == null || !targetLevel.isLoaded(pos)) {
            ((ServerPlayer) player).sendSystemMessage(Component.literal("Container is currently unloaded."), true);
            return 0; // Force 0 uses
        }

        BlockState state = targetLevel.getBlockState(pos);
        BlockEntity blockEntity = targetLevel.getBlockEntity(pos);

        Container container = null;
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            container = ChestBlock.getContainer(chestBlock, state, targetLevel, pos, true);
        } else if (blockEntity instanceof Container c) {
            container = c;
        }

        if (container == null) {
            ((ServerPlayer) player).sendSystemMessage(Component.literal("Container destroyed or missing."), true);
            return 0;
        }

        int totalStock = 0;
        ItemStack targetItem = shop.getItemSold();
        
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (i < 3) continue; // Skip configuration slots
            
            if (ItemStack.isSameItemSameComponents(stack, targetItem)) {
                totalStock += stack.getCount();
            }
        }
        
        return totalStock;
    }
}
