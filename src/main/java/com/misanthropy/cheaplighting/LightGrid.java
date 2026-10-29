package com.misanthropy.cheaplighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.Mth;

import java.util.ArrayList;

final class LightGrid {
	static final int MIN_SOURCES = 24;
	private static final int SHIFT = 4;
	private static final double MOVE_MARGIN = 4.0;
	private static final DynamicLightSource[] EMPTY = new DynamicLightSource[0];

	final int stamp;
	private final double reach;
	private final Long2ObjectOpenHashMap<DynamicLightSource[]> cells;

	LightGrid(DynamicLightSource[] view, int stamp, double radius) {
		this.stamp = stamp;
		this.reach = radius + MOVE_MARGIN;
		Long2ObjectOpenHashMap<ArrayList<DynamicLightSource>> building = new Long2ObjectOpenHashMap<>();
		for (DynamicLightSource source : view) {
			long key = key(cell(source.cl$getDynamicLightX()), cell(source.cl$getDynamicLightY()), cell(source.cl$getDynamicLightZ()));
			ArrayList<DynamicLightSource> list = building.get(key);
			if (list == null) {
				list = new ArrayList<>(4);
				building.put(key, list);
			}
			list.add(source);
		}
		this.cells = new Long2ObjectOpenHashMap<>(building.size());
		for (var entry : building.long2ObjectEntrySet()) {
			this.cells.put(entry.getLongKey(), entry.getValue().toArray(EMPTY));
		}
	}

	double query(double x, double y, double z) {
		int minX = cell(x - this.reach), maxX = cell(x + this.reach);
		int minY = cell(y - this.reach), maxY = cell(y + this.reach);
		int minZ = cell(z - this.reach), maxZ = cell(z + this.reach);
		double result = 0;
		for (int cx = minX; cx <= maxX; cx++) {
			for (int cy = minY; cy <= maxY; cy++) {
				for (int cz = minZ; cz <= maxZ; cz++) {
					DynamicLightSource[] sources = this.cells.get(key(cx, cy, cz));
					if (sources == null) {
						continue;
					}
					for (DynamicLightSource source : sources) {
						result = CheapLighting.maxDynamicLightLevel(x, y, z, source, result);
					}
				}
			}
		}
		return result;
	}

	private static int cell(double coordinate) {
		return Mth.floor(coordinate) >> SHIFT;
	}

	private static long key(int x, int y, int z) {
		return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
	}
}
