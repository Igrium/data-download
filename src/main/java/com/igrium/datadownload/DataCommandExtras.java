package com.igrium.datadownload;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.DataCommandObject;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.DataCommand.ObjectType;
import net.minecraft.text.Text;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.ServerCommandSource;

import static net.minecraft.server.command.CommandManager.*;

public class DataCommandExtras {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            RegistrationEnvironment environment) {
        
        LiteralArgumentBuilder<ServerCommandSource> data = literal("data")
                .requires(source -> source.hasPermissionLevel(2));

        for (ObjectType objectType : DataCommand.TARGET_OBJECT_TYPES) {
            data.then(
                objectType.addArgumentsToBuilder(literal("download"), builder -> builder.executes(
                    context -> upload(context, objectType.getObject(context))
                ))
            );
        }
        
        dispatcher.register(data);
    }
    
    private static int upload(CommandContext<ServerCommandSource> context, DataCommandObject object) throws CommandSyntaxException {
        context.getSource().sendFeedback(() -> Text.literal("You ran an upload!"), false);

        return 1;
    }
}
