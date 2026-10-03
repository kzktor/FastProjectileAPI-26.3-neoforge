package dev.xkmc.fastprojectileapi.spellcircle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xkmc.fastprojectileapi.FastProjectileAPI;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SerialClass
public class SpellComponent {

	@Nullable
	public static SpellComponent getFromConfig(String s) {
		return FastProjectileAPI.SPELL.getMerged().map.get(s);
	}

	@SerialField
	public ArrayList<Stroke> strokes = new ArrayList<>();

	@SerialField
	public ArrayList<Layer> layers = new ArrayList<>();

	public void render(Supplier<RenderHandle> handle) {
		handle.get().matrix.pushPose();
		for (Stroke stroke : strokes) {
			stroke.render(handle);
		}
		for (Layer layer : layers) {
			layer.render(handle);
		}
		handle.get().matrix.popPose();
	}

	@SerialClass
	public static class Value {

		@SerialField
		public float value, delta, amplitude, period, dt;

		public float get(float tick) {
			float ans = value + delta * tick;
			if (period > 0) ans += amplitude * (float) Math.sin((tick - dt) * 2 * Math.PI / period);
			return ans;
		}

	}

	@SerialClass
	public static class Stroke {

		@SerialField
		public int vertex, cycle = 1, rune = 0;

		@SerialField
		public String color;

		@SerialField
		public float width, radius, z, angle;

		public void render(Supplier<RenderHandle> handle) {
			float da = (float) Math.PI * 2 * cycle / vertex;
			float a = angle;
			float w = width / (float) Math.cos(da / 2);
			int col = getColor();
			float dv = (rune > 0 ? 8 : 1) / 128f;
			float du = (int) (Math.PI * 2 * radius * cycle / width * 8) / 8f / vertex * dv;
			for (int i = 0; i < vertex; i++) {
				handle.get().rect(a + da * i, da, radius, w, z, col, i * du, rune == 0 ? 0 : (rune - 1) * dv, du, dv);
			}

		}

		private int getColor() {
			if (color == null) return -1;
			String str = color;
			if (str.startsWith("0x")) {
				str = str.substring(2);
			}
			return Integer.parseUnsignedInt(str, 16);
		}

	}

	@SerialClass
	public static class Layer {

		@SerialField
		public ArrayList<String> children = new ArrayList<>();

		private List<SpellComponent> _children;

		@Nullable
		@SerialField
		public Value z_offset, scale, radius, rotation, alpha;

		public void render(Supplier<RenderHandle> sup) {
			var handle = sup.get();
			if (_children == null) {
				_children = children.stream().map(SpellComponent::getFromConfig).collect(Collectors.toList());
				return;
			}
			int n = _children.size();
			float z = get(z_offset, sup, 0);
			float s = get(scale, sup, 1);
			float a = get(rotation, sup, 0);
			double r = get(radius, sup, 0);
			float al = handle.alpha;
			if (alpha != null) {
				handle.alpha *= alpha.get(handle.tick);
			}
			handle.matrix.pushPose();
			handle.matrix.translate(0, 0, z);
			handle.matrix.scale(s, s, s);
			for (SpellComponent child : _children) {
				handle.matrix.pushPose();
				handle.matrix.rotate(Axis.ZP.rotationDegrees(a));
				handle.matrix.translate(r, 0, 0);
				child.render(sup);
				handle.matrix.popPose();
				a += 360f / n;
			}
			handle.matrix.popPose();
			handle.alpha = al;
		}

		private float get(@Nullable Value val, Supplier<RenderHandle> handle, float def) {
			return val == null ? def : val.get(handle.get().tick);
		}


	}

	public static class RenderHandle {

		public final PoseStack matrix;
		public final VertexConsumer builder;
		public final float tick;
		public final int light;

		public float alpha = 1;

		public RenderHandle(PoseStack matrix, VertexConsumer builder, float tick, int light) {
			this.matrix = matrix;
			this.builder = builder;
			this.tick = tick;
			this.light = light;
		}

		private void rect(float a, float da, float r, float w, float z, int col, float u, float v, float du, float dv) {
			float inner = r - w / 2;
			float outer = r + w / 2;
			vertex(a, inner, z, col, u, v);
			vertex(a, outer, z, col, u, v + dv);
			vertex(a + da, outer, z, col, u + du, v + dv);
			vertex(a + da, inner, z, col, u + du, v);
		}

		private void vertex(float a, float r, float z, int col, float u, float v) {
			int alp = (int) ((col >> 24 & 0xff) * alpha);
			builder.addVertex(matrix.last().pose(),
					r * (float) Math.cos(a),
					r * (float) Math.sin(a),
					z).setUv(u, v).setColor(
					col >> 16 & 0xff,
					col >> 8 & 0xff,
					col & 0xff,
					alp);
		}

	}

}
