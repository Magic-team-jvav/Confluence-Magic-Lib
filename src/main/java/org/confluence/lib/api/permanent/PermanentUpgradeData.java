package org.confluence.lib.api.permanent;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.UnknownNullability;
import org.mesdag.portlib.wrapper.IPortNBTSerializable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MagicLib 提供的通用永久强化玩家数据。
 *
 * <p>容器只保存稳定 ID 与有符号等级，不绑定具体物品或效果。零级不会写入存档。
 * 1.20 重写版本只读取当前格式，不迁移旧格式。</p>
 */
public final class PermanentUpgradeData implements IPortNBTSerializable<CompoundTag> {
    private static final int DATA_VERSION = 1;
    private final Map<ResourceLocation, Integer> levels = new HashMap<>();

    /**
     * 读取指定强化的等级，未记录时返回零。
     */
    public int getLevel(ResourceLocation id) {
        return levels.getOrDefault(Objects.requireNonNull(id, "id"), 0);
    }

    /**
     * 返回指定强化是否保存了非零等级。
     */
    public boolean has(ResourceLocation id) {
        return getLevel(id) != 0;
    }

    /**
     * 写入等级；零级会删除对应条目。
     */
    public void setLevel(ResourceLocation id, int level) {
        Objects.requireNonNull(id, "id");
        if (level == 0) levels.remove(id);
        else levels.put(id, level);
    }

    @Override
    public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag root = new CompoundTag();
        root.putInt("version", DATA_VERSION);
        ListTag entries = new ListTag();
        levels.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    CompoundTag tag = new CompoundTag();
                    tag.putString("id", entry.getKey().toString());
                    tag.putInt("level", entry.getValue());
                    entries.add(tag);
                });
        root.put("upgrades", entries);
        return root;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag root) {
        levels.clear();
        if (root.getInt("version") != DATA_VERSION) return;

        ListTag entries = root.getList("upgrades", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag tag = entries.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            int level = tag.getInt("level");
            if (id != null && level != 0) levels.putIfAbsent(id, level);
        }
    }

    public static PermanentUpgradeData of(LivingEntity entity) {
        return entity.getData(ConfluenceMagicLib.PERMANENT_UPGRADES);
    }
}
