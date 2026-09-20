package org.confluence.lib.common.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

/// 一条动态群系判定规则。
///
/// 规则按注册顺序求值（见 {@link DynamicBiomeUtils#registerRule(BiomeRule)}），
/// 第一条返回非 null 的生效 —— 所以「谁优先」由注册顺序表达。
@FunctionalInterface
public interface BiomeRule {
    /// @return 该 section 应该变成的群系；null 表示本条规则不命中，交给下一条
    @Nullable Holder<Biome> test(SectionContext context);
}
