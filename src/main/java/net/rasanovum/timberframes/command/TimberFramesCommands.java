package net.rasanovum.timberframes.command;

import net.rasanovum.timberframes.TimberFrames;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TimberFramesCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(TimberFrames.MODID)
                .then(Commands.literal("base")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("trigger")
                                .executes(TimberFramesCommands::baseCommand))));
    }

    /**
    * Removes all nodes from Client & Server Path Graphs
     */
    private static int baseCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return 0;
    }
}
