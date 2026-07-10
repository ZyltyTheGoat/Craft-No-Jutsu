package net.mcreator.naruto.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.entity.ShinobiCloneEntity.CloneType;
import net.mcreator.naruto.entity.ElementalBulletEntity;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.JutsuData;

public class UseAbilityOnKeyReleasedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.level().isClientSide()) {
			clearCharge(entity);
			return;
		}
		NarutoModVariables.PlayerVariables vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
		if ("water_prison".equals(vars.activeJutsu) && WaterPrisonAbility.release(entity)) {
			return;
		}
		if (vars.chargingAbility || vars.chargeTime > 0) {
			JutsuRegistry.initializeJutsus();
			String activeJutsu = vars.activeJutsu;
			JutsuData jutsuData = JutsuRegistry.getJutsu(activeJutsu);
			if ("fireball".equals(activeJutsu) && GreatFireballAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("phoenix_flower".equals(activeJutsu) && PhoenixFlowerAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("water_bullet".equals(activeJutsu) && ElementalBulletAbility.tryRelease(entity, jutsuData, ElementalBulletEntity.ELEMENT_WATER, activeJutsu)) {
				return;
			}
			if ("air_bullets".equals(activeJutsu) && ElementalBulletAbility.tryRelease(entity, jutsuData, ElementalBulletEntity.ELEMENT_WIND, activeJutsu)) {
				return;
			}
			if ("chidori".equals(activeJutsu) && ChidoriAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("earth_wall".equals(activeJutsu) && EarthWallAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("earth_flow_spears".equals(activeJutsu) && EarthFlowSpearsAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("great_breakthrough".equals(activeJutsu) && GreatBreakthroughAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("vacuum_wave".equals(activeJutsu) && VacuumWaveAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("severing_wave".equals(activeJutsu) && SeveringWaveAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("shadow_clone".equals(activeJutsu) && CloneJutsuAbility.tryUse(entity, jutsuData, CloneType.SHADOW, activeJutsu)) {
				return;
			}
			if ("water_clone".equals(activeJutsu) && CloneJutsuAbility.tryUse(entity, jutsuData, CloneType.WATER, activeJutsu)) {
				return;
			}
			if ("lightning_clone".equals(activeJutsu) && CloneJutsuAbility.tryUse(entity, jutsuData, CloneType.LIGHTNING, activeJutsu)) {
				return;
			}
			if ("wood_clone".equals(activeJutsu) && CloneJutsuAbility.tryUse(entity, jutsuData, CloneType.WOOD, activeJutsu)) {
				return;
			}
			if ("suicide_bombing_clone".equals(activeJutsu) && CloneJutsuAbility.tryUse(entity, jutsuData, CloneType.EXPLOSION, activeJutsu)) {
				return;
			}
			if ("false_darkness".equals(activeJutsu) && FalseDarknessAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("iron_sand_shower".equals(activeJutsu) && IronSandShowerAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("iron_sand_gathering_assault".equals(activeJutsu) && IronSandGatheringAssaultAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("needle_sphere".equals(activeJutsu) && NeedleSphereAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("solid_fog_jutsu".equals(activeJutsu) && SolidFogJutsuAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("sand_burial_prison".equals(activeJutsu) && SandBindingCoffinAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if ("fire_dragon_bullet".equals(activeJutsu) && FireDragonBulletAbility.tryRelease(entity, jutsuData)) {
				return;
			}
			if (("genjutsu".equals(activeJutsu) || "yin_genjutsu".equals(activeJutsu)) && GenjutsuAbility.tryRelease(entity, jutsuData, activeJutsu)) {
				return;
			}
			clearCharge(entity);
		}
	}

	private static void clearCharge(Entity entity) {
		NarutoModVariables.PlayerVariables vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
