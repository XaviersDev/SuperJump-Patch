package com.allisighs.caesar.sjp.gui;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;

public class SjpInventory extends InventoryBasic {

    public SjpInventory(int size) {
        super("Настройки SJP", false, size);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStack.EMPTY;
    }
}
