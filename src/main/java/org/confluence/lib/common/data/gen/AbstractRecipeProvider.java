package org.confluence.lib.common.data.gen;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractRecipeProvider extends RecipeProvider {
    protected PackOutput output;
    private final List<Appender<?>> appenders = new LinkedList<>();

    public AbstractRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
        this.output = output;
    }

    @Override
    protected CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider registries) {
        CompletableFuture<?> future = super.run(output, registries);
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<?>> futures = new LinkedList<>();
            futures.add(future);
            for (Appender<?> appender : appenders) {
                for (Map.Entry<Path, JsonElement> entry : appender.generate(pathProvider()).entrySet()) {
                    futures.add(DataProvider.saveStable(output, entry.getValue(), entry.getKey()));
                }
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
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
        Appender<T> appender = new Appender<>(codec, (t, pathProvider) -> path);
        appenders.add(appender);
        return appender;
    }

    public static class Appender<T> {
        private final Codec<T> codec;
        private final BiFunction<T, PackOutput.PathProvider, Path> pathGetter;
        private final List<T> recipes = new LinkedList<>();

        public Appender(Codec<T> codec, BiFunction<T, PackOutput.PathProvider, Path> pathGetter) {
            this.codec = codec;
            this.pathGetter = pathGetter;
        }

        public Appender<T> addRecipe(T... recipe) {
            recipes.addAll(Arrays.asList(recipe));
            return this;
        }

        @ApiStatus.Internal
        public Map<Path, JsonElement> generate(PackOutput.PathProvider pathProvider) {
            Map<Path, JsonElement> map = new HashMap<>();
            for (T recipe : recipes) {
                map.put(pathGetter.apply(recipe, pathProvider), codec.encodeStart(JsonOps.INSTANCE, recipe).getOrThrow());
            }
            return map;
        }
    }
}
