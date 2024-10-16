package com.knightsheraldry.items.custom.item;

import com.knightsheraldry.entity.custom.KHArrowEntity;
import com.knightsheraldry.util.KHDamageCalculator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.Optional;

public class KHRangeWeapons extends Item {
    private final KHDamageCalculator.DamageType damageType;
    private final float damage;
    private final double blockRange;
    private final UseAction useAction;
    private final int maxUseTime;
    private final int animationTime;

    public KHRangeWeapons(Settings settings, KHDamageCalculator.DamageType damageType, float damage, double blockRange,
                          UseAction useAction, int maxUseTime) {
        super(settings);
        this.damageType = damageType;
        this.damage = damage;
        this.blockRange = blockRange;
        this.useAction = useAction;
        this.maxUseTime = maxUseTime;
        this.animationTime = 0;
    }

    public KHRangeWeapons(Settings settings, KHDamageCalculator.DamageType damageType, float damage, double blockRange,
                          UseAction useAction, int maxUseTime, int animationTime) {
        super(settings);
        this.damageType = damageType;
        this.damage = damage;
        this.blockRange = blockRange;
        this.useAction = useAction;
        this.maxUseTime = maxUseTime;
        this.animationTime = animationTime;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return useAction == UseAction.BOW ? useAction : UseAction.NONE;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        boolean hasArrow = getArrowFromInventory(user).isPresent();

        if (useAction == UseAction.BOW && hasArrow) {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(itemStack);
        }

        if (useAction == UseAction.CROSSBOW && hasArrow) {
            return handleCrossbowUse(world, user, hand, itemStack);
        }

        return TypedActionResult.fail(itemStack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return maxUseTime;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient || !(user instanceof PlayerEntity player)) return;
        int useTime = getMaxUseTime(stack) - remainingUseTicks;
        float crossbowPullProgress = getCrossbowPullProgress(useTime);

        if (useAction == UseAction.CROSSBOW && crossbowPullProgress >= 1.0F && !isCharged(stack)) {
            getArrowFromInventory(player).ifPresent(arrowStack -> {
                loadAndPlayCrossbowSound(world, stack, player, arrowStack);
            });
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient || !(user instanceof PlayerEntity player)) return;

        int useTime = getMaxUseTime(stack) - remainingUseTicks;

        getArrowFromInventory(player).ifPresent(arrowStack -> {
            float bowPullProgress = getBowPullProgress(useTime);
            if (useAction == UseAction.BOW && bowPullProgress > 0.1f) {
                shootArrow(world, stack, player, arrowStack, bowPullProgress);
            }

            float crossbowPullProgress = getCrossbowPullProgress(useTime);
            if (useAction == UseAction.CROSSBOW && crossbowPullProgress < 1.0F) {
                setReload(stack, false);
            }
        });
    }

    private TypedActionResult<ItemStack> handleCrossbowUse(World world, PlayerEntity user, Hand hand, ItemStack itemStack) {
        Optional<ItemStack> arrowStackOpt = getArrowFromInventory(user);
        if (arrowStackOpt.isPresent() && arrowStackOpt.get().getItem() instanceof KHArrow khArrow) {
            KHArrowEntity arrowEntity = (KHArrowEntity) khArrow.createArrowEntity(user, world);

            if (isCharged(itemStack)) {
                setCharged(itemStack, false);
                setShooting(itemStack, true);
                unloadProjectiles(itemStack, arrowEntity);
                shootArrow(world, itemStack, user, khArrow.getDefaultStack(), 1f);
                return TypedActionResult.consume(itemStack);
            } else if (!isLoaded(itemStack, arrowEntity)) {
                setShooting(itemStack, false);
                setReload(itemStack, true);
                user.setCurrentHand(hand);
                return TypedActionResult.consume(itemStack);
            } else {
                return TypedActionResult.fail(itemStack);
            }
        }
        return TypedActionResult.fail(itemStack);
    }

    private void shootArrow(World world, ItemStack stack, PlayerEntity player, ItemStack arrowStack, float pullProgress) {
        KHArrowEntity arrowEntity = (KHArrowEntity) ((KHArrow) arrowStack.getItem()).createArrowEntity(player, world);
        arrowEntity.setDamageAmount(this.damage);
        arrowEntity.setDamageType(this.damageType);

        float velocityMultiplier = (float) (this.blockRange / 18.0d);
        arrowEntity.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, pullProgress * velocityMultiplier, 1.0F);

        if (player.getAbilities().creativeMode) {
            stack.damage(1, player, p -> p.sendToolBreakStatus(player.getActiveHand()));
            arrowEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
        }

        world.spawnEntity(arrowEntity);
        world.playSoundFromEntity(null, arrowEntity, SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);

        if (!player.getAbilities().creativeMode) {
            player.getInventory().removeStack(getArrowSlot(player), 1);
        }
    }

    private void loadAndPlayCrossbowSound(World world, ItemStack stack, PlayerEntity player, ItemStack arrowStack) {
        KHArrowEntity arrowEntity = (KHArrowEntity) ((KHArrow) arrowStack.getItem()).createArrowEntity(player, world);
        loadProjectiles(stack, arrowEntity);
        setReload(stack, false);
        setCharged(stack, true);

        SoundCategory soundCategory = player instanceof PlayerEntity ? SoundCategory.PLAYERS : SoundCategory.HOSTILE;
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_CROSSBOW_LOADING_END, soundCategory, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
    }

    private Optional<ItemStack> getArrowFromInventory(PlayerEntity player) {
        return player.getInventory().main.stream()
                .filter(stack -> stack.getItem() instanceof KHArrow)
                .findFirst();
    }

    private int getArrowSlot(PlayerEntity player) {
        return player.getInventory().main.stream()
                .filter(stack -> stack.getItem() instanceof KHArrow)
                .map(player.getInventory().main::indexOf)
                .findFirst().orElse(-1);
    }

    public static float getBowPullProgress(int useTicks) {
        float f = useTicks / 20.0F;
        return Math.min((f * f + f * 2.0F) / 3.0F, 1.0F);
    }

    public float getCrossbowPullProgress(int useTicks) {
        float f = (float)useTicks / this.animationTime;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    private static void setBooleanTag(ItemStack stack, String key, boolean value) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(key, value);
    }

    private static boolean getBooleanTag(ItemStack stack, String key) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(key);
    }

    public static boolean isReloading(ItemStack stack) {
        return getBooleanTag(stack, "Reload");
    }

    public static void setReload(ItemStack stack, boolean reload) {
        setBooleanTag(stack, "Reload", reload);
    }

    public static boolean isCharged(ItemStack stack) {
        return getBooleanTag(stack, "Charged");
    }

    public static void setCharged(ItemStack stack, boolean charged) {
        setBooleanTag(stack, "Charged", charged);
    }

    public static boolean isShooting(ItemStack stack) {
        return getBooleanTag(stack, "Shoot");
    }

    public static void setShooting(ItemStack stack, boolean shoot) {
        setBooleanTag(stack, "Shoot", shoot);
    }

    public static void loadProjectiles(ItemStack itemStack, KHArrowEntity arrowEntity) {
        itemStack.getOrCreateNbt().putInt(arrowEntity.getEntityName(), 1);
    }

    public static void unloadProjectiles(ItemStack itemStack, KHArrowEntity arrowEntity) {
        itemStack.getOrCreateNbt().putInt(arrowEntity.getEntityName(), 0);
    }

    public static boolean isLoaded(ItemStack stack, KHArrowEntity arrowEntity) {
        return stack.getNbt() != null && stack.getNbt().getInt(arrowEntity.getEntityName()) == 1;
    }

    public float getDamage() {
        return this.damage;
    }
}