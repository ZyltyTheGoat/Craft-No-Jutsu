package net.mcreator.naruto;

import java.util.Map;
import java.util.LinkedHashMap;

public class JutsuRegistry {
	private static final Map<String, JutsuData> jutsuRegistry = new LinkedHashMap<>();

	// initialize all jutsus
	public static void initializeJutsus() {
		if (!jutsuRegistry.isEmpty()) {
			return;
		}
		/*
		 * JutsuData parameters:
		 * name - display name of the jutsu
		 * type - jutsu type (NATURE, NINJUTSU, KG, DOJUTSU)
		 * nature - chakra nature (FIRE, WATER, EARTH, LIGHTNING, WIND, YIN, YANG, WOOD, LAVA, BOIL, ICE, MAGNET, EXPLOSION, STORM, DUST, NONE)
		 * chakraCost - chakra cost to use (or per tick for toggleable)
		 * chargeTime - time to charge in ticks before full power (0 for toggleable/instant)
		 * cooldown - cooldown time in ticks (20 ticks = 1 second)
		 * isToggle - true if move is a toggle on/off
		 * color - 0xFFFFFFFF format
		 */
		//
		// ===== NINJUTSU =====
		//
		registerJutsu("shadow_clone", new JutsuData("Shadow Clone", "NINJUTSU", "NINJUTSU", 30, 0, 0, false, 0x96ffffff));
		registerJutsu("transformation", new JutsuData("Transformation Jutsu", "NINJUTSU", "NINJUTSU", 30, 0, 0, false, 0x96ffffff));
		registerJutsu("substitution", new JutsuData("Substitution Jutsu", "NINJUTSU", "NINJUTSU", 45, 0, 80, false, 0x96ffffff));
		registerJutsu("body_flicker", new JutsuData("Body Flicker", "NINJUTSU", "NINJUTSU", 30, 0, 15, false, 0x96ffffff));
		registerJutsu("flying_raijin", new JutsuData("Flying Raijin", "NINJUTSU", "NINJUTSU", 30, 0, 0, false, 0x96ffffff));
		//
		// ===== NATURE RELEASES =====
		//
		// Fire Release
		registerJutsu("fireball", new JutsuData("Great Fireball", "NATURE", "FIRE", 30, 60, 10, false, 0x96bc0712));
		registerJutsu("phoenix_flower", new JutsuData("Phoenix Flower", "NATURE", "FIRE", 30, 40, 30, false, 0x96bc0712));
		registerJutsu("hiding_in_ash", new JutsuData("Hiding in Ash", "NATURE", "FIRE", 2, 0, 0, true, 0x96bc0712));
		registerJutsu("fire_dragon_bullet", new JutsuData("Fire Dragon Bullet", "NATURE", "FIRE", 45, 80, 60, false, 0x96bc0712));
		registerJutsu("great_flame", new JutsuData("Great Flame", "NATURE", "FIRE", 60, 100, 80, false, 0x96bc0712));
		registerJutsu("great_fire_annihilation", new JutsuData("Great Fire Annihilation", "NATURE", "FIRE", 3000, 100, 200, false, 0x96bc0712));
		// Water Release
		registerJutsu("water_bullet", new JutsuData("Water Bullet", "NATURE", "WATER", 20, 60, 10, false, 0x960e85fe));
		registerJutsu("water_dragon_bullet", new JutsuData("Water Dragon Bullet", "NATURE", "WATER", 40, 80, 60, false, 0x960e85fe));
		registerJutsu("hiding_in_mist", new JutsuData("Hiding in Mist", "NATURE", "WATER", 2, 0, 0, true, 0x960e85fe));
		registerJutsu("water_prison", new JutsuData("Water Prison", "NATURE", "WATER", 15, 0, 100, false, 0x960e85fe));
		registerJutsu("water_clone", new JutsuData("Water Clone", "NATURE", "WATER", 20, 0, 10, false, 0x960e85fe));
		registerJutsu("severing_wave", new JutsuData("Severing Wave", "NATURE", "WATER", 50, 60, 120, false, 0x960e85fe));
		// Earth Release
		registerJutsu("earth_wall", new JutsuData("Earth Wall", "NATURE", "EARTH", 40, 50, 100, false, 0x967c5013));
		registerJutsu("earth_flow_spears", new JutsuData("Earth Spear", "NATURE", "EARTH", 40, 30, 10, false, 0x967c5013));
		registerJutsu("hiding_in_rock", new JutsuData("Hiding in Rock", "NATURE", "EARTH", 2, 0, 0, true, 0x967c5013));
		registerJutsu("rock_fists", new JutsuData("Rock Fists", "NATURE", "EARTH", 2, 0, 0, true, 0x967c5013));
		registerJutsu("earth_dragon_bullet", new JutsuData("Earth Dragon Bullet", "NATURE", "EARTH", 30, 80, 60, false, 0x967c5013));
		registerJutsu("earth_golem", new JutsuData("Earth Golem", "NATURE", "EARTH", 60, 100, 120, false, 0x967c5013));
		// Lightning Release
		registerJutsu("chidori", new JutsuData("Chidori", "NATURE", "LIGHTNING", 65, 100, 60, false, 0x96d3c62b));
		registerJutsu("lightning_clone", new JutsuData("Lightning Clone", "NATURE", "LIGHTNING", 80, 0, 10, false, 0x96d3c62b));
		registerJutsu("false_darkness", new JutsuData("False Darkness", "NATURE", "LIGHTNING", 45, 100, 60, false, 0x96d3c62b));
		registerJutsu("beast_tracking_fang", new JutsuData("Beast Tracking Fang", "NATURE", "LIGHTNING", 35, 50, 30, false, 0x96d3c62b));
		registerJutsu("lightning_chakra_mode", new JutsuData("Lightning Chakra Mode", "NATURE", "LIGHTNING", 4, 0, 0, true, 0x96d3c62b));
		registerJutsu("lighthing_chakra_mode", new JutsuData("Lightning Chakra Mode", "NATURE", "LIGHTNING", 4, 0, 0, true, 0x96d3c62b));
		registerJutsu("kirin", new JutsuData("Kirin", "NATURE", "LIGHTNING", 4000, 200, 300, false, 0x96d3c62b));
		// Wind Release
		registerJutsu("great_breakthrough", new JutsuData("Great Breakthrough", "NATURE", "WIND", 30, 40, 20, false, 0x961cb878));
		registerJutsu("air_bullets", new JutsuData("Air Bullets", "NATURE", "WIND", 50, 60, 60, false, 0x961cb878));
		registerJutsu("vacuum_wave", new JutsuData("Vacuum Wave", "NATURE", "WIND", 40, 50, 50, false, 0x961cb878));
		registerJutsu("chakra_flow", new JutsuData("Chakra Flow", "NATURE", "WIND", 3, 0, 0, true, 0x961cb878));
		registerJutsu("wind_blade", new JutsuData("Blade of Wind", "NATURE", "WIND", 35, 0, 60, false, 0x961cb878));
		registerJutsu("rasenshuriken", new JutsuData("Rasenshuriken", "NATURE", "WIND", 2000, 200, 200, false, 0x961cb878));
		// Yin Release
		registerJutsu("yin_genjutsu", new JutsuData("Genjutsu", "NATURE", "YIN", 2000, 200, 200, false, 0x96222222));
		registerJutsu("mind_body_transfer", new JutsuData("Mind Body Transfer", "NATURE", "YIN", 2000, 200, 200, false, 0x96222222));
		registerJutsu("shadow_imitation", new JutsuData("Shadow Imitation", "NATURE", "YIN", 2000, 200, 200, false, 0x96222222));
		// Yang Release
		registerJutsu("multi_size", new JutsuData("Multi-Size Jutsu", "NATURE", "YANG", 2000, 200, 200, false, 0x96E8E8E8));
		registerJutsu("four_symbol_seal", new JutsuData("Four-Symbol Seal", "NATURE", "YANG", 2000, 200, 200, false, 0x96E8E8E8));
		//
		// ===== KEKKEI GENKAI =====
		//
		// Wood Release
		registerJutsu("wood_clone", new JutsuData("Wood Clone", "KEKKEI_GENKAI", "WOOD", 30, 60, 100, false, 0x963d811a));
		registerJutsu("wood_golem", new JutsuData("Wood Golem", "KEKKEI_GENKAI", "WOOD", 30, 60, 100, false, 0x963d811a));
		registerJutsu("wood_dragon", new JutsuData("Wood Dragon", "KEKKEI_GENKAI", "WOOD", 30, 60, 100, false, 0x963d811a));
		registerJutsu("deep_forest_emergence", new JutsuData("Deep Forest Emergence", "KEKKEI_GENKAI", "WOOD", 30, 60, 100, false, 0x963d811a));
		registerJutsu("tree_burial", new JutsuData("Tree Burial", "KEKKEI_GENKAI", "WOOD", 30, 60, 100, false, 0x963d811a));
		// Lava Release
		registerJutsu("scorching_rocks", new JutsuData("Scorching Rocks", "KEKKEI_GENKAI", "LAVA", 30, 60, 100, false, 0x96dc0910));
		registerJutsu("melting_apparition", new JutsuData("Melting Apparition", "KEKKEI_GENKAI", "LAVA", 30, 60, 100, false, 0x96dc0910));
		registerJutsu("quicklime", new JutsuData("Quicklime", "KEKKEI_GENKAI", "LAVA", 30, 60, 100, false, 0x96dc0910));
		registerJutsu("lava_chakra_mode", new JutsuData("Lava Chakra Mode", "KEKKEI_GENKAI", "LAVA", 30, 60, 100, true, 0x96dc0910));
		// Boil Release
		registerJutsu("solid_fog_jutsu", new JutsuData("Solid Fog Jutsu", "KEKKEI_GENKAI", "BOIL", 30, 60, 100, false, 0x96aa2329));
		registerJutsu("fog_fist", new JutsuData("Fog Fist", "KEKKEI_GENKAI", "BOIL", 30, 60, 100, true, 0x96aa2329));
		registerJutsu("solid_fog_spear", new JutsuData("Solid Fog Spear", "KEKKEI_GENKAI", "BOIL", 30, 60, 100, false, 0x96aa2329));
		// Ice Release
		registerJutsu("demonic_ice_mirrors", new JutsuData("Demonic Ice Mirrors", "KEKKEI_GENKAI", "ICE", 30, 60, 100, false, 0x96a2dff1));
		registerJutsu("ice_prison", new JutsuData("Ice Prison", "KEKKEI_GENKAI", "ICE", 30, 60, 100, false, 0x96a2dff1));
		registerJutsu("white_whale", new JutsuData("White Whale", "KEKKEI_GENKAI", "ICE", 30, 60, 100, false, 0x96a2dff1));
		registerJutsu("shooting_ice_spears", new JutsuData("Shooting Ice Spears", "KEKKEI_GENKAI", "ICE", 30, 60, 100, false, 0x96a2dff1));
		// Magnet Release
		registerJutsu("iron_sand_shower", new JutsuData("Iron Sand Shower", "KEKKEI_GENKAI", "MAGNET", 30, 60, 100, false, 0x96920fff));
		registerJutsu("iron_sand_gathering_assault", new JutsuData("Iron Sand Gathering Assault", "KEKKEI_GENKAI", "MAGNET", 30, 60, 100, false, 0x96920fff));
		registerJutsu("needle_sphere", new JutsuData("Needle Sphere", "KEKKEI_GENKAI", "MAGNET", 30, 60, 100, false, 0x96920fff));
		registerJutsu("sand_burial_prison", new JutsuData("Sand Burial Prison", "KEKKEI_GENKAI", "MAGNET", 30, 60, 100, false, 0x96920fff));
		registerJutsu("sand_levitation", new JutsuData("Sand Levitation", "KEKKEI_GENKAI", "MAGNET", 30, 60, 100, true, 0x96920fff));
		// Explosion Release
		registerJutsu("landmine_fist", new JutsuData("Landmine Fist", "KEKKEI_GENKAI", "EXPLOSION", 30, 60, 100, true, 0x96dc611d));
		registerJutsu("c0", new JutsuData("C0", "KEKKEI_GENKAI", "EXPLOSION", 30, 60, 100, false, 0x96dc611d));
		registerJutsu("c1", new JutsuData("C1", "KEKKEI_GENKAI", "EXPLOSION", 30, 60, 100, false, 0x96dc611d));
		registerJutsu("c2", new JutsuData("C2", "KEKKEI_GENKAI", "EXPLOSION", 30, 60, 100, false, 0x96dc611d));
		registerJutsu("c3", new JutsuData("C3", "KEKKEI_GENKAI", "EXPLOSION", 30, 60, 100, false, 0x96dc611d));
		registerJutsu("c4", new JutsuData("C4", "KEKKEI_GENKAI", "EXPLOSION", 30, 60, 100, false, 0x96dc611d));
		registerJutsu("suicide_bombing_clone", new JutsuData("Suicide Bombing Clone", "KEKKEI_GENKAI", "EXPLOSION", 30, 60, 100, false, 0x96dc611d));
		// Storm Release
		registerJutsu("fang_of_light", new JutsuData("Fang of Light", "KEKKEI_GENKAI", "STORM", 30, 60, 100, false, 0x962b263c));
		registerJutsu("laser_circus", new JutsuData("Laser Circus", "KEKKEI_GENKAI", "STORM", 30, 60, 100, false, 0x962b263c));
		registerJutsu("thundering_void_wave", new JutsuData("Thundering Void Wave", "KEKKEI_GENKAI", "STORM", 30, 60, 100, false, 0x962b263c));
		//
		// ===== DOJUTSU ======
		//
		// == Sharingan ==
		registerJutsu("sharingan_activate", new JutsuData("Activate", "DOJUTSU", "SHARINGAN", 20, 30, 60, true, 0x96b20000));
		registerJutsu("genjutsu", new JutsuData("Genjutsu", "DOJUTSU", "SHARINGAN", 20, 30, 60, false, 0x96b20000));
		// Itachi Sharingan
		registerJutsu("sharingan_itachi_activate", new JutsuData("Activate", "DOJUTSU", "SHARINGAN_ITACHI", 20, 30, 60, true, 0x96b20000));
		registerJutsu("tsukuyomi", new JutsuData("Tsukuyomi", "DOJUTSU", "SHARINGAN_ITACHI", 20, 30, 60, false, 0x96b20000));
		registerJutsu("amaterasu_itachi", new JutsuData("Amaterasu", "DOJUTSU", "SHARINGAN_ITACHI", 20, 30, 60, false, 0x96b20000));
		registerJutsu("susanoo_itachi", new JutsuData("Susanoo", "DOJUTSU", "SHARINGAN_ITACHI", 20, 30, 60, false, 0x96b20000));
		// Kamui Sharingan
		registerJutsu("sharingan_kamui_activate", new JutsuData("Activate", "DOJUTSU", "SHARINGAN_KAMUI", 20, 30, 60, true, 0x96b20000));
		registerJutsu("kamui_short", new JutsuData("Kamui - Short Range", "DOJUTSU", "SHARINGAN_KAMUI", 20, 30, 60, false, 0x96b20000));
		registerJutsu("kamui_long", new JutsuData("Kamui - Long Range", "DOJUTSU", "SHARINGAN_KAMUI", 20, 30, 60, false, 0x96b20000));
		registerJutsu("kamui_intangibility", new JutsuData("Intangibility", "DOJUTSU", "SHARINGAN_KAMUI", 20, 30, 60, false, 0x96b20000));
		registerJutsu("kamui_intagibility", new JutsuData("Intangibility", "DOJUTSU", "SHARINGAN_KAMUI", 20, 30, 60, false, 0x96b20000));
		registerJutsu("susanoo_kamui", new JutsuData("Susanoo", "DOJUTSU", "SHARINGAN_KAMUI", 20, 30, 60, false, 0x96b20000));
		// Sasuke Sharingan
		registerJutsu("sharingan_sasuke_activate", new JutsuData("Activate", "DOJUTSU", "SHARINGAN_SASUKE", 20, 30, 60, true, 0x96b20000));
		registerJutsu("amaterasu_sasuke", new JutsuData("Amaterasu", "DOJUTSU", "SHARINGAN_SASUKE", 20, 30, 60, false, 0x96b20000));
		registerJutsu("kagutsuchi", new JutsuData("Kagutsuchi", "DOJUTSU", "SHARINGAN_SASUKE", 20, 30, 60, false, 0x96b20000));
		registerJutsu("susanoo_sasuke", new JutsuData("Susanoo", "DOJUTSU", "SHARINGAN_SASUKE", 20, 30, 60, false, 0x96b20000));
		// Kotoamatsukami Sharingan
		registerJutsu("sharingan_koto_activate", new JutsuData("Activate", "DOJUTSU", "SHARINGAN_KOTOAMATSUKAMI", 20, 30, 60, true, 0x96b20000));
		registerJutsu("kotoamatsukami", new JutsuData("Kotoamatsukami", "DOJUTSU", "SHARINGAN_KOTOAMATSUKAMI", 20, 30, 60, false, 0x96b20000));
		registerJutsu("susanoo_kotoamatsukami", new JutsuData("Susanoo", "DOJUTSU", "SHARINGAN_KOTOAMATSUKAMI", 20, 30, 60, false, 0x96b20000));
		// Ōhirume Sharingan
		registerJutsu("sharingan_ohirume_activate", new JutsuData("Activate", "DOJUTSU", "SHARINGAN_OHIRUME", 20, 30, 60, true, 0x96b20000));
		registerJutsu("ohirume", new JutsuData("Ōhirume", "DOJUTSU", "SHARINGAN_OHIRUME", 20, 30, 60, false, 0x96b20000));
		registerJutsu("susanoo_ohirume", new JutsuData("Susanoo", "DOJUTSU", "SHARINGAN_OHIRUME", 20, 30, 60, false, 0x96b20000));
		// == Byakugan ==
		registerJutsu("byakugan_activate", new JutsuData("Activate", "DOJUTSU", "BYAKUGAN", 20, 30, 60, true, 0x96FFFFFF));
		registerJutsu("enhanced_vision", new JutsuData("Enhanced Vision", "DOJUTSU", "BYAKUGAN", 20, 30, 60, true, 0x96FFFFFF));
		registerJutsu("eight_trigrams_vacuum_palm", new JutsuData("8 Trigrams Vacuum Palm", "DOJUTSU", "BYAKUGAN", 20, 30, 60, false, 0x96FFFFFF));
		registerJutsu("eight_trigrams_sixty_four_lms", new JutsuData("8 Trigrams 64 Palms", "DOJUTSU", "BYAKUGAN", 20, 30, 60, false, 0x96FFFFFF));
		registerJutsu("eight_trigrams_revolving_heaven", new JutsuData("8 Trigrams Revolving Heaven", "DOJUTSU", "BYAKUGAN", 20, 30, 60, false, 0x96FFFFFF));
		// == Rinnegan ==
		registerJutsu("rinnegan_activate", new JutsuData("Activate", "DOJUTSU", "RINNEGAN", 20, 30, 60, true, 0x9690889e));
		registerJutsu("rinnegan_almighty_push_pull", new JutsuData("Almighty Push/Pull", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("planetary_devastation", new JutsuData("Planetary Devastation", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("planetary_devestation", new JutsuData("Planetary Devastation", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("king_of_hell", new JutsuData("King of Hell", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("flaming_arrow_warhead", new JutsuData("Flaming Arrow Warhead", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("soul_extraction", new JutsuData("Soul Extraction", "DOJUTSU", "RINNEGAN", 20, 30, 60, true, 0x9690889e));
		registerJutsu("summon_multi_headed_dog", new JutsuData("Multi-Headed Dog", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("summon_rhino", new JutsuData("Rhino", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("summon_centipede", new JutsuData("Centipede", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		registerJutsu("demonic_statue_of_the_outer_path", new JutsuData("Demonic Statue", "DOJUTSU", "RINNEGAN", 20, 30, 60, false, 0x9690889e));
		// == Tenseigan ==
		registerJutsu("tenseigan_activate", new JutsuData("Activate", "DOJUTSU", "TENSEIGAN", 20, 30, 60, true, 0x9634758f));
		registerJutsu("tenseigan_almighty_push_pull", new JutsuData("Almighty Push/Pull", "DOJUTSU", "TENSEIGAN", 20, 30, 60, false, 0x9634758f));
		registerJutsu("tenseigan_chakra_mode", new JutsuData("Tenseigan Chakra Mode", "DOJUTSU", "TENSEIGAN", 20, 30, 60, true, 0x9634758f));
		registerJutsu("summon_truth_seeking_balls", new JutsuData("Truth-Seeking Balls", "DOJUTSU", "TENSEIGAN", 20, 30, 60, false, 0x9634758f));
	}

	// register a jutsu
	public static void registerJutsu(String id, JutsuData jutsu) {
		jutsuRegistry.put(id, jutsu);
	}

	// get jutsu by id
	public static JutsuData getJutsu(String id) {
		if (id == null || id.isEmpty()) {
			return null;
		}
		return jutsuRegistry.get(id);
	}

	public static String getDisplayName(String id) {
		JutsuData jutsu = getJutsu(id);
		if (jutsu != null) {
			return jutsu.getName();
		}
		if (id == null || id.isEmpty()) {
			return "";
		}
		String[] words = id.replace('_', ' ').split(" ");
		StringBuilder displayName = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (displayName.length() > 0) {
				displayName.append(' ');
			}
			displayName.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) {
				displayName.append(word.substring(1));
			}
		}
		return displayName.toString();
	}

	// check if jutsu exists
	public static boolean jutsuExists(String id) {
		if (id == null || id.isEmpty()) {
			return false;
		}
		return jutsuRegistry.containsKey(id);
	}

	// get all jutsu of nature
	public static Map<String, JutsuData> getJutsusByNature(String nature) {
		Map<String, JutsuData> filtered = new LinkedHashMap<>();
		for (Map.Entry<String, JutsuData> entry : jutsuRegistry.entrySet()) {
			if (entry.getValue().getNature().equalsIgnoreCase(nature)) {
				filtered.put(entry.getKey(), entry.getValue());
			}
		}
		return filtered;
	}

	// get all jutsu of type
	public static Map<String, JutsuData> getJutsusByType(String type) {
		Map<String, JutsuData> filtered = new LinkedHashMap<>();
		for (Map.Entry<String, JutsuData> entry : jutsuRegistry.entrySet()) {
			if (entry.getValue().getType().equalsIgnoreCase(type)) {
				filtered.put(entry.getKey(), entry.getValue());
			}
		}
		return filtered;
	}

	// get all jutsu ids
	public static String[] getAllJutsuIds() {
		return jutsuRegistry.keySet().toArray(new String[0]);
	}
}
