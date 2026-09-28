package org.confluence.lib.mixed;

import net.minecraft.world.entity.Entity;

/// 实体上的「重力反转」状态（WP6c：整条特性从 1.20 的 Lib 搬到 1.21 的 Lib）。
///
/// 1.20 逐字搬运（`Confluence-Magic-Lib` 的 `lib/mixed/ILibEntity.java`），只改了包路径无关的部分：
/// 1.21 的 `SelfGetter`（`org.confluence.lib.mixed.SelfGetter`）与 1.20 同名同形，`of(...)` 也照抄。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** 这里只有「能力声明」：
/// 真正的注入在 `org.confluence.lib.mixin.EntityMixin`（`@Unique` 字段 `confluence$isShouldRot` /
/// `confluence$dimensionHeight`），而写入该状态的唯一入口是
/// `org.confluence.lib.network.c2s.GravitationPacketC2S`（**尚未在 `LibModEvents` 注册**）。
/// 因此在第二步接线之前，`confluence$isShouldRot()` 恒为 `false`，全部注入都是 no-op —— 行为零变化。
///
/// 与 1.21 现状的关系：这条特性目前住在 **TerraCurio**（`terra_curio.mixed.IEntity`）。
/// 两个接口不能同时挂在 `Entity` 上又用同一套 `@Unique` 字段名（会因重复字段崩），
/// 所以 Lib 这份的字段名一律用 `confluence$` 前缀（见 `EntityMixin` 的类注释），
/// 第二步做**原子切换**时删掉 TerraCurio 那份即可。
public interface ILibEntity extends SelfGetter<Entity> {
    void confluence$setShouldRot(boolean bool);

    boolean confluence$isShouldRot();

    float confluence$getDimensionHeight();

    static ILibEntity of(Entity entity) {
        return (ILibEntity) entity;
    }
}
