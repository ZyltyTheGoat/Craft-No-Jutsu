package net.mcreator.naruto;

import java.util.Map;
import java.util.Set;

public final class DojutsuStateHelper {
	private DojutsuStateHelper() {
	}

	public static boolean isDojutsu(JutsuData jutsuData) {
		return jutsuData != null && "DOJUTSU".equalsIgnoreCase(jutsuData.getType());
	}

	public static boolean isActivation(String jutsuId) {
		JutsuData jutsuData = JutsuRegistry.getJutsu(jutsuId);
		return isDojutsu(jutsuData) && jutsuId != null && jutsuId.toLowerCase().endsWith("_activate");
	}

	public static String activationForNature(String nature) {
		if (nature == null || nature.isEmpty()) {
			return "";
		}
		for (Map.Entry<String, JutsuData> entry : JutsuRegistry.getJutsusByType("DOJUTSU").entrySet()) {
			String id = entry.getKey();
			JutsuData jutsuData = entry.getValue();
			if (jutsuData.getNature().equalsIgnoreCase(nature) && id.toLowerCase().endsWith("_activate")) {
				return id;
			}
		}
		return "";
	}

	public static String toggleActivation(String currentToggles, String activationJutsuId) {
		JutsuData activation = JutsuRegistry.getJutsu(activationJutsuId);
		if (!isDojutsu(activation)) {
			return JutsuIdList.normalize(currentToggles);
		}
		Set<String> ids = removeDojutsuToggles(JutsuIdList.toSet(currentToggles), null);
		if (!JutsuIdList.has(currentToggles, activationJutsuId)) {
			ids.add(activationJutsuId);
		}
		return JutsuIdList.join(ids);
	}

	public static String ensureActive(String currentToggles, String nature) {
		Set<String> ids = removeDojutsuToggles(JutsuIdList.toSet(currentToggles), nature);
		String activationId = activationForNature(nature);
		if (!activationId.isEmpty()) {
			ids.add(activationId);
		}
		return JutsuIdList.join(ids);
	}

	public static String toggleDojutsuAbility(String currentToggles, String jutsuId, String nature) {
		Set<String> ids = JutsuIdList.toSet(ensureActive(currentToggles, nature));
		if (JutsuIdList.has(JutsuIdList.join(ids), jutsuId)) {
			ids.remove(jutsuId);
		} else {
			ids.add(jutsuId);
		}
		return JutsuIdList.join(ids);
	}

	public static String normalize(String currentToggles) {
		Set<String> ids = JutsuIdList.toSet(currentToggles);
		String activeNature = "";
		for (String id : ids) {
			JutsuData jutsuData = JutsuRegistry.getJutsu(id);
			if (isDojutsu(jutsuData)) {
				activeNature = jutsuData.getNature();
			}
		}
		if (activeNature.isEmpty()) {
			return JutsuIdList.join(ids);
		}
		ids = removeDojutsuToggles(ids, activeNature);
		String activationId = activationForNature(activeNature);
		if (!activationId.isEmpty()) {
			ids.add(activationId);
		}
		return JutsuIdList.join(ids);
	}

	private static Set<String> removeDojutsuToggles(Set<String> ids, String keepNature) {
		ids.removeIf(id -> {
			JutsuData jutsuData = JutsuRegistry.getJutsu(id);
			if (!isDojutsu(jutsuData)) {
				return false;
			}
			return keepNature == null || !jutsuData.getNature().equalsIgnoreCase(keepNature);
		});
		return ids;
	}
}
