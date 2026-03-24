package com.tino.shops.mixin;

import com.tino.shops.api.IShopVillager;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class VillagerShopMixin implements IShopVillager {

    @Unique private boolean isShop = false;
    @Unique private int shopX = 0;
    @Unique private int shopY = 0;
    @Unique private int shopZ = 0;
    @Unique private String shopDimension = "minecraft:overworld";
    @Unique private int storedCurrency = 0;
    @Unique private ItemStack itemSold = ItemStack.EMPTY;
    @Unique private ItemStack costItem = ItemStack.EMPTY;
    @Unique private ItemStack adminKey = ItemStack.EMPTY;

    public Villager getVillager() { return (Villager) (Object) this; }

    @Override
    public boolean isShop() { return isShop; }
    @Override
    public void setShop(boolean isShop) { this.isShop = isShop; }

    @Override
    public int getShopX() { return shopX; }
    @Override
    public void setShopX(int x) { this.shopX = x; }

    @Override
    public int getShopY() { return shopY; }
    @Override
    public void setShopY(int y) { this.shopY = y; }

    @Override
    public int getShopZ() { return shopZ; }
    @Override
    public void setShopZ(int z) { this.shopZ = z; }

    @Override
    public String getShopDimension() { return shopDimension; }
    @Override
    public void setShopDimension(String dim) { this.shopDimension = dim; }

    @Override
    public int getStoredCurrency() { return storedCurrency; }
    @Override
    public void setStoredCurrency(int amount) { this.storedCurrency = amount; }

    @Override
    public ItemStack getItemSold() { return itemSold; }
    @Override
    public void setItemSold(ItemStack stack) { this.itemSold = stack; }

    @Override
    public ItemStack getCostItem() { return costItem; }
    @Override
    public void setCostItem(ItemStack stack) { this.costItem = stack; }

    @Override
    public ItemStack getAdminKey() { return adminKey; }
    @Override
    public void setAdminKey(ItemStack stack) { this.adminKey = stack; }

    @Inject(method = "getOffers", at = @At("HEAD"), cancellable = true)
    private void injectGetOffers(CallbackInfoReturnable<MerchantOffers> cir) {
        if (!this.isShop) return;

        Villager villager = (Villager)(Object)this;
        Level level = villager.level();
        
        // We only generate offers on the server. On client, they are synced by vanilla packets.
        if (level.isClientSide()) {
            return; 
        }

        BlockPos pos = new BlockPos(this.shopX, this.shopY, this.shopZ);
        MerchantOffers offers = new MerchantOffers();
        
        ServerLevel targetLevel = null;
        if (villager.level().getServer() != null) {
            for (ServerLevel sl : villager.level().getServer().getAllLevels()) {
                if (sl.dimension().identifier().toString().equals(this.shopDimension)) {
                    targetLevel = sl;
                    break;
                }
            }
        }

        // 1. Calculate Stock
        int availableStock = 0;
        
        if (targetLevel != null && targetLevel.isLoaded(pos)) {
            BlockState state = targetLevel.getBlockState(pos);
            BlockEntity blockEntity = targetLevel.getBlockEntity(pos);
            Container container = null;
            if (state.getBlock() instanceof ChestBlock chestBlock) {
                container = ChestBlock.getContainer(chestBlock, state, targetLevel, pos, true);
            } else if (blockEntity instanceof Container c) {
                container = c;
            }

            if (container != null) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(stack, this.itemSold)) {
                        availableStock += stack.getCount();
                    }
                }
            }
        }

        int maxUses = availableStock / Math.max(1, this.itemSold.getCount());

        // Guard: never send empty ItemStacks over network (causes EncoderException)
        if (!this.itemSold.isEmpty() && !this.costItem.isEmpty()) {
            ItemCost customerCost = new ItemCost(
                this.costItem.getItem().builtInRegistryHolder(),
                this.costItem.getCount(),
                DataComponentExactPredicate.allOf(this.costItem.getComponents())
            );

            // 2. Add Customer Trade (Ensure we use .copy() which copies components in modern MC)
            offers.add(new MerchantOffer(
                customerCost,
                this.itemSold.copy(),
                maxUses,
                0, // <--- XP set to 0 to prevent any vanilla UI XP bar increases
                0.05f
            ));
        }

        // 3. Add Cash Out Trade — always shown, maxUses=0 means "out of stock" when no currency
        if (!this.adminKey.isEmpty() && !this.costItem.isEmpty()) {
            ItemStack payout = this.costItem.copy();
            // When no currency, payout count must be at least 1 (can't send empty stack)
            payout.setCount(Math.min(payout.getMaxStackSize(), Math.max(1, this.storedCurrency)));
            ItemCost cashOutCost = new ItemCost(
                this.adminKey.getItem().builtInRegistryHolder(),
                1,
                DataComponentExactPredicate.allOf(this.adminKey.getComponents())
            );

            // maxUses=0 → trade visible but "out of stock" when no currency
            int maxCashOutUses = this.storedCurrency > 0
                ? this.storedCurrency / Math.max(1, payout.getCount())
                : 0;

            offers.add(new MerchantOffer(
                cashOutCost,
                payout,
                maxCashOutUses,
                0, -1.0f // <--- Negative price multiplier used as a unique identifier for Cash Out
            ));
        }

        cir.setReturnValue(offers);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void injectWriteShopData(ValueOutput output, CallbackInfo ci) {
        output.store("IsShop", Codec.BOOL, this.isShop);
        if (this.isShop) {
            output.store("ShopX", Codec.INT, this.shopX);
            output.store("ShopY", Codec.INT, this.shopY);
            output.store("ShopZ", Codec.INT, this.shopZ);
            output.store("ShopDimension", Codec.STRING, this.shopDimension);
            output.store("StoredCurrency", Codec.INT, this.storedCurrency);
            
            output.store("ItemSold", ItemStack.CODEC, this.itemSold);
            output.store("CostItem", ItemStack.CODEC, this.costItem);
            output.store("AdminKey", ItemStack.CODEC, this.adminKey);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void injectReadShopData(ValueInput input, CallbackInfo ci) {
        this.isShop = input.read("IsShop", Codec.BOOL).orElse(false);
        if (this.isShop) {
            this.shopX = input.read("ShopX", Codec.INT).orElse(0);
            this.shopY = input.read("ShopY", Codec.INT).orElse(0);
            this.shopZ = input.read("ShopZ", Codec.INT).orElse(0);
            this.shopDimension = input.read("ShopDimension", Codec.STRING).orElse("minecraft:overworld");
            this.storedCurrency = input.read("StoredCurrency", Codec.INT).orElse(0);
            
            this.itemSold = input.read("ItemSold", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            this.costItem = input.read("CostItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            this.adminKey = input.read("AdminKey", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }
    }

    @Inject(method = "notifyTrade", at = @At("HEAD"), cancellable = true)
    private void onNotifyTrade(MerchantOffer offer, CallbackInfo ci) {
        if (!this.isShop) return;

        Villager villager = this.getVillager();
        Level level = villager.level();
        if (level.isClientSide()) return;

        // ── Cash Out Trade ─────────────────────────────────────────────────────
        // Handle BEFORE container lookup: cash out only needs storedCurrency,
        // never touches the container. Checking container first caused dupes when
        // the container was missing (vanilla gave items → container null → refund admin key).
        if (ItemStack.isSameItemSameComponents(offer.getBaseCostA(), this.adminKey)) {
            this.storedCurrency -= offer.getResult().getCount();
            if (this.storedCurrency < 0) this.storedCurrency = 0;
            ci.cancel();
            return;
        }

        // ── Customer Trade ─────────────────────────────────────────────────────
        // Needs the container — resolve it now.
        BlockPos pos = new BlockPos(this.shopX, this.shopY, this.shopZ);

        ServerLevel targetLevel = null;
        if (villager.level().getServer() != null) {
            for (ServerLevel sl : villager.level().getServer().getAllLevels()) {
                if (sl.dimension().identifier().toString().equals(this.shopDimension)) {
                    targetLevel = sl;
                    break;
                }
            }
        }

        if (targetLevel == null || !targetLevel.isLoaded(pos)) {
            refundAndResync(villager, offer);
            ci.cancel(); return;
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
            refundAndResync(villager, offer);
            ci.cancel(); return;
        }

        // Customer Trade: result matches itemSold
        if (ItemStack.isSameItemSameComponents(offer.getResult(), this.itemSold)) {
            int needed = this.itemSold.getCount();
            
            // 1. Verify exact stock in this tick to prevent dupe/desync
            int availableStock = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (ItemStack.isSameItemSameComponents(stack, this.itemSold)) {
                    availableStock += stack.getCount();
                }
            }
            if (availableStock < needed) {
                refundAndResync(villager, offer);
                ci.cancel(); 
                return;
            }

            // 2. Deduct physical stock
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (ItemStack.isSameItemSameComponents(stack, this.itemSold)) {
                    int deduct = Math.min(needed, stack.getCount());
                    stack.shrink(deduct);
                    needed -= deduct;
                    if (stack.isEmpty()) {
                        container.setItem(i, ItemStack.EMPTY);
                    }
                    if (needed <= 0) break;
                }
            }

            // Pay currency
            this.storedCurrency += this.costItem.getCount();
        }
        
        container.setChanged();

        // Cancel vanilla behavior (XP gain, reputation changes, sound spam).
        ci.cancel(); 
    }

    @Unique
    private void refundAndResync(Villager villager, MerchantOffer offer) {
        if (villager.getTradingPlayer() instanceof ServerPlayer sp) {
            sp.getInventory().placeItemBackInInventory(offer.getBaseCostA().copy());
            if (!offer.getCostB().isEmpty()) {
                sp.getInventory().placeItemBackInInventory(offer.getCostB().copy());
            }
            if (sp.containerMenu instanceof MerchantMenu menu) {
                menu.sendAllDataToRemote(); 
                sp.sendMerchantOffers(menu.containerId, villager.getOffers(), villager.getVillagerData().level(), villager.getVillagerXp(), villager.showProgressBar(), villager.canRestock());
            }
        }
    }

}
