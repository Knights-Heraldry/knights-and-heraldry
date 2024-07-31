package com.knightsheraldry.items.custom;

import com.knightsheraldry.KnightsHeraldry;
import com.knightsheraldry.items.ModToolMaterials;
import com.knightsheraldry.util.IEntityDataSaver;
import com.knightsheraldry.util.ModTags;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KHWeapons extends SwordItem {
    public KHWeapons(float attackSpeed, Settings settings) {
        super(ModToolMaterials.WEAPONS, 1, attackSpeed, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        IEntityDataSaver dataSaver = (IEntityDataSaver) player;
        boolean isEquipped = player.getMainHandStack().isIn(ModTags.Items.KH_WEAPONS) ||
                player.getOffHandStack().isIn(ModTags.Items.KH_WEAPONS);

        dataSaver.knightsheraldry$getPersistentData().putBoolean("able_stamina", isEquipped);
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
                throw new IllegalStateException("Index " + (i - 1) + " is higher than index " + i + ". Radius values: " + java.util.Arrays.toString(radiusValues));
            }
        }
    }

    public int getPiercingAnimation() {
        return 0;
    }

    public int getAnimation() {
        return 0;
    }

    private float getSlashingDamage(double distance) {
        return calculateDamage(distance, 0, 4);
    }

    private float getPiercingDamage(double distance) {
        return calculateDamage(distance, 5, 9);
    }

    private float getBludgeoningDamage(double distance) {
        return calculateDamage(distance, 10, 14);
    }

    private float calculateDamage(double distance, int startIndex, int endIndex) {
        for (int i = startIndex; i <= endIndex; i++) {
            if (distance < getRadius(i - startIndex)) {
                return getAttackDamage(i);
            }
        }
        return 0.0F;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            Vec3d playerPos = playerEntity.getPos();
            double maxDistance = getRadius(4);
            Box detectionBox = new Box(playerEntity.getBlockPos()).expand(maxDistance);

            playerEntity.getWorld().getEntitiesByClass(LivingEntity.class, detectionBox, entity ->
                    entity != playerEntity && entity == target && playerEntity.getBlockPos().isWithinDistance(entity.getBlockPos(), maxDistance + 1)).forEach(entity -> {
                double distance = playerPos.distanceTo(target.getPos());
                int comboCount = ((PlayerAttackProperties) playerEntity).getComboCount();

                float damage;
                boolean bludgeoning = stack.getOrCreateNbt().getInt("CustomModelData") == 1;
                boolean piercing = false;
                if (stack.isIn(ModTags.Items.KH_WEAPONS_PIERCING)) piercing = comboCount % getPiercingAnimation() == getAnimation() - 1;
                if (stack.isIn(ModTags.Items.KH_WEAPONS_ONLY_PIERCING)) piercing = true;

                if (bludgeoning) {
                    damage = getBludgeoningDamage(distance);
                } else if (piercing && stack.isIn(ModTags.Items.KH_WEAPONS_PIERCING)) {
                    damage = getPiercingDamage(distance);
                } else {
                    damage = getSlashingDamage(distance);
                }

                if (stack.isIn(ModTags.Items.KH_WEAPONS_DAMAGE_BEHIND)) {
                    Vec3d targetToAttacker = playerPos.subtract(target.getPos()).normalize();
                    Vec3d targetFacing = target.getRotationVec(1.0F).normalize();
                    boolean isBehind = targetFacing.dotProduct(targetToAttacker) < -0.5;

                    if (isBehind) {
                        damage *= 2;
                    }
                }

                if (stack.isIn(ModTags.Items.KH_WEAPONS_IGNORES_ARMOR)) {
                    target.setHealth(target.getHealth() - damage);
                    if (target.getHealth() <= 0.0F) {
                        target.onDeath(playerEntity.getDamageSources().playerAttack(playerEntity));
                    }
                    return;
                }

                if (stack.isIn(ModTags.Items.KH_WEAPONS_DISABLE_SHIELD) && target instanceof PlayerEntity targetEntity)
                    disableShield(targetEntity, 60);

                playerEntity.sendMessage(Text.literal("Damage: " + damage));
                entity.damage(playerEntity.getWorld().getDamageSources().playerAttack(playerEntity), damage);
            });
        }
        return true;
    }

    private void disableShield(PlayerEntity target, int ticks) {
        if (target.getOffHandStack().getItem() instanceof ShieldItem || target.getOffHandStack().isIn(ModTags.Items.KH_WEAPONS_SHIELD))
            target.getItemCooldownManager().set(target.getOffHandStack().getItem(), ticks);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return stack.isIn(ModTags.Items.KH_WEAPONS_SHIELD) ? UseAction.BLOCK : UseAction.NONE;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient && itemStack.isIn(ModTags.Items.KH_WEAPONS_BLUDGEONING) && user.isSneaking()) {
            int currentVariant = itemStack.getOrCreateNbt().getInt("CustomModelData");
            int newVariant = (currentVariant + 1) % 2;

            itemStack.getOrCreateNbt().putInt("CustomModelData", newVariant);
            return TypedActionResult.success(itemStack);
        }
        if (!itemStack.isIn(ModTags.Items.KH_WEAPONS_SHIELD)) return TypedActionResult.fail(itemStack);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (stack.isIn(ModTags.Items.KH_WEAPONS_BLUDGEONING))
            tooltip.add(Text.translatable("tooltip.knightsheraldry.shift-right_click"));
    }
}