package dev.xkmc.fastprojectileapi.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.xkmc.fastprojectileapi.FastProjectileAPI;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 26.3 replacement for the old {@code RenderType.create(name, VertexFormat, Mode, ..., CompositeState.builder()
 * .setShaderState(...).setTextureState(...).setTransparencyState(...))} pipeline, which no longer exists.
 * <p>
 * Every render type now needs a registered {@link RenderPipeline}. Danmaku and spell-circle geometry is
 * {@code POSITION_TEX_COLOR} (position + uv0 + color, no lightmap/overlay), so these pipelines reuse the
 * vanilla {@code core/position_tex_color} shader with a single {@code Sampler0} texture binding, and differ
 * only in backface culling and blend mode.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = FastProjectileAPI.MODID)
public class ProjectileRenderTypes {

	public enum Blend {SOLID, TRANSLUCENT, ADDITIVE}

	private static final RenderPipeline[][] PIPELINES = new RenderPipeline[2][Blend.values().length];

	static {
		for (boolean cull : new boolean[]{false, true}) {
			for (Blend blend : Blend.values()) {
				String name = "projectile_" + (cull ? "cull_" : "nocull_") + blend.name().toLowerCase(Locale.ROOT);
				PIPELINES[cull ? 1 : 0][blend.ordinal()] = build(name, cull, blend);
			}
		}
	}

	private static RenderPipeline build(String name, boolean cull, Blend blend) {
		var builder = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
				.withBindGroupLayout(BindGroupLayouts.PROJECTION)
				.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
				.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
				.withLocation(FastProjectileAPI.loc("pipeline/" + name))
				.withVertexShader("core/position_tex_color")
				.withFragmentShader("core/position_tex_color")
				.withColorTargetState(switch (blend) {
					case SOLID -> ColorTargetState.DEFAULT;
					case TRANSLUCENT -> new ColorTargetState(BlendFunction.TRANSLUCENT);
					case ADDITIVE -> new ColorTargetState(BlendFunction.ADDITIVE);
				})
				.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
				.withPrimitiveTopology(PrimitiveTopology.QUADS)
				.withDepthStencilState(DepthStencilState.DEFAULT)
				.withCull(cull);
		return builder.build();
	}

	@SubscribeEvent
	public static void registerPipelines(RegisterRenderPipelinesEvent event) {
		for (var row : PIPELINES) {
			for (var pipeline : row) {
				event.registerPipeline(pipeline);
			}
		}
	}

	private record Key(String name, Identifier tex, boolean cull, Blend blend) {}

	private static final Map<Key, RenderType> CACHE = new HashMap<>();

	public static RenderType create(String name, Identifier tex, boolean cull, Blend blend) {
		return CACHE.computeIfAbsent(new Key(name, tex, cull, blend), k -> RenderType.create(
				k.name(), RenderSetup.builder(PIPELINES[k.cull() ? 1 : 0][k.blend().ordinal()])
						.withTexture("Sampler0", k.tex())
						.createRenderSetup()));
	}

}
