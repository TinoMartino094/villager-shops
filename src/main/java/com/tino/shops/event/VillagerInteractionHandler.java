package com.tino.shops.event;

import com.tino.shops.api.IShopVillager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class VillagerInteractionHandler {

    /** 
     * Finds the container a villager is standing on.
     * Chests are 14/16 blocks tall so the villager's blockPosition() Y equals the
     * chest's Y — check there first for ChestBlock only. Full-height containers
     * (barrels, shulker boxes) always resolve correctly via .below().
     */
    private static BlockPos findContainerPos(Villager villager, Level level) {
        BlockPos atFeet = villager.blockPosition();
        // Only check atFeet for chests (partial-height block)
        if (level.getBlockState(atFeet).getBlock() instanceof ChestBlock) return atFeet;
        BlockPos below = atFeet.below();
        if (isContainer(level, below)) return below;
        return null;
    }

    private static boolean isContainer(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) return true;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container;
    }

    private static Container resolveContainer(Level level, BlockPos pos) {
        if (pos == null) return null;
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (state.getBlock() instanceof ChestBlock chestBlock) {
            return ChestBlock.getContainer(chestBlock, state, level, pos, true);
        } else if (be instanceof Container c) {
            return c;
        }
        return null;
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) return InteractionResult.PASS;

            if (entity instanceof Villager villager) {
                IShopVillager shop = (IShopVillager) villager;
                ItemStack handStack = player.getItemInHand(hand);

                // Setup New Shop
                if (!shop.isShop()) {
                    if (handStack.is(Items.NAME_TAG) && handStack.getHoverName().getString().equals("Shop")) {
                        net.minecraft.core.Holder<VillagerProfession> prof = villager.getVillagerData().profession();
                        if (prof.is(VillagerProfession.NITWIT) || prof.is(VillagerProfession.NONE)) {
                            
                            BlockPos containerPos = findContainerPos(villager, level);
                            Container container = resolveContainer(level, containerPos);

                            if (container != null && container.getContainerSize() >= 9) {
                                ItemStack soldStack = container.getItem(0);
                                ItemStack costStack = container.getItem(1);
                                ItemStack adminStack = container.getItem(2);

                                if (soldStack.isEmpty() || costStack.isEmpty() || adminStack.isEmpty()) {
                                    player.sendSystemMessage(Component.literal("Shop setup failed! Slot 1 (Item to Sell), Slot 2 (Prize), and Slot 3 (Signed book) cannot be empty."));
                                    return InteractionResult.FAIL;
                                }

                                shop.setShop(true);
                                shop.setShopX(containerPos.getX());
                                shop.setShopY(containerPos.getY());
                                shop.setShopZ(containerPos.getZ());
                                shop.setShopDimension(level.dimension().identifier().toString());
                                
                                shop.setItemSold(soldStack.copy());
                                shop.setCostItem(costStack.copy());
                                shop.setAdminKey(container.getItem(2).copy());
                                shop.setStoredCurrency(0);

                                villager.setCustomName(Component.literal(player.getName().getString() + "'s Shop"));
                                
                                if (!player.isCreative()) {
                                    handStack.shrink(1);
                                }
                                
                                player.sendSystemMessage(Component.literal("Shop created successfully!"));
                                return InteractionResult.SUCCESS;
                            } else {
                                player.sendSystemMessage(Component.literal("The block below the Villager must be a container (9+ slots) to create a shop!"));
                                return InteractionResult.FAIL;
                            }
                        }
                    }
                } 
                // Unlink Existing Shop
                else {
                    if (handStack.is(Items.NAME_TAG)) {
                        String name = handStack.getHoverName().getString();
                        if (!name.equals("Shop")) {
                            if (shop.getStoredCurrency() > 0) {
                                player.sendSystemMessage(Component.literal("Cannot unlink shop: There is still stored currency inside! Cash out first."));
                                return InteractionResult.FAIL;
                            }

                            BlockPos containerPos = findContainerPos(villager, level);
                            Container container = resolveContainer(level, containerPos);

                            if (container != null) {
                                for (int i = 0; i < container.getContainerSize(); i++) {
                                    ItemStack stack = container.getItem(i);
                                    if (ItemStack.isSameItemSameComponents(stack, shop.getItemSold())) {
                                        player.sendSystemMessage(Component.literal("Cannot unlink shop: There is still stock in the chest! Empty it first."));
                                        return InteractionResult.FAIL;
                                    }
                                }
                            }

                            // Wipe Shop Data
                            shop.setShop(false);
                            shop.setShopX(0); shop.setShopY(0); shop.setShopZ(0);
                            shop.setShopDimension("minecraft:overworld");
                            shop.setItemSold(ItemStack.EMPTY);
                            shop.setCostItem(ItemStack.EMPTY);
                            shop.setAdminKey(ItemStack.EMPTY);
                            shop.setStoredCurrency(0);

                            // Restore AI & Rename
                            villager.setNoAi(false);
                            villager.setCustomName(handStack.getHoverName());

                            if (!player.isCreative()) {
                                handStack.shrink(1);
                            }
                            
                            player.sendSystemMessage(Component.literal("Shop unlinked successfully."));
                            return InteractionResult.SUCCESS;
                        } else {
                            // Trying to name an active shop "Shop" again
                            return InteractionResult.FAIL;
                        }
                    }
                }
            }

            return InteractionResult.PASS;
        });
    }
}
