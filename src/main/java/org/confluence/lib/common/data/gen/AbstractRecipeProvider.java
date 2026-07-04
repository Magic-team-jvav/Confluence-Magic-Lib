package org.confluence.lib.common.data.gen;

import PortLib.extensions.com.mojang.serialization.DataResult.PortDataResultExtension;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public abstract class AbstractRecipeProvider extends RecipeProvider {
    private final List<Appender<?>> appenders = new LinkedList<>();
    private @Nullable CompletableFuture<HolderLookup.Provider> registries;

    public AbstractRecipeProvider(PackOutput output) {
        super(output);
    }

    public AbstractRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output);
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        if (registries == null) {
            return CompletableFuture.supplyAsync(() -> {
                List<CompletableFuture<?>> futures = new LinkedList<>();
                futures.add(super.run(output));

                for (Appender<?> appender : appenders) {
                    for (Map.Entry<Path, JsonElement> entry : appender.generate(pathProvider()).entrySet()) {
                        futures.add(DataProvider.saveStable(output, entry.getValue(), entry.getKey()));
                    }
                }
                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            }, Util.backgroundExecutor()).thenCompose(completableFuture -> completableFuture);
        }
        return registries.thenCompose(provider -> run(output, provider));
    }

    public CompletableFuture<?> run(CachedOutput output, HolderLookup.Provider provider) {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<?>> futures = new LinkedList<>();
            Set<ResourceLocation> idSet = Sets.newHashSet();
            List<CompletableFuture<?>> savedAdvancements = new ArrayList<>();
            buildRecipes((recipe) -> {
                if (!idSet.add(recipe.getId())) {
                    throw new IllegalStateException("Duplicate recipe " + recipe.getId());
                }
                savedAdvancements.add(DataProvider.saveStable(output, recipe.serializeRecipe(), recipePathProvider.json(recipe.getId())));
                JsonObject jsonAdvancement = recipe.serializeAdvancement();
                if (jsonAdvancement != null) {
                    var savedAdvancement = saveAdvancement(output, recipe, jsonAdvancement);
                    if (savedAdvancement != null)
                        savedAdvancements.add(savedAdvancement);
                }
            }, provider);
            futures.add(CompletableFuture.allOf(savedAdvancements.toArray(CompletableFuture[]::new)));

            for (Appender<?> appender : appenders) {
                for (Map.Entry<Path, JsonElement> entry : appender.generate(pathProvider()).entrySet()) {
                    futures.add(DataProvider.saveStable(output, entry.getValue(), entry.getKey()));
                }
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }, Util.backgroundExecutor()).thenCompose(completableFuture -> completableFuture);
    }

    protected void buildRecipes(Consumer<FinishedRecipe> writer, HolderLookup.Provider provider) {
        buildRecipes(writer);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {}

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
