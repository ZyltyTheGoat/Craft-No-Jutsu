package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.SolidFogJutsuPacket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber
public final class SolidFogJutsuAbility {
	private static final String JUTSU_ID = "solid_fog_jutsu";
	private static final float MIN_LENGTH = 7.5F;
	private static final float MAX_LENGTH = 12.0F;
	private static final float MIN_RADIUS = 3.0F;
	private static final float MAX_RADIUS = 4.8F;
	private static final int MIN_DURATION = 100;
	private static final int MAX_DURATION = 140;
	private static final int FORM_TICKS = 18;
	private static final int DAMAGE_INTERVAL = 12;
	private static final List<ActiveFog> ACTIVE_FOG = new ArrayList<>();

	private SolidFogJutsuAbility() {
	}

	public static boolean tryRelease(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof ServerPlayer player) || jutsuData == null) {
			return false;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		float chargePercent = Mth.clamp((float) (vars.chargeTime / Math.max(1.0D, jutsuData.getChargeTime())), 0.0F, 1.0F);
		clearCharge(player);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, Math.max(1, jutsuData.getCooldown()))) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		createFog(player, chargePercent);
		return true;
	}

	private static void createFog(ServerPlayer player, float chargePercent) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		Vec3 direction = player.getLookAngle().normalize();
		Vec3 origin = player.getEyePosition().add(direction.scale(0.22D)).add(0.0D, -0.16D, 0.0D);
		float length = Mth.lerp(chargePercent, MIN_LENGTH, MAX_LENGTH);
		float radius = Mth.lerp(chargePercent, MIN_RADIUS, MAX_RADIUS);
		int duration = Mth.floor(Mth.lerp(chargePercent, MIN_DURATION, MAX_DURATION));
		float pulseDamage = NarutoDamageFormula.calculateJutsuDamage(player, 1.35F, chargePercent, NarutoDamageFormula.TYPE_SCALE_MULTI_HIT_LOW);
		ACTIVE_FOG.add(new ActiveFog(level, player.getUUID(), origin, direction, length, radius, duration, pulseDamage));
		PacketDistributor.sendToPlayersInDimension(level, new SolidFogJutsuPacket(player.getUUID(), origin.x, origin.y, origin.z, direction.x, direction.y, direction.z, length, radius, duration));
		level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.85F, 0.58F);
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		Iterator<ActiveFog> iterator = ACTIVE_FOG.iterator();
		while (iterator.hasNext()) {
			ActiveFog fog = iterator.next();
			fog.age++;
			if (fog.age >= fog.duration) {
				iterator.remove();
				continue;
			}
			if (fog.age >= FORM_TICKS && fog.age % DAMAGE_INTERVAL == 0) {
				pulse(fog);
			}
		}
	}

	private static void pulse(ActiveFog fog) {
		ServerPlayer caster = fog.level.getServer().getPlayerList().getPlayer(fog.casterId);
		if (caster == null || caster.level() != fog.level) {
			return;
		}
		Vec3 center = fog.origin.add(fog.direction.scale(fog.length * 0.56D));
		AABB bounds = new AABB(center, center).inflate(fog.length * 0.62D + fog.radius, fog.radius + 1.2D, fog.length * 0.62D + fog.radius);
		for (LivingEntity target : fog.level.getEntitiesOfClass(LivingEntity.class, bounds, living -> living.isAlive() && living != caster && !living.isSpectator())) {
			Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			Vec3 offset = targetCenter.subtract(fog.origin);
			double forward = offset.dot(fog.direction);
			if (forward < 0.35D || forward > fog.length + target.getBbWidth() * 0.5D) {
				continue;
			}
			double spread = fog.radius * (0.28D + 0.72D * Mth.clamp(forward / fog.length, 0.0D, 1.0D));
			Vec3 lateral = offset.subtract(fog.direction.scale(forward));
			if (lateral.lengthSqr() > spread * spread) {
				continue;
			}
			target.invulnerableTime = 0;
			target.hurt(caster.damageSources().playerAttack(caster), fog.pulseDamage);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, true));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, true));
		}
	}

	private static void clearCharge(ServerPlayer player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private static final class ActiveFog {
		private final ServerLevel level;
		private final UUID casterId;
		private final Vec3 origin;
		private final Vec3 direction;
		private final float length;
		private final float radius;
		private final int duration;
		private final float pulseDamage;
		private int age;

		private ActiveFog(ServerLevel level, UUID casterId, Vec3 origin, Vec3 direction, float length, float radius, int duration, float pulseDamage) {
			this.level = level;
			this.casterId = casterId;
			this.origin = origin;
			this.direction = direction;
			this.length = length;
			this.radius = radius;
			this.duration = duration;
			this.pulseDamage = pulseDamage;
		}
	}
}
