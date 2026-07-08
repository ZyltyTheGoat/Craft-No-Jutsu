package net.mcreator.naruto.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

import net.mcreator.naruto.init.NarutoModParticleTypes;

import java.util.ArrayList;

public class DrawLightningLineProcedure {
	public static void execute(LevelAccessor world, double end_x, double end_y, double end_z, double start_x, double start_y, double start_z) {
		ArrayList<Object> offsetX = new ArrayList<>();
		ArrayList<Object> offsetY = new ArrayList<>();
		ArrayList<Object> offsetZ = new ArrayList<>();
		boolean hasBranched = false;
		Vec3 dir = Vec3.ZERO;
		Vec3 perp1 = Vec3.ZERO;
		Vec3 perp2 = Vec3.ZERO;
		Vec3 randomDir = Vec3.ZERO;
		Vec3 branchDir = Vec3.ZERO;
		double spacing = 0;
		double totalDist = 0;
		double steps = 0;
		double stepX = 0;
		double stepY = 0;
		double stepZ = 0;
		double segLen = 0;
		double maxDev = 0;
		double zigzagPoints = 0;
		double prog = 0;
		double devStr = 0;
		double rand1 = 0;
		double rand2 = 0;
		double x = 0;
		double y = 0;
		double z = 0;
		double segPos = 0;
		double segIdx = 0;
		double localProg = 0;
		double branchStartX = 0;
		double branchStartY = 0;
		double branchStartZ = 0;
		double branchEndX = 0;
		double branchEndY = 0;
		double branchEndZ = 0;
		double branchDist = 0;
		spacing = 0.01;
		maxDev = 0.8;
		segLen = 25;
		totalDist = Math.sqrt(Math.pow(end_x - start_x, 2) + Math.pow(end_y - start_y, 2) + Math.pow(end_z - start_z, 2));
		steps = Math.round(totalDist / spacing);
		if (steps <= 0) {
			return;
		}
		stepX = (end_x - start_x) / steps;
		stepY = (end_y - start_y) / steps;
		stepZ = (end_z - start_z) / steps;
		dir = (new Vec3(stepX, stepY, stepZ)).normalize();
		perp1 = dir.cross(new Vec3(0, 1, 0)).normalize();
		if (perp1.length() < 0.1) {
			perp1 = dir.cross(new Vec3(1, 0, 0)).normalize();
		}
		perp2 = dir.cross(perp1).normalize();
		zigzagPoints = steps / segLen + 1;
		for (int index0 = 0; index0 < (int) zigzagPoints; index0++) {
			prog = index0 / (zigzagPoints - 1);
			devStr = Math.sin(prog * Math.PI * maxDev);
			rand1 = (Mth.nextDouble(RandomSource.create(), 0, 1) - 0.5) * 2 * devStr;
			rand2 = (Mth.nextDouble(RandomSource.create(), 0, 1) - 0.5) * 2 * devStr;
			offsetX.add(toSupportedType((perp1.x() * rand1 + perp2.x() * rand2)));
			offsetY.add(toSupportedType((perp1.y() * rand1 + perp2.y() * rand2)));
			offsetZ.add(toSupportedType((perp1.z() * rand1 + perp2.z() * rand2)));
		}
		offsetX.set(0, 0);
		offsetY.set(0, 0);
		offsetZ.set(0, 0);
		offsetX.set((int) (zigzagPoints - 1), 0);
		offsetY.set((int) (zigzagPoints - 1), 0);
		offsetZ.set((int) (zigzagPoints - 1), 0);
		for (int index1 = 0; index1 < (int) (steps + 1); index1++) {
			x = start_x + stepX * index1;
			y = start_y + stepY * index1;
			z = start_z + stepZ * index1;
			if (index1 > 0 && index1 < steps) {
				segPos = index1 / segLen;
				segIdx = (int) segPos;
				localProg = segPos - segIdx;
				if (segIdx >= zigzagPoints - 1) {
					segIdx = zigzagPoints - 2;
					localProg = 1;
				}
				x = x + /*@Double*/(getListElement(offsetX, (int) segIdx, Double.class, 0.0))
						+ (/*@Double*/(getListElement(offsetX, (int) (segIdx + 1), Double.class, 0.0)) - /*@Double*/(getListElement(offsetX, (int) segIdx, Double.class, 0.0))) * localProg;
				y = y + /*@Double*/(getListElement(offsetY, (int) segIdx, Double.class, 0.0))
						+ (/*@Double*/(getListElement(offsetY, (int) (segIdx + 1), Double.class, 0.0)) - /*@Double*/(getListElement(offsetY, (int) segIdx, Double.class, 0.0))) * localProg;
				z = z + /*@Double*/(getListElement(offsetZ, (int) segIdx, Double.class, 0.0))
						+ (/*@Double*/(getListElement(offsetZ, (int) (segIdx + 1), Double.class, 0.0)) - /*@Double*/(getListElement(offsetZ, (int) segIdx, Double.class, 0.0))) * localProg;
			}
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (NarutoModParticleTypes.LIGHTNING.get()), x, y, z, 1, 0, 0, 0, 0);
			if (!hasBranched && index1 > steps * 0.2 && Math.random() <= 0.05) {
				hasBranched = true;
				branchDist = totalDist * Mth.nextDouble(RandomSource.create(), 0.1, 0.3);
				randomDir = (new Vec3((Mth.nextDouble(RandomSource.create(), -1, 1)), (Mth.nextDouble(RandomSource.create(), -1, 1)), (Mth.nextDouble(RandomSource.create(), -1, 1)))).normalize();
				branchDir = ((dir.scale(0.3)).add((randomDir.scale(0.3)))).normalize();
				branchStartX = x;
				branchStartY = y;
				branchStartZ = z;
				branchEndX = x + branchDir.x() * branchDist;
				branchEndY = y + branchDir.y() * branchDist;
				branchEndZ = z + branchDir.z() * branchDist;
			}
		}
		if (hasBranched) {
			offsetX.clear();
			offsetY.clear();
			offsetZ.clear();
			totalDist = Math.sqrt(Math.pow(branchEndX - branchStartX, 2) + Math.pow(branchEndY - branchStartY, 2) + Math.pow(branchEndZ - branchStartZ, 2));
			steps = Math.round(totalDist / spacing);
			if (steps > 0) {
				stepX = (branchEndX - branchStartX) / steps;
				stepY = (branchEndY - branchStartY) / steps;
				stepZ = (branchEndZ - branchStartZ) / steps;
				dir = (new Vec3(stepX, stepY, stepZ)).normalize();
				perp1 = dir.cross(new Vec3(0, 1, 0)).normalize();
				if (perp1.length() < 0.1) {
					perp1 = dir.cross(new Vec3(1, 0, 0)).normalize();
				}
				perp2 = dir.cross(perp1).normalize();
				zigzagPoints = steps / segLen + 1;
				for (int index2 = 0; index2 < (int) zigzagPoints; index2++) {
					prog = index2 / (zigzagPoints - 1);
					devStr = Math.sin(prog * Math.PI * maxDev);
					rand1 = (Mth.nextDouble(RandomSource.create(), 0, 1) - 0.5) * 2 * devStr;
					rand2 = (Mth.nextDouble(RandomSource.create(), 0, 1) - 0.5) * 2 * devStr;
					offsetX.add(toSupportedType((perp1.x() * rand1 + perp2.x() * rand2)));
					offsetY.add(toSupportedType((perp1.y() * rand1 + perp2.y() * rand2)));
					offsetZ.add(toSupportedType((perp1.z() * rand1 + perp2.z() * rand2)));
				}
				offsetX.set(0, 0);
				offsetY.set(0, 0);
				offsetZ.set(0, 0);
				offsetX.set((int) (zigzagPoints - 1), 0);
				offsetY.set((int) (zigzagPoints - 1), 0);
				offsetZ.set((int) (zigzagPoints - 1), 0);
				for (int index3 = 0; index3 < (int) (steps + 1); index3++) {
					x = branchStartX + stepX * index3;
					y = branchStartY + stepY * index3;
					z = branchStartZ + stepZ * index3;
					if (index3 > 0 && index3 < steps) {
						segPos = index3 / segLen;
						segIdx = (int) segPos;
						localProg = segPos - segIdx;
						if (segIdx >= zigzagPoints - 1) {
							segIdx = zigzagPoints - 2;
							localProg = 1;
						}
						x = x + /*@Double*/(getListElement(offsetX, (int) segIdx, Double.class, 0.0))
								+ (/*@Double*/(getListElement(offsetX, (int) (segIdx + 1), Double.class, 0.0)) - /*@Double*/(getListElement(offsetX, (int) segIdx, Double.class, 0.0))) * localProg;
						y = y + /*@Double*/(getListElement(offsetY, (int) segIdx, Double.class, 0.0))
								+ (/*@Double*/(getListElement(offsetY, (int) (segIdx + 1), Double.class, 0.0)) - /*@Double*/(getListElement(offsetY, (int) segIdx, Double.class, 0.0))) * localProg;
						z = z + /*@Double*/(getListElement(offsetZ, (int) segIdx, Double.class, 0.0))
								+ (/*@Double*/(getListElement(offsetZ, (int) (segIdx + 1), Double.class, 0.0)) - /*@Double*/(getListElement(offsetZ, (int) segIdx, Double.class, 0.0))) * localProg;
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (NarutoModParticleTypes.LIGHTNING.get()), x, y, z, 1, 0, 0, 0, 0);
				}
			}
		}
	}

	private static double toSupportedType(Number value) {
		return value.doubleValue();
	}

	private static <E> E toSupportedType(E e) {
		return e;
	}

	private static <E> E getListElement(ArrayList<Object> objects, int index, Class<E> eClass, Object defaultValue) {
		if (index < objects.size()) {
			var element = objects.get(index);
			if (eClass.isInstance(element)) {
				return eClass.cast(element);
			}
		}
		return eClass.cast(defaultValue);
	}
}