package com.misanthropy.cheaplighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.Mth;

import java.util.Arrays;

final class LightGrid {
	static final int MIN_SOURCES = 24;
	private static final int SHIFT = 3;
	private static final int CELL_SIZE = 1 << SHIFT;

	final int stamp;
	private final double radius;
	private final Long2IntOpenHashMap cellIndex;
	private final int[] cellStart;
	private final int[] cellMaxLuminance;
	private final double[] xs;
	private final double[] ys;
	private final double[] zs;
	private final int[] luminances;

	LightGrid(DynamicLightSource[] view, int stamp, double radius) {
		this.stamp = stamp;
		this.radius = radius;
		int count = view.length;
		long[] order = new long[count];
		double[] rawX = new double[count];
		double[] rawY = new double[count];
		double[] rawZ = new double[count];
		int[] rawLuminance = new int[count];
		int live = 0;
		for (DynamicLightSource source : view) {
			int luminance = source.cl$getLuminance();
			if (luminance <= 0) {
				continue;
			}
			rawX[live] = source.cl$getDynamicLightX();
			rawY[live] = source.cl$getDynamicLightY();
			rawZ[live] = source.cl$getDynamicLightZ();
			rawLuminance[live] = luminance;
			live++;
		}
		long[] keys = new long[live];
		Integer[] indices = new Integer[live];
		for (int i = 0; i < live; i++) {
			keys[i] = key(cell(rawX[i]), cell(rawY[i]), cell(rawZ[i]));
			indices[i] = i;
		}
		Arrays.sort(indices, (a, b) -> Long.compare(keys[a], keys[b]));
		this.xs = new double[live];
		this.ys = new double[live];
		this.zs = new double[live];
		this.luminances = new int[live];
		this.cellIndex = new Long2IntOpenHashMap(Math.max(16, live));
		this.cellIndex.defaultReturnValue(-1);
		int[] starts = new int[live + 1];
		int[] maxima = new int[live + 1];
		int cells = 0;
		long previous = 0L;
		for (int i = 0; i < live; i++) {
			int from = indices[i];
			this.xs[i] = rawX[from];
			this.ys[i] = rawY[from];
			this.zs[i] = rawZ[from];
			this.luminances[i] = rawLuminance[from];
			long key = keys[from];
			if (i == 0 || key != previous) {
				this.cellIndex.put(key, cells);
				starts[cells] = i;
				maxima[cells] = 0;
				cells++;
				previous = key;
			}
			maxima[cells - 1] = Math.max(maxima[cells - 1], rawLuminance[from]);
		}
		starts[cells] = live;
		this.cellStart = starts;
		this.cellMaxLuminance = maxima;
	}

	double query(double x, double y, double z) {
		double result = this.scan(this.cellIndex.get(key(cell(x), cell(y), cell(z))), x, y, z, 0);
		int homeX = cell(x), homeY = cell(y), homeZ = cell(z);
		int minX = cell(x - this.radius), maxX = cell(x + this.radius);
		int minY = cell(y - this.radius), maxY = cell(y + this.radius);
		int minZ = cell(z - this.radius), maxZ = cell(z + this.radius);
		double radius = this.radius;
		for (int cx = minX; cx <= maxX; cx++) {
			double dx = gap(x, cx);
			for (int cy = minY; cy <= maxY; cy++) {
				double dy = gap(y, cy);
				double xy = dx * dx + dy * dy;
				for (int cz = minZ; cz <= maxZ; cz++) {
					if (cx == homeX && cy == homeY && cz == homeZ) {
						continue;
					}
					int cell = this.cellIndex.get(key(cx, cy, cz));
					if (cell < 0) {
						continue;
					}
					double dz = gap(z, cz);
					double near = xy + dz * dz;
					if (near > radius * radius) {
						continue;
					}
					if (this.cellMaxLuminance[cell] * (1.0 - Math.sqrt(near) / radius) <= result) {
						continue;
					}
					result = this.scan(cell, x, y, z, result);
				}
			}
		}
		return result;
	}

	private double scan(int cell, double x, double y, double z, double current) {
		if (cell < 0) {
			return current;
		}
		double result = current;
		double radius = this.radius;
		int end = this.cellStart[cell + 1];
		for (int i = this.cellStart[cell]; i < end; i++) {
			int luminance = this.luminances[i];
			if (luminance <= result) {
				continue;
			}
			double dx = x - this.xs[i];
			double dy = y - this.ys[i];
			double dz = z - this.zs[i];
			double distanceSquared = dx * dx + dy * dy + dz * dz;
			if (distanceSquared <= radius * radius) {
				double level = (1.0 - Math.sqrt(distanceSquared) / radius) * luminance;
				if (level > result) {
					result = level;
				}
			}
		}
		return result;
	}

	private static double gap(double coordinate, int cell) {
		double low = (double) (cell << SHIFT);
		double high = low + CELL_SIZE;
		return coordinate < low ? low - coordinate : coordinate > high ? coordinate - high : 0.0;
	}

	private static int cell(double coordinate) {
		return Mth.floor(coordinate) >> SHIFT;
	}

	private static long key(int x, int y, int z) {
		return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
	}
}
