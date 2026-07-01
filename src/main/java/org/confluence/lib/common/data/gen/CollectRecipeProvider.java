package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class CollectRecipeProvider extends RecipeProvider {
    private final List<AbstractRecipeProvider> subProviders;
    private final String name;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public CollectRecipeProvider(String name, PackOutput output, CompletableFuture<HolderLookup.Provider> registries, Factory... factories) {
        super(output);
        this.name = name;
        this.registries = registries;
        this.subProviders = Arrays.stream(factories).map(factory -> factory.create(output)).toList();
    }

    @Override
    protected final void buildRecipes(Consumer<FinishedRecipe> writer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return registries.thenCompose(provider -> CompletableFuture.allOf(subProviders.stream()
                .map(subProvider -> subProvider.run(output, provider))
                .toArray(CompletableFuture[]::new)));
    }

    @Override
    public String getName() {
        return name;
    }

    @FunctionalInterface
    public interface Factory {
        AbstractRecipeProvider create(PackOutput output);
    }
}
