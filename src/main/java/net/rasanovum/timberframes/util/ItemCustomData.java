package net.rasanovum.timberframes.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?}

/** Cross-version access to legacy item NBT and the 1.20.5+ custom-data component. */
public final class ItemCustomData {
    private ItemCustomData() {}

    public static CompoundTag copy(ItemStack stack) {
        //? if <1.20.5 {
        /*CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
        *///?} else {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        //?}
    }

    public static void set(ItemStack stack, CompoundTag tag) {
        //? if <1.20.5 {
        /*stack.setTag(tag.isEmpty() ? null : tag.copy());
        *///?} else {
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        //?}
    }

    public static void clear(ItemStack stack) {
        //? if <1.20.5
        /*stack.setTag(null);*/
        //? if >=1.20.5
        stack.remove(DataComponents.CUSTOM_DATA);
    }
}
