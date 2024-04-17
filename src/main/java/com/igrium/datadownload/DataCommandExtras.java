package com.igrium.datadownload;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.igrium.datadownload.filebin.FilebinApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.DataCommandObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.DataCommand;
import net.minecraft.server.command.DataCommand.ObjectType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DataCommandExtras {

    private static final SimpleCommandExceptionType UPLOAD_FAILED = new SimpleCommandExceptionType(Text.literal("Error exporting data. See console for details."));
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            RegistrationEnvironment environment) {
        
        LiteralArgumentBuilder<ServerCommandSource> data = literal("data")
                .requires(source -> source.hasPermissionLevel(2));

        for (ObjectType objectType : DataCommand.TARGET_OBJECT_TYPES) {
            data.then(
                objectType.addArgumentsToBuilder(literal("export"), builder -> builder.then(
                    argument("compress", BoolArgumentType.bool()).executes(
                        context -> download(context, BoolArgumentType.getBool(context, "compress"), objectType.getObject(context))
                    )
                ).executes(
                    context -> download(context, false, objectType.getObject(context))
                ))
            );
        }
        
        dispatcher.register(data);
    }
    
    private static int download(CommandContext<ServerCommandSource> context, boolean compress, DataCommandObject object) throws CommandSyntaxException {
        UUID uuid = UUID.randomUUID();
        FilebinApi filebin = DataDownload.getInstance().getFilebin();

        context.getSource().sendFeedback(() -> Text.literal("Exporting data..."), false);

        NbtCompound nbt = object.getNbt();
        byte[] data;

        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            if (compress) {
                NbtIo.writeCompressed(nbt, out);
            } else {
                NbtIo.write(nbt, new DataOutputStream(out));
            }

            data = out.toByteArray();
        } catch (IOException e) {
            DataDownload.LOGGER.error("Error exporting data.", e);
            throw UPLOAD_FAILED.create();
        }
      
        filebin.upload(uuid.toString(), "data.nbt", data).handle((res, e) -> {
            e = FilebinApi.decomposeCompletionException(e);
            if (e != null) {
                context.getSource().sendError(Text.literal("Error exporting data. See console for details"));
                DataDownload.LOGGER.error("Error uploading data to filebin.", e);
                return null;
            }
            context.getSource().sendFeedback(() -> getDownloadLink(uuid.toString(), "data.nbt", filebin), false);
            return null;
        });

        return 1;
    }

    private static Text getDownloadLink(String bin, String file, FilebinApi filebin) {
        String directLink = filebin.getFile(bin, file).toString();
        return Text.literal("Export complete! [")
                .append(getLinkText(Text.literal("download"), directLink, Text.literal(directLink)))
                .append("]");
                
    }

    private static MutableText getLinkText(MutableText text, String link, Text hover) {
        return text.setStyle(
                Style.EMPTY.withColor(Formatting.GREEN).withUnderline(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }
}
