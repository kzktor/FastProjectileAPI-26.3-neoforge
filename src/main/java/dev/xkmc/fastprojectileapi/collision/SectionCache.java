package dev.xkmc.fastprojectileapi.collision;

import dev.xkmc.fastprojectileapi.mixin.ClientLevelAccessor;
import dev.xkmc.fastprojectileapi.mixin.PersistentEntitySectionManagerAccessor;
import dev.xkmc.fastprojectileapi.mixin.ServerLevelAccessor;
import dev.xkmc.fastprojectileapi.mixin.TransientEntitySectionManagerAccessor;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class SectionCache {

	static SectionCache of(Level level, int x, int y, int z, ChoiceType type) {
		if (level instanceof ServerLevel sl) {
			return ofServer(sl, x, y, z, type);
		} else {
			return ofClient(level, x, y, z, type);
		}
	}

	private static SectionCache ofServer(ServerLevel sl, int x, int y, int z, ChoiceType type) {
		var manager = ((ServerLevelAccessor) sl).getEntityManager();
		var storage = ((PersistentEntitySectionManagerAccessor<Entity>) manager).getSectionStorage();
		return new SectionCache(sl, storage, x, y, z, type);
	}

	private static SectionCache ofClient(Level cl, int x, int y, int z, ChoiceType type) {
		var manager = ((ClientLevelAccessor) cl).getEntityStorage();
		var storage = ((TransientEntitySectionManagerAccessor<Entity>) manager).getSectionStorage();
		return new SectionCache(cl, storage, x, y, z, type);
	}

	private final ChoiceType type;
	private final AABB aabb;
	private final List<Entity> all = new ArrayList<>();
	private final List<Entity> margin = new ArrayList<>();

	SectionCache(Level level, EntitySectionStorage<Entity> storage, int x, int y, int z, ChoiceType type) {
		this.type = type;
		aabb = new AABB(x << 4, y << 4, z << 4,
				(x + 1) << 4, (y + 1) << 4, (z + 1) << 4);
		var sect = storage.getSection(SectionPos.asLong(x, y, z));
		if (sect != null) sect.getEntities().forEach(this::add);
		for (var e : level.dragonParts()) {
			add(e);
		}
	}

	private void add(Entity e) {
		if (!type.test(e)) return;
		var ebox = e.getBoundingBox();
		if (aabb.minX <= ebox.minX && ebox.maxX <= aabb.maxX &&
				aabb.minY <= ebox.minY && ebox.maxY <= aabb.maxY &&
				aabb.minZ <= ebox.minZ && ebox.maxZ <= aabb.maxZ) {
			all.add(e);
		} else {
			all.add(e);
			margin.add(e);
		}
	}

	public Iterable<Entity> intersect(AABB box) {
		return aabb.intersects(box) ? all : margin;
	}

}
