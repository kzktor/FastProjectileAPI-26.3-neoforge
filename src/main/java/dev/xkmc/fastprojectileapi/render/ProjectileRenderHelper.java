package dev.xkmc.fastprojectileapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.fastprojectileapi.FastProjectileAPI;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT, modid = FastProjectileAPI.MODID)
public class ProjectileRenderHelper {

	public static final List<ClientObjectCache.Provider> LIST = new ArrayList<>();

	private static RenderQueue QUEUE;

	public static synchronized void setup() {
		ProjTypeHolder.setup();
		QUEUE = new RenderQueue();
	}

	public static <T extends RenderableProjectileType<T, I>, I> Collection<I> setOf(ProjTypeHolder<T, I> key) {
		return QUEUE.setOf(key);
	}

	public static <T extends RenderableProjectileType<T, I>, I> void add(ProjTypeHolder<T, I> key, I ins) {
		setOf(key).add(ins);
	}

	@SubscribeEvent
	public static void clientTick(LevelTickEvent.Post event) {
		var level = Minecraft.getInstance().level;
		if (level != event.getLevel()) return;
		for (var e : LIST) {
			e.get(level).tick();
		}
	}

	/**
	 * 26.3: {@code RenderLevelStageEvent.Stage.AFTER_ENTITIES/AFTER_PARTICLES} are gone and
	 * {@code MultiBufferSource} no longer exists. Custom geometry is now submitted through
	 * {@link SubmitCustomGeometryEvent}, using the level pose stack and the camera render state.
	 */
	@SubscribeEvent
	public static void submitGeometry(SubmitCustomGeometryEvent event) {
		var level = Minecraft.getInstance().level;
		if (level == null || QUEUE == null) return;
		var state = event.getLevelRenderState();
		PoseStack pose = event.getPoseStack();
		SubmitNodeCollector collector = event.getSubmitNodeCollector();
		float pTick = state.worldPartialTicks;
		boolean hitboxes = Minecraft.getInstance().options.keyDebugShowHitboxes.isDown()
				&& !Minecraft.getInstance().showOnlyReducedInfo();
		for (var e : LIST) {
			e.get(level).renderAll(collector, pose, state.cameraRenderState, pTick, hitboxes);
		}
		QUEUE.flush(collector, pose);
	}

	private static class RenderQueue {

		private final ArrayList<?>[] lists = new ArrayList<?>[ProjTypeHolder.HOLDERS.size()];

		public <I> ArrayList<I> setOf(ProjTypeHolder<?, I> key) {
			if (lists[key.index] == null) {
				lists[key.index] = new ArrayList<>();
			}
			return Wrappers.cast(lists[key.index]);
		}

		public void flush(SubmitNodeCollector collector, PoseStack pose) {
			int n = lists.length;
			for (int i = 0; i < n; i++) {
				var list = lists[i];
				lists[i] = null;
				if (list != null) {
					ProjTypeHolder.HOLDERS.get(i).type.start(collector, pose, Wrappers.cast(list));
				}
			}
		}

	}

}
