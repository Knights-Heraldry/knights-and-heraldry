package com.knightsheraldry.items.custom.item;

public class Dagger extends KHWeapons {
    public Dagger(float attackSpeed, Settings settings) {
        super(attackSpeed, settings);
    }

    @Override
    public float[] getDefaultAttackDamageValues() {
        return new float[] {
                0.0F, 6.0F, 9.0F, 6.0F, 3.0F, //Slashing
                0.0F, 3.0F, 4.5F, 3.0F, 1.5F, //Piercing
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F //Bludgeoning
        };
    }

    @Override
    public double[] getDefaultRadiusValues() {
        return new double[] {
                // Values cannot be higher or equal than its next value
                0.0d, //1st Distance
                0.5d, //2nd Distance
                0.8d, //3rd Distance
                0.9d, //4th Distance
                1.0d //5th Distance
        };
    }

    @Override
    public int getAnimation() {
        // How many animations has your item
        return 3;
    }

    @Override
    public int[] getPiercingAnimation() {
        // In which animation is Piercing Damage applied
        // Item needs to have Tag KH_WEAPONS_PIERCING
        // Max length of index is 2
        return new int[] {
                3
        };
    }
}