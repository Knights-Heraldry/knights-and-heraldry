package com.knightsheraldry.mixin;

import com.knightsheraldry.event.KeyInputHandler;
import com.knightsheraldry.items.custom.item.KHRangeWeapon;
import com.knightsheraldry.items.custom.item.KHWeapon;
import com.knightsheraldry.util.itemdata.KHTags;
import com.knightsheraldry.util.weaponutil.KHRangeWeaponUtil;
import com.knightsheraldry.util.weaponutil.KHWeaponUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {
    @Unique
    private static final String NBT_BLUDGEONING_KEY = "kh_bludgeoning";

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    public void knightsheraldry$getUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if (stack.getItem() instanceof KHWeapon) {
            cir.setReturnValue(stack.isIn(KHTags.WEAPONS_SHIELD.getTag()) ? UseAction.BLOCK : UseAction.NONE);
        } else if (stack.getItem() instanceof KHRangeWeapon khRangeWeapon) {
            cir.setReturnValue(khRangeWeapon.useAction() == UseAction.BOW ? UseAction.BOW : UseAction.NONE);
        }
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    public void knightsheraldry$getMaxUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof KHRangeWeapon khRangeWeapon) {
            cir.setReturnValue(khRangeWeapon.maxUseTime());
        }
    }

    @Inject(method = "usageTick", at = @At("HEAD"))
    public void knightsheraldry$usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks, CallbackInfo ci) {
        if (world.isClient
                || !(user instanceof PlayerEntity player)
                || !(stack.getItem() instanceof KHRangeWeapon khRangeWeapon)
                || khRangeWeapon.ammoRequirement() != null) {
            return;
        }

        int useTime = khRangeWeapon.maxUseTime() - remainingUseTicks;
        if (khRangeWeapon.useAction() == UseAction.CROSSBOW) {
            handleCrossbowCharging(world, stack, player, khRangeWeapon, useTime);
        }
    }

    @Unique
    private void handleCrossbowCharging(World world, ItemStack stack, PlayerEntity player,
                                        KHRangeWeapon weapon, int useTime) {
        float pullProgress = KHRangeWeaponUtil.getCrossbowPullProgress(useTime, weapon);
        KHRangeWeaponUtil.WeaponState weaponState = KHRangeWeaponUtil.getWeaponState(stack);

        if (pullProgress >= 1.0F && !weaponState.isCharged()) {
            KHRangeWeaponUtil.getArrowFromInventory(player).ifPresent(arrowStack ->
                    KHRangeWeaponUtil.loadAndPlayCrossbowSound(world, stack, player, arrowStack)
            );
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void knightsheraldry$use(World world, PlayerEntity user, Hand hand,
                                    CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack stack = user.getStackInHand(hand);

        if (stack.getItem() instanceof KHWeapon) {
            handleKHWeaponUse(world, user, hand, stack, cir);
        } else if (stack.getItem() instanceof KHRangeWeapon khRangeWeapon) {
            handleRangeWeaponUse(world, user, hand, stack, khRangeWeapon, cir);
        }
    }

    @Unique
    private void handleKHWeaponUse(World world, PlayerEntity user, Hand hand, ItemStack stack,
                                   CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!world.isClient && user.isSneaking() && isBludgeoningWeapon(stack)) {
            toggleBludgeoningMode(stack);
            cir.setReturnValue(TypedActionResult.success(stack));
            return;
        }

        user.setCurrentHand(hand);
        cir.setReturnValue(stack.isIn(KHTags.WEAPONS_SHIELD.getTag())
                ? TypedActionResult.consume(stack)
                : TypedActionResult.fail(stack));
    }

    @Unique
    private boolean isBludgeoningWeapon(ItemStack stack) {
        return stack.isIn(KHTags.WEAPONS_BLUDGEONING.getTag())
                || stack.isIn(KHTags.WEAPONS_BLUDGEONING_TO_PIERCING.getTag());
    }

    @Unique
    private void toggleBludgeoningMode(ItemStack stack) {
        stack.getOrCreateNbt().putBoolean(NBT_BLUDGEONING_KEY,
                !stack.getOrCreateNbt().getBoolean(NBT_BLUDGEONING_KEY));
    }

    @Unique
    private void handleRangeWeaponUse(World world, PlayerEntity user, Hand hand, ItemStack stack,
                                      KHRangeWeapon weapon, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (world.isClient) return;

        if (weapon.ammoRequirement() != null) {
            handleAmmoBasedWeapon(world, user, hand, stack, weapon, cir);
        } else {
            handleProjectileWeapon(hand, user, stack, weapon, cir);
        }
    }

    @Unique
    private void handleAmmoBasedWeapon(World world, PlayerEntity user, Hand hand, ItemStack stack,
                                       KHRangeWeapon weapon, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        KHRangeWeaponUtil.WeaponState weaponState = KHRangeWeaponUtil.getWeaponState(stack);

        if (!weaponState.isCharged()) {
            cir.setReturnValue(TypedActionResult.fail(stack));
            return;
        }

        ItemStack offHandStack = user.getOffHandStack();
        if (weapon.needsFlintAndSteel() && offHandStack.getItem() != Items.FLINT_AND_STEEL && !user.isCreative()) {
            cir.setReturnValue(TypedActionResult.fail(stack));
            return;
        }

        user.setCurrentHand(hand);
        KHRangeWeaponUtil.shootBullet(world, stack, weapon, user);
        KHRangeWeaponUtil.setWeaponState(stack, new KHRangeWeaponUtil.WeaponState(
                weaponState.isReloading(), false, true));

        if (!user.getAbilities().creativeMode && weapon.needsFlintAndSteel() && user instanceof ServerPlayerEntity serverPlayer) {
            offHandStack.damage(1, serverPlayer, p -> p.sendToolBreakStatus(hand));
        }

        cir.setReturnValue(TypedActionResult.consume(stack));
    }

    @Unique
    private void handleProjectileWeapon(Hand hand, PlayerEntity user, ItemStack stack, KHRangeWeapon weapon,
                                        CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        boolean hasAmmo = KHRangeWeaponUtil.getArrowFromInventory(user).isPresent();

        if (!hasAmmo) {
            cir.setReturnValue(TypedActionResult.fail(stack));
            return;
        }

        user.setCurrentHand(hand);
        if (weapon.useAction() == UseAction.CROSSBOW) {
            cir.setReturnValue(KHRangeWeaponUtil.handleCrossbowUse(user.getWorld(), user, hand, weapon, stack));
        } else {
            cir.setReturnValue(TypedActionResult.consume(stack));
        }
    }

    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    public void knightsheraldry$onStoppedUsing(ItemStack stack, World world, LivingEntity user,
                                               int remainingUseTicks, CallbackInfo ci) {
        if (world.isClient
                || !(user instanceof PlayerEntity player)
                || !(stack.getItem() instanceof KHRangeWeapon khRangeWeapon)) {
            return;
        }

        int useTime = khRangeWeapon.maxUseTime() - remainingUseTicks;
        KHRangeWeaponUtil.getArrowFromInventory(player).ifPresent(arrowStack ->
                handleWeaponRelease(stack, world, player, khRangeWeapon, useTime, arrowStack)
        );
    }

    @Unique
    private void handleWeaponRelease(ItemStack stack, World world, PlayerEntity player,
                                     KHRangeWeapon weapon, int useTime, ItemStack arrowStack) {
        if (weapon.useAction() == UseAction.BOW) {
            handleBowRelease(world, stack, weapon, player, arrowStack, useTime);
        } else if (weapon.useAction() == UseAction.CROSSBOW) {
            handleCrossbowRelease(stack, useTime, weapon);
        }
    }

    @Unique
    private void handleBowRelease(World world, ItemStack stack, KHRangeWeapon weapon,
                                  PlayerEntity player, ItemStack arrowStack, int useTime) {
        float pullProgress = KHRangeWeaponUtil.getBowPullProgress(useTime);
        if (pullProgress > 0.1f) {
            KHRangeWeaponUtil.shootArrow(world, stack, weapon, player, arrowStack, pullProgress);
        }
    }

    @Unique
    private void handleCrossbowRelease(ItemStack stack, int useTime, KHRangeWeapon weapon) {
        float pullProgress = KHRangeWeaponUtil.getCrossbowPullProgress(useTime, weapon);
        if (pullProgress < 1.0F) {
            KHRangeWeaponUtil.WeaponState currentState = KHRangeWeaponUtil.getWeaponState(stack);
            KHRangeWeaponUtil.setWeaponState(stack, new KHRangeWeaponUtil.WeaponState(
                    false, currentState.isCharged(), currentState.isShooting()));
        }
    }

    @Inject(method = "appendTooltip", at = @At("HEAD"))
    public void knightsheraldry$appendTooltip(ItemStack stack, World world, List<Text> tooltip,
                                              TooltipContext context, CallbackInfo ci) {
        if (stack.getItem() instanceof KHWeapon) {
            if (stack.isIn(KHTags.WEAPONS_BLUDGEONING.getTag())) {
                tooltip.add(Text.translatable("tooltip.knightsheraldry.shift-right_click-bludgeoning"));
            }
            if (stack.isIn(KHTags.WEAPONS_BLUDGEONING_TO_PIERCING.getTag())) {
                tooltip.add(Text.translatable("tooltip.knightsheraldry.shift-right_click-bludgeoning-piercing"));
            }
            if (stack.isIn(KHTags.WEAPONS_HARVEST.getTag())) {
                tooltip.add(Text.translatable("tooltip.knightsheraldry.right_click-replant"));
            }
        }

        if (stack.getItem() instanceof KHRangeWeapon khRangeWeapon
                && khRangeWeapon.ammoRequirement() != null) {
            tooltip.add(Text.translatable("tooltip.knightsheraldry.need_to_hold",
                    KeyInputHandler.reload.getBoundKeyLocalizedText()));
        }
    }

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void knightsheraldry$useOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = context.getStack();
        if (!(stack.getItem() instanceof KHWeapon) || !stack.isIn(KHTags.WEAPONS_HARVEST.getTag())) {
            return;
        }

        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        BlockState state = world.getBlockState(pos);

        if (!world.isClient && player != null) {
            handleCropHarvest(world, pos, state, player, stack, context.getHand(), cir);
        }
    }

    @Unique
    private void handleCropHarvest(World world, BlockPos pos, BlockState state, PlayerEntity player,
                                   ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop && crop.isMature(state) && world.breakBlock(pos, true, player)) {
            KHWeaponUtil.replantCrop(world, pos, crop, player, stack, hand);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}