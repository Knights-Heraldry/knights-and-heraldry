package com.knightsheraldry.items.custom.item;

public class Pike extends KHWeapons {
    public Pike(float attackSpeed, Settings settings) {
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
                5.5d, //1st Distance
                6.2d, //2nd Distance
                7.0d, //3rd Distance
                7.8d, //4th Distance
                8.5d  //5th Distance
        };
    }
}