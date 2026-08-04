package org.confluence.lib.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.confluence.lib.ConfluenceMagicLib;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class SingleJsonFileReloadListener extends ContextAwareReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Override
    public CompletableFuture<Void> reload(
            SharedState currentReload,
            Executor taskExecutor,
            PreparationBarrier preparationBarrier,
            Executor reloadExecutor
    ) {
        ResourceManager manager = currentReload.resourceManager();
        return CompletableFuture.supplyAsync(() -> prepare(manager), taskExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(this::apply, reloadExecutor);
    }

    protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager) {
        Map<Identifier, JsonElement> map = new HashMap<>();
        Identifier id = resourcePath();
        for (Resource resource : resourceManager.getResourceStack(id)) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject jsonobject = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                for (Map.Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                    Identifier loc = Identifier.parse(entry.getKey());
                    map.put(loc, entry.getValue());
                }
            } catch (RuntimeException | IOException ioexception) {
                ConfluenceMagicLib.LOGGER.error("Couldn't read {} {} in {} pack {}", identifier(), id, resource.sourcePackId(), packType().getDirectory(), ioexception);
            }
        }
        return map;
    }

    protected abstract void apply(Map<Identifier, JsonElement> resourceList);

    protected abstract Identifier resourcePath();

    protected abstract String identifier();

    protected PackType packType() {
        return PackType.SERVER_DATA;
    }
}
