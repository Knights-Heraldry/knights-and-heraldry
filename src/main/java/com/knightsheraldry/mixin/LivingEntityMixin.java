package com.knightsheraldry.mixin;

import com.knightsheraldry.KnightsHeraldry;
import com.knightsheraldry.items.custom.item.KHWeapon;
import com.knightsheraldry.items.custom.item.Lance;
import com.knightsheraldry.util.itemdata.KHTags;
import com.knightsheraldry.util.playerdata.IEntityDataSaver;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private int stuckSwallowTailArrowTimer;

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void knightsheraldry$onJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object) this;
        if (entity instanceof PlayerEntity playerEntity
                && ((IEntityDataSaver) playerEntity).knightsheraldry$getPersistentData().getBoolean("stamina_blocked")) {
            ci.cancel();
        }
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void knightsheraldry$onSprinting(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object) this;
        if (entity instanceof PlayerEntity playerEntity
                && ((IEntityDataSaver) playerEntity).knightsheraldry$getPersistentData().getBoolean("stamina_blocked")) {
            ci.cancel();
        }
    }

    @Inject(method = "setCurrentHand", at = @At("HEAD"), cancellable = true)
    private void knightsheraldry$onSetCurrentHand(Hand hand, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object) this;
        if (entity instanceof PlayerEntity playerEntity
                && ((IEntityDataSaver) playerEntity).knightsheraldry$getPersistentData().getBoolean("stamina_blocked")) {
            ci.cancel();
        }
    }


    @Inject(method = "disablesShield", at = @At("HEAD"), cancellable = true)
    public void knightsheraldry$disablesShield(CallbackInfoReturnable<Boolean> cir) {
        ItemStack mainStack = ((LivingEntity) (Object) this).getMainHandStack();
        boolean isWeaponOrInTag = mainStack.getItem() instanceof AxeItem
                || mainStack.isIn(KHTags.WEAPONS_DISABLE_SHIELD.getTag());

        if (KnightsHeraldry.config().getVanillaWeaponsDamage0()) {
            cir.setReturnValue(mainStack.isIn(KHTags.WEAPONS_DISABLE_SHIELD.getTag()));
        } else {
            cir.setReturnValue(isWeaponOrInTag);
        }
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void knightsheraldry$injectDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() instanceof PlayerEntity attacker) {
            if (attacker.getMainHandStack().isIn(KHTags.WEAPONS_BYPASS_BLOCK.getTag())) {
                this.blockShield = false;
            }
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float knightsheraldry$modifyDamageAmount(float amount, DamageSource source) {
        if (source.getAttacker() instanceof PlayerEntity playerEntity) {
            if (amount > 1 && playerEntity.hasStatusEffect(StatusEffects.STRENGTH)) {
                int amplifier = playerEntity.getStatusEffect(StatusEffects.STRENGTH).getAmplifier();
                amount += (float) (3 * (amplifier + 1));
            }

            if (playerEntity.hasStatusEffect(StatusEffects.WEAKNESS)) {
                int amplifier = playerEntity.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier();
                amount -= (float) (4 * (amplifier + 1));
            }

            if (amount <= 0) amount = 0;
        }

        return amount;
    }

    @Inject(method = "applyDamage", at = @At("TAIL"))
    private void knightsheraldry$sendDamage(DamageSource source, float amount, CallbackInfo ci) {
        if (KnightsHeraldry.config().getDamageIndicator() && source.getAttacker() instanceof PlayerEntity playerEntity
                && playerEntity.getMainHandStack().getItem() instanceof KHWeapon) {
            if (!playerEntity.hasStatusEffect(StatusEffects.WEAKNESS)) {
                if (amount <= 0) amount = 0;
                else if (!(playerEntity.getMainHandStack().getItem() instanceof Lance)) amount = amount + 1;
            }
            playerEntity.sendMessage(Text.literal("Damage: " + (int) (amount)), true);
        }
    }

    @Unique
    private boolean blockShield = true;

    @Redirect(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;blockedByShield(Lnet/minecraft/entity/damage/DamageSource;)Z"))
    private boolean knightsheraldry$redirectBlockedByShield(LivingEntity instance, DamageSource source) {
        return blockShield && instance.blockedByShield(source);
    }


    @Inject(method = "tick", at = @At("HEAD"))
    public void knightsheraldry$tick(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object) this;
        if (entity instanceof PlayerEntity playerEntity && ((IEntityDataSaver) playerEntity).knightsheraldry$getPersistentData().getInt("swallowtail_arrow_count") >= 0) {
            int i = playerEntity.getStuckArrowCount();
            if (i > 0) {
                if (this.stuckSwallowTailArrowTimer <= 0) {
                    this.stuckSwallowTailArrowTimer = 20 * (30 - i);
                }

                --this.stuckSwallowTailArrowTimer;
                if (this.stuckSwallowTailArrowTimer <= 0) {
                    NbtCompound nbt = ((IEntityDataSaver) playerEntity).knightsheraldry$getPersistentData();
                    int swallowTailArrowCount = nbt.getInt("swallowtail_arrow_count");
                    swallowTailArrowCount = swallowTailArrowCount - 1;
                    nbt.putInt("swallowtail_arrow_count", swallowTailArrowCount);
                }
            }
        }
    }
}