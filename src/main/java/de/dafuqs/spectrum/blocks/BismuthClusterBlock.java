package de.dafuqs.spectrum.blocks;

import de.dafuqs.spectrum.networking.*;
import de.dafuqs.spectrum.particle.*;
import net.minecraft.block.*;
import net.minecraft.registry.tag.*;
import net.minecraft.server.world.*;
import net.minecraft.sound.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.*;
import net.minecraft.world.*;
import org.jetbrains.annotations.*;

public class BismuthClusterBlock extends AmethystClusterBlock {
	
	public static final int GROWTH_CHECK_RADIUS = 3;
	public static final int GROWTH_CHECK_TRIES = 5;
	public static final TagKey<Block> CONSUMED_TAG_TO_GROW = BlockTags.BEACON_BASE_BLOCKS;
	public static final BlockState CONSUMED_TARGET_STATE = Blocks.COBBLESTONE.getDefaultState();
	
	public final int height;
	public final @Nullable AmethystClusterBlock grownBlock;
	
	public BismuthClusterBlock(int height, int xzOffset, @Nullable AmethystClusterBlock grownBlock, Settings settings) {
		super(height, xzOffset, settings);
		this.height = height;
		this.grownBlock = grownBlock;
	}
	
	@Override
	public boolean hasRandomTicks(BlockState state) {
		return grownBlock != null;
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		super.randomTick(state, world, pos, random);
		if (!world.isClient && grownBlock != null && searchAndConsumeBlock(world, pos, GROWTH_CHECK_RADIUS, CONSUMED_TAG_TO_GROW, CONSUMED_TARGET_STATE, GROWTH_CHECK_TRIES, random)) {
			BlockState newState = grownBlock.getDefaultState().with(FACING, state.get(FACING)).with(WATERLOGGED, state.get(WATERLOGGED));
			world.setBlockState(pos, newState);
			world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.8F, 0.9F + random.nextFloat() * 0.2F);
			
			Vec3d sourcePos = new Vec3d(pos.getX() + 0.5D, pos.getY() + height / 16.0, pos.getZ() + 0.5D);
			Vec3d randomOffset = new Vec3d(0.25, height / 32.0, 0.25);
			Vec3d randomVelocity = new Vec3d(0.1, 0.1, 0.1);
			SpectrumS2CPacketSender.playParticleWithRandomOffsetAndVelocity(world, sourcePos, SpectrumParticleTypes.YELLOW_CRAFTING, 2, randomOffset, randomVelocity);
			SpectrumS2CPacketSender.playParticleWithRandomOffsetAndVelocity(world, sourcePos, SpectrumParticleTypes.LIME_CRAFTING, 2, randomOffset, randomVelocity);
			SpectrumS2CPacketSender.playParticleWithRandomOffsetAndVelocity(world, sourcePos, SpectrumParticleTypes.PURPLE_CRAFTING, 2, randomOffset, randomVelocity);
			SpectrumS2CPacketSender.playParticleWithRandomOffsetAndVelocity(world, sourcePos, SpectrumParticleTypes.ORANGE_CRAFTING, 2, randomOffset, randomVelocity);
		}
	}
	
	public static boolean searchAndConsumeBlock(World world, BlockPos pos, int radius, TagKey<Block> tagKey, BlockState targetState, int tries, Random random) {
		for (int i = 0; i < tries; i++) {
			BlockPos offsetPos = pos.add(radius - random.nextInt(1 + radius + radius), radius - random.nextInt(1 + radius + radius), radius - random.nextInt(1 + radius + radius));
			BlockState offsetState = world.getBlockState(offsetPos);
			if (offsetState.isIn(tagKey)) {
				world.setBlockState(offsetPos, targetState);
				world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), offsetState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 0.8F, 0.9F + random.nextFloat() * 0.2F);
				return true;
			}
		}
		return false;
	}
	
}
