package de.dafuqs.spectrum.items.food.beverages.properties;

import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.text.*;
import net.minecraft.util.*;

import java.util.*;

// wrapper for beverage itemstack nbt
// unique for each beverage
public class BeverageProperties {
	
	public long ageDays = 0;
	public int alcPercent = 0;
	public float thickness = 0;
	
	public BeverageProperties(long ageDays, int alcPercent, float thickness) {
		this.ageDays = ageDays;
		this.alcPercent = alcPercent;
		this.thickness = thickness;
	}
	
	public BeverageProperties(NbtCompound nbtCompound) {
		if (nbtCompound != null) {
			this.ageDays = nbtCompound.contains("AgeDays") ? nbtCompound.getLong("AgeDays") : 0;
			this.alcPercent = nbtCompound.contains("AlcPercent") ? nbtCompound.getInt("AlcPercent") : 0;
			this.thickness = nbtCompound.contains("Thickness") ? nbtCompound.getFloat("Thickness") : 0;
		}
	}
	
	public static BeverageProperties getFromStack(ItemStack itemStack) {
		NbtCompound nbtCompound = itemStack.getNbt();
		return new BeverageProperties(nbtCompound);
	}
	
	public void addTooltip(ItemStack itemStack, List<Text> tooltip) {
		tooltip.add(Text.translatable("item.spectrum.infused_beverage.tooltip.age", ageDays, alcPercent).formatted(Formatting.GRAY));
	}
	
	protected void toNbt(NbtCompound nbtCompound) {
		nbtCompound.putLong("AgeDays", this.ageDays);
		nbtCompound.putInt("AlcPercent", this.alcPercent);
		nbtCompound.putFloat("Thickness", this.thickness);
	}
	
	public ItemStack getStack(ItemStack itemStack) {
		NbtCompound compound = itemStack.getOrCreateNbt();
		toNbt(compound);
		itemStack.setNbt(compound);
		return itemStack;
	}
	
}
