package net.mcreator.naruto.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.naruto.network.NarutoModVariables;

public class ReturnCurrentJutsuProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return entity.getData(NarutoModVariables.PLAYER_VARIABLES).activeJutsu;
	}
}