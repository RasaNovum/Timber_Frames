package net.rasanovum.timberframes.mixins;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.rasanovum.timberframes.items.BroadKnife;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if <1.21 {
/*import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.DiggingEnchantment;
import net.minecraft.world.item.enchantment.MendingEnchantment;
*///?} else {
//?}

@Mixin(Enchantment.class)
public abstract class BroadKnifeEnchantmentMixin {
    //? if <1.21 {
    /*@Inject(method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void timberFrames$restrictEnchantments(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof BroadKnife)) return;

        Enchantment enchantment = (Enchantment) (Object) this;
        if (!(enchantment instanceof DiggingEnchantment
                || enchantment instanceof DigDurabilityEnchantment
                || enchantment instanceof MendingEnchantment)) {
            cir.setReturnValue(false);
        }
    }
    *///?} else {
    //?}
}
