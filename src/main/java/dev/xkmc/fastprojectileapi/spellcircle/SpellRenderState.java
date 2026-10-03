package dev.xkmc.fastprojectileapi.spellcircle;

import dev.xkmc.fastprojectileapi.render.ProjectileRenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * 26.3: the old {@code RenderStateShard} + {@code RenderType.CompositeState} construction is gone.
 * The translucent, no-cull, textured quad render type is now produced through
 * {@link ProjectileRenderTypes}.
 */
public class SpellRenderState {

	public static RenderType getSpell(Identifier id) {
		return ProjectileRenderTypes.create("spell_blend", id, false, ProjectileRenderTypes.Blend.TRANSLUCENT);
	}

}
