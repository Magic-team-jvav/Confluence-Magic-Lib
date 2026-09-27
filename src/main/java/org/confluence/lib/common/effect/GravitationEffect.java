package org.confluence.lib.common.effect;

import com.google.common.collect.ImmutableMultimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.confluence.lib.ConfluenceMagicLib;

/// 反转重力的效果本体。
///
/// **1.20 侧的三处差异（都不是照抄，逐条说明）**：
/// 1. id 形态：1.20 用 `UUID.nameUUIDFromBytes("gravitation_flip")` + `AttributeModifier(UUID, String, double, Operation)`，
///    那是 PortLib 为模拟 1.21 而提供的旧构造器；1.21.1 的 `AttributeModifier` 是 record，
///    第一参数收 `ResourceLocation`（1.21 的 TerraCurio 版本本来就这么写）。
/// 2. 属性载体：1.20 写 `Attributes.GRAVITY.value()`（Forge 的 `Holder`），1.21.1 原版
///    `Attributes.GRAVITY` 本身就是 `Holder<Attribute>`（`Attributes.java:74`），不需要 `.value()`。
/// 3. `@Diff` 注解来自 PortLib（`org.mesdag.portlib.diff.Diff`，PortLib 一律不移植），去掉。
/// 修饰符数值与 Operation 与 1.20 一致（-2.0 / `MULTIPLY_TOTAL` → 1.21 的 `ADD_MULTIPLIED_TOTAL`）。
public class GravitationEffect extends MobEffect {
    public static final ResourceLocation ID = ConfluenceMagicLib.asResource("gravitation_flip");
    public static final ImmutableMultimap<Holder<Attribute>, AttributeModifier> GRAVITY = ImmutableMultimap.of(
            Attributes.GRAVITY, new AttributeModifier(ID, -2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );

    public GravitationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAA00AA);
    }
}
