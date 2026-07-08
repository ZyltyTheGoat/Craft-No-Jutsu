package net.mcreator.naruto.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import net.mcreator.naruto.network.NarutoModVariables;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;

public class CommandUnlockKekkeiGenkaiProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		{
			NarutoModVariables.PlayerVariables _vars = (commandParameterEntity(arguments, "name")).getData(NarutoModVariables.PLAYER_VARIABLES);
			_vars.unlockedKekkeiGenkai = (commandParameterEntity(arguments, "name")).getData(NarutoModVariables.PLAYER_VARIABLES).unlockedKekkeiGenkai + "" + StringArgumentType.getString(arguments, "kekkei_genkai") + ",";
			_vars.markSyncDirty();
		}
	}

	private static Entity commandParameterEntity(CommandContext<CommandSourceStack> arguments, String parameter) {
		try {
			return EntityArgument.getEntity(arguments, parameter);
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}