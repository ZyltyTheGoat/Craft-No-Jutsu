package net.mcreator.naruto;

import java.util.LinkedHashSet;
import java.util.Set;

public final class JutsuIdList {
	private JutsuIdList() {
	}

	public static boolean has(String csv, String id) {
		if (id == null || id.trim().isEmpty()) {
			return false;
		}
		return toSet(csv).contains(id.trim());
	}

	public static String add(String csv, String id) {
		if (id == null || id.trim().isEmpty()) {
			return normalize(csv);
		}
		Set<String> ids = toSet(csv);
		ids.add(id.trim());
		return join(ids);
	}

	public static String remove(String csv, String id) {
		if (id == null || id.trim().isEmpty()) {
			return normalize(csv);
		}
		Set<String> ids = toSet(csv);
		ids.remove(id.trim());
		return join(ids);
	}

	public static Set<String> toSet(String csv) {
		Set<String> ids = new LinkedHashSet<>();
		if (csv == null || csv.trim().isEmpty()) {
			return ids;
		}
		for (String part : csv.split(",")) {
			String id = part.trim();
			if (!id.isEmpty()) {
				ids.add(id);
			}
		}
		return ids;
	}

	public static String join(Set<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return "";
		}
		return String.join(",", ids);
	}

	private static String normalize(String csv) {
		return join(toSet(csv));
	}
}
