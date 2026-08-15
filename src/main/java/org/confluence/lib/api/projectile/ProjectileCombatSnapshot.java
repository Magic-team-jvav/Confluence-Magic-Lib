package org.confluence.lib.api.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * 玩家武器弹幕在发射瞬间冻结的不可变战斗数据。
 *
 * <p>武器在写入和读取边界都会复制，并统一保存为一件。飞行中弹幕必须使用本对象，
 * 不能在命中时重新读取所有者当前主手。当前格式不负责读取任何旧 NBT。</p>
 */
public final class ProjectileCombatSnapshot {
    public static final int CURRENT_FORMAT_VERSION = 1;
    private static final String VERSION_TAG = "Version";
    private static final String WEAPON_TAG = "Weapon";
    private static final String DAMAGE_CHANNEL_TAG = "DamageChannel";
    private static final String BASE_DAMAGE_TAG = "BaseDamage";
    private static final String CHANNEL_MULTIPLIER_TAG = "ChannelMultiplier";
    private static final String RESOLVED_VELOCITY_TAG = "ResolvedVelocity";
    private static final String KNOCKBACK_TAG = "Knockback";
    private static final String ARMOR_PENETRATION_TAG = "ArmorPenetration";
    private static final String CRITICAL_RESOLUTION_TAG = "CriticalResolution";

    private final ItemStack weapon;
    private final ProjectileDamageChannel damageChannel;
    private final float baseDamage;
    private final float channelMultiplier;
    private final float resolvedVelocity;
    private final float knockback;
    private final float armorPenetration;
    private final CriticalResolution criticalResolution;

    /**
     * 创建当前格式的战斗快照。
     *
     * @param weapon             发射时使用的非空武器；内部固定复制为一件
     * @param damageChannel      唯一主伤害通道
     * @param baseDamage         应用通道倍率前的基础伤害
     * @param channelMultiplier  已解析且只会应用一次的主通道倍率
     * @param resolvedVelocity   已解析的实际发射速度
     * @param knockback          已解析的最终击退
     * @param armorPenetration   已解析的护甲穿透
     * @param criticalResolution 发射时暴击三态
     */
    public ProjectileCombatSnapshot(
            ItemStack weapon,
            ProjectileDamageChannel damageChannel,
            float baseDamage,
            float channelMultiplier,
            float resolvedVelocity,
            float knockback,
            float armorPenetration,
            CriticalResolution criticalResolution
    ) {
        Objects.requireNonNull(weapon, "Weapon must not be null");
        if (weapon.isEmpty()) {
            throw new IllegalArgumentException("Weapon must not be empty");
        }
        this.damageChannel = Objects.requireNonNull(damageChannel, "Damage channel must not be null");
        this.criticalResolution = Objects.requireNonNull(
                criticalResolution, "Critical resolution must not be null");
        this.baseDamage = requireNonNegative(baseDamage, "Base damage");
        this.channelMultiplier = requireNonNegative(channelMultiplier, "Channel multiplier");
        this.resolvedVelocity = requirePositive(resolvedVelocity, "Resolved velocity");
        this.knockback = requireNonNegative(knockback, "Knockback");
        this.armorPenetration = requireNonNegative(armorPenetration, "Armor penetration");

        // 先验证乘法不会产生无穷值，避免损坏配置在命中阶段才暴露。
        float resolvedDamage = baseDamage * channelMultiplier;
        if (!Float.isFinite(resolvedDamage)) {
            throw new IllegalArgumentException("Resolved damage must be finite");
        }
        this.weapon = weapon.copyWithCount(1);
    }

    public int formatVersion() {
        return CURRENT_FORMAT_VERSION;
    }

    /**
     * 返回防御性复制，调用方不能修改快照内部武器。
     */
    public ItemStack weapon() {
        return weapon.copy();
    }

    public ProjectileDamageChannel damageChannel() {
        return damageChannel;
    }

    public float baseDamage() {
        return baseDamage;
    }

    public float channelMultiplier() {
        return channelMultiplier;
    }

    public float resolvedVelocity() {
        return resolvedVelocity;
    }

    public float knockback() {
        return knockback;
    }

    public float armorPenetration() {
        return armorPenetration;
    }

    public CriticalResolution criticalResolution() {
        return criticalResolution;
    }

    /**
     * 为命中特效、分裂弹或其他派生弹幕创建新的冻结快照。
     *
     * <p>武器、主伤害通道、通道倍率、护甲穿透和本次暴击结果全部沿用父快照；调用方只声明
     * 派生弹幕自身的基础伤害、实际速度和击退。该方法不会重新读取玩家当前装备或属性，因此
     * 父弹幕飞行期间切换武器不会改变后续派生弹幕的数值。</p>
     *
     * @param derivedBaseDamage       派生弹幕应用通道倍率前的基础伤害
     * @param derivedResolvedVelocity 派生弹幕的已解析实际速度
     * @param derivedKnockback        派生弹幕的已解析击退
     */
    public ProjectileCombatSnapshot derive(
            float derivedBaseDamage,
            float derivedResolvedVelocity,
            float derivedKnockback
    ) {
        return new ProjectileCombatSnapshot(
                weapon,
                damageChannel,
                derivedBaseDamage,
                channelMultiplier,
                derivedResolvedVelocity,
                derivedKnockback,
                armorPenetration,
                criticalResolution
        );
    }

    /**
     * 将主通道倍率应用到传入伤害；不会读取攻击者当前属性。
     */
    public float applyChannelMultiplier(float amount) {
        requireNonNegative(amount, "Damage amount");
        float result = amount * channelMultiplier;
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException("Resolved damage amount must be finite");
        }
        return result;
    }

    /**
     * 写出完整的当前格式标签。
     */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_TAG, CURRENT_FORMAT_VERSION);
        tag.put(WEAPON_TAG, weapon.save(new CompoundTag()));
        tag.putString(DAMAGE_CHANNEL_TAG, damageChannel.name());
        tag.putFloat(BASE_DAMAGE_TAG, baseDamage);
        tag.putFloat(CHANNEL_MULTIPLIER_TAG, channelMultiplier);
        tag.putFloat(RESOLVED_VELOCITY_TAG, resolvedVelocity);
        tag.putFloat(KNOCKBACK_TAG, knockback);
        tag.putFloat(ARMOR_PENETRATION_TAG, armorPenetration);
        tag.putString(CRITICAL_RESOLUTION_TAG, criticalResolution.name());
        return tag;
    }

    /**
     * 读取完整的当前格式标签。
     *
     * <p>旧版本、缺字段或损坏值会立即抛出英文开发者错误。实体读取方应捕获该错误、
     * 记录限频日志并让弹幕安全失效，不得回退到当前主手或默认高伤害。</p>
     */
    public static ProjectileCombatSnapshot fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "Snapshot tag must not be null");
        requireTag(tag, VERSION_TAG, Tag.TAG_INT);
        int version = tag.getInt(VERSION_TAG);
        if (version != CURRENT_FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported projectile combat snapshot version: " + version);
        }
        requireTag(tag, WEAPON_TAG, Tag.TAG_COMPOUND);
        requireTag(tag, DAMAGE_CHANNEL_TAG, Tag.TAG_STRING);
        requireTag(tag, BASE_DAMAGE_TAG, Tag.TAG_ANY_NUMERIC);
        requireTag(tag, CHANNEL_MULTIPLIER_TAG, Tag.TAG_ANY_NUMERIC);
        requireTag(tag, RESOLVED_VELOCITY_TAG, Tag.TAG_ANY_NUMERIC);
        requireTag(tag, KNOCKBACK_TAG, Tag.TAG_ANY_NUMERIC);
        requireTag(tag, ARMOR_PENETRATION_TAG, Tag.TAG_ANY_NUMERIC);
        requireTag(tag, CRITICAL_RESOLUTION_TAG, Tag.TAG_STRING);

        ItemStack weapon = ItemStack.of(tag.getCompound(WEAPON_TAG));
        if (weapon.isEmpty()) {
            throw new IllegalArgumentException("Snapshot weapon must not be empty");
        }
        ProjectileDamageChannel channel = parseEnum(
                ProjectileDamageChannel.class, tag.getString(DAMAGE_CHANNEL_TAG), "damage channel");
        CriticalResolution critical = parseEnum(
                CriticalResolution.class, tag.getString(CRITICAL_RESOLUTION_TAG), "critical resolution");
        return new ProjectileCombatSnapshot(
                weapon,
                channel,
                tag.getFloat(BASE_DAMAGE_TAG),
                tag.getFloat(CHANNEL_MULTIPLIER_TAG),
                tag.getFloat(RESOLVED_VELOCITY_TAG),
                tag.getFloat(KNOCKBACK_TAG),
                tag.getFloat(ARMOR_PENETRATION_TAG),
                critical
        );
    }

    private static void requireTag(CompoundTag tag, String key, int expectedType) {
        if (!tag.contains(key, expectedType)) {
            throw new IllegalArgumentException("Missing or invalid projectile combat field: " + key);
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, String fieldName) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown projectile combat " + fieldName + ": " + value, exception);
        }
    }

    private static float requireNonNegative(float value, String fieldName) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
    }

    private static float requirePositive(float value, String fieldName) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
        return value;
    }
}
