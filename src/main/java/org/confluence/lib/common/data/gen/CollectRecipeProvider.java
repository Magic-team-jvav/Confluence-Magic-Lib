package org.confluence.lib.common.data.gen;

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

    public CollectRecipeProvider(String name, PackOutput output, Factory... factories) {
        super(output);
        this.name = name;
        this.subProviders = Arrays.stream(factories).map(factory -> factory.create(output)).toList();
    }

    @Override
    protected final void buildRecipes(Consumer<FinishedRecipe> writer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.allOf(subProviders.stream()
                .map(subProvider -> subProvider.run(output))
                .toArray(CompletableFuture[]::new));
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
