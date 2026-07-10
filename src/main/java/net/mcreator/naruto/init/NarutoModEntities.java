/*
*    MCreator note: This file will be REGENERATED on each build.
*/
package net.mcreator.naruto.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;

import net.mcreator.naruto.entity.ShinobiCloneEntity;
import net.mcreator.naruto.entity.PhoenixFlowerProjectileEntity;
import net.mcreator.naruto.entity.GreatFireballEntity;
import net.mcreator.naruto.entity.ElementalBulletEntity;
import net.mcreator.naruto.entity.DummyEntity;
import net.mcreator.naruto.NarutoMod;

@EventBusSubscriber
public class NarutoModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, NarutoMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<DummyEntity>> DUMMY = register("dummy",
			EntityType.Builder.<DummyEntity>of(DummyEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(3).ridingOffset(-0.6f).sized(0.6f, 1.8f));
	// Start of user code block custom entities
	public static final DeferredHolder<EntityType<?>, EntityType<GreatFireballEntity>> GREAT_FIREBALL = register("great_fireball",
			EntityType.Builder.<GreatFireballEntity>of(GreatFireballEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(96).setUpdateInterval(1).sized(1.35F, 1.35F));
	public static final DeferredHolder<EntityType<?>, EntityType<PhoenixFlowerProjectileEntity>> PHOENIX_FLOWER_PROJECTILE = register("phoenix_flower_projectile",
			EntityType.Builder.<PhoenixFlowerProjectileEntity>of(PhoenixFlowerProjectileEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(96).setUpdateInterval(1).sized(0.45F, 0.45F));
	public static final DeferredHolder<EntityType<?>, EntityType<ElementalBulletEntity>> ELEMENTAL_BULLET = register("elemental_bullet",
			EntityType.Builder.<ElementalBulletEntity>of(ElementalBulletEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(96).setUpdateInterval(1).sized(0.42F, 0.42F));
	public static final DeferredHolder<EntityType<?>, EntityType<ShinobiCloneEntity>> SHINOBI_CLONE = register("shinobi_clone",
			EntityType.Builder.<ShinobiCloneEntity>of(ShinobiCloneEntity::new, MobCategory.CREATURE).setShouldReceiveVelocityUpdates(true).setTrackingRange(96).setUpdateInterval(2).sized(0.6F, 1.8F));

	// End of user code block custom entities
	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(RegisterSpawnPlacementsEvent event) {
		DummyEntity.init(event);
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(DUMMY.get(), DummyEntity.createAttributes().build());
	}
}