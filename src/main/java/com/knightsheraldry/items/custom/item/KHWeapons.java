package com.knightsheraldry.items.custom.item;

import com.knightsheraldry.KnightsHeraldry;
import com.knightsheraldry.items.ModToolMaterials;
import com.knightsheraldry.util.KHDamageCalculator;
import com.knightsheraldry.util.KHTags;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SwordItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KHWeapons extends SwordItem {
    public KHWeapons(float attackSpeed, Settings settings) {
        super(ModToolMaterials.WEAPONS, 1, attackSpeed, settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return stack.isIn(KHTags.Weapon.KH_WEAPONS_SHIELD) ? UseAction.BLOCK : UseAction.NONE;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            if ((itemStack.isIn(KHTags.Weapon.KH_WEAPONS_BLUDGEONING)
                    || itemStack.isIn(KHTags.Weapon.KH_WEAPONS_BLUDGEONING_TO_PIERCING)) && user.isSneaking()) {
                int currentVariant = itemStack.getOrCreateNbt().getInt("CustomModelData");
                int newVariant = (currentVariant + 1) % 2;
                itemStack.getOrCreateNbt().putInt("CustomModelData", newVariant);
                return TypedActionResult.success(itemStack);
            }
        }
        user.setCurrentHand(hand);
        if (!itemStack.isIn(KHTags.Weapon.KH_WEAPONS_SHIELD)) return TypedActionResult.fail(itemStack);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (stack.isIn(KHTags.Weapon.KH_WEAPONS_BLUDGEONING))
            tooltip.add(Text.translatable("tooltip.knightsheraldry.shift-right_click-bludgeoning"));
        if (stack.isIn(KHTags.Weapon.KH_WEAPONS_BLUDGEONING_TO_PIERCING))
            tooltip.add(Text.translatable("tooltip.knightsheraldry.shift-right_click-bludgeoning-piercing"));
        if (stack.isIn(KHTags.Weapon.KH_WEAPONS_HARVEST))
            tooltip.add(Text.translatable("tooltip.knightsheraldry.right_click-replant"));
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            handlePostHit(stack, target, playerEntity);
            stack.damage(1, playerEntity, p -> p.sendToolBreakStatus(playerEntity.getActiveHand()));
        }
        return true;
    }

    private void handlePostHit(ItemStack stack, LivingEntity target, PlayerEntity playerEntity) {
        Vec3d playerPos = playerEntity.getPos();
        double maxDistance = getRadius(4);
        Box detectionBox = new Box(playerEntity.getBlockPos()).expand(maxDistance);

        playerEntity.getWorld().getEntitiesByClass(LivingEntity.class, detectionBox, entity ->
                        entity != playerEntity && entity == target && playerEntity.getBlockPos().isWithinDistance(entity.getBlockPos(), maxDistance + 1))
                .forEach(entity -> {
                    double distance = playerPos.distanceTo(target.getPos());
                    float damage = calculateDamageBasedOnWeaponType(target, stack, distance, ((PlayerAttackProperties) playerEntity).getComboCount());

                    if (stack.isIn(KHTags.Weapon.KH_WEAPONS_DAMAGE_BEHIND)) {
                        damage = adjustDamageForBackstab(target, playerPos, damage);
                    }

                    applyDamage(target, playerEntity, stack, damage);
                });
    }

    public float calculateDamageBasedOnWeaponType(LivingEntity livingEntity, ItemStack stack, double distance, int comboCount) {
        boolean bludgeoning = stack.getOrCreateNbt().getInt("CustomModelData") == 1;
        if (stack.isIn(KHTags.Weapon.KH_WEAPONS_BLUDGEONING_TO_PIERCING)) bludgeoning = !bludgeoning;
        boolean piercing = false;
        if (stack.isIn(KHTags.Weapon.KH_WEAPONS_PIERCING)) {
            int[] piercingAnimations = getPiercingAnimation();
            validatePiercingValues();
            int animationLength = getAnimation();
            for (int piercingAnimation : piercingAnimations) {
                if (comboCount % animationLength == piercingAnimation - 1) {
                    piercing = true;
                    break;
                }
            }

            if (piercingAnimations.length == animationLength) piercing = true;
        }
        if (stack.isIn(KHTags.Weapon.KH_WEAPONS_ONLY_PIERCING)) piercing = true;

        KHDamageCalculator calculator = new KHDamageCalculator();
        if (bludgeoning || stack.isIn(KHTags.Weapon.KH_WEAPONS_ONLY_BLUDGEONING)) {
            return calculator.getKHDamage(livingEntity, calculateDamage(distance, 10, 14), KHDamageCalculator.DamageType.BLUDGEONING);
        } else if (piercing || stack.isIn(KHTags.Weapon.KH_WEAPONS_BLUDGEONING_TO_PIERCING)) {
            return calculator.getKHDamage(livingEntity, calculateDamage(distance, 5, 9), KHDamageCalculator.DamageType.PIERCING);
        } else {
            return calculator.getKHDamage(livingEntity, calculateDamage(distance, 0, 4), KHDamageCalculator.DamageType.SLASHING);
        }
    }

    private float adjustDamageForBackstab(LivingEntity target, Vec3d playerPos, float damage) {
        Vec3d targetToAttacker = playerPos.subtract(target.getPos()).normalize();
        Vec3d targetFacing = target.getRotationVec(1.0F).normalize();
        boolean isBehind = targetFacing.dotProduct(targetToAttacker) < -0.5;

        if (isBehind) {
            damage *= 2;
        }
        return damage;
    }

    private void applyDamage(LivingEntity target, PlayerEntity playerEntity, ItemStack stack, float damage) {
        if (stack.isIn(KHTags.Weapon.KH_WEAPONS_IGNORES_ARMOR) && target.getHealth() - damage > 0) {
            target.setHealth(target.getHealth() - damage);
        } else {
            target.damage(playerEntity.getWorld().getDamageSources().playerAttack(playerEntity), damage);
        }
    }

    public float getAttackDamage(int index) {
        float[] defaultDamageValues = getDefaultAttackDamageValues();
        return (index >= 0 && index < defaultDamageValues.length) ? defaultDamageValues[index] : 0.0F;
    }

    public float[] getDefaultAttackDamageValues() {
        return new float[0];
    }

    protected double getRadius(int index) {
        double[] defaultRadiusValues = getDefaultRadiusValues();
        validateRadiusValues(defaultRadiusValues);
        return (index >= 0 && index < defaultRadiusValues.length) ? defaultRadiusValues[index] : 0.0d;
    }

    public double[] getDefaultRadiusValues() {
        return new double[0];
    }

    private void validateRadiusValues(double[] radiusValues) {
        for (int i = 1; i < radiusValues.length; i++) {
            if (radiusValues[i - 1] > radiusValues[i]) {
                KnightsHeraldry.LOGGER.error("Critical error: Index {} is higher than index {}. Radius values: {}", i - 1, i, java.util.Arrays.toString(radiusValues));
                throw new IndexOutOfBoundsException("Index " + (i - 1) + " is higher than index " + i + ". Radius values: " + java.util.Arrays.toString(radiusValues));
            }
        }
    }

    private void validatePiercingValues() {
        if (getPiercingAnimation().length > 2) {
            KnightsHeraldry.LOGGER.error("Critical error: Piercing Animations Index is higher than 2");
            throw new IndexOutOfBoundsException("Critical error: Piercing Animations Index is higher than 2");
        }
    }

    public int[] getPiercingAnimation() {
        return new int[0];
    }

    public int getAnimation() {
        return 0;
    }

    private float calculateDamage(double distance, int startIndex, int endIndex) {
        for (int i = startIndex; i <= endIndex; i++) {
            if (distance < getRadius(i - startIndex) + 0.25F) {
                return getAttackDamage(i);
            }
        }
        return 0.0F;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        BlockState state = world.getBlockState(pos);

        if (!world.isClient && player != null) {
            ItemStack stack = context.getStack();
            Block block = state.getBlock();

            if (block instanceof CropBlock cropBlock) {
                if (cropBlock.isMature(state) && world.breakBlock(pos, true, player)) {
                    replantCrop(world, pos, cropBlock, player);
                    stack.damage(1, player, p -> p.sendToolBreakStatus(context.getHand()));
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS;
    }

    private void replantCrop(World world, BlockPos pos, CropBlock cropBlock, PlayerEntity player) {
        ItemStack seedStack = new ItemStack(cropBlock.asItem());

        if (!seedStack.isEmpty()) {
            world.setBlockState(pos, cropBlock.getDefaultState());
            world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(player, cropBlock.getDefaultState()));
            if (!player.isCreative()) {
                seedStack.decrement(1);
            }
        }
    }
}