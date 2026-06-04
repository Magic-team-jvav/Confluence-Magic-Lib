package org.confluence.lib.common.data.gen;

import PortLib.extensions.com.mojang.serialization.DataResult.PortDataResultExtension;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public abstract class AbstractRecipeProvider extends RecipeProvider {
    private final List<Appender<?>> appenders = new LinkedList<>();

    public AbstractRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        CompletableFuture<?> future = super.run(output);
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<?>> futures = new LinkedList<>();
            futures.add(future);
            for (Appender<?> appender : appenders) {
                for (Map.Entry<Path, JsonElement> entry : appender.generate(pathProvider()).entrySet()) {
                    futures.add(DataProvider.saveStable(output, entry.getValue(), entry.getKey()));
                }
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }, Util.backgroundExecutor()).thenCompose(completableFuture -> completableFuture);
    }

    protected PackOutput.PathProvider pathProvider() {
        return recipePathProvider;
    }

    protected <T> Appender<T> recipe(Codec<T> codec, BiFunction<T, PackOutput.PathProvider, Path> pathGetter) {
        Appender<T> appender = new Appender<>(codec, pathGetter);
        appenders.add(appender);
        return appender;
    }

    protected <T> Appender<T> recipe(Codec<T> codec, Path path) {
        return recipe(codec, (t, pathProvider) -> path);
    }

    public static class Appender<T> {
        private final Codec<T> codec;
        private final BiFunction<T, PackOutput.PathProvider, Path> pathGetter;
        private final List<T> recipes = new LinkedList<>();

        public Appender(Codec<T> codec, BiFunction<T, PackOutput.PathProvider, Path> pathGetter) {
            this.codec = codec;
            this.pathGetter = pathGetter;
        }

        public Appender<T> addRecipe(T recipe) {
            recipes.add(recipe);
            return this;
        }

        @ApiStatus.Internal
        public Map<Path, JsonElement> generate(PackOutput.PathProvider pathProvider) {
            Map<Path, JsonElement> map = new HashMap<>();
            for (T recipe : recipes) {
                map.put(pathGetter.apply(recipe, pathProvider), PortDataResultExtension.getOrThrow(codec.encodeStart(JsonOps.INSTANCE, recipe)));
            }
            return map;
        }
    }
}
