package io.github.synthrose.artofalchemy.render.model;

import com.mojang.datafixers.util.Pair;
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
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class PipeModel implements UnbakedModel, BakedModel, FabricBakedModel {
	private static final SpriteIdentifier[] SPRITE_IDS = new SpriteIdentifier[]{
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_blocker")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_core")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_endcap")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_sidecap")),
		new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEX, new Identifier("artofalchemy:block/essentia_pipe_tube")),
	};
	private Sprite[] SPRITES = new Sprite[SPRITE_IDS.length];
	private Mesh mesh;

	// UnbakedModel
	@Override
	public Collection<Identifier> getModelDependencies() {
		return Collections.emptyList(); // This model does not depend on other models.
	}

	@Override
	public Collection<SpriteIdentifier> getTextureDependencies(Function<Identifier, UnbakedModel> unbakedModelGetter, Set<Pair<String, String>> unresolvedTextureReferences) {
		return Arrays.asList(SPRITE_IDS); // The textures this model (and all its model dependencies, and their dependencies, etc...!) depends on.
	}

	private void emitCenterTexCoords(final QuadEmitter emitter, final float minU, final float maxU, final float minV, final float maxV) {
		emitter.sprite(0, 0, minU, minV);
		emitter.sprite(1, 0, minU, maxV);
		emitter.sprite(2, 0, maxU, maxV);
		emitter.sprite(3, 0, maxU, minV);
		emitter.spriteColor(0, -1, -1, -1, -1);
	}

	@Override
	public BakedModel bake(ModelLoader loader, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
		// Get the sprites
		for(int i = 0; i < SPRITE_IDS.length; ++i) {
			SPRITES[i] = textureGetter.apply(SPRITE_IDS[i]);
		}
		// Build the mesh using the Renderer API
		final Renderer renderer = RendererAccess.INSTANCE.getRenderer();
		final MeshBuilder builder = renderer.meshBuilder();
		final QuadEmitter emitter = builder.getEmitter();

		final Sprite coreSprite = SPRITES[1];
		final float minU = coreSprite.getMinU();
		final float maxU = coreSprite.getMaxU();
		final float minV = coreSprite.getMinV();
		final float maxV = coreSprite.getMaxV();
		final float pxU = (maxU - minU) / 16.0f;
		final float pxV = (maxV - minV) / 16.0f;
		// Centermost square of the texture, at [5; 11]²
		final float centerMinU = minU + pxU *  5;
		final float centerMaxU = minU + pxU * 11;
		final float centerMinV = minV + pxV *  5;
		final float centerMaxV = minV + pxV * 11;

		emitter.pos(0, 0.25f, 0.25f, 0.25f);
		emitter.pos(1, 0.25f, 0.25f, 0.75f);
		emitter.pos(2, 0.25f, 0.75f, 0.75f);
		emitter.pos(3, 0.25f, 0.75f, 0.25f);
		emitCenterTexCoords(emitter, centerMinU, centerMaxU, centerMinV, centerMaxV);
		emitter.emit();

		emitter.pos(0, 0.75f, 0.25f, 0.25f);
		emitter.pos(1, 0.75f, 0.75f, 0.25f);
		emitter.pos(2, 0.75f, 0.75f, 0.75f);
		emitter.pos(3, 0.75f, 0.25f, 0.75f);
		emitCenterTexCoords(emitter, centerMinU, centerMaxU, centerMinV, centerMaxV);
		emitter.emit();

		// Bottom
		emitter.pos(0, 0.25f, 0.25f, 0.25f);
		emitter.pos(1, 0.75f, 0.25f, 0.25f);
		emitter.pos(2, 0.75f, 0.25f, 0.75f);
		emitter.pos(3, 0.25f, 0.25f, 0.75f);
		emitCenterTexCoords(emitter, centerMinU, centerMaxU, centerMinV, centerMaxV);
		emitter.emit();

		// Top
		emitter.pos(0, 0.25f, 0.75f, 0.25f);
		emitter.pos(1, 0.25f, 0.75f, 0.75f);
		emitter.pos(2, 0.75f, 0.75f, 0.75f);
		emitter.pos(3, 0.75f, 0.75f, 0.25f);
		emitCenterTexCoords(emitter, centerMinU, centerMaxU, centerMinV, centerMaxV);
		emitter.emit();

		emitter.pos(0, 0.25f, 0.25f, 0.25f);
		emitter.pos(1, 0.25f, 0.75f, 0.25f);
		emitter.pos(2, 0.75f, 0.75f, 0.25f);
		emitter.pos(3, 0.75f, 0.25f, 0.25f);
		emitCenterTexCoords(emitter, centerMinU, centerMaxU, centerMinV, centerMaxV);
		emitter.emit();

		emitter.pos(0, 0.25f, 0.25f, 0.75f);
		emitter.pos(1, 0.75f, 0.25f, 0.75f);
		emitter.pos(2, 0.75f, 0.75f, 0.75f);
		emitter.pos(3, 0.25f, 0.75f, 0.75f);
		emitCenterTexCoords(emitter, centerMinU, centerMaxU, centerMinV, centerMaxV);
		emitter.emit();

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
		mesh = builder.build();
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
		return SPRITES[1]; // Block break particle, let's use furnace_top
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
		renderContext.meshConsumer().accept(mesh);
		//~ System.out.printf("CHUNKYYYY REBUILDYYY >)\n");
	}

	@Override
	public void emitItemQuads(ItemStack itemStack, Supplier<Random> supplier, RenderContext renderContext) {

	}
}
