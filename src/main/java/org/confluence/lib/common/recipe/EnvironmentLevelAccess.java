package org.confluence.lib.common.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class EnvironmentLevelAccess implements ContainerLevelAccess {
    protected @Nullable Level level;
    protected @Nullable BlockPos pos;

    public EnvironmentLevelAccess(@Nullable Level level, @Nullable BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    public void initializeIfNeeded(Level pLevel, BlockPos pPos) {
        if (level == null) this.level = pLevel;
        if (pos == null) this.pos = pPos;
    }

    public void initializeIfNeeded(Player player) {
        if (level == null) this.level = player.level();
        if (pos == null) {
            Vec3 start = player.getEyePosition(0.5F);
            Vec3 lookVector = player.getViewVector(0.5F);
            double range = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
            Vec3 end = start.add(lookVector.x * range, lookVector.y * range, lookVector.z * range);
            ClipContext context = new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.of(player));
            BlockHitResult blockResult = player.level().clip(context);
            if (blockResult.getType() == HitResult.Type.BLOCK) {
                this.pos = blockResult.getBlockPos();
            }
        }
    }

    public @Nullable Level getLevel() {
        return level;
    }

    public @Nullable BlockPos getPos() {
        return pos;
    }

    @ApiStatus.OverrideOnly
    public <R extends Recipe<?>> boolean matches(R recipe) {
        return true;
    }

    public Optional<Holder<Biome>> getBiome() {
        return level == null || pos == null ? Optional.empty() : Optional.of(level.getBiome(pos));
    }

    public boolean isBiome(Function<Holder<Biome>, Boolean> predicate) {
        return getBiome().map(predicate).orElse(false);
    }

    public Iterable<BlockPos> searchBox(int inflate) {
        return level == null || pos == null ? List.of() : BlockPos.betweenClosed(pos.offset(-inflate, -inflate, -inflate), pos.offset(inflate, inflate, inflate));
    }

    public boolean anyMatch(Predicate<BlockState> predicate, int inflate) {
        if (level != null) {
            for (BlockPos blockPos : searchBox(inflate)) {
                if (predicate.test(level.getBlockState(blockPos))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> levelPosConsumer) {
        return level == null || pos == null ? Optional.empty() : Optional.of(levelPosConsumer.apply(level, pos));
    }

    public static Matcher matcher(@Nullable HolderSet<Biome> biome, @Nullable SearchContext block, boolean graveyard) {
        return new Matcher(Optional.ofNullable(biome), Optional.ofNullable(block), graveyard);
    }

    public record Matcher(Optional<HolderSet<Biome>> biome, Optional<SearchContext> block, boolean graveyard) {
        public static final Matcher EMPTY = new Matcher(Optional.empty(), Optional.empty(), false);
        public static final Codec<Matcher> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.BIOME).lenientOptionalFieldOf("biome").forGetter(Matcher::biome),
                SearchContext.CODEC.lenientOptionalFieldOf("block").forGetter(Matcher::block),
                Codec.BOOL.lenientOptionalFieldOf("graveyard", false).forGetter(Matcher::graveyard)
        ).apply(instance, Matcher::new));
        public static final MapCodec<Matcher> MAP_CODEC = CODEC.lenientOptionalFieldOf("environment", EMPTY);
        public static final StreamCodec<RegistryFriendlyByteBuf, Matcher> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.BIOME)), Matcher::biome,
                ByteBufCodecs.optional(SearchContext.STREAM_CODEC), Matcher::block,
                ByteBufCodecs.BOOL, Matcher::graveyard,
                Matcher::new
        );

        public boolean matches(EnvironmentLevelAccess access) {
            Level level = access.getLevel();
            BlockPos pos = access.getPos();
            if (level == null || pos == null) return false;
            if (!matchesBiome(level, pos)) return false;
            if (!matchesBlock(level, pos)) return false;
            if (!matchesGraveyard(level, pos)) return false;
            return true;
        }

        public boolean matchesBiome(Level level, BlockPos pos) {
            return biome.isEmpty() || biome.get().contains(level.getBiome(pos));
        }

        public boolean matchesBlock(Level level, BlockPos pos) {
            return block.isEmpty() || block.get().matches(level, pos);
        }

        public boolean matchesGraveyard(Level level, BlockPos pos) {
            return !graveyard || isGraveyard(level, pos);
        }

        private static boolean isGraveyard(Level level, BlockPos pos) {
            return true; // confluence mixin here
        }
    }

    public record SearchContext(int inflate, Optional<HolderSet<Block>> blocks, List<StatePropertiesPredicate> statePredicates, Optional<HolderSet<Fluid>> fluids) {
        public static final Codec<SearchContext> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("inflate").forGetter(SearchContext::inflate),
                RegistryCodecs.homogeneousList(Registries.BLOCK).lenientOptionalFieldOf("blocks").forGetter(SearchContext::blocks),
                StatePropertiesPredicate.CODEC.listOf().lenientOptionalFieldOf("state_predicates", List.of()).forGetter(SearchContext::statePredicates),
                RegistryCodecs.homogeneousList(Registries.FLUID).lenientOptionalFieldOf("fluids").forGetter(SearchContext::fluids)
        ).apply(instance, SearchContext::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, SearchContext> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SearchContext::inflate,
                ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.BLOCK)), SearchContext::blocks,
                StatePropertiesPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), SearchContext::statePredicates,
                ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.FLUID)), SearchContext::fluids,
                SearchContext::new
        );

        public boolean matches(Level level, BlockPos pos) {
            if (blocks.isPresent() || !statePredicates.isEmpty() || fluids.isPresent()) {
                for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-inflate, -inflate, -inflate), pos.offset(inflate, inflate, inflate))) {
                    BlockState blockState = level.getBlockState(blockPos);
                    if (blocks.isPresent() && blockState.is(blocks.get())) return true;
                    if (statePredicates.stream().anyMatch(predicates -> predicates.matches(blockState))) return true;
                    if (fluids.isPresent() && blockState.getFluidState().is(fluids.get())) return true;
                }
                return false;
            }
            return true;
        }

        public String toDescription() {
            // todo
            return "{" +
                    ", blocks=" + blocks +
                    ", statePredicates=" + statePredicates +
                    ", fluids=" + fluids +
                    '}';
        }
    }
}
