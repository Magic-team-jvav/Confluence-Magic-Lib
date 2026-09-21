package org.confluence.lib.common.worldgen.biome;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.confluence.lib.mixed.IPalettedContainer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/// section 总计数负责粗筛，稀疏 4 格计数负责邻域判定；底图与覆盖分开保存。
public final class DynamicBiomeUtils {
    private static final List<BiomeRule> RULES = new ArrayList<>();
    private static final List<BlockCounters.Counter> COUNTERS = new ArrayList<>();
    private static final List<BlockCounters.Counter> SOURCES = new ArrayList<>();
    private static final Object2IntOpenHashMap<ResourceKey<Biome>> PRIORITY = new Object2IntOpenHashMap<>();
    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> PURE_BIOMES = new HashMap<>();
    private static final Map<ServerLevel, LongLinkedOpenHashSet> PENDING = new IdentityHashMap<>();
    /// Watch 在完整区块包发送后触发；视距范围不能代表区块已发送。
    private static final Map<ServerLevel, Map<ServerPlayer, LongOpenHashSet>> WATCHED = new IdentityHashMap<>();
    private static Predicate<Holder<Biome>> spreadable = biome -> false;
    /// 暂定时间预算；完成当前 section 后检查，不是严格的单 tick 耗时上限。
    private static final long TIME_BUDGET_NS = 2_000_000;
    /// 核半径：水平两单元、垂直一单元，三角权重总和为 9 × 9 × 4。
    public static final int HORIZONTAL_RADIUS = 8;
    public static final int VERTICAL_RADIUS = 4;
    private static final int KERNEL_WEIGHT = 324;

    static {PRIORITY.defaultReturnValue(Integer.MAX_VALUE);}

    private DynamicBiomeUtils() {}

    public static void enable() {BlockCounters.enable();}

    public static boolean isEnabled() {return BlockCounters.isEnabled();}

    public static void registerRule(BiomeRule rule) {RULES.add(rule);}

    public static void registerPriority(ResourceKey<Biome> biome, int priority) {PRIORITY.put(biome, priority);}

    public static void registerPureBiome(ResourceKey<Biome> biome, ResourceKey<Biome> pure) {PURE_BIOMES.put(biome, pure);}

    public static void registerSpreadable(Predicate<Holder<Biome>> predicate) {spreadable = predicate;}

    public static boolean isSpreadable(Holder<Biome> biome) {return spreadable.test(biome);}

    /// source 表示能独立形成覆盖的计数；水、向日葵等辅助计数不触发整片世界的加载重判。
    public static void registerCounter(BlockCounters.Counter counter, boolean source) {
        COUNTERS.add(counter.spatial());
        if (source) SOURCES.add(counter);
    }

    /// 迷你群系专用计数变化不需要重算实际群系覆盖。
    public static boolean affectsDynamicBiome(BlockState before, BlockState after) {
        int[] from = BlockCounters.matched(before);
        int[] to = BlockCounters.matched(after);
        for (BlockCounters.Counter counter : COUNTERS)
            if ((Arrays.binarySearch(from, counter.index()) >= 0) != (Arrays.binarySearch(to, counter.index()) >= 0))
                return true;
        return false;
    }

    /// 把局部三角核的加权计数换算为等效 section 密度，最大值仍为 4096。
    static int equivalentCount(int weightedCount) {
        return weightedCount * 64 / KERNEL_WEIGHT;
    }

    static int kernelWeight(int dx, int dy, int dz) {
        return (3 - Math.abs(dx)) * (2 - Math.abs(dy)) * (3 - Math.abs(dz));
    }

    public static int priorityOf(Holder<Biome> biome) {
        return biome.unwrapKey().map(PRIORITY::getInt).orElse(Integer.MAX_VALUE);
    }

    public static boolean isDynamic(Holder<Biome> biome) {
        return isSpreadable(biome) || biome.unwrapKey().map(PRIORITY::containsKey).orElse(false);
    }

    /// 兼容整段诊断入口；实际覆盖写入使用每个采样点自己的上下文。
    public static @Nullable Holder<Biome> judgeSection(LevelChunkSection section, HolderLookup.RegistryLookup<Biome> lookup) {
        return judge(new SectionContext(section, ILevelChunkSection.of(section).confluence$getBlockCounts(), lookup));
    }

    private static @Nullable Holder<Biome> judge(SectionContext context) {
        if (!isEnabled()) return null;
        for (BiomeRule rule : RULES) {
            Holder<Biome> result = rule.test(context);
            if (result != null) return result;
        }
        return null;
    }

    /// 天然感染群系逐点转换，不丢弃混合 section 内的纯净群系。
    public static PalettedContainer<Holder<Biome>> judgeBackupBiome(LevelChunkSection section, HolderLookup.RegistryLookup<Biome> lookup) {
        PalettedContainer<Holder<Biome>> current = (PalettedContainer<Holder<Biome>>) section.getBiomes();
        PalettedContainer<Holder<Biome>> backup = IPalettedContainer.copyBiomes(current);
        for (int y = 0; y < 4; y++)
            for (int z = 0; z < 4; z++)
                for (int x = 0; x < 4; x++) {
                    Holder<Biome> biome = current.get(x, y, z);
                    if (isSpreadable(biome))
                        backup.set(x, y, z, lookup.getOrThrow(biome.unwrapKey().map(PURE_BIOMES::get).orElse(Biomes.PLAINS)));
                }
        return backup;
    }

    public static @Nullable LevelChunkSection getSection(LevelAccessor level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4))
            return null;
        return level.getChunk(pos).getSection(level.getSectionIndex(pos.getY()));
    }

    public static @Nullable ILevelChunkSection getISection(LevelAccessor level, BlockPos pos) {
        LevelChunkSection section = getSection(level, pos);
        return section == null ? null : ILevelChunkSection.of(section);
    }

    private static boolean hasSource(LevelChunkSection section) {
        BlockCounts counts = ILevelChunkSection.of(section).confluence$getBlockCounts();
        for (BlockCounters.Counter source : SOURCES) {
            if (source.get(counts) != 0) return true;
        }
        return false;
    }

    /// 登记变化位置影响的邻域，不加载区块；重复任务由更新队列去重。
    public static void markChanged(ServerLevel level, BlockPos pos) {
        markRange(level, pos.getX() - HORIZONTAL_RADIUS, pos.getY() - VERTICAL_RADIUS, pos.getZ() - HORIZONTAL_RADIUS,
                pos.getX() + HORIZONTAL_RADIUS, pos.getY() + VERTICAL_RADIUS, pos.getZ() + HORIZONTAL_RADIUS);
    }

    private static void markRange(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        LongLinkedOpenHashSet pending = PENDING.computeIfAbsent(level, ignored -> new LongLinkedOpenHashSet());
        for (int x = minX >> 4; x <= maxX >> 4; x++)
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) continue;
                for (int y = minY >> 4; y <= maxY >> 4; y++)
                    if (y >= level.getMinSection() && y < level.getMaxSection())
                        pending.add(SectionPos.asLong(x, y, z));
            }
    }

    /// 只处理附近含来源或已有覆盖的 section；邻区块到达时也补齐跨边界影响。
    public static void markLoaded(ServerLevel level, int x, int z) {
        if (!isEnabled()) return;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x + dx, z + dz);
                if (chunk == null) continue;
                for (int index = 0; index < chunk.getSectionsCount(); index++) {
                    LevelChunkSection section = chunk.getSection(index);
                    if (!hasSource(section) && !section.getBiomes().maybeHas(DynamicBiomeUtils::isDynamic))
                        continue;
                    int bx = (x + dx) << 4;
                    int by = level.getSectionYFromSectionIndex(index) << 4;
                    int bz = (z + dz) << 4;
                    markRange(level, bx - HORIZONTAL_RADIUS, by - VERTICAL_RADIUS, bz - HORIZONTAL_RADIUS,
                            bx + 15 + HORIZONTAL_RADIUS, by + 15 + VERTICAL_RADIUS, bz + 15 + HORIZONTAL_RADIUS);
                }
            }
    }

    /// 每段先取一次相邻 section，64 个采样点复用；不为每个点查区块或建立世界级缓存。
    private static boolean updateSection(ServerLevel level, LevelChunk chunk, int sectionY, HolderLookup.RegistryLookup<Biome> lookup) {
        BlockCounts[] neighbors = new BlockCounts[27];
        LevelChunkSection[] neighborSections = new LevelChunkSection[27];
        boolean hasSources = false;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk neighbor = level.getChunkSource().getChunkNow(chunk.getPos().x + dx, chunk.getPos().z + dz);
                if (neighbor == null) continue;
                for (int dy = -1; dy <= 1; dy++) {
                    int sy = sectionY + dy;
                    if (sy < level.getMinSection() || sy >= level.getMaxSection()) continue;
                    LevelChunkSection section = neighbor.getSection(level.getSectionIndexFromSectionY(sy));
                    BlockCounts counts = ILevelChunkSection.of(section).confluence$getBlockCounts();
                    int slot = ((dy + 1) * 3 + dz + 1) * 3 + dx + 1;
                    neighbors[slot] = counts;
                    neighborSections[slot] = section;
                    if (hasSource(section)) hasSources = true;
                }
            }
        LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY));
        /// 水或向日葵单独变化不会形成覆盖，粗筛后再建立邻域空间索引。
        if (!hasSources && !section.getBiomes().maybeHas(DynamicBiomeUtils::isDynamic))
            return false;
        for (int slot = 0; slot < neighbors.length; slot++)
            if (neighbors[slot] != null) neighbors[slot].prepareCells(neighborSections[slot]);
        var base = ILevelChunkSection.of(section).confluence$getBackupBiome();
        var current = section.getBiomes();
        PalettedContainer<Holder<Biome>> replacement = null;
        BlockCounts sample = new BlockCounts();
        int[] sums = new int[BlockCounters.size()];
        for (int y = 0; y < 4; y++)
            for (int z = 0; z < 4; z++)
                for (int x = 0; x < 4; x++) {
                    Arrays.fill(sums, 0);
                    boolean complete = true;
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -2; dz <= 2; dz++)
                            for (int dx = -2; dx <= 2; dx++) {
                                int cx = x + dx, cy = y + dy, cz = z + dz;
                                BlockCounts counts = neighbors[(((cy >> 2) + 1) * 3 + (cz >> 2) + 1) * 3 + (cx >> 2) + 1];
                                if (counts == null) {
                                    int neighborY = sectionY + (cy >> 2);
                                    if (neighborY >= level.getMinSection() && neighborY < level.getMaxSection())
                                        complete = false;
                                    continue;
                                }
                                int weight = kernelWeight(dx, dy, dz);
                                for (BlockCounters.Counter counter : COUNTERS)
                                    sums[counter.index()] += counts.cell(counter.index(), cx & 3, cy & 3, cz & 3) * weight;
                            }
                    /// 邻区块未到达不是净化；保留原值，邻区块加载后会重新登记此范围。
                    if (!complete) continue;
                    /// 换算为等效 4096 方块密度，保留原阈值的数量级。
                    for (BlockCounters.Counter counter : COUNTERS)
                        sample.set(counter.index(), equivalentCount(sums[counter.index()]));
                    Holder<Biome> original = base.get(x, y, z);
                    Holder<Biome> before = current.get(x, y, z);
                    Holder<Biome> result = judge(new SectionContext(section, sample, lookup, original, before));
                    if (result == null) result = isDynamic(before) ? original : before;
                    if (!result.equals(before)) {
                        if (replacement == null)
                            replacement = IPalettedContainer.copyBiomes(current);
                        replacement.set(x, y, z, result);
                    }
                }
        if (replacement == null) return false;
        ILevelChunkSection.of(section).confluence$setBiomes(replacement);
        return true;
    }

    public static void tick(ServerLevel level) {
        LongLinkedOpenHashSet pending = PENDING.get(level);
        if (pending == null) return;
        long started = System.nanoTime();
        LinkedHashSet<ChunkAccess> changedChunks = new LinkedHashSet<>();
        var lookup = level.registryAccess().lookupOrThrow(Registries.BIOME);
        while (!pending.isEmpty()) {
            long position = pending.removeFirstLong();
            LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.x(position), SectionPos.z(position));
            if (chunk != null && updateSection(level, chunk, SectionPos.y(position), lookup)) {
                chunk.setUnsaved(true);
                changedChunks.add(chunk);
            }
            if (System.nanoTime() - started >= TIME_BUDGET_NS) break;
        }
        if (pending.isEmpty()) PENDING.remove(level);
        /// 当 tick 有实际变化才按区块去重同步，不保留跨 tick 的同步队列。
        Map<ServerPlayer, LongOpenHashSet> watchers = WATCHED.get(level);
        if (!changedChunks.isEmpty() && watchers != null) {
            for (var entry : watchers.entrySet()) {
                ServerPlayer player = entry.getKey();
                if (player.level() != level) continue;
                List<LevelChunk> updates = new ArrayList<>();
                for (ChunkAccess chunk : changedChunks) {
                    if (entry.getValue().contains(chunk.getPos().toLong()))
                        updates.add((LevelChunk) chunk);
                }
                if (!updates.isEmpty())
                    player.connection.send(ClientboundChunksBiomesPacket.forChunks(updates));
            }
        }
    }

    /// 未 Watch 的区块无需补发：之后的完整区块包自带最新群系。
    public static void watch(ServerLevel level, ServerPlayer player, ChunkPos pos) {
        WATCHED.computeIfAbsent(level, key -> new IdentityHashMap<>()).computeIfAbsent(player, key -> new LongOpenHashSet()).add(pos.toLong());
    }

    public static void unwatch(ServerLevel level, ServerPlayer player, ChunkPos pos) {
        var watchers = WATCHED.get(level);
        if (watchers == null) return;
        var chunks = watchers.get(player);
        if (chunks == null) return;
        chunks.remove(pos.toLong());
        if (chunks.isEmpty()) watchers.remove(player);
        if (watchers.isEmpty()) WATCHED.remove(level);
    }

    public static void forgetPlayer(ServerPlayer player) {
        WATCHED.values().removeIf(watchers -> {
            watchers.remove(player);
            return watchers.isEmpty();
        });
    }

    public static void unload(ServerLevel level) {
        PENDING.remove(level);
        WATCHED.remove(level);
    }

    public static void clear() {
        PENDING.clear();
        WATCHED.clear();
    }

    /// 生成阶段只补计数；跨区块覆盖等到加载事件统一计算，不再复制下层群系。
    public static void applyDynamicBiome(ChunkAccess chunk, HolderLookup.RegistryLookup<Biome> lookup) {
        if (!isEnabled()) return;
        for (LevelChunkSection section : chunk.getSections()) {
            ILevelChunkSection data = ILevelChunkSection.of(section);
            if (!data.confluence$isCountsFresh()) {
                section.recalcBlockCounts();
                data.confluence$setCountsFresh(true);
            }
        }
    }
}
