/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import net.minecraft.world.item.Item;

import net.mcreator.naruto.item.*;
import net.mcreator.naruto.NarutoMod;

public class NarutoModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(NarutoMod.MODID);
	public static final DeferredItem<Item> DUMMY_SPAWN_EGG;
	public static final DeferredItem<Item> SCROLL_GREAT_FIREBALL;
	public static final DeferredItem<Item> SCROLL_PHOENIX_FIRE;
	public static final DeferredItem<Item> SCROLL_HIDING_IN_ASH;
	public static final DeferredItem<Item> SCROLL_GREAT_FIRE_ANNIHILATION;
	public static final DeferredItem<Item> SCROLL_FIRE_DRAGON_BULLET;
	public static final DeferredItem<Item> SCROLL_GREAT_FLAME;
	public static final DeferredItem<Item> SCROLL_HIDING_IN_MIST;
	public static final DeferredItem<Item> SCROLL_WATER_CLONE;
	public static final DeferredItem<Item> SCROLL_WATER_PRISON;
	public static final DeferredItem<Item> SCROLL_WATER_DRAGON_BULLET;
	public static final DeferredItem<Item> SCROLL_WATER_BULLET;
	public static final DeferredItem<Item> SCROLL_SEVERING_WAVE;
	public static final DeferredItem<Item> SCROLL_EARTH_DRAGON_BULLET;
	public static final DeferredItem<Item> SCROLL_EARTH_WALL;
	public static final DeferredItem<Item> SCROLL_ROCK_FISTS;
	public static final DeferredItem<Item> SCROLL_EARTH_FLOW_SPEARS;
	public static final DeferredItem<Item> SCROLL_EARTH_GOLEM;
	public static final DeferredItem<Item> SCROLL_GREAT_BREAKTHROUGH;
	public static final DeferredItem<Item> SCROLL_VACUUM_WAVE;
	public static final DeferredItem<Item> SCROLL_BLADE_OF_WIND;
	public static final DeferredItem<Item> SCROLL_AIR_BULLETS;
	public static final DeferredItem<Item> SCROLL_CHAKRA_FLOW;
	public static final DeferredItem<Item> SCROLL_RASENSHURIKEN;
	public static final DeferredItem<Item> SCROLL_BEAST_TRACKING_FANG;
	public static final DeferredItem<Item> SCROLL_FALSE_DARKNESS;
	public static final DeferredItem<Item> SCROLL_CHIDORI;
	public static final DeferredItem<Item> SCROLL_LIGHTNING_CLONE;
	public static final DeferredItem<Item> SCROLL_LIGHTNING_CHAKRA_MODE;
	public static final DeferredItem<Item> SCROLL_KIRIN;
	public static final DeferredItem<Item> SCROLL_HIDING_IN_ROCK;
	public static final DeferredItem<Item> SCROLL_SHADOW_CLONE;
	public static final DeferredItem<Item> SCROLL_TRANSFORMATION;
	public static final DeferredItem<Item> SCROLL_SUBSTITUTION;
	public static final DeferredItem<Item> SCROLL_BODY_FLICKER;
	public static final DeferredItem<Item> SCROLL_FLYING_RAIJIN;
	public static final DeferredItem<Item> SCROLL_GENJUTSU;
	public static final DeferredItem<Item> SCROLL_MIND_BODY_TRANSFER;
	public static final DeferredItem<Item> SCROLL_SHADOW_IMITATION;
	public static final DeferredItem<Item> SCROLL_MULTI_SIZE;
	public static final DeferredItem<Item> SCROLL_FOUR_SYMBOL_SEAL;
	public static final DeferredItem<Item> GOURD_CHESTPLATE;
	static {
		DUMMY_SPAWN_EGG = REGISTRY.register("dummy_spawn_egg", () -> new DeferredSpawnEggItem(NarutoModEntities.DUMMY, -1, -1, new Item.Properties()));
		SCROLL_GREAT_FIREBALL = REGISTRY.register("scroll_great_fireball", ScrollGreatFireballItem::new);
		SCROLL_PHOENIX_FIRE = REGISTRY.register("scroll_phoenix_fire", ScrollPhoenixFireItem::new);
		SCROLL_HIDING_IN_ASH = REGISTRY.register("scroll_hiding_in_ash", ScrollHidingInAshItem::new);
		SCROLL_GREAT_FIRE_ANNIHILATION = REGISTRY.register("scroll_great_fire_annihilation", ScrollGreatFireAnnihilationItem::new);
		SCROLL_FIRE_DRAGON_BULLET = REGISTRY.register("scroll_fire_dragon_bullet", ScrollFireDragonBulletItem::new);
		SCROLL_GREAT_FLAME = REGISTRY.register("scroll_great_flame", ScrollGreatFlameItem::new);
		SCROLL_HIDING_IN_MIST = REGISTRY.register("scroll_hiding_in_mist", ScrollHidingInMistItem::new);
		SCROLL_WATER_CLONE = REGISTRY.register("scroll_water_clone", ScrollWaterCloneItem::new);
		SCROLL_WATER_PRISON = REGISTRY.register("scroll_water_prison", ScrollWaterPrisonItem::new);
		SCROLL_WATER_DRAGON_BULLET = REGISTRY.register("scroll_water_dragon_bullet", ScrollWaterDragonBulletItem::new);
		SCROLL_WATER_BULLET = REGISTRY.register("scroll_water_bullet", ScrollWaterBulletItem::new);
		SCROLL_SEVERING_WAVE = REGISTRY.register("scroll_severing_wave", ScrollSeveringWaveItem::new);
		SCROLL_EARTH_DRAGON_BULLET = REGISTRY.register("scroll_earth_dragon_bullet", ScrollEarthDragonBulletItem::new);
		SCROLL_EARTH_WALL = REGISTRY.register("scroll_earth_wall", ScrollEarthWallItem::new);
		SCROLL_ROCK_FISTS = REGISTRY.register("scroll_rock_fists", ScrollRockFistsItem::new);
		SCROLL_EARTH_FLOW_SPEARS = REGISTRY.register("scroll_earth_flow_spears", ScrollEarthFlowSpearsItem::new);
		SCROLL_EARTH_GOLEM = REGISTRY.register("scroll_earth_golem", ScrollEarthGolemItem::new);
		SCROLL_GREAT_BREAKTHROUGH = REGISTRY.register("scroll_great_breakthrough", ScrollGreatBreakthroughItem::new);
		SCROLL_VACUUM_WAVE = REGISTRY.register("scroll_vacuum_wave", ScrollVacuumWaveItem::new);
		SCROLL_BLADE_OF_WIND = REGISTRY.register("scroll_blade_of_wind", ScrollBladeOfWindItem::new);
		SCROLL_AIR_BULLETS = REGISTRY.register("scroll_air_bullets", ScrollAirBulletsItem::new);
		SCROLL_CHAKRA_FLOW = REGISTRY.register("scroll_chakra_flow", ScrollChakraFlowItem::new);
		SCROLL_RASENSHURIKEN = REGISTRY.register("scroll_rasenshuriken", ScrollRasenshurikenItem::new);
		SCROLL_BEAST_TRACKING_FANG = REGISTRY.register("scroll_beast_tracking_fang", ScrollBeastTrackingFangItem::new);
		SCROLL_FALSE_DARKNESS = REGISTRY.register("scroll_false_darkness", ScrollFalseDarknessItem::new);
		SCROLL_CHIDORI = REGISTRY.register("scroll_chidori", ScrollChidoriItem::new);
		SCROLL_LIGHTNING_CLONE = REGISTRY.register("scroll_lightning_clone", ScrollLightningCloneItem::new);
		SCROLL_LIGHTNING_CHAKRA_MODE = REGISTRY.register("scroll_lightning_chakra_mode", ScrollLightningChakraModeItem::new);
		SCROLL_KIRIN = REGISTRY.register("scroll_kirin", ScrollKirinItem::new);
		SCROLL_HIDING_IN_ROCK = REGISTRY.register("scroll_hiding_in_rock", ScrollHidingInRockItem::new);
		SCROLL_SHADOW_CLONE = REGISTRY.register("scroll_shadow_clone", ScrollShadowCloneItem::new);
		SCROLL_TRANSFORMATION = REGISTRY.register("scroll_transformation", ScrollTransformationItem::new);
		SCROLL_SUBSTITUTION = REGISTRY.register("scroll_substitution", ScrollSubstitutionItem::new);
		SCROLL_BODY_FLICKER = REGISTRY.register("scroll_body_flicker", ScrollBodyFlickerItem::new);
		SCROLL_FLYING_RAIJIN = REGISTRY.register("scroll_flying_raijin", ScrollFlyingRaijinItem::new);
		SCROLL_GENJUTSU = REGISTRY.register("scroll_genjutsu", ScrollGenjutsuItem::new);
		SCROLL_MIND_BODY_TRANSFER = REGISTRY.register("scroll_mind_body_transfer", ScrollMindBodyTransferItem::new);
		SCROLL_SHADOW_IMITATION = REGISTRY.register("scroll_shadow_imitation", ScrollShadowImitationItem::new);
		SCROLL_MULTI_SIZE = REGISTRY.register("scroll_multi_size", ScrollMultiSizeItem::new);
		SCROLL_FOUR_SYMBOL_SEAL = REGISTRY.register("scroll_four_symbol_seal", ScrollFourSymbolSealItem::new);
		GOURD_CHESTPLATE = REGISTRY.register("gourd_chestplate", GourdItem.Chestplate::new);
	}
	// Start of user code block custom items
	// End of user code block custom items
}