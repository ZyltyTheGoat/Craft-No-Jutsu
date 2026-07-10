package net.mcreator.naruto;

import net.mcreator.naruto.network.NarutoModVariables;

import java.util.LinkedHashSet;
import java.util.Set;

public final class JutsuLoadouts {
	public static final int SLOT_COUNT = 4;

	private JutsuLoadouts() {
	}

	public static Loadout get(String data, int slot) {
		if (slot < 0 || slot >= SLOT_COUNT) {
			return Loadout.EMPTY;
		}
		String[] entries = splitSlots(data);
		if (slot >= entries.length || entries[slot].isEmpty()) {
			return Loadout.EMPTY;
		}
		return parseEntry(entries[slot]);
	}

	public static String set(String data, int slot, Loadout loadout) {
		if (slot < 0 || slot >= SLOT_COUNT) {
			return normalize(data);
		}
		String[] entries = normalizedSlots(data);
		Loadout safeLoadout = loadout == null ? Loadout.EMPTY : loadout;
		entries[slot] = safeLoadout.isEmpty() ? "" : safeLoadout.serialize();
		return join(entries);
	}

	public static String clear(String data, int slot) {
		return set(data, slot, Loadout.EMPTY);
	}

	public static String normalize(String data) {
		return join(normalizedSlots(data));
	}

	public static Loadout fromVariables(NarutoModVariables.PlayerVariables vars) {
		if (vars == null) {
			return Loadout.EMPTY;
		}
		return new Loadout(vars.activeJutsu, vars.favouriteJutsu, vars.activeToggleJutsu);
	}

	private static Loadout parseEntry(String entry) {
		String[] parts = entry == null ? new String[0] : entry.split("~", -1);
		String active = parts.length > 0 ? sanitize(parts[0]) : "";
		String favourites = parts.length > 1 ? sanitizeCsv(parts[1]) : "";
		String toggles = parts.length > 2 ? sanitizeCsv(parts[2]) : "";
		return new Loadout(active, favourites, toggles);
	}

	private static String[] normalizedSlots(String data) {
		String[] source = splitSlots(data);
		String[] slots = new String[SLOT_COUNT];
		for (int i = 0; i < SLOT_COUNT; i++) {
			Loadout loadout = i < source.length ? parseEntry(source[i]) : Loadout.EMPTY;
			slots[i] = loadout.isEmpty() ? "" : loadout.serialize();
		}
		return slots;
	}

	private static String[] splitSlots(String data) {
		return data == null || data.isEmpty() ? new String[0] : data.split(";", -1);
	}

	private static String join(String[] slots) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < SLOT_COUNT; i++) {
			if (i > 0) {
				builder.append(';');
			}
			if (slots != null && i < slots.length && slots[i] != null) {
				builder.append(slots[i]);
			}
		}
		return builder.toString();
	}

	private static String sanitizeCsv(String csv) {
		Set<String> ids = new LinkedHashSet<>();
		for (String id : JutsuIdList.toSet(csv)) {
			String safe = sanitize(id);
			if (!safe.isEmpty()) {
				ids.add(safe);
			}
		}
		return JutsuIdList.join(ids);
	}

	private static String sanitize(String value) {
		return value == null ? "" : value.trim().replace(";", "").replace("~", "").replace("|", "").replace("=", "");
	}

	public record Loadout(String activeJutsu, String favouriteJutsu, String activeToggleJutsu) {
		public static final Loadout EMPTY = new Loadout("", "", "");

		private String serialize() {
			return sanitize(activeJutsu) + "~" + sanitizeCsv(favouriteJutsu) + "~" + sanitizeCsv(activeToggleJutsu);
		}

		public boolean isEmpty() {
			return sanitize(activeJutsu).isEmpty() && sanitizeCsv(favouriteJutsu).isEmpty() && sanitizeCsv(activeToggleJutsu).isEmpty();
		}
	}
}
