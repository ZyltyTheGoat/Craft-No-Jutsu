package net.mcreator.naruto.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.entity.ShinobiCloneEntity.CloneType;
import net.mcreator.naruto.JutsuRegistry;
import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.DojutsuStateHelper;

public class UseAbilityOnKeyPressedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.level().isClientSide())
			return;
		double chakraCost = 0;
		JutsuRegistry.initializeJutsus();
		JutsuData jutsuData = JutsuRegistry.getJutsu(entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeJutsu);
		if (jutsuData == null)
			return;
		String activeJutsu = entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeJutsu;
		if (DojutsuStateHelper.isDojutsu(jutsuData)) {
			NarutoModVariables.PlayerVariables vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
			vars.activeDojutsu = jutsuData.getNature();
			vars.activeToggleJutsu = DojutsuStateHelper.ensureActive(vars.activeToggleJutsu, jutsuData.getNature());
			vars.markSyncDirty();
		}
		if (entity instanceof Player player && !player.level().isClientSide()) {
			int remaining = JutsuCooldowns.getRemainingTicks(player, activeJutsu);
			if (remaining > 0) {
				clearCharge(player);
				JutsuFeedback.showCooldown(player, jutsuData.getName(), remaining);
				return;
			}
		}
		if ("water_prison".equals(activeJutsu) && WaterPrisonAbility.tryUse(entity, jutsuData)) {
			return;
		}
		if ("body_flicker".equals(activeJutsu) && BodyFlickerAbility.tryUse(entity, jutsuData)) {
			return;
		}
		if ("substitution".equals(activeJutsu) && SubstitutionAbility.tryPrime(entity, jutsuData)) {
			return;
		}
		if ("wind_blade".equals(activeJutsu) && BladeOfWindAbility.tryUse(entity, jutsuData)) {
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
		chakraCost = jutsuData.getChakraCost();
		if (entity.getData(NarutoModVariables.PLAYER_VARIABLES).currentChakra - chakraCost >= 0) {
			{
				NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
				_vars.chargeTime = 0;
				_vars.chargingAbility = true;
				_vars.markSyncDirty();
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				JutsuFeedback.notEnoughChakra(_player);
		}
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}
}
