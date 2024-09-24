
package com.knightsheraldry.items.custom.armor;

import com.google.common.collect.Multimap;
import com.knightsheraldry.KnightsHeraldry;
import com.knightsheraldry.model.TrinketsBootsModel;
import com.knightsheraldry.model.TrinketsChestplateModel;
import com.knightsheraldry.model.TrinketsHelmetModel;
import com.knightsheraldry.model.TrinketsLeggingsModel;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class KHTrinketsItem extends TrinketItem implements TrinketRenderer {
    protected final Type type;
    protected final double armor;
    protected final double toughness;
    protected final double hungerDrainAddition;

    public KHTrinketsItem(Settings settings, Type type, double armor, double toughness, double hungerDrainAddition) {
        super(settings);
        this.type = type;
        this.armor = armor;
        this.toughness = toughness;
        this.hungerDrainAddition = hungerDrainAddition;
    }

    public double getHungerDrainAddition() {
        return this.hungerDrainAddition;
    }

    private BipedEntityModel<LivingEntity> model;

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, UUID uuid) {
        var modifiers = super.getModifiers(stack, slot, entity, uuid);
        modifiers.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(uuid, KnightsHeraldry.MOD_ID + ":" + "protection", this.armor, EntityAttributeModifier.Operation.ADDITION));
        modifiers.put(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(uuid, KnightsHeraldry.MOD_ID + ":" + "protection", this.toughness, EntityAttributeModifier.Operation.ADDITION));
        return modifiers;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        BipedEntityModel<LivingEntity> model = this.getModel();
        model.setAngles(entity, limbAngle, limbDistance, animationProgress, animationProgress, headPitch);
        model.animateModel(entity, limbAngle, limbDistance, tickDelta);
        TrinketRenderer.followBodyRotations(entity, model);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(new Identifier(KnightsHeraldry.MOD_ID, "textures/entity/trinket/" + stack.getItem() + ".png")));
        model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
    }

    @Environment(EnvType.CLIENT)
    private BipedEntityModel<LivingEntity> getModel() {
        if (this.model == null) {
            switch (this.type) {
                case HELMET ->
                        this.model = new TrinketsHelmetModel(TrinketsChestplateModel.getTexturedModelData().createModel());
                case CHESTPLATE ->
                        this.model = new TrinketsChestplateModel(TrinketsChestplateModel.getTexturedModelData().createModel());
                case LEGGINGS ->
                        this.model = new TrinketsLeggingsModel(TrinketsChestplateModel.getTexturedModelData().createModel());
                case BOOTS ->
                        this.model = new TrinketsBootsModel(TrinketsChestplateModel.getTexturedModelData().createModel());
                default -> KnightsHeraldry.LOGGER.error("Don't specified Model or not a Valid Model");
            }
        }

        return this.model;
    }

    public enum Type {
        HELMET("helmet"),
        CHESTPLATE("chestplate"),
        LEGGINGS("leggings"),
        BOOTS("boots");

        private final String model;

        Type(String model) {
            this.model = model;
        }

        public String getName() {
            return this.model;
        }
    }
}