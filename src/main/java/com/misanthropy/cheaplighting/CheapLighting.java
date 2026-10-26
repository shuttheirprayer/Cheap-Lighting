package com.misanthropy.cheaplighting;

import com.misanthropy.cheaplighting.accessor.WorldRendererAccessor;
import com.misanthropy.cheaplighting.api.DynamicLightHandlers;
import com.misanthropy.cheaplighting.api.item.ItemLightSources;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

@Mod("cheaplighting")
public class CheapLighting  {
	public static final String NAMESPACE = "cheaplighting";
	private static final double MAX_RADIUS = 7.75;
	private static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
	private static CheapLighting INSTANCE;
	public static BlockPos lastLitPos;
	public static int lastLitColor;
	private static final DynamicLightSource[] NO_LIGHT_SOURCES = new DynamicLightSource[0];
	public final Logger logger = LoggerFactory.getLogger(NAMESPACE);
	public final DynamicLightsConfig config = new DynamicLightsConfig();
	private final Set<DynamicLightSource> dynamicLightSources = new HashSet<>();
	private final ReentrantReadWriteLock lightSourcesLock = new ReentrantReadWriteLock();
	private volatile DynamicLightSource[] lightSourcesView = NO_LIGHT_SOURCES;
	private long lastUpdate = System.currentTimeMillis();
	private int lastUpdateCount = 0;

	public CheapLighting() {
		INSTANCE = this;
		var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::clientSetup);
		modEventBus.addListener(this::registerReloadListeners);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, DynamicLightsConfig.SPECS);
	}

	public void onInitializeClient() {
		this.log("Initializing CheapLighting...");
		DynamicLightHandlers.registerDefaultHandlers();
	}

	private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((ResourceManagerReloadListener) ItemLightSources::load);
	}

	public void clientSetup(FMLClientSetupEvent event) {
		onInitializeClient();

		if (net.minecraftforge.fml.ModList.get().isLoaded("embeddium")) {
			XenonSodiumOptions.register();
		}
	}

	private void refreshLightSourcesView() {
		this.lightSourcesView = this.dynamicLightSources.toArray(NO_LIGHT_SOURCES);
	}

	public void updateAll(@NotNull LevelRenderer renderer) {
		if (!this.config.getDynamicLightsMode().isEnabled())
			return;

		long now = System.currentTimeMillis();
		if (now >= this.lastUpdate + 50) {
			this.lastUpdate = now;
			this.lastUpdateCount = 0;

			for (var lightSource : this.lightSourcesView) {
				if (lightSource.cheaplighting$updateDynamicLight(renderer)) this.lastUpdateCount++;
			}
		}
	}

	public int getLastUpdateCount() {
		return this.lastUpdateCount;
	}

	public int getLightmapWithDynamicLight(@NotNull BlockPos pos, int lightmap) {
		return this.getLightmapWithDynamicLight(this.getDynamicLightLevel(pos), lightmap);
	}

	public int getLightmapWithDynamicLight(@NotNull Entity entity, int lightmap) {
		int posLightLevel = (int) this.getDynamicLightLevel(entity.getOnPos());
		int entityLuminance = ((DynamicLightSource) entity).cl$getLuminance();

		return this.getLightmapWithDynamicLight(Math.max(posLightLevel, entityLuminance), lightmap);
	}

	public int getLightmapWithDynamicLight(double dynamicLightLevel, int lightmap) {
		if (dynamicLightLevel > 0) {
			int blockLevel = LightTexture.block(lightmap);
			if (dynamicLightLevel > blockLevel) {
				int luminance = (int) (dynamicLightLevel * 16.0);
				lightmap &= 0xffff0000;
				lightmap |= luminance & 0x0000ffff;
			}
		}

		return lightmap;
	}

	public double getDynamicLightLevel(@NotNull BlockPos pos) {
		return this.getDynamicLightLevel(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	public double getDynamicLightLevel(double x, double y, double z) {
		var lightSources = this.lightSourcesView;
		if (lightSources.length == 0)
			return 0;

		double result = 0;
		for (var lightSource : lightSources) {
			result = maxDynamicLightLevel(x, y, z, lightSource, result);
		}

		return Mth.clamp(result, 0, 15);
	}

	public static double maxDynamicLightLevel(double x, double y, double z, @NotNull DynamicLightSource lightSource, double currentLightLevel) {
		int luminance = lightSource.cl$getLuminance();
		if (luminance > 0 && luminance > currentLightLevel) {
			double dx = x - lightSource.cl$getDynamicLightX();
			double dy = y - lightSource.cl$getDynamicLightY();
			double dz = z - lightSource.cl$getDynamicLightZ();

			double distanceSquared = dx * dx + dy * dy + dz * dz;

			if (distanceSquared <= MAX_RADIUS_SQUARED) {
				double multiplier = 1.0 - Math.sqrt(distanceSquared) / MAX_RADIUS;
				double lightLevel = multiplier * (double) luminance;
				if (lightLevel > currentLightLevel) {
					return lightLevel;
				}
			}
		}
		return currentLightLevel;
	}

	public void addLightSource(@NotNull DynamicLightSource lightSource) {
		if (!lightSource.cl$getDynamicLightLevel().isClientSide())
			return;
		if (!this.config.getDynamicLightsMode().isEnabled())
			return;
		if (this.containsLightSource(lightSource))
			return;
		this.lightSourcesLock.writeLock().lock();
		if (this.dynamicLightSources.add(lightSource))
			this.refreshLightSourcesView();
		this.lightSourcesLock.writeLock().unlock();
	}

	public boolean containsLightSource(@NotNull DynamicLightSource lightSource) {
		if (!lightSource.cl$getDynamicLightLevel().isClientSide())
			return false;

		boolean result;
		this.lightSourcesLock.readLock().lock();
		result = this.dynamicLightSources.contains(lightSource);
		this.lightSourcesLock.readLock().unlock();
		return result;
	}

	public int getLightSourcesCount() {
		return this.lightSourcesView.length;
	}

	public void removeLightSource(@NotNull DynamicLightSource lightSource) {
		this.lightSourcesLock.writeLock().lock();

		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSource it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			if (it.equals(lightSource)) {
				dynamicLightSources.remove();
				this.refreshLightSourcesView();
				lightSource.cheaplighting$scheduleTrackedChunksRebuild(Minecraft.getInstance().levelRenderer);
				break;
			}
		}

		this.lightSourcesLock.writeLock().unlock();
	}

	public void clearLightSources() {
		this.removeLightSources(lightSource -> true);
	}

	public void removeLightSources(@NotNull Predicate<DynamicLightSource> filter) {
		this.lightSourcesLock.writeLock().lock();

		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSource it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			if (filter.test(it)) {
				dynamicLightSources.remove();
				if (it.cl$getLuminance() > 0)
					it.cl$resetDynamicLight();
				it.cheaplighting$scheduleTrackedChunksRebuild(Minecraft.getInstance().levelRenderer);
			}
		}

		this.refreshLightSourcesView();
		this.lightSourcesLock.writeLock().unlock();
	}

	public void removeEntitiesLightSource() {
		this.removeLightSources(lightSource -> (lightSource instanceof Entity && !(lightSource instanceof Player)));
	}

	public void removeCreeperLightSources() {
		this.removeLightSources(entity -> entity instanceof Creeper);
	}

	public void removeTntLightSources() {
		this.removeLightSources(entity -> entity instanceof PrimedTnt);
	}

	public void removeBlockEntitiesLightSource() {
		this.removeLightSources(lightSource -> lightSource instanceof BlockEntity);
	}

	public void log(String info) {
        this.logger.info("[LambDynLights] {}", info);
	}

	public void warn(String info) {
        this.logger.warn("[LambDynLights] {}", info);
	}

	public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, @NotNull BlockPos chunkPos) {
		scheduleChunkRebuild(renderer, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
	}

	public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, long chunkPos) {
		scheduleChunkRebuild(renderer, BlockPos.getX(chunkPos), BlockPos.getY(chunkPos), BlockPos.getZ(chunkPos));
	}

	public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, int x, int y, int z) {
		if (Minecraft.getInstance().level != null)
			((WorldRendererAccessor) renderer).cheaplighting$scheduleChunkRebuild(x, y, z, false);
	}

	public static void updateTrackedChunks(@NotNull BlockPos chunkPos, @Nullable LongOpenHashSet old, @Nullable LongOpenHashSet newPos) {
		if (old != null || newPos != null) {
			long pos = chunkPos.asLong();
			if (old != null)
				old.remove(pos);
			if (newPos != null)
				newPos.add(pos);
		}
	}

	public static void updateTracking(@NotNull DynamicLightSource lightSource) {
		boolean enabled = lightSource.cl$isDynamicLightEnabled();
		int luminance = lightSource.cl$getLuminance();

		if (!enabled && luminance > 0) {
			lightSource.cl$setDynamicLightEnabled(true);
		} else if (enabled && luminance < 1) {
			lightSource.cl$setDynamicLightEnabled(false);
		}
	}

	private static boolean isEyeSubmergedInFluid(LivingEntity entity) {
		if (!CheapLighting.get().config.getWaterSensitiveCheck().get()) {
			return false;
		}

		var eyePos = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());
		return !entity.level().getFluidState(eyePos).isEmpty();
	}

	public static int getLivingEntityLuminanceFromItems(LivingEntity entity) {
		boolean submergedInFluid = isEyeSubmergedInFluid(entity);
		int luminance = 0;

		for (var equipped : entity.getAllSlots()) {
			if (!equipped.isEmpty())
				luminance = Math.max(luminance, CheapLighting.getLuminanceFromItemStack(equipped, submergedInFluid));
		}

		return luminance;
	}

	public static int getLuminanceFromItemStack(@NotNull ItemStack stack, boolean submergedInWater) {
		return ItemLightSources.getLuminance(stack, submergedInWater);
	}

	public static CheapLighting get() {
		return INSTANCE;
	}
}
