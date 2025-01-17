package com.knightsheraldry.util.itemdata;

import com.knightsheraldry.util.KHDamageCalculator;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.UseAction;

public record RangeWeaponConfig(
        DamageSettings damageSettings,
        AmmoRequirement ammoRequirement,
        SoundSettings soundSettings,
        UseAction useAction,
        int rechargeTime,
        boolean needsFlintAndSteel
) {
    public record DamageSettings(KHDamageCalculator.DamageType damageType, int maxUseTime, float damage, float speed) { }

    public record AmmoRequirement(
            int amountFirstItem, Item firstItem, Item firstItem2nOption,
            int amountSecondItem, Item secondItem, Item secondItem2nOption,
            int amountThirdItem, Item thirdItem, Item thirdItem2nOption
    ) {}

    public record SoundSettings(SoundEvent... soundEvents) {}
}