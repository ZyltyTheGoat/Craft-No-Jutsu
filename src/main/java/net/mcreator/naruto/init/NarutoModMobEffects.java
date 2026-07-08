/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.Registries;

import net.mcreator.naruto.potion.GenjutsuMobEffect;
import net.mcreator.naruto.NarutoMod;

public class NarutoModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, NarutoMod.MODID);
	public static final DeferredHolder<MobEffect, MobEffect> GENJUTSU = REGISTRY.register("genjutsu", () -> new GenjutsuMobEffect());
}