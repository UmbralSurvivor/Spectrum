package de.dafuqs.spectrum.blocks.dd_deco;

import de.dafuqs.spectrum.registries.*;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.registry.*;
import net.minecraft.server.world.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.feature.*;

import java.util.*;

public class BristleSproutsBlock extends PlantBlock implements Fertilizable {
	
	protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 3.0, 14.0);
	
	public BristleSproutsBlock(AbstractBlock.Settings settings) {
		super(settings);
	}
	
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
	
	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity instanceof LivingEntity && !entity.getType().isIn(SpectrumEntityTypeTags.POKING_DAMAGE_IMMUNE)) {
			entity.slowMovement(state, new Vec3d(0.8, 0.75, 0.8));
			if (!world.isClient && (entity.lastRenderX != entity.getX() || entity.lastRenderZ != entity.getZ())) {
				double difX = Math.abs(entity.getX() - entity.lastRenderX);
				double difZ = Math.abs(entity.getZ() - entity.lastRenderZ);
				if (difX >= 0.003 || difZ >= 0.003) {
					entity.damage(SpectrumDamageTypes.bristeSprouts(world), 1.0F);
				}
			}
		}
    }

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
		return true;
	}

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		Optional<PlacedFeature> feature = world.getRegistryManager().get(RegistryKeys.PLACED_FEATURE).getOrEmpty(SpectrumPlacedFeatures.BRISTLE_SPROUT_PATCH);
		feature.ifPresent(placedFeature -> placedFeature.generate(world, world.getChunkManager().getChunkGenerator(), random, pos));
    }

    @Override
    public float getMaxHorizontalModelOffset() {
        return 0.265F;
    }
}
