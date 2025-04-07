package org.confluence.lib.common.recipe;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractRecipeProvider implements DataProvider {
    protected PackOutput output;
    private final List<Data> jsons = new LinkedList<>();

    public AbstractRecipeProvider(PackOutput output) {
        this.output = output;
    }

    GsonBuilder builder = new GsonBuilder().setPrettyPrinting();

    abstract protected void run();

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        run();
        List<CompletableFuture<?>> futures = new LinkedList<>();
        for (Data data : jsons) {
            ItemStack result = data.result;
            ResourceLocation loc;
            if (result == null || result.isEmpty()) {
                loc = ResourceLocation.parse(data.suffix);
            } else {
                loc = result.getItemHolder().getKey().location();
            }

            Path path = getPath(loc, data.suffix);
            futures.add(DataProvider.saveStable(cachedOutput, data.json, path));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    protected void addJson(JsonObject json, ItemStack result, String suffix) {
        int sameCount = 0;
        for (var pair : jsons) {
            if (pair.result.getItem() == result.getItem() && pair.suffix.equals(suffix)) {
                sameCount++;
            }
        }
        if (sameCount > 0) {
            suffix = suffix + "_" + sameCount;
        }
        jsons.add(new Data(json, result, suffix));
    }

    protected Path getPath(ResourceLocation loc, String nameSuffix) {
        return getRoot(loc).resolve(loc.getPath() + nameSuffix + "_gen" + pathSuffix() + ".json");
    }

    protected String pathSuffix() {
        return "";
    }

    protected Path getRoot(ResourceLocation loc) {
        return this.output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(loc.getNamespace()).resolve("recipe");
    }

    protected JsonElement parseCodec(DataResult<?> result) {
        return JsonParser.parseString(builder.create().toJson(result.result().get()));
    }

    private record Data(JsonObject json, ItemStack result, String suffix) {}
}
