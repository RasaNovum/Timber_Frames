package net.rasanovum.timberframes.init;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.rasanovum.rosetta.registry.ModRegistrar;
import net.rasanovum.rosetta.registry.RegistrationContext;
import net.rasanovum.rosetta.registry.RegistryHandle;
import net.rasanovum.timberframes.TimberFrames;
import net.rasanovum.timberframes.block.TimberFrameBlock;
import net.rasanovum.timberframes.block.entity.TimberFrameBlockEntity;
import net.rasanovum.timberframes.items.BroadKnife;
import net.rasanovum.timberframes.items.TimberFrameItem;
import net.rasanovum.timberframes.recipe.RecipeGenerator;

public final class ContentInit {
    private static final ModRegistrar REGISTRAR = new ModRegistrar(TimberFrames.MODID);
    private static final ModRegistrar.BlockItemEntry<TimberFrameBlock, TimberFrameItem> TIMBER_FRAME_ENTRY =
            REGISTRAR.blockWithItem(
                    "timber_frame",
                    TimberFrameBlock::new,
                    BlockBehaviour.Properties.of().strength(2.0f, 3.0f).sound(SoundType.WOOD),
                    TimberFrameItem::new,
                    new Item.Properties()
            );

    public static final RegistryHandle<TimberFrameBlock> TIMBER_FRAME = TIMBER_FRAME_ENTRY.block();
    public static final RegistryHandle<BlockEntityType<TimberFrameBlockEntity>> TIMBER_FRAME_ENTITY =
            REGISTRAR.blockEntity("timber_frame", TimberFrameBlockEntity::new, TIMBER_FRAME);
    public static final RegistryHandle<BroadKnife> BROAD_KNIFE =
            REGISTRAR.item("broad_knife", BroadKnife::new, new Item.Properties());

    static {
        REGISTRAR.creativeTab(CreativeModeTabs.TOOLS_AND_UTILITIES).add(BROAD_KNIFE);
        REGISTRAR.creativeTab(CreativeModeTabs.BUILDING_BLOCKS)
                .addStacks("timber_frame_variants", output -> RecipeGenerator.getTimberIds()
                        .forEach(id -> output.accept(TimberFrameItem.createVariantStack(id))));
    }

    private ContentInit() {}

    public static void register(RegistrationContext context) {
        REGISTRAR.register(context);
    }
}
