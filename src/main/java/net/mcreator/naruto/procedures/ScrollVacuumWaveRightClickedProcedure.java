package net.mcreator.naruto.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;

import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.init.NarutoModItems;

public class ScrollVacuumWaveRightClickedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (!entity.getData(NarutoModVariables.PLAYER_VARIABLES).unlockedJutsu.contains("vacuum_wave")) {
			if (entity instanceof Player _player) {
				ItemStack _stktoremove = new ItemStack(NarutoModItems.SCROLL_VACUUM_WAVE.get());
				_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
			}
			{
				NarutoModVariables.PlayerVariables _vars = entity.getData(NarutoModVariables.PLAYER_VARIABLES);
				_vars.unlockedJutsu = entity.getData(NarutoModVariables.PLAYER_VARIABLES).unlockedJutsu + "vacuum_wave,";
				_vars.unlockedNatures = entity.getData(NarutoModVariables.PLAYER_VARIABLES).unlockedNatures.contains("WIND")
						? entity.getData(NarutoModVariables.PLAYER_VARIABLES).unlockedNatures
						: entity.getData(NarutoModVariables.PLAYER_VARIABLES).unlockedNatures + "WIND,";
				_vars.markSyncDirty();
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("You already know this jutsu!"), true);
		}
	}
}