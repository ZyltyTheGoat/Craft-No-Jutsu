package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.WaterPrisonStatePacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class WaterPrisonAbility {
	private static final String JUTSU_ID = "water_prison";
	private static final double CAPTURE_RANGE = 4.25D;
	private static final double BREAK_DISTANCE = 5.5D;
	private static final float VIEW_YAW_LIMIT = 38.0F;
	private static final float VIEW_PITCH_LIMIT = 24.0F;
	private static final int MAX_HOLD_TICKS = 420;
	private static final int DROWN_START_TICKS = 100;
	private static final Map<UUID, PrisonState> ACTIVE_BY_CASTER = new HashMap<>();
	private static final Map<UUID, UUID> CASTER_BY_TARGET = new HashMap<>();

	public static boolean tryUse(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof Player player) || jutsuData == null) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		clearCharge(player);
		if (ACTIVE_BY_CASTER.containsKey(player.getUUID())) {
			return true;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		LivingEntity target = findTarget(player);
		if (target == null) {
			JutsuFeedback.show(player, "No target in range");
			return true;
		}
		if (CASTER_BY_TARGET.containsKey(target.getUUID())) {
			JutsuFeedback.show(player, "Target already trapped");
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		startPrison(player, target);
		return true;
	}

	public static boolean release(Entity entity) {
		if (!(entity instanceof Player player)) {
			return false;
		}
		if (player.level().isClientSide()) {
			return true;
		}
		PrisonState state = ACTIVE_BY_CASTER.get(player.getUUID());
		if (state == null) {
			return false;
		}
		endPrison(player, state, true);
		return true;
	}

	private static void startPrison(Player caster, LivingEntity target) {
		Vec3 casterPos = caster.position();
		Vec3 targetPos = target.position();
		PrisonState state = new PrisonState(target.getUUID(), target.getId(), casterPos, targetPos, caster.getYRot(), caster.getXRot());
		ACTIVE_BY_CASTER.put(caster.getUUID(), state);
		CASTER_BY_TARGET.put(target.getUUID(), caster.getUUID());
		lockEntity(caster, casterPos);
		lockEntity(target, targetPos);
		if (caster.level() instanceof ServerLevel serverLevel) {
			Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			serverLevel.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, 28, 0.65D, 0.45D, 0.65D, 0.12D);
			serverLevel.sendParticles(ParticleTypes.BUBBLE, center.x, center.y, center.z, 20, 0.45D, 0.32D, 0.45D, 0.04D);
			PacketDistributor.sendToPlayersInDimension(serverLevel, new WaterPrisonStatePacket(caster.getUUID(), target.getId(), true));
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player caster = event.getEntity();
		if (caster.level().isClientSide()) {
			return;
		}
		PrisonState state = ACTIVE_BY_CASTER.get(caster.getUUID());
		if (state == null) {
			return;
		}
		tickPrison(caster, state);
	}

	private static void tickPrison(Player caster, PrisonState state) {
		if (!(caster.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		Entity found = serverLevel.getEntity(state.targetUuid);
		if (!(found instanceof LivingEntity target) || !caster.isAlive() || !target.isAlive()) {
			endPrison(caster, state, false);
			return;
		}
		NarutoModVariables.PlayerVariables vars = caster.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (!JUTSU_ID.equals(vars.activeJutsu) || state.ticks >= MAX_HOLD_TICKS || caster.distanceToSqr(state.casterPos) > BREAK_DISTANCE * BREAK_DISTANCE) {
			endPrison(caster, state, false);
			return;
		}
		lockEntity(caster, state.casterPos);
		clampCasterView(caster, state);
		lockEntity(target, state.targetPos);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 255, true, false));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 9, true, false));
		target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 8, 9, true, false));
		if (state.ticks % 8 == 0) {
			Vec3 center = state.targetPos.add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			serverLevel.sendParticles(ParticleTypes.BUBBLE, center.x, center.y, center.z, 4, 0.42D, 0.32D, 0.42D, 0.025D);
		}
		if (state.ticks >= DROWN_START_TICKS && state.ticks % 20 == 0) {
			target.hurt(target.damageSources().drown(), 2.0F);
		}
		state.ticks++;
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		Player attacker = event.getEntity();
		if (attacker.level().isClientSide()) {
			return;
		}
		if (ACTIVE_BY_CASTER.containsKey(attacker.getUUID()) || CASTER_BY_TARGET.containsKey(attacker.getUUID())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		Entity source = event.getSource().getEntity();
		if (source instanceof LivingEntity sourceLiving && (ACTIVE_BY_CASTER.containsKey(sourceLiving.getUUID()) || CASTER_BY_TARGET.containsKey(sourceLiving.getUUID()))) {
			event.setCanceled(true);
			return;
		}
		LivingEntity living = event.getEntity();
		if (living.level().isClientSide()) {
			return;
		}
		if (living instanceof Player player) {
			PrisonState state = ACTIVE_BY_CASTER.get(player.getUUID());
			if (state != null) {
				endPrison(player, state, false);
			}
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity living = event.getEntity();
		if (living.level().isClientSide()) {
			return;
		}
		if (living instanceof Player player) {
			PrisonState state = ACTIVE_BY_CASTER.get(player.getUUID());
			if (state != null) {
				endPrison(player, state, false);
			}
		}
		UUID casterId = CASTER_BY_TARGET.get(living.getUUID());
		if (casterId != null && living.level() instanceof ServerLevel serverLevel && serverLevel.getEntity(casterId) instanceof Player caster) {
			PrisonState state = ACTIVE_BY_CASTER.get(caster.getUUID());
			if (state != null) {
				endPrison(caster, state, false);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		PrisonState state = ACTIVE_BY_CASTER.get(player.getUUID());
		if (state != null) {
			endPrison(player, state, false);
		}
		UUID casterId = CASTER_BY_TARGET.get(player.getUUID());
		if (casterId != null && player.level() instanceof ServerLevel serverLevel && serverLevel.getEntity(casterId) instanceof Player caster) {
			PrisonState casterState = ACTIVE_BY_CASTER.get(caster.getUUID());
			if (casterState != null) {
				endPrison(caster, casterState, false);
			}
		}
	}

	public static void clearInvalidStates() {
		Iterator<Map.Entry<UUID, PrisonState>> iterator = ACTIVE_BY_CASTER.entrySet().iterator();
		while (iterator.hasNext()) {
			PrisonState state = iterator.next().getValue();
			if (state.ticks > MAX_HOLD_TICKS + 40) {
				CASTER_BY_TARGET.remove(state.targetUuid);
				iterator.remove();
			}
		}
	}

	private static LivingEntity findTarget(Player player) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		Vec3 end = start.add(look.scale(CAPTURE_RANGE));
		AABB searchArea = player.getBoundingBox().expandTowards(look.scale(CAPTURE_RANGE)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, end, searchArea, target -> target instanceof LivingEntity living && living.isAlive() && target.isPickable() && target != player);
		if (hit != null && hit.getEntity() instanceof LivingEntity living) {
			return living;
		}
		return null;
	}

	private static void endPrison(Player caster, PrisonState state, boolean released) {
		ACTIVE_BY_CASTER.remove(caster.getUUID());
		CASTER_BY_TARGET.remove(state.targetUuid);
		caster.setDeltaMovement(Vec3.ZERO);
		if (caster.level() instanceof ServerLevel serverLevel) {
			Entity found = serverLevel.getEntity(state.targetUuid);
			if (found instanceof LivingEntity target) {
				target.setDeltaMovement(Vec3.ZERO);
				Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
				serverLevel.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z, released ? 18 : 10, 0.62D, 0.38D, 0.62D, 0.08D);
			}
			PacketDistributor.sendToPlayersInDimension(serverLevel, new WaterPrisonStatePacket(caster.getUUID(), state.targetId, false));
		}
		clearCharge(caster);
	}

	private static void lockEntity(Entity entity, Vec3 position) {
		entity.setDeltaMovement(Vec3.ZERO);
		entity.teleportTo(position.x, position.y, position.z);
		entity.resetFallDistance();
		entity.hurtMarked = true;
	}

	private static void clampCasterView(Player caster, PrisonState state) {
		float yRot = clampAngle(caster.getYRot(), state.casterYRot, VIEW_YAW_LIMIT);
		float xRot = clampAngle(caster.getXRot(), state.casterXRot, VIEW_PITCH_LIMIT);
		caster.setYRot(yRot);
		caster.setYHeadRot(yRot);
		caster.setYBodyRot(yRot);
		caster.setXRot(xRot);
	}

	private static float clampAngle(float current, float center, float maxDelta) {
		float delta = Mth.wrapDegrees(current - center);
		return center + Mth.clamp(delta, -maxDelta, maxDelta);
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private static final class PrisonState {
		private final UUID targetUuid;
		private final int targetId;
		private final Vec3 casterPos;
		private final Vec3 targetPos;
		private final float casterYRot;
		private final float casterXRot;
		private int ticks;

		private PrisonState(UUID targetUuid, int targetId, Vec3 casterPos, Vec3 targetPos, float casterYRot, float casterXRot) {
			this.targetUuid = targetUuid;
			this.targetId = targetId;
			this.casterPos = casterPos;
			this.targetPos = targetPos;
			this.casterYRot = casterYRot;
			this.casterXRot = casterXRot;
		}
	}
}
