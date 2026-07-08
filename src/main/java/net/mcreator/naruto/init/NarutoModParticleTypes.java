/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.naruto.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;

import net.mcreator.naruto.NarutoMod;

public class NarutoModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, NarutoMod.MODID);
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_SMOKE = REGISTRY.register("big_smoke", () -> new SimpleParticleType(true));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MIST = REGISTRY.register("mist", () -> new SimpleParticleType(true));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHOENIX_FLOWER_PARTICLE = REGISTRY.register("phoenix_flower_particle", () -> new SimpleParticleType(true));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIREBALL_PARTICLE = REGISTRY.register("fireball_particle", () -> new SimpleParticleType(true));
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHTNING = REGISTRY.register("lightning", () -> new SimpleParticleType(true));
}