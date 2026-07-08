/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.mcreator.naruto.NarutoMod;

public class NarutoModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, NarutoMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> POOF = REGISTRY.register("poof", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("naruto", "poof")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_SPARK = REGISTRY.register("electric_spark", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("naruto", "electric_spark")));
}