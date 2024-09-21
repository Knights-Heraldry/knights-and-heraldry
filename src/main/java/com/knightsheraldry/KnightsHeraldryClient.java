package com.knightsheraldry;

import com.knightsheraldry.client.UnderArmourRenderer;
import com.knightsheraldry.client.WarDartRenderer;
import com.knightsheraldry.entity.ModEntities;
import com.knightsheraldry.event.AttackCancelHandler;
import com.knightsheraldry.items.ModItems;
import com.knightsheraldry.items.custom.armor.GambesonItem;
import com.knightsheraldry.items.custom.armor.KHTrinketsItem;
import com.knightsheraldry.items.custom.armor.MailItem;
import com.knightsheraldry.items.custom.item.KHWeapons;
import com.knightsheraldry.networking.ModMessages;
import dev.emi.trinkets.api.client.TrinketRenderer;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class KnightsHeraldryClient implements ClientModInitializer {
    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        ModMessages.registerS2CPackets();
        ClientPreAttackCallback.EVENT.register(new AttackCancelHandler());
        registerModelPredicate(ModItems.RAPIER, ModItems.SWORD, ModItems.V_SWORD, ModItems.ARMING_SWORD, ModItems.AXE,
                ModItems.BROAD_AXE, ModItems.CROOKED_AXE, ModItems.STRAIGHT_CROOKED_AXE, ModItems.MACE,
                ModItems.SPIKED_MACE, ModItems.HAMMER, ModItems.WAR_HAMMER, ModItems.LONGSWORD, ModItems.V_LONGSWORD,
                ModItems.FALCHION, ModItems.SCIMITAR, ModItems.KATANA, ModItems.PITCHFORK, ModItems.SPEAR, ModItems.PIKE,
                ModItems.BILLHOOK, ModItems.GLAIVE, ModItems.CURVED_GLAIVE, ModItems.HALBERD, ModItems.LANCE,
                ModItems.WOODEN_LANCE, ModItems.POLEAXE, ModItems.POLEHAMMER, ModItems.BEC_DE_CORBIN,
                ModItems.MORNING_STAR, ModItems.BARDICHE, ModItems.WARSWORD, ModItems.WARSWORD_CLAYMORE,
                ModItems.WARSWORD_FLAMBERGE, ModItems.WARSWORD_ZWEIHANDER, ModItems.WARDART);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) ->
                        tintIndex > 0 ? -1 : ((DyeableItem) stack.getItem()).getColor(stack), ModItems.WOODEN_LANCE);

        EntityRendererRegistry.register(ModEntities.WARDART_PROJECTILE, WarDartRenderer::new);
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            removeAttackDamage(stack, lines);
            onTooltip(stack, lines);
        });

        ModItems.items.forEach(item -> {
            if (item instanceof KHTrinketsItem) {
                TrinketRendererRegistry.registerRenderer(item, (TrinketRenderer) item);
            }
            if (item instanceof MailItem) {
                ArmorRenderer.register(new UnderArmourRenderer(), item);
            }
            if (item instanceof GambesonItem) {
                ArmorRenderer.register(new UnderArmourRenderer(), item);
            }
        });
    }

    private void onTooltip(ItemStack itemStack, List<Text> texts) {
        if (itemStack.getItem() instanceof KHTrinketsItem khTrinketsItem
                && khTrinketsItem.getHungerDrainAddition() != 0.0d) {
            double hungerDrainAddition = khTrinketsItem.getHungerDrainAddition();
            texts.add(Text.literal("+" + ((int) (hungerDrainAddition * 100)) + "% Hunger Drain").formatted(Formatting.BLUE));
        }
        if (itemStack.getItem() instanceof GambesonItem) {
            texts.add(Text.literal("+4% Slashing Resistance").formatted(Formatting.BLUE));
            texts.add(Text.literal("+10% Bludgeoning Resistance").formatted(Formatting.BLUE));
        }
        if (itemStack.getItem() instanceof MailItem) {
            texts.add(Text.literal("+10% Slashing Resistance").formatted(Formatting.BLUE));
            texts.add(Text.literal("+4% Bludgeoning Resistance").formatted(Formatting.BLUE));
        }
    }

    private void registerModelPredicate(Item... items) {
        for (Item item : items) {
            ModelPredicateProviderRegistry.register(item, new Identifier("charged"),
                    (stack, world, entity, seed) -> entity != null
                            && (entity.getMainHandStack() == stack || entity.getOffHandStack() == stack)
                            && stack.getNbt() != null
                            && stack.getNbt().getBoolean("Charged") ? 1.0F : 0.0F);
            ModelPredicateProviderRegistry.register(item, new Identifier("blocking"),
                    (stack, world, entity, seed) -> entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0F : 0.0F);
        }
    }

    private void removeAttackDamage(ItemStack itemStack, List<Text> tooltip) {
        if (itemStack.getItem() instanceof KHWeapons || itemStack.getItem() == ModItems.LANCE
                || itemStack.getItem() == ModItems.WOODEN_LANCE)
            tooltip.removeIf(line -> line.getString().contains("Attack Damage"));
    }
}