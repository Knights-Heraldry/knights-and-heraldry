package com.knightsheraldry.client;

import com.knightsheraldry.KnightsHeraldry;
import com.knightsheraldry.items.custom.KHWeapons;
import com.knightsheraldry.util.ModTags;
import com.mojang.blaze3d.systems.RenderSystem;
import net.bettercombat.logic.PlayerAttackProperties;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

public class DistanceCrosshairOverlay implements HudRenderCallback {
    private static final Identifier TOO_FAR_CLOSE = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/too_far_close.png");
    private static final Identifier SLASHING_EFFECTIVE = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/slashing_effective.png");
    private static final Identifier SLASHING_CRITICAL = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/slashing_critical.png");
    private static final Identifier SLASHING_MAXIMUM = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/slashing_maximum.png");
    private static final Identifier BLUDGEONING_EFFECTIVE = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/bludgeoning_effective.png");
    private static final Identifier BLUDGEONING_CRITICAL = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/bludgeoning_critical.png");
    private static final Identifier BLUDGEONING_MAXIMUM = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/bludgeoning_maximum.png");
    private static final Identifier PIERCING_EFFECTIVE = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/piercing_effective.png");
    private static final Identifier PIERCING_CRITICAL = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/piercing_critical.png");
    private static final Identifier PIERCING_MAXIMUM = new Identifier(KnightsHeraldry.MOD_ID, "textures/overlay/piercing_maximum.png");

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || player.getWorld() == null) {
            return;
        }

        Vec3d playerPos = player.getPos();
        double closestDistance = Double.MAX_VALUE;

        double distance;
        if (MinecraftClient.getInstance().targetedEntity == null) distance = 9999;
        else distance = playerPos.distanceTo(MinecraftClient.getInstance().targetedEntity.getPos());
        if (distance < closestDistance) {
            closestDistance = distance;
        }

        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        KHWeapons weapon = null;

        if (mainHandStack.getItem() instanceof KHWeapons) {
            weapon = (KHWeapons) mainHandStack.getItem();
        } else if (offHandStack.getItem() instanceof KHWeapons) {
            weapon = (KHWeapons) offHandStack.getItem();
        }

        if (weapon != null) {
            boolean bludgeoning = player.getMainHandStack().getOrCreateNbt().getInt("CustomModelData") == 1 ||
                    player.getMainHandStack().getOrCreateNbt().getInt("CustomModelData") == 1;
            if (weapon.getDefaultStack().isIn(ModTags.Items.KH_WEAPONS_BLUDGEONING_PIERCING)) bludgeoning = !bludgeoning;
            int comboCount = ((PlayerAttackProperties) player).getComboCount();
            boolean piercing = false;
            if (weapon.getDefaultStack().isIn(ModTags.Items.KH_WEAPONS_PIERCING)) {
                int[] piercingAnimations = weapon.getPiercingAnimation();
                for (int piercingAnimation : piercingAnimations) {
                    if (comboCount == 0 && piercingAnimation == 1) {
                        piercing = true;
                        break;
                    }
                    if (piercingAnimations.length == 1 && comboCount % piercingAnimation == weapon.getAnimation() - 1) {
                        piercing = true;
                        break;
                    }

                    if (piercingAnimations.length == 2 && (comboCount - (piercingAnimation - 1)) % weapon.getAnimation() == 0) {
                        piercing = true;
                        break;
                    }
                }

                if (piercingAnimations.length == weapon.getAnimation()) piercing = true;
            }
            float[] damageValues = weapon.getDefaultAttackDamageValues();
            double[] radiusValues = weapon.getDefaultRadiusValues();

            if (bludgeoning || weapon.getDefaultStack().isIn(ModTags.Items.KH_WEAPONS_ONLY_BLUDGEONING)) { // Bludgeoning
                renderBludgeoningOverlay(drawContext, closestDistance, radiusValues, damageValues);
            } else if (piercing && weapon.getDefaultStack().isIn(ModTags.Items.KH_WEAPONS_PIERCING)
                    || weapon.getDefaultStack().isIn(ModTags.Items.KH_WEAPONS_BLUDGEONING_PIERCING)
                    || weapon.getDefaultStack().isIn(ModTags.Items.KH_WEAPONS_ONLY_PIERCING)) { // Piercing
                renderPiercingOverlay(drawContext, closestDistance, radiusValues, damageValues);
            } else { // Slashing (default case)
                renderSlashingOverlay(drawContext, closestDistance, radiusValues, damageValues);
            }
        }

    }

    private void renderBludgeoningOverlay(DrawContext drawContext, double distance, double[] radiusValues, float[] damageValues) {
        int x = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
        int y = MinecraftClient.getInstance().getWindow().getScaledHeight();
        Integer[] indices = {10, 11, 12, 13, 14};
        Arrays.sort(indices, (i1, i2) -> Float.compare(damageValues[i2], damageValues[i1]));

        Identifier[] textures = new Identifier[radiusValues.length];
        textures[indices[0] - 10] = BLUDGEONING_CRITICAL;
        textures[indices[1] - 10] = BLUDGEONING_EFFECTIVE;
        textures[indices[2] - 10] = BLUDGEONING_EFFECTIVE;
        textures[indices[3] - 10] = BLUDGEONING_MAXIMUM;
        textures[indices[4] - 10] = TOO_FAR_CLOSE;

        for (int i = 0; i < radiusValues.length; i++) {
            if (distance <= radiusValues[i] + 0.25F) {
                Identifier texture = textures[i];
                int width, height, xT, yT;
                if (texture == BLUDGEONING_CRITICAL) {
                    width = height = 9;
                    xT = yT = 5;
                } else if (texture == BLUDGEONING_EFFECTIVE) {
                    width = height = 9;
                    xT = yT = 5;
                } else if (texture == BLUDGEONING_MAXIMUM) {
                    width = height = 7;
                    xT = yT = 4;
                } else if (texture == TOO_FAR_CLOSE) {
                    width = height = 1;
                    xT = yT = 1;
                } else {
                    width = height = 9;
                    xT = yT = 5;
                }
                RenderSystem.setShaderTexture(0, texture);
                drawContext.drawTexture(texture, x - xT, y / 2 - yT, 0, 0, width, height, width, height);
                RenderSystem.enableDepthTest();
                return;
            }
        }
        RenderSystem.setShaderTexture(0, TOO_FAR_CLOSE);
        drawContext.drawTexture(TOO_FAR_CLOSE, x - 1, y / 2 - 1, 0, 0, 1, 1, 1, 1);
        RenderSystem.enableDepthTest();
    }

    private void renderPiercingOverlay(DrawContext drawContext, double distance, double[] radiusValues, float[] damageValues) {
        int x = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
        int y = MinecraftClient.getInstance().getWindow().getScaledHeight();
        Integer[] indices = {5, 6, 7, 8, 9};
        Arrays.sort(indices, (i1, i2) -> Float.compare(damageValues[i2], damageValues[i1]));

        Identifier[] textures = new Identifier[radiusValues.length];
        textures[indices[0] - 5] = PIERCING_CRITICAL;
        textures[indices[1] - 5] = PIERCING_EFFECTIVE;
        textures[indices[2] - 5] = PIERCING_EFFECTIVE;
        textures[indices[3] - 5] = PIERCING_MAXIMUM;
        textures[indices[4] - 5] = TOO_FAR_CLOSE;

        for (int i = 0; i < radiusValues.length; i++) {
            if (distance <= radiusValues[i] + 0.25F) {
                Identifier texture = textures[i];
                int width, height, xT, yT;
                if (texture == PIERCING_CRITICAL) {
                    width = height = 11;
                    xT = yT = 6;
                } else if (texture == PIERCING_EFFECTIVE) {
                    width = height = 11;
                    xT = yT = 6;
                } else if (texture == PIERCING_MAXIMUM) {
                    width = height = 7;
                    xT = yT = 4;
                } else if (texture == TOO_FAR_CLOSE) {
                    width = height = 1;
                    xT = yT = 1;
                } else {
                    width = height = 11;
                    xT = yT = 6;
                }
                RenderSystem.setShaderTexture(0, texture);
                drawContext.drawTexture(texture, x - xT, y / 2 - yT, 0, 0, width, height, width, height);
                RenderSystem.enableDepthTest();
                return;
            }
        }
        RenderSystem.setShaderTexture(0, TOO_FAR_CLOSE);
        drawContext.drawTexture(TOO_FAR_CLOSE, x - 1, y / 2 - 1, 0, 0, 1, 1, 1, 1);
        RenderSystem.enableDepthTest();
    }

    private void renderSlashingOverlay(DrawContext drawContext, double distance, double[] radiusValues, float[] damageValues) {
        int x = MinecraftClient.getInstance().getWindow().getScaledWidth() / 2;
        int y = MinecraftClient.getInstance().getWindow().getScaledHeight();
        Integer[] indices = {0, 1, 2, 3, 4};
        Arrays.sort(indices, (i1, i2) -> Float.compare(damageValues[i2], damageValues[i1]));

        Identifier[] textures = new Identifier[radiusValues.length];
        textures[indices[0]] = SLASHING_CRITICAL;
        textures[indices[1]] = SLASHING_EFFECTIVE;
        textures[indices[2]] = SLASHING_EFFECTIVE;
        textures[indices[3]] = SLASHING_MAXIMUM;
        textures[indices[4]] = TOO_FAR_CLOSE;

        boolean textureFound = false;
        for (int i = 0; i < radiusValues.length; i++) {
            if (distance <= radiusValues[i] + 0.25F) {
                Identifier texture = textures[i];
                int width, height, xT, yT;
                if (texture == SLASHING_CRITICAL) {
                    width = height = 9;
                    xT = yT = 5;
                } else if (texture == SLASHING_EFFECTIVE) {
                    width = height = 9;
                    xT = yT = 5;
                } else if (texture == SLASHING_MAXIMUM) {
                    width = height = 7;
                    xT = yT = 4;
                } else if (texture == TOO_FAR_CLOSE) {
                    width = height = 1;
                    xT = yT = 1;
                } else {
                    width = height = 9;
                    xT = yT = 5;
                }
                RenderSystem.setShaderTexture(0, texture);
                drawContext.drawTexture(texture, x - xT, y / 2 - yT, 0, 0, width, height, width, height);
                RenderSystem.enableDepthTest();
                textureFound = true;
                break;
            }
        }
        if (!textureFound) {
            RenderSystem.setShaderTexture(0, TOO_FAR_CLOSE);
            drawContext.drawTexture(TOO_FAR_CLOSE, x - 1, y / 2 - 1, 0, 0, 1, 1, 1, 1);
            RenderSystem.enableDepthTest();
        }
    }
}
