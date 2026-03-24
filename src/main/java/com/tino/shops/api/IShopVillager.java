package com.tino.shops.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public interface IShopVillager {
    boolean isShop();
    void setShop(boolean isShop);

    int getShopX();
    void setShopX(int x);

    int getShopY();
    void setShopY(int y);

    int getShopZ();
    void setShopZ(int z);

    String getShopDimension();
    void setShopDimension(String dimension);

    int getStoredCurrency();
    void setStoredCurrency(int amount);

    ItemStack getItemSold();
    void setItemSold(ItemStack stack);

    ItemStack getCostItem();
    void setCostItem(ItemStack stack);

    ItemStack getAdminKey();
    void setAdminKey(ItemStack stack);
}
