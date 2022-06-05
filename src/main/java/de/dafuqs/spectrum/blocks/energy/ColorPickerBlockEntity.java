package de.dafuqs.spectrum.blocks.energy;

import de.dafuqs.spectrum.energy.InkStorageBlockEntity;
import de.dafuqs.spectrum.energy.InkStorageItem;
import de.dafuqs.spectrum.energy.color.InkColor;
import de.dafuqs.spectrum.energy.color.InkColors;
import de.dafuqs.spectrum.energy.storage.InkStorage;
import de.dafuqs.spectrum.energy.storage.TotalCappedSimpleInkStorage;
import de.dafuqs.spectrum.interfaces.PlayerOwned;
import de.dafuqs.spectrum.inventories.ColorPickerScreenHandler;
import de.dafuqs.spectrum.recipe.SpectrumRecipeTypes;
import de.dafuqs.spectrum.recipe.ink_converting.InkConvertingRecipe;
import de.dafuqs.spectrum.registries.SpectrumBlockEntityRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class ColorPickerBlockEntity extends LootableContainerBlockEntity implements ExtendedScreenHandlerFactory, PlayerOwned, InkStorageBlockEntity<TotalCappedSimpleInkStorage> {
	
	private UUID ownerUUID;
	
	public static final int INVENTORY_SIZE = 2; // input & output slots
	public static final int INPUT_SLOT_ID = 0;
	public static final int OUTPUT_SLOT_ID = 1;
	public DefaultedList<ItemStack> inventory;
	
	public static final long TICKS_PER_CONVERSION = 5;
	public static final long STORAGE_AMOUNT = 64*64*64;
	protected TotalCappedSimpleInkStorage inkStorage;
	
	protected boolean paused;
	protected @Nullable InkConvertingRecipe cachedRecipe;
	protected InkColor selectedColor;
	
	public ColorPickerBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(SpectrumBlockEntityRegistry.COLOR_PICKER, blockPos, blockState);
		
		this.inventory = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
		this.inkStorage = new TotalCappedSimpleInkStorage(STORAGE_AMOUNT);
		this.selectedColor = InkColors.BLACK;
	}
	
	public static void tick(World world, BlockPos pos, BlockState state, ColorPickerBlockEntity blockEntity) {
		if (!world.isClient && !blockEntity.paused) {
			boolean didSomething;
			if (world.getTime() % TICKS_PER_CONVERSION == 0) {
				didSomething = blockEntity.tryConvertPigmentToEnergy(world);
			} else {
				didSomething = true;
			}
			didSomething = didSomething | blockEntity.tryFillInkContainer(); // that's an OR
			
			if(didSomething) {
				blockEntity.markDirty();
			} else {
				blockEntity.paused = true;
			}
		}
	}
	
	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		if(nbt.contains("InkStorage", NbtElement.COMPOUND_TYPE)) {
			this.inkStorage = TotalCappedSimpleInkStorage.fromNbt(nbt.getCompound("InkStorage"));
		}
		if(nbt.contains("OwnerUUID")) {
			this.ownerUUID = nbt.getUuid("OwnerUUID");
		} else {
			this.ownerUUID = null;
		}
		if(nbt.contains("SelectedColor", NbtElement.STRING_TYPE)) {
			this.selectedColor = InkColor.of(nbt.getString("SelectedColor"));
		}
	}
	
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.put("InkStorage", this.inkStorage.toNbt());
		if(this.ownerUUID != null) {
			nbt.putUuid("OwnerUUID", this.ownerUUID);
		}
		nbt.putString("SelectedColor", this.selectedColor.toString());
	}
	
	@Override
	protected Text getContainerName() {
		return new TranslatableText("block.spectrum.color_picker");
	}
	
	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new ColorPickerScreenHandler(syncId, playerInventory, this.pos);
	}
	
	@Override
	public UUID getOwnerUUID() {
		return this.ownerUUID;
	}
	
	@Override
	public void setOwner(PlayerEntity playerEntity) {
		this.ownerUUID = playerEntity.getUuid();
	}
	
	@Override
	public TotalCappedSimpleInkStorage getEnergyStorage() {
		return inkStorage;
	}

	
	@Override
	protected DefaultedList<ItemStack> getInvStackList() {
		return this.inventory;
	}
	
	@Override
	protected void setInvStackList(DefaultedList<ItemStack> list) {
		this.inventory = list;
		this.paused = false;
	}
	
	public ItemStack removeStack(int slot, int amount) {
		this.paused = false;
		return super.removeStack(slot, amount);
	}
	
	public ItemStack removeStack(int slot) {
		this.paused = false;
		return super.removeStack(slot);
	}
	
	public void setStack(int slot, ItemStack stack) {
		this.paused = false;
		super.setStack(slot, stack);
	}
	
	@Override
	public int size() {
		return INVENTORY_SIZE;
	}
	
	@Override
	public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
		buf.writeBlockPos(pos);
	}
	
	protected boolean tryConvertPigmentToEnergy(World world) {
		InkConvertingRecipe recipe = getInkConvertingRecipe(world);
		if(recipe != null) {
			InkColor color = recipe.getInkColor();
			long amount = recipe.getInkAmount();
			if(this.inkStorage.getEnergy(color) + amount <= this.inkStorage.getMaxPerColor()) {
				inventory.get(INPUT_SLOT_ID).decrement(1);
				this.inkStorage.addEnergy(color, amount);
				return true;
			}
		}
		return false;
	}
	
	protected @Nullable InkConvertingRecipe getInkConvertingRecipe(World world) {
		// is the current stack empty?
		ItemStack inputStack = inventory.get(INPUT_SLOT_ID);
		if(inputStack.isEmpty()) {
			this.cachedRecipe = null;
			return null;
		}
		
		// does the cached recipe match?
		if(this.cachedRecipe != null) {
			if(this.cachedRecipe.getIngredients().get(0).test(inputStack)) {
				return this.cachedRecipe;
			}
		}
		
		// search matching recipe
		Optional<InkConvertingRecipe> recipe = world.getRecipeManager().getFirstMatch(SpectrumRecipeTypes.INK_CONVERTING, this, world);
		if(recipe.isPresent()) {
			this.cachedRecipe = recipe.get();
			return this.cachedRecipe;
		} else {
			this.cachedRecipe = null;
			return null;
		}
	}
	
	protected boolean tryFillInkContainer() {
		boolean didSomething = false;
		
		ItemStack stack = inventory.get(OUTPUT_SLOT_ID);
		if(stack.getItem() instanceof InkStorageItem inkStorageItem) {
			InkStorage itemStorage = inkStorageItem.getEnergyStorage(stack);
			
			if(this.selectedColor == null) {
				for(InkColor color : InkColor.all()) {
					didSomething = didSomething | transferInk(inkStorage, itemStorage, color);
				}
			} else {
				didSomething = transferInk(inkStorage, itemStorage, this.selectedColor);
			}
			
			inkStorageItem.setEnergyStorage(stack, itemStorage);
		}
		
		return didSomething;
	}
	
	// TODO: move to InkStorage class
	// TODO: move to "pressure" system instead of fixed amount where more energy is transferred when source is very full
	public static boolean transferInk(@NotNull InkStorage source, @NotNull InkStorage destination, @NotNull InkColor color, long amount) {
		long sourceAmount = source.getEnergy(color);
		if(sourceAmount > 0) {
			long destinationRoom = destination.getRoom(color);
			if(destinationRoom > 0) {
				long transferAmount = Math.min(amount, Math.min(sourceAmount, destinationRoom));
				if (transferAmount > 0) {
					destination.addEnergy(color, transferAmount);
					source.drainEnergy(color, transferAmount);
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean transferInk(@NotNull InkStorage source, @NotNull InkStorage destination, @NotNull InkColor color) {
		long sourceAmount = source.getEnergy(color);
		if(sourceAmount > 0) {
			long destinationRoom = destination.getRoom(color);
			if(destinationRoom > 0) {
				long destinationAmount = destination.getEnergy(color);
				long transferAmount = Math.max(0, (sourceAmount - destinationAmount) / 4);
				transferAmount = Math.min(transferAmount, Math.min(sourceAmount, destinationRoom));
				if (transferAmount > 0) {
					destination.addEnergy(color, transferAmount);
					source.drainEnergy(color, transferAmount);
					return true;
				}
			}
		}
		return false;
	}
	
}