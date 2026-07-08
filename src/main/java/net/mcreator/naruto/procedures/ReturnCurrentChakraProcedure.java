package net.mcreator.naruto.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.naruto.network.NarutoModVariables;

public class ReturnCurrentChakraProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return new java.text.DecimalFormat("##").format(entity.getData(NarutoModVariables.PLAYER_VARIABLES).currentChakra) + "/" + new java.text.DecimalFormat("##").format(entity.getData(NarutoModVariables.PLAYER_VARIABLES).maxChakra);
	}
}