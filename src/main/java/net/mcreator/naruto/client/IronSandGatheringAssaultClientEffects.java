package net.mcreator.naruto.client;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@EventBusSubscriber(value = Dist.CLIENT)
public final class IronSandGatheringAssaultClientEffects {
	private static final List<Assault> ASSAULTS = new ArrayList<>();
	private static final int WEAPON_GRAINS = 1150;
	private static final float GRAIN_BASE_SIZE = 0.028F;
	private static final float GRAIN_SIZE_IMPERFECTION = 0.017F;
	private static final int IMPACT_TICKS = 9;
	private static final double HOMING_STRENGTH = 0.075D;
	private static final int[] GRAIN_COLORS = {0xFF252729, 0xFF2b2d2e, 0xFF333436, 0xFF3b3d3e, 0xFF434547, 0xFF4b4d4f};

	private IronSandGatheringAssaultClientEffects() {
	}

	public static void addAssault(UUID casterId, int targetId, Vec3 origin, Vec3 direction, float range, float radius, float chargePercent, int formationTicks, int travelTicks) {
		if (origin == null || direction == null || direction.lengthSqr() < 0.001D) {
			return;
		}
		ASSAULTS.add(new Assault(casterId, targetId, origin, direction.normalize(), Math.max(0.5F, range), Math.max(0.3F, radius), Mth.clamp(chargePercent, 0.0F, 1.0F), Math.max(6, formationTicks), Math.max(4, travelTicks)));
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		Iterator<Assault> iterator = ASSAULTS.iterator();
		while (iterator.hasNext()) {
			Assault assault = iterator.next();
			assault.age++;
			if (minecraft.level != null && !assault.impacted && assault.age >= assault.formationTicks && assault.age <= assault.formationTicks + assault.travelTicks) {
				tickMovement(minecraft, assault);
			}
			if ((assault.impacted && assault.age > assault.impactAge + IMPACT_TICKS) || assault.age > assault.totalTicks) {
				iterator.remove();
			}
		}
	}

	private static void tickMovement(Minecraft minecraft, Assault assault) {
		Entity target = assault.targetId < 0 ? null : minecraft.level.getEntity(assault.targetId);
		if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
			Vec3 targetCenter = livingTarget.position().add(0.0D, livingTarget.getBbHeight() * 0.55D, 0.0D);
			Vec3 desiredDirection = targetCenter.subtract(assault.position);
			if (desiredDirection.lengthSqr() > 0.001D) {
				assault.direction = steer(assault.direction, desiredDirection.normalize(), HOMING_STRENGTH);
			}
		}
		double step = Math.min(assault.range - assault.distanceTravelled, assault.range / assault.travelTicks);
		if (step <= 0.001D) {
			markImpact(assault);
			return;
		}
		assault.previousPosition = assault.position;
		Vec3 candidate = assault.position.add(assault.direction.scale(step));
		Entity collisionContext = assault.casterId == null ? minecraft.player : minecraft.level.getPlayerByUUID(assault.casterId);
		if (collisionContext == null) {
			collisionContext = minecraft.player;
		}
		if (collisionContext != null) {
			BlockHitResult blockHit = minecraft.level.clip(new ClipContext(assault.position, candidate, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, collisionContext));
			if (blockHit.getType() == HitResult.Type.BLOCK) {
				assault.position = blockHit.getLocation();
				markImpact(assault);
				return;
			}
		}
		assault.position = candidate;
		assault.distanceTravelled += step;
		if (assault.distanceTravelled >= assault.range - 0.001D) {
			markImpact(assault);
		}
	}

	private static void markImpact(Assault assault) {
		assault.impacted = true;
		assault.impactAge = assault.age;
		assault.previousPosition = assault.position;
	}

	private static Vec3 steer(Vec3 current, Vec3 desired, double strength) {
		Vec3 steered = current.scale(1.0D - strength).add(desired.scale(strength));
		return steered.lengthSqr() < 0.001D ? current : steered.normalize();
	}

	@SubscribeEvent
	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ASSAULTS.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		boolean hasRenderableGeometry = false;
		for (Assault assault : ASSAULTS) {
			if (hasRenderableGeometry(assault, partialTick)) {
				hasRenderableGeometry = true;
				break;
			}
		}
		if (!hasRenderableGeometry) {
			return;
		}
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.depthMask(true);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix = poseStack.last().pose();
		for (Assault assault : ASSAULTS) {
			renderAssault(buffer, matrix, minecraft, camera, assault, partialTick);
		}
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static boolean hasRenderableGeometry(Assault assault, float partialTick) {
		float age = assault.age + partialTick;
		if (age > assault.formationTicks * 0.45F) {
			return true;
		}
		for (int i = 0; i < WEAPON_GRAINS; i++) {
			float delay = (i % 29) * 0.18F + hash(i * 43 + 7) * 1.8F;
			if (age > delay) {
				return true;
			}
		}
		return false;
	}

	private static void renderAssault(BufferBuilder buffer, Matrix4f matrix, Minecraft minecraft, Vec3 camera, Assault assault, float partialTick) {
		float age = assault.age + partialTick;
		float impact = assault.impactAge < 0 ? 0.0F : Mth.clamp((age - assault.impactAge) / IMPACT_TICKS, 0.0F, 1.0F);
		Vec3 center = age < assault.formationTicks ? assault.origin : assault.previousPosition.lerp(assault.position, partialTick);
		float spin = age * (0.095F + assault.chargePercent * 0.085F);
		Basis basis = rotatedBasis(assault.direction, spin);
		float alpha = 1.0F - smoothStep(impact);

		Player caster = assault.casterId == null ? null : minecraft.level.getPlayerByUUID(assault.casterId);
		Vec3 source = caster == null ? assault.origin.subtract(assault.direction.scale(1.4D)) : gourdSource(caster, partialTick);
		Vec3 arcSide = basis.right.scale(0.72D).add(basis.up.scale(0.62D));
		for (int i = 0; i < WEAPON_GRAINS; i++) {
			Vec3 settled = grainPoint(center, assault, basis, i);
			Vec3 point;
			float grainAlpha = alpha;
			if (age < assault.formationTicks) {
				float delay = (i % 29) * 0.18F + hash(i * 43 + 7) * 1.8F;
				float gather = Mth.clamp((age - delay) / Math.max(3.0F, assault.formationTicks - 4.5F), 0.0F, 1.0F);
				if (gather <= 0.0F) {
					continue;
				}
				float eased = smoothStep(gather);
				Vec3 control = source.lerp(assault.origin, 0.46D).add(arcSide.scale(0.45D + hash(i * 19 + 3) * 0.5D));
				point = quadraticBezier(source, control, settled, eased);
				double orbit = i * 2.399963D + age * 0.42D;
				double orbitRadius = Math.sin(gather * Math.PI) * (0.09D + hash(i * 31 + 5) * 0.22D);
				point = point.add(basis.right.scale(Math.cos(orbit) * orbitRadius)).add(basis.up.scale(Math.sin(orbit) * orbitRadius));
				grainAlpha *= Mth.clamp(gather * 1.7F, 0.0F, 1.0F);
			} else if (impact > 0.0F) {
				Vec3 burst = settled.subtract(center);
				if (burst.lengthSqr() < 0.001D) {
					burst = basis.right;
				}
				burst = burst.normalize().add(assault.direction.scale(hash(i * 23 + 9) - 0.72D)).normalize();
				point = settled.add(burst.scale(impact * impact * (0.45D + hash(i * 67 + 2) * 1.65D)));
			} else {
				point = settled;
			}
			float size = grainSize(i * 17 + 5);
			addBox(buffer, matrix, point.subtract(camera), size, size * (0.68F + hash(i * 47 + 1) * 0.34F), color(i, grainAlpha));
		}

		if (age > assault.formationTicks * 0.45F && impact < 0.78F) {
			float coreScale = Mth.clamp((age - assault.formationTicks * 0.45F) / (assault.formationTicks * 0.45F), 0.0F, 1.0F) * (1.0F - impact);
			renderCore(buffer, matrix, center.subtract(camera), assault, basis, coreScale, alpha * 0.84F);
		}
	}

	private static void renderCore(BufferBuilder buffer, Matrix4f matrix, Vec3 center, Assault assault, Basis basis, float scale, float alpha) {
		float radius = assault.radius * 0.87F * scale;
		float length = assault.radius * (3.05F + assault.chargePercent * 0.55F) * scale;
		Vec3 tip = center.add(assault.direction.scale(length * 0.62F));
		Vec3 rear = center.subtract(assault.direction.scale(length * 0.38F));
		Vec3 a = rear.add(basis.up.scale(radius));
		Vec3 b = rear.add(basis.right.scale(radius * 0.866D)).subtract(basis.up.scale(radius * 0.5D));
		Vec3 c = rear.subtract(basis.right.scale(radius * 0.866D)).subtract(basis.up.scale(radius * 0.5D));
		addTriangle(buffer, matrix, tip, a, b, color(1, alpha));
		addTriangle(buffer, matrix, tip, b, c, darken(color(2, alpha), 0.72F));
		addTriangle(buffer, matrix, tip, c, a, darken(color(3, alpha), 0.84F));
		addTriangle(buffer, matrix, a, c, b, darken(color(4, alpha), 0.56F));
	}

	private static Vec3 grainPoint(Vec3 center, Assault assault, Basis basis, int seed) {
		float radius = assault.radius * (0.92F + (hash(seed * 13 + 2) - 0.5F) * 0.08F);
		float length = assault.radius * (3.12F + assault.chargePercent * 0.58F);
		Vec3 tip = center.add(assault.direction.scale(length * 0.62F));
		Vec3 rear = center.subtract(assault.direction.scale(length * 0.38F));
		Vec3 a = rear.add(basis.up.scale(radius));
		Vec3 b = rear.add(basis.right.scale(radius * 0.866D)).subtract(basis.up.scale(radius * 0.5D));
		Vec3 c = rear.subtract(basis.right.scale(radius * 0.866D)).subtract(basis.up.scale(radius * 0.5D));
		int face = Math.floorMod(seed, 4);
		Vec3 p0 = face == 3 ? a : tip;
		Vec3 p1 = face == 0 ? a : face == 1 ? b : face == 2 ? c : b;
		Vec3 p2 = face == 0 ? b : face == 1 ? c : face == 2 ? a : c;
		float root = Mth.sqrt(hash(seed * 37 + 11));
		float along = hash(seed * 59 + 17);
		Vec3 point = p0.scale(1.0D - root).add(p1.scale(root * (1.0F - along))).add(p2.scale(root * along));
		Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
		if (normal.lengthSqr() > 0.001D) {
			point = point.add(normal.normalize().scale(grainSize(seed) * (0.35D + hash(seed * 71 + 4) * 0.65D)));
		}
		return point;
	}

	private static Vec3 gourdSource(Player caster, float partialTick) {
		Vec3 position = caster.getPosition(partialTick);
		float bodyYaw = Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot);
		Vec3 forward = Vec3.directionFromRotation(0.0F, bodyYaw).multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		return position.add(forward.scale(-0.55D)).add(right.scale(0.13D)).add(0.0D, 1.56D, 0.0D);
	}

	private static Basis rotatedBasis(Vec3 direction, float angle) {
		Vec3 upHint = Math.abs(direction.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 right = upHint.cross(direction).normalize();
		Vec3 up = direction.cross(right).normalize();
		double cosine = Math.cos(angle);
		double sine = Math.sin(angle);
		return new Basis(right.scale(cosine).add(up.scale(sine)), up.scale(cosine).subtract(right.scale(sine)));
	}

	private static Vec3 quadraticBezier(Vec3 start, Vec3 control, Vec3 end, float t) {
		double inverse = 1.0D - t;
		return start.scale(inverse * inverse).add(control.scale(2.0D * inverse * t)).add(end.scale(t * t));
	}

	private static float grainSize(int seed) {
		return GRAIN_BASE_SIZE + (hash(seed) - 0.5F) * GRAIN_SIZE_IMPERFECTION;
	}

	private static void addBox(BufferBuilder buffer, Matrix4f matrix, Vec3 center, float radius, float halfHeight, int color) {
		Vec3 nnn = center.add(-radius, -halfHeight, -radius);
		Vec3 nnp = center.add(-radius, -halfHeight, radius);
		Vec3 npn = center.add(-radius, halfHeight, -radius);
		Vec3 npp = center.add(-radius, halfHeight, radius);
		Vec3 pnn = center.add(radius, -halfHeight, -radius);
		Vec3 pnp = center.add(radius, -halfHeight, radius);
		Vec3 ppn = center.add(radius, halfHeight, -radius);
		Vec3 ppp = center.add(radius, halfHeight, radius);
		addQuad(buffer, matrix, npn, ppn, ppp, npp, color);
		addQuad(buffer, matrix, pnn, nnn, nnp, pnp, darken(color, 0.54F));
		addQuad(buffer, matrix, nnn, npn, npp, nnp, darken(color, 0.73F));
		addQuad(buffer, matrix, pnp, ppp, ppn, pnn, darken(color, 0.82F));
		addQuad(buffer, matrix, nnp, npp, ppp, pnp, darken(color, 0.68F));
		addQuad(buffer, matrix, pnn, ppn, npn, nnn, darken(color, 0.61F));
	}

	private static void addTriangle(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, int color) {
		addQuad(buffer, matrix, a, b, c, c, color);
	}

	private static void addQuad(BufferBuilder buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
		addVertex(buffer, matrix, a, color);
		addVertex(buffer, matrix, b, color);
		addVertex(buffer, matrix, c, color);
		addVertex(buffer, matrix, d, color);
	}

	private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, int color) {
		buffer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(color);
	}

	private static int color(int seed, float alpha) {
		int rgb = GRAIN_COLORS[Math.floorMod(seed * 31 + 7, GRAIN_COLORS.length)] & 0x00FFFFFF;
		return (Mth.clamp((int) (255.0F * alpha), 0, 255) << 24) | rgb;
	}

	private static int darken(int color, float amount) {
		int a = color >>> 24;
		int r = (int) (((color >>> 16) & 255) * amount);
		int g = (int) (((color >>> 8) & 255) * amount);
		int b = (int) ((color & 255) * amount);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static float hash(int value) {
		int mixed = value * 1664525 + 1013904223;
		mixed ^= mixed >>> 16;
		return (mixed & 0x00FFFFFF) / 16777215.0F;
	}

	private static float smoothStep(float value) {
		value = Mth.clamp(value, 0.0F, 1.0F);
		return value * value * (3.0F - 2.0F * value);
	}

	private record Basis(Vec3 right, Vec3 up) {
	}

	private static final class Assault {
		private final UUID casterId;
		private final int targetId;
		private final Vec3 origin;
		private Vec3 direction;
		private Vec3 position;
		private Vec3 previousPosition;
		private final float range;
		private final float radius;
		private final float chargePercent;
		private final int formationTicks;
		private final int travelTicks;
		private final int totalTicks;
		private double distanceTravelled;
		private int age;
		private int impactAge = -1;
		private boolean impacted;

		private Assault(UUID casterId, int targetId, Vec3 origin, Vec3 direction, float range, float radius, float chargePercent, int formationTicks, int travelTicks) {
			this.casterId = casterId;
			this.targetId = targetId;
			this.origin = origin;
			this.direction = direction;
			this.position = origin;
			this.previousPosition = origin;
			this.range = range;
			this.radius = radius;
			this.chargePercent = chargePercent;
			this.formationTicks = formationTicks;
			this.travelTicks = travelTicks;
			this.totalTicks = formationTicks + travelTicks + IMPACT_TICKS;
		}
	}
}
