package dev.xkmc.fastprojectileapi.spellcircle;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.fastprojectileapi.FastProjectileAPI;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.function.BiConsumer;

/**
 * 26.3: render layers are driven from render states, not live entities, so the spell circle data is
 * extracted into the entity render state through a {@link RegisterRenderStateModifiersEvent} modifier
 * and read back in {@link #submit}.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = FastProjectileAPI.MODID)
public class SpellCircleLayer<S extends EntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {

	private static final Identifier SPELL = FastProjectileAPI.loc("textures/entities/spell_circle.png");

	public record Data(Identifier circle, float size, float tick, float bbHeight, float bbWidth) {}

	public static final ContextKey<Data> KEY = new ContextKey<>(FastProjectileAPI.loc("spell_circle"));

	// The raw EntityRenderer.class applies to every entity renderer (NeoForge applies a modifier to
	// subclasses of the registered class); the type parameters are erased here on purpose.
	@SubscribeEvent
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void registerRenderState(RegisterRenderStateModifiersEvent event) {
		event.registerEntityModifier((Class) EntityRenderer.class, (BiConsumer<Entity, EntityRenderState>) (e, s) -> {
			if (!(e instanceof SpellCircleHolder holder)) return;
			if (!holder.shouldShowSpellCircle()) return;
			Identifier id = holder.getSpellCircle();
			if (id == null) return;
			s.setRenderData(KEY, new Data(id, holder.getCircleSize(s.partialTick),
					e.tickCount + s.partialTick, e.getBbHeight(), e.getBbWidth()));
		});
	}

	public SpellCircleLayer(RenderLayerParent<S, M> pRenderer) {
		super(pRenderer);
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float swing, float swingAmp) {
		Data data = state.getRenderData(KEY);
		if (data == null) return;
		SpellComponent component = SpellCircleConfig.getFromConfig(data.circle());
		if (component == null) return;
		pose.pushPose();
		pose.translate(0, data.bbHeight() / 2, data.bbWidth());
		float scale = data.size();
		pose.scale(scale / 16f, scale / 16f, scale / 16f);
		collector.submitCustomGeometry(pose, SpellRenderState.getSpell(SPELL), (entry, vc) -> {
			PoseStack local = new PoseStack();
			local.last().set(entry);
			SpellComponent.RenderHandle handle = new SpellComponent.RenderHandle(local, vc, data.tick(), light);
			component.render(() -> handle);
		});
		pose.popPose();
	}

}
