package dev.xkmc.fastprojectileapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.Level;

public interface ClientObjectCache {

	void tick();

	/**
	 * 26.3: geometry is submitted through {@link SubmitNodeCollector} instead of a
	 * {@code MultiBufferSource}, and camera data comes from {@link CameraRenderState}.
	 * {@code pose} is the level pose stack (world origin); submissions translate by
	 * {@code entityPos - camera.pos}.
	 */
	void renderAll(SubmitNodeCollector collector, PoseStack pose, CameraRenderState camera, float pTick, boolean hitboxes);

	interface Provider {

		ClientObjectCache get(Level level);

	}

}
