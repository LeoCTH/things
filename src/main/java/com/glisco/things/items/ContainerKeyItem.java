package com.glisco.things.items;

import com.glisco.things.ThingsCommon;
import com.glisco.things.mixin.ContainerLockAccessor;
import com.glisco.things.mixin.LockableContainerBlockEntityAccessor;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ContainerKeyItem extends ItemWithOptionalTooltip {

    private static final List<Text> TOOLTIP;

    static {
        TOOLTIP = new ArrayList<>();
        TOOLTIP.add(new LiteralText("§7Used to lock containers"));
        TOOLTIP.add(new LiteralText("§7 - Sneak-click to lock/unlock"));
        TOOLTIP.add(new LiteralText("§7 - Required to open a locked container"));
    }

    public ContainerKeyItem() {
        super(new Item.Settings().group(ThingsCommon.THINGS_ITEMS).maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getPlayer().isSneaking()) return ActionResult.PASS;
        World w = context.getWorld();
        BlockPos pos = context.getBlockPos();
        ItemStack stack = context.getStack();

        if (!(w.getBlockEntity(pos) instanceof LockableContainerBlockEntity)) return ActionResult.PASS;

        String existingLock = getExistingLock(w, pos);

        if (existingLock.isEmpty()) {
            setLock((LockableContainerBlockEntity) w.getBlockEntity(pos), String.valueOf(stack.getTag().getInt("Lock")));

            if (w.isClient) {
                sendLockedState(context, true);
            }

            return ActionResult.SUCCESS;
        } else if (existingLock.equals(String.valueOf(stack.getTag().getInt("Lock")))) {
            setLock((LockableContainerBlockEntity) w.getBlockEntity(pos), "");

            if (w.isClient) {
                sendLockedState(context, false);
            }

            return ActionResult.SUCCESS;
        } else {
            return ActionResult.PASS;
        }
    }

    private static String getExistingLock(World w, BlockPos pos) {
        String existingLock = ((ContainerLockAccessor) ((LockableContainerBlockEntityAccessor) w.getBlockEntity(pos)).getLock()).getKey();

        if (existingLock.isEmpty() && w.getBlockEntity(pos) instanceof ChestBlockEntity) {
            if (!w.getBlockState(pos).get(Properties.CHEST_TYPE).equals(ChestType.SINGLE)) {
                BlockPos otherChest = pos.offset(ChestBlock.getFacing(w.getBlockState(pos)));
                if (!((ContainerLockAccessor) ((LockableContainerBlockEntityAccessor) w.getBlockEntity(otherChest)).getLock()).getKey().isEmpty()) {
                    existingLock = ((ContainerLockAccessor) ((LockableContainerBlockEntityAccessor) w.getBlockEntity(otherChest)).getLock()).getKey();
                }
            }
        }

        return existingLock;
    }

    private static void sendLockedState(ItemUsageContext ctx, boolean locked) {
        ctx.getPlayer().playSound(SoundEvents.BLOCK_CHEST_LOCKED, 1, 1);

        MutableText containerName = (MutableText) ((LockableContainerBlockEntity) ctx.getWorld().getBlockEntity(ctx.getBlockPos())).getDisplayName();
        ctx.getPlayer().sendMessage(containerName.append(new LiteralText(locked ? " locked!" : " unlocked!")), true);
    }

    private static void setLock(LockableContainerBlockEntity entity, String lock) {
        CompoundTag lockTag = new CompoundTag();
        lockTag.putString("Lock", lock);

        ContainerLock containerLock = lock.isEmpty() ? ContainerLock.EMPTY : ContainerLock.fromTag(lockTag);

        ((LockableContainerBlockEntityAccessor) entity).setLock(containerLock);
        if (entity instanceof ChestBlockEntity) {
            if (!entity.getWorld().getBlockState(entity.getPos()).get(Properties.CHEST_TYPE).equals(ChestType.SINGLE)) {
                ((LockableContainerBlockEntityAccessor) entity.getWorld().getBlockEntity(entity.getPos().offset(ChestBlock.getFacing(entity.getCachedState())))).setLock(containerLock);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!stack.getOrCreateTag().contains("Lock")) {
            stack.getOrCreateTag().putInt("Lock", world.random.nextInt(200000));
        }
    }

    @Override
    List<Text> getTooltipText() {
        return TOOLTIP;
    }
}
