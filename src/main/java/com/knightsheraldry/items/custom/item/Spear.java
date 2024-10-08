package com.knightsheraldry.items.custom.item;

public class Spear extends KHWeapons {
    public Spear(float attackSpeed, Settings settings) {
        super(attackSpeed, settings);
    }

    @Override
    public float[] getDefaultAttackDamageValues() {
        return new float[] {
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F, //Slashing
                0.0F, 6.0F, 9.0F, 6.0F, 3.0F, //Piercing
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F //Bludgeoning
        };
    }

    @Override
    public double[] getDefaultRadiusValues() {
        return new double[] {
                // Values cannot be higher or equal than its next value
                4.4d, //1st Distance
                5.1d, //2nd Distance
                5.9d, //3rd Distance
                6.7d, //4th Distance
                7.5d  //5th Distance
        };
    }
}