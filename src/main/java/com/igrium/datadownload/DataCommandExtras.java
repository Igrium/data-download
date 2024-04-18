package com.igrium.datadownload;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import com.igrium.datadownload.filebin.FilebinApi;
import com.igrium.datadownload.filebin.FilebinMeta;
import com.igrium.datadownload.filebin.FilebinMeta.FilebinFileMeta;
import com.igrium.datadownload.filebin.HttpException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
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
    private static final DynamicCommandExceptionType INVALID_URL = new DynamicCommandExceptionType(s -> Text.literal(null));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess,
            RegistrationEnvironment environment) {
        
        LiteralArgumentBuilder<ServerCommandSource> data = literal("data")
                .requires(source -> source.hasPermissionLevel(2));

        for (ObjectType objectType : DataCommand.SOURCE_OBJECT_TYPES) {
            data.then(
                objectType.addArgumentsToBuilder(literal("export"), builder -> builder.then(
                    argument("compress", BoolArgumentType.bool()).executes(
                        context -> export(context, BoolArgumentType.getBool(context, "compress"), objectType.getObject(context))
                    )
                ).executes(
                    context -> export(context, false, objectType.getObject(context))
                ))
            );
        }

        for (ObjectType objectType : DataCommand.TARGET_OBJECT_TYPES) {
            data.then(
                objectType.addArgumentsToBuilder(literal("import"), builder -> builder.then(
                    literal("from").then(
                        literal("filebin").then(
                            argument("binId", StringArgumentType.word()).executes(
                                context -> importFilebin(context, objectType.getObject(context))
                            )
                        )
                    ).then(
                        literal("url").then(
                            argument("url", StringArgumentType.greedyString()).executes(
                                context -> importUrl(context, objectType.getObject(context))
                            )
                        )
                    )
                ))
            );
        }

        // for (ObjectType objectType : DataCommand.O)
        
        dispatcher.register(data);
    }

    private static class DownloadException extends RuntimeException {
        DownloadException(String message) {
            super(message);
        }

        // DownloadException(String message, Throwable cause) {
        //     super(message, cause);
        // }
    }

    private static CompletableFuture<?> downloadNbt(CommandContext<ServerCommandSource> context, DataCommandObject object,
            Function<FilebinApi, CompletableFuture<NbtCompound>> downloadFunction, Text notFoundText) {
        
        context.getSource().sendFeedback(() -> Text.literal("Importing data..."), false);
        
        FilebinApi filebin = DataDownload.getInstance().getFilebin();
        return downloadFunction.apply(filebin).whenCompleteAsync((nbt, e) -> {
            if (e != null) {
                e = FilebinApi.decomposeCompletionException(e);
                if (e instanceof DownloadException) {
                    context.getSource().sendError(Text.literal(e.getMessage()));

                } else if (e instanceof HttpException http) {
                    if (http.getStatusCode() == 404) {
                        context.getSource().sendError(notFoundText);
                    } else {
                        context.getSource().sendError(Text.literal("The server returned status code " + http.getStatusCode()));
                    }
                } else {
                    context.getSource().sendError(Text.literal("An error occured importing the file: " + e.getMessage()));
                }
                return;
            }

            try {
                object.setNbt(nbt);
            } catch (CommandSyntaxException e1) {
                context.getSource().sendError(Text.of(e1.getRawMessage()));
                DataDownload.LOGGER.error("Error importing NBT file.", e);
            }

            context.getSource().sendFeedback(() -> Text.literal("Successfully imported data!"), false);
        }, context.getSource().getServer());
    }

    private static int importFilebin(CommandContext<ServerCommandSource> context, DataCommandObject object) throws CommandSyntaxException {
        String binId = StringArgumentType.getString(context, "binId");

        downloadNbt(context, object, filebin -> filebin.getBinMeta(binId).thenApply(bin -> {
            var file = getBestFile(bin);

            try(InputStream in = new BufferedInputStream(filebin.download(bin.bin.id, file.filename))) {
                return NbtIo.readCompound(new DataInputStream(in));
            } catch (IOException e) {
                throw new CompletionException(e);
            }

        }), Text.literal("The supplied bin was not found."));

        return 1;
    }

    private static int importUrl(CommandContext<ServerCommandSource> context, DataCommandObject object) throws CommandSyntaxException {
        String urlString = StringArgumentType.getString(context, "url");
        URI url;
        try {
            url = new URI(urlString);
        } catch (URISyntaxException e) {
           throw INVALID_URL.create(e.getMessage());
        }

        var request = HttpRequest.newBuilder(url)
            .GET()
            .build();

        downloadNbt(context, object,
                filebin -> filebin.getClient().sendAsync(request, BodyHandlers.ofInputStream()).thenApply(res -> {
                    if (res.statusCode() >= 400) {
                        throw new CompletionException(new HttpException(res.statusCode()));
                    }

                    try(InputStream in = new BufferedInputStream(res.body())) {
                        return NbtIo.readCompound(new DataInputStream(in));
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                    
                }), Text.literal("404 not found."));

        return 1;
    }

    private static FilebinFileMeta getBestFile(FilebinMeta bin) {
        if (bin.files == null || bin.files.isEmpty()) {
            throw new DownloadException("The bin did not contain any files.");
        }

        for (var f : bin.files)  {
            if (f.filename.endsWith(".nbt")) {
                return f;
            }
        }
        return bin.files.get(0);
    }
    
    private static int export(CommandContext<ServerCommandSource> context, boolean compress, DataCommandObject object) throws CommandSyntaxException {
        UUID uuid = UUID.randomUUID();
        FilebinApi filebin = DataDownload.getInstance().getFilebin();

        context.getSource().sendFeedback(() -> Text.literal("Exporting data..."), false);

        NbtCompound nbt = object.getNbt();
        byte[] data;

        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            NbtIo.write(nbt, new DataOutputStream(out));

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
