package com.glisco.things.items;

import com.glisco.things.ThingsCommon;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

public class RecallPotionItem extends Item {

    public RecallPotionItem() {
        super(new Item.Settings().group(ThingsCommon.THINGS_ITEMS).maxCount(16));
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 15;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        PlayerEntity player = user instanceof PlayerEntity ? (PlayerEntity) user : null;

        if (player == null) return new ItemStack(Items.GLASS_BOTTLE);
        if (player instanceof ServerPlayerEntity) {

            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            ServerWorld spawnWorld = serverPlayer.getServer().getWorld(serverPlayer.getSpawnPointDimension());

            Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);

            if (serverPlayer.getSpawnPointPosition() != null) {
                Optional<Vec3d> posOptional = PlayerEntity.findRespawnPosition(spawnWorld, serverPlayer.getSpawnPointPosition(), serverPlayer.getSpawnAngle(), true, false);
                if (posOptional.isPresent()) {
                    BlockPos spawn = posOptional.map(BlockPos::new).get();
                    serverPlayer.teleport(spawnWorld, spawn.getX() + 0.5f, spawn.getY(), spawn.getZ() + 0.5f, serverPlayer.getSpawnAngle(), 0f);
                } else {
                    serverPlayer.sendMessage(new LiteralText("No respawn point"), true);
                }
            } else {
                serverPlayer.sendMessage(new LiteralText("No respawn point"), true);
            }
        }

        if (!player.abilities.creativeMode) {
            stack.decrement(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            } else {
                player.inventory.offerOrDrop(player.world, new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        return stack;
    }

}
