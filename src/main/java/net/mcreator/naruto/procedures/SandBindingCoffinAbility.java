package net.mcreator.naruto.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.mcreator.naruto.JutsuData;
import net.mcreator.naruto.MagnetSandReserve;
import net.mcreator.naruto.network.NarutoModVariables;
import net.mcreator.naruto.network.SandBindingCoffinStatePacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public final class SandBindingCoffinAbility {
	private static final String JUTSU_ID = "sand_burial_prison";
	private static final double CAPTURE_RANGE = 7.0D;
	private static final double BREAK_DISTANCE = 9.0D;
	private static final double SAND_COST = 30.0D;
	private static final double NORMAL_RETURN_RATE = 0.60D;
	private static final double INTERRUPTED_RETURN_RATE = 0.35D;
	private static final int MAX_HOLD_TICKS = 100;
	private static final int DAMAGE_INTERVAL_TICKS = 33;
	private static final float CRUSH_DAMAGE = 1.0F;
	private static final float INTERRUPT_DAMAGE_THRESHOLD = 3.0F;
	private static final int SYNC_INTERVAL_TICKS = 10;
	private static final Map<UUID, BindState> ACTIVE_BY_CASTER = new HashMap<>();
	private static final Map<UUID, UUID> CASTER_BY_TARGET = new HashMap<>();

	private SandBindingCoffinAbility() {
	}

	public static boolean tryRelease(Entity entity, JutsuData jutsuData) {
		if (!(entity instanceof ServerPlayer player) || jutsuData == null) {
			return false;
		}
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		float chargePercent = Mth.clamp((float) (vars.chargeTime / Math.max(1.0D, jutsuData.getChargeTime())), 0.0F, 1.0F);
		clearCharge(player);
		if (ACTIVE_BY_CASTER.containsKey(player.getUUID())) {
			return true;
		}
		if (vars.currentChakra < jutsuData.getChakraCost()) {
			JutsuFeedback.notEnoughChakra(player);
			return true;
		}
		LivingEntity target = findTarget(player, chargePercent);
		if (target == null) {
			JutsuFeedback.show(player, "No target in range");
			return true;
		}
		if (CASTER_BY_TARGET.containsKey(target.getUUID())) {
			JutsuFeedback.show(player, "Target already bound");
			return true;
		}
		if (MagnetSandReserve.get(player) < SAND_COST) {
			JutsuFeedback.show(player, "Sand Binding Coffin requires 30 sand", 60);
			return true;
		}
		int cooldown = Math.max(1, jutsuData.getCooldown());
		if (!JutsuCooldowns.tryStart(player, JUTSU_ID, cooldown)) {
			JutsuFeedback.showCooldown(player, jutsuData.getName(), JutsuCooldowns.getRemainingTicks(player, JUTSU_ID));
			return true;
		}
		MagnetSandReserve.tryConsume(player, SAND_COST);
		vars.currentChakra = Math.max(0.0D, vars.currentChakra - jutsuData.getChakraCost());
		vars.markSyncDirty();
		startBind(player, target);
		return true;
	}

	private static LivingEntity findTarget(ServerPlayer player, float chargePercent) {
		Vec3 start = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		if (look.lengthSqr() < 0.001D) {
			look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		}
		look = look.normalize();
		double range = Mth.lerp(chargePercent, CAPTURE_RANGE * 0.78D, CAPTURE_RANGE);
		Vec3 end = start.add(look.scale(range));
		AABB searchArea = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, end, searchArea, target -> target instanceof LivingEntity living && living.isAlive() && target.isPickable() && target != player);
		if (hit != null && hit.getEntity() instanceof LivingEntity living) {
			return living;
		}
		return null;
	}

	private static void startBind(ServerPlayer caster, LivingEntity target) {
		Vec3 targetPos = target.position();
		BindState state = new BindState(target.getUUID(), target.getId(), targetPos);
		ACTIVE_BY_CASTER.put(caster.getUUID(), state);
		CASTER_BY_TARGET.put(target.getUUID(), caster.getUUID());
		lockTarget(target, targetPos);
		if (caster.level() instanceof ServerLevel level) {
			Vec3 center = targetPos.add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), center.x, center.y, center.z, 28, 0.45D, 0.55D, 0.45D, 0.08D);
			level.playSound(null, target.blockPosition(), SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.9F, 0.68F);
			sendState(level, caster, state, SandBindingCoffinStatePacket.START);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer caster)) {
			return;
		}
		BindState state = ACTIVE_BY_CASTER.get(caster.getUUID());
		if (state != null) {
			tickBind(caster, state);
		}
	}

	private static void tickBind(ServerPlayer caster, BindState state) {
		if (!(caster.level() instanceof ServerLevel level)) {
			return;
		}
		Entity found = level.getEntity(state.targetUuid);
		if (!(found instanceof LivingEntity target) || !caster.isAlive() || !target.isAlive()) {
			endBind(caster, state, false);
			return;
		}
		NarutoModVariables.PlayerVariables vars = caster.getData(NarutoModVariables.PLAYER_VARIABLES);
		if (!JUTSU_ID.equals(vars.activeJutsu) || state.ticks >= MAX_HOLD_TICKS || caster.distanceToSqr(target) > BREAK_DISTANCE * BREAK_DISTANCE) {
			endBind(caster, state, false);
			return;
		}
		lockTarget(target, state.targetPos);
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 255, true, false));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 4, true, false));
		target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 8, 6, true, false));
		if (state.ticks > 0 && state.ticks % DAMAGE_INTERVAL_TICKS == 0) {
			target.hurt(caster.damageSources().playerAttack(caster), CRUSH_DAMAGE);
			Vec3 center = state.targetPos.add(0.0D, target.getBbHeight() * 0.52D, 0.0D);
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), center.x, center.y, center.z, 8, 0.32D, 0.42D, 0.32D, 0.04D);
		}
		if (state.ticks % SYNC_INTERVAL_TICKS == 0) {
			sendState(level, caster, state, SandBindingCoffinStatePacket.REFRESH);
		}
		state.ticks++;
		if (state.ticks >= MAX_HOLD_TICKS) {
			endBind(caster, state, true);
		}
	}

	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		Entity source = event.getSource().getEntity();
		if (source instanceof LivingEntity sourceLiving && CASTER_BY_TARGET.containsKey(sourceLiving.getUUID())) {
			event.setCanceled(true);
			return;
		}
		LivingEntity living = event.getEntity();
		if (living.level().isClientSide()) {
			return;
		}
		if (living instanceof ServerPlayer caster) {
			BindState state = ACTIVE_BY_CASTER.get(caster.getUUID());
			if (state != null && event.getAmount() >= INTERRUPT_DAMAGE_THRESHOLD) {
				endBind(caster, state, false);
			}
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity living = event.getEntity();
		if (living.level().isClientSide()) {
			return;
		}
		if (living instanceof ServerPlayer caster) {
			BindState state = ACTIVE_BY_CASTER.get(caster.getUUID());
			if (state != null) {
				endBind(caster, state, false);
			}
		}
		UUID casterId = CASTER_BY_TARGET.get(living.getUUID());
		if (casterId != null && living.level() instanceof ServerLevel level && level.getEntity(casterId) instanceof ServerPlayer caster) {
			BindState state = ACTIVE_BY_CASTER.get(caster.getUUID());
			if (state != null) {
				endBind(caster, state, false);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		Player player = event.getEntity();
		if (player.level().isClientSide()) {
			return;
		}
		if (player instanceof ServerPlayer caster) {
			BindState state = ACTIVE_BY_CASTER.get(caster.getUUID());
			if (state != null) {
				endBind(caster, state, false);
			}
		}
		UUID casterId = CASTER_BY_TARGET.get(player.getUUID());
		if (casterId != null && player.level() instanceof ServerLevel level && level.getEntity(casterId) instanceof ServerPlayer caster) {
			BindState state = ACTIVE_BY_CASTER.get(caster.getUUID());
			if (state != null) {
				endBind(caster, state, false);
			}
		}
	}

	public static void clearInvalidStates() {
		Iterator<Map.Entry<UUID, BindState>> iterator = ACTIVE_BY_CASTER.entrySet().iterator();
		while (iterator.hasNext()) {
			BindState state = iterator.next().getValue();
			if (state.ticks > MAX_HOLD_TICKS + 40) {
				CASTER_BY_TARGET.remove(state.targetUuid);
				iterator.remove();
			}
		}
	}

	private static void endBind(ServerPlayer caster, BindState state, boolean completed) {
		ACTIVE_BY_CASTER.remove(caster.getUUID());
		CASTER_BY_TARGET.remove(state.targetUuid);
		double returnRate = completed ? NORMAL_RETURN_RATE : INTERRUPTED_RETURN_RATE;
		MagnetSandReserve.add(caster, SAND_COST * returnRate);
		if (caster.level() instanceof ServerLevel level) {
			Entity found = level.getEntity(state.targetUuid);
			if (found instanceof LivingEntity target) {
				target.setDeltaMovement(Vec3.ZERO);
				target.hurtMarked = true;
				Vec3 center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
				level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), center.x, center.y, center.z, completed ? 18 : 10, 0.42D, 0.46D, 0.42D, 0.05D);
			}
			level.playSound(null, caster.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.65F, 0.58F);
			sendState(level, caster, state, SandBindingCoffinStatePacket.STOP);
		}
		clearCharge(caster);
	}

	private static void lockTarget(Entity entity, Vec3 position) {
		entity.setDeltaMovement(Vec3.ZERO);
		entity.teleportTo(position.x, position.y, position.z);
		entity.resetFallDistance();
		entity.hurtMarked = true;
	}

	private static void sendState(ServerLevel level, ServerPlayer caster, BindState state, byte action) {
		PacketDistributor.sendToPlayersInDimension(level, new SandBindingCoffinStatePacket(caster.getUUID(), state.targetId, action, MAX_HOLD_TICKS));
	}

	private static void clearCharge(Player player) {
		NarutoModVariables.PlayerVariables vars = player.getData(NarutoModVariables.PLAYER_VARIABLES);
		vars.chargingAbility = false;
		vars.chargeTime = 0;
		vars.markSyncDirty();
	}

	private static final class BindState {
		private final UUID targetUuid;
		private final int targetId;
		private final Vec3 targetPos;
		private int ticks;

		private BindState(UUID targetUuid, int targetId, Vec3 targetPos) {
			this.targetUuid = targetUuid;
			this.targetId = targetId;
			this.targetPos = targetPos;
		}
	}
}
