package io.github.synthrose.artofalchemy.render.model;

import com.mojang.datafixers.util.Pair;
import io.github.synthrose.artofalchemy.blockentity.BlockEntityPipe;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.world.BlockRenderView;

public class PipeModel implements UnbakedModel, BakedModel, FabricBakedModel {
	private static final SpriteIdentifier[] SPRITE_IDS = new SpriteIdentifier[] {
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_core")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_tube")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_endcap")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_sidecap")),
	};
	private Sprite[] SPRITES = new Sprite[SPRITE_IDS.length];

	private static final class FaceMeshes {
		private Mesh tube;
		private Mesh inserterPort;
		private Mesh extractorPort;
		private Mesh passivePort;
	}

	private Mesh coreMesh;
	// Indexed via Direction
	private FaceMeshes[] faceMeshes = new FaceMeshes[6];

	// UnbakedModel
	@Override
	public Collection<Identifier> getModelDependencies() {
		return Collections.emptyList(); // This model does not depend on other models.
	}

	@Override
	public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
		return Arrays.asList(SPRITE_IDS); // The textures this model (and all its model dependencies, and their dependencies, etc...!) depends on.
	}

	private static final class TexCoordEmitter {
		final float minU;
		final float maxU;
		final float minV;
		final float maxV;

		// 'sx' stands for "sixteenth of a sprite texture area"
		public TexCoordEmitter(final Sprite sprite, final int sxMinU, final int sxMinV, final int sxMaxU, final int sxMaxV) {
			final float spriteMinU = sprite.getMinU();
			final float spriteMaxU = sprite.getMaxU();
			final float spriteMinV = sprite.getMinV();
			final float spriteMaxV = sprite.getMaxV();
			final float pxU = (spriteMaxU - spriteMinU) / 16.0f;
			final float pxV = (spriteMaxV - spriteMinV) / 16.0f;
			minU = spriteMinU + pxU * sxMinU;
			maxU = spriteMinU + pxU * sxMaxU;
			minV = spriteMinV + pxV * sxMinV;
			maxV = spriteMinV + pxV * sxMaxV;
		}

		public void emit(final QuadEmitter emitter) {
			// Start at the origin (0,0), and move in clockwise direction
			emitter.sprite(0, 0, minU, minV);
			emitter.sprite(1, 0, minU, maxV);
			emitter.sprite(2, 0, maxU, maxV);
			emitter.sprite(3, 0, maxU, minV);
			emitter.spriteColor(0, -1, -1, -1, -1);
		}
	}

	private static void squareSx(final QuadEmitter emitter, Direction nominalFace, float left, float bottom, float right, float top, float depth) {
		emitter.square(nominalFace, left / 16.0f, bottom / 16.0f, right / 16.0f, top / 16.0f, depth / 16.0f);
	}

	private static void emitPosSx(final QuadEmitter emitter, final Matrix4f transformation, final int i, final int x, final int y, final int z) {
		final Vector4f pos = new Vector4f(x / 16.0f, y / 16.0f, z / 16.0f, 1.0f);
		pos.transform(transformation);
		emitter.pos(i, pos.getX(), pos.getY(), pos.getZ());
	}

	private static void emitRingMesh(final QuadEmitter emitter, final TexCoordEmitter texEmitter, final Matrix4f transformation, final int left, final int bottom, final int right, final int top) {

		emitPosSx(emitter, transformation, 0, right, top,    left);
		emitPosSx(emitter, transformation, 1, right, bottom, left);
		emitPosSx(emitter, transformation, 2, left,  bottom, left);
		emitPosSx(emitter, transformation, 3, left,  top,    left);
		texEmitter.emit(emitter);
		emitter.emit();

		emitPosSx(emitter, transformation, 0, left,  top,    right);
		emitPosSx(emitter, transformation, 1, left,  bottom, right);
		emitPosSx(emitter, transformation, 2, right, bottom, right);
		emitPosSx(emitter, transformation, 3, right, top,    right);
		texEmitter.emit(emitter);
		emitter.emit();

		emitPosSx(emitter, transformation, 0, left, top,     left);
		emitPosSx(emitter, transformation, 1, left, bottom,  left);
		emitPosSx(emitter, transformation, 2, left, bottom, right);
		emitPosSx(emitter, transformation, 3, left, top,    right);
		texEmitter.emit(emitter);
		emitter.emit();

		emitPosSx(emitter, transformation, 0, right, top,    right);
		emitPosSx(emitter, transformation, 1, right, bottom, right);
		emitPosSx(emitter, transformation, 2, right, bottom,  left);
		emitPosSx(emitter, transformation, 3, right, top,     left);
		texEmitter.emit(emitter);
		emitter.emit();
	}

	private static void emitTubeMesh(final QuadEmitter emitter, final TexCoordEmitter texEmitter, final Matrix4f transformation, final int length) {
		final int l = 5 - length;

		emitRingMesh(emitter, texEmitter, transformation, 6, l, 10, 5);
	}

	private static void emitEndCapMesh(final QuadEmitter emitter, final TexCoordEmitter texEmitter, final Matrix4f transformation) {
		emitPosSx(emitter, transformation, 0,  4, 4,  4);
		emitPosSx(emitter, transformation, 1,  4, 4, 12);
		emitPosSx(emitter, transformation, 2, 12, 4, 12);
		emitPosSx(emitter, transformation, 3, 12, 4,  4);
		texEmitter.emit(emitter);
		emitter.emit();

		emitPosSx(emitter, transformation, 0,  4, 0,  4);
		emitPosSx(emitter, transformation, 1, 12, 0,  4);
		emitPosSx(emitter, transformation, 2, 12, 0, 12);
		emitPosSx(emitter, transformation, 3,  4, 0, 12);
		texEmitter.emit(emitter);
		emitter.emit();
	}

	private static void emitPortMesh(final QuadEmitter emitter, final TexCoordEmitter shortTubeTexEmitter, final TexCoordEmitter endTexEmitter, final TexCoordEmitter sideTexEmitter, final Matrix4f transformation) {
		// Short tube
		emitTubeMesh(emitter, shortTubeTexEmitter, transformation, 1);
		// End
		emitEndCapMesh(emitter, endTexEmitter, transformation);
		// Side
		emitRingMesh(emitter, sideTexEmitter, transformation, 4, 0, 12, 4);
	}

	// Transformations that will rotate our base meshes into all 6 cardinal directions
	private static Matrix4f[] buildCardinalTransformations() {
		final Matrix4f[] matrices = new Matrix4f[6];

		final Matrix4f transform = new Matrix4f();
		transform.loadIdentity();
		matrices[Direction.DOWN.ordinal()] = new Matrix4f(transform);

		transform.multiply(Matrix4f.translate(0.5f, 0.5f, 0.5f));

		Matrix4f subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(0, 0, 1), 90, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.WEST.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(0, 0, 1), 180, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.UP.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(0, 0, 1), 270, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.EAST.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(1, 0, 0), 90, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.NORTH.ordinal()] = subTransform;

		subTransform = new Matrix4f(transform);
		subTransform.multiply(new Quaternion(new Vector3f(1, 0, 0), 270, true));
		subTransform.multiply(Matrix4f.translate(-0.5f, -0.5f, -0.5f));
		matrices[Direction.SOUTH.ordinal()] = subTransform;

		return matrices;
	}

	@Override
	public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
		// Get the sprites
		for(int i = 0; i < SPRITE_IDS.length; ++i) {
			SPRITES[i] = textureGetter.apply(SPRITE_IDS[i]);
		}
		final Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		final MeshBuilder builder = renderer.meshBuilder();
		final QuadEmitter emitter = builder.getEmitter();

		// Build core mesh
		final TexCoordEmitter coreTexEmitter = new TexCoordEmitter(SPRITES[0], 5, 5, 11, 11);

		for (final Direction dir : Direction.values()) {
			squareSx(emitter, dir, 5, 5, 11, 11, 5);
			coreTexEmitter.emit(emitter);
			emitter.emit();
		}

		coreMesh = builder.build();

		final Matrix4f[] cardTransforms = buildCardinalTransformations();
		final TexCoordEmitter tubeTexEmitter = new TexCoordEmitter(SPRITES[1], 0, 0, 4, 5);
		final TexCoordEmitter shortTubeTexEmitter = new TexCoordEmitter(SPRITES[1], 0, 0, 4, 1);
		final TexCoordEmitter portEndCapTexEmitter = new TexCoordEmitter(SPRITES[2], 8, 8, 16, 16);
		final TexCoordEmitter portSideCapTexEmitter = new TexCoordEmitter(SPRITES[3], 0, 8, 8, 12);

		for (int i = 0; i < 6; ++i) {
			final FaceMeshes meshes = new FaceMeshes();
			final Matrix4f transformation = cardTransforms[i];

			emitTubeMesh(emitter, tubeTexEmitter, transformation, 5);
			meshes.tube = builder.build();

			emitPortMesh(emitter, shortTubeTexEmitter, portEndCapTexEmitter, portSideCapTexEmitter, transformation);
			meshes.passivePort = builder.build();

			faceMeshes[i] = meshes;
		}


		//~ for(Direction direction : Direction.values()) {
			//~ int spriteIdx = direction == Direction.UP || direction == Direction.DOWN ? 1 : 0;
			//~ // Add a new face to the mesh
			//~ emitter.square(direction, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
			//~ // Set the sprite of the face, must be called after .square()
			//~ // We haven't specified any UV coordinates, so we want to use the whole texture. BAKE_LOCK_UV does exactly that.
			//~ emitter.spriteBake(0, SPRITES[spriteIdx], MutableQuadView.BAKE_LOCK_UV);
			//~ // Enable texture usage
			//~ emitter.spriteColor(0, -1, -1, -1, -1);
			//~ // Add the quad to the mesh
			//~ emitter.emit();
		//~ }

		System.out.printf("Baking pipes yummy =3\n");

		return this;
	}

	// BakedModel
	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
		return null; // Don't need because we use FabricBakedModel instead
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false; // Again, we don't really care, etc...
	}

	@Override
	public boolean hasDepth() {
		return false;
	}

	@Override
	public boolean isSideLit() {
		return false;
	}

	@Override
	public boolean isBuiltin() {
		return false;
	}

	@Override
	public Sprite getSprite() {
		return SPRITES[0]; // Block break particle, let's use furnace_top
	}

	@Override
	public ModelTransformation getTransformation() {
		return null;
	}

	@Override
	public ModelOverrideList getOverrides() {
		return null;
	}

	// FabricBakedModel
	@Override
	public boolean isVanillaAdapter() {
		return false; // False to trigger FabricBakedModel rendering
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockRenderView, BlockState blockState, BlockPos blockPos, Supplier<Random> supplier, RenderContext renderContext) {
		// Render function

		// We just render the mesh
		// XXX
		renderContext.meshConsumer().accept(coreMesh);
		for (int i = 0; i<6; ++i) {
		renderContext.meshConsumer().accept(faceMeshes[i].passivePort);
		}
		//~ renderContext.meshConsumer().accept(faceMeshes[Direction.UP.ordinal()].tube);
		//~ renderContext.meshConsumer().accept(faceMeshes[Direction.NORTH.ordinal()].tube);
		//~ System.out.printf("CHUNKYYYY REBUILDYYY >)\n");
	}

	@Override
	public void emitItemQuads(ItemStack itemStack, Supplier<Random> supplier, RenderContext renderContext) {

	}
}
