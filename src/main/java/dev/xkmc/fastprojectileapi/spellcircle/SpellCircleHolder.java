package dev.xkmc.fastprojectileapi.spellcircle;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public interface SpellCircleHolder {

	boolean shouldShowSpellCircle();

	@Nullable
	Identifier getSpellCircle();

	float getCircleSize(float pTick);

}
