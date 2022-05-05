package de.dafuqs.spectrum.registries.client;

import de.dafuqs.spectrum.energy.storage.SinglePigmentEnergyStorage;
import de.dafuqs.spectrum.items.ExperienceStorageItem;
import de.dafuqs.spectrum.items.magic.InkFlaskItem;
import de.dafuqs.spectrum.items.magic_items.EnderSpliceItem;
import de.dafuqs.spectrum.items.trinkets.AshenCircletItem;
import de.dafuqs.spectrum.registries.SpectrumItems;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

// Vanilla models see: ModelPredicateProviderRegistry
public class SpectrumItemPredicates {
	
	public static void registerClient() {
		registerBowPredicates(SpectrumItems.BEDROCK_BOW);
		registerCrossbowPredicates(SpectrumItems.BEDROCK_CROSSBOW);
		registerFishingRodPredicates(SpectrumItems.BEDROCK_FISHING_ROD);
		registerEnderSplicePredicates(SpectrumItems.ENDER_SPLICE);
		registerAnimatedWandPredicates(SpectrumItems.NATURES_STAFF);
		registerAnimatedWandPredicates(SpectrumItems.RADIANCE_STAFF);
		registerKnowledgeDropPredicates(SpectrumItems.KNOWLEDGE_GEM);
		registerAshenCircletPredicates(SpectrumItems.ASHEN_CIRCLET);
		registerSinglePigmentEnergyStorageItemPredicates(SpectrumItems.INK_FLASK);
	}
	
	private static void registerBowPredicates(BowItem bowItem) {
		FabricModelPredicateProviderRegistry.register(bowItem, new Identifier("pull"), (itemStack, world, livingEntity, i) -> {
			if (livingEntity == null) {
				return 0.0F;
			} else {
				return livingEntity.getActiveItem() != itemStack ? 0.0F : (float) (itemStack.getMaxUseTime() - livingEntity.getItemUseTimeLeft()) / 20.0F;
			}
		});
		FabricModelPredicateProviderRegistry.register(bowItem, new Identifier("pulling"), (itemStack, clientWorld, livingEntity, i) -> livingEntity != null && livingEntity.isUsingItem() && livingEntity.getActiveItem() == itemStack ? 1.0F : 0.0F);
	}
	
	private static void registerCrossbowPredicates(CrossbowItem crossbowItem) {
		FabricModelPredicateProviderRegistry.register(crossbowItem, new Identifier("pull"), (itemStack, clientWorld, livingEntity, i) -> {
			if (livingEntity == null) {
				return 0.0F;
			} else {
				return CrossbowItem.isCharged(itemStack) ? 0.0F : (float) (itemStack.getMaxUseTime() - livingEntity.getItemUseTimeLeft()) / (float) CrossbowItem.getPullTime(itemStack);
			}
		});
		
		FabricModelPredicateProviderRegistry.register(crossbowItem, new Identifier("pulling"), (itemStack, clientWorld, livingEntity, i) -> {
			return livingEntity != null && livingEntity.isUsingItem() && livingEntity.getActiveItem() == itemStack && !CrossbowItem.isCharged(itemStack) ? 1.0F : 0.0F;
		});
		
		FabricModelPredicateProviderRegistry.register(crossbowItem, new Identifier("charged"), (itemStack, clientWorld, livingEntity, i) -> {
			return livingEntity != null && CrossbowItem.isCharged(itemStack) ? 1.0F : 0.0F;
		});
		
		FabricModelPredicateProviderRegistry.register(crossbowItem, new Identifier("firework"), (itemStack, clientWorld, livingEntity, i) -> {
			return livingEntity != null && CrossbowItem.isCharged(itemStack) && CrossbowItem.hasProjectile(itemStack, Items.FIREWORK_ROCKET) ? 1.0F : 0.0F;
		});
	}
	
	private static void registerFishingRodPredicates(FishingRodItem fishingRodItem) {
		FabricModelPredicateProviderRegistry.register(fishingRodItem, new Identifier("cast"), (itemStack, clientWorld, livingEntity, i) -> {
			if (livingEntity == null) {
				return 0.0F;
			} else {
				boolean bl = livingEntity.getMainHandStack() == itemStack;
				boolean bl2 = livingEntity.getOffHandStack() == itemStack;
				if (livingEntity.getMainHandStack().getItem() instanceof FishingRodItem) {
					bl2 = false;
				}
				
				return (bl || bl2) && livingEntity instanceof PlayerEntity && ((PlayerEntity) livingEntity).fishHook != null ? 1.0F : 0.0F;
			}
		});
	}
	
	private static void registerEnderSplicePredicates(EnderSpliceItem enderSpliceItem) {
		FabricModelPredicateProviderRegistry.register(enderSpliceItem, new Identifier("bound"), (itemStack, clientWorld, livingEntity, i) -> {
			NbtCompound compoundTag = itemStack.getNbt();
			if (compoundTag != null && (compoundTag.contains("PosX") || compoundTag.contains("TargetPlayerUUID"))) {
				return 1.0F;
			} else {
				return 0.0F;
			}
		});
	}
	
	private static void registerAshenCircletPredicates(Item ashenCircletItem) {
		FabricModelPredicateProviderRegistry.register(ashenCircletItem, new Identifier("cooldown"), (itemStack, clientWorld, livingEntity, i) -> {
			if (livingEntity != null && AshenCircletItem.getCooldownTicks(itemStack, livingEntity.world) == 0) {
				return 0.0F;
			} else {
				return 1.0F;
			}
		});
	}
	
	private static void registerAnimatedWandPredicates(Item item) {
		FabricModelPredicateProviderRegistry.register(item, new Identifier("in_use"), (itemStack, clientWorld, livingEntity, i) -> {
			return (livingEntity != null && livingEntity.isUsingItem() && livingEntity.getActiveItem() == itemStack) ? 1.0F : 0.0F;
		});
	}
	
	private static void registerKnowledgeDropPredicates(Item item) {
		FabricModelPredicateProviderRegistry.register(item, new Identifier("stored_experience_10000"), (itemStack, clientWorld, livingEntity, i) -> {
			if (item instanceof ExperienceStorageItem) {
				return ExperienceStorageItem.getStoredExperience(itemStack) / 10000F;
			} else {
				return 0;
			}
		});
	}
	
	private static void registerSinglePigmentEnergyStorageItemPredicates(InkFlaskItem singlePigmentEnergyStorage) {
		FabricModelPredicateProviderRegistry.register(singlePigmentEnergyStorage, new Identifier("fill_state"), (itemStack, world, livingEntity, i) -> {
			SinglePigmentEnergyStorage storage = singlePigmentEnergyStorage.getEnergyStorage(itemStack);
			long current = storage.getCurrentTotal();
			if (current == 0) {
				return 0.0F;
			} else {
				long max = storage.getMaxTotal();
				return (float) ((double) current / (double) max);
			}
		});
	}
	
}