package org.confluence.lib.common.data.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.common.data.LanguageProvider;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.util.LibUtils;

public final class LibLanguageProvider extends LanguageProvider {
    private final boolean isEn;

    LibLanguageProvider(PackOutput output, boolean isEn) {
        super(output, ConfluenceMagicLib.LIB_ID, isEn ? "en_us" : "zh_cn");
        this.isEn = isEn;
    }

    @Override
    protected void addTranslations() {
        add("attribute.name.generic.armor_penetration", "Armor Penetration", "护甲穿透");
        add("attribute.name.generic.critical_chance", "Critical Chance", "暴击率");
        add("attribute.name.generic.dodge_chance", "Dodge Chance", "闪避率");
        add("attribute.name.generic.magic_damage", "Magic Damage", "魔法伤害");
        add("attribute.name.generic.ranged_damage", "Ranged Damage", "远程伤害");
        add("attribute.name.generic.ranged_velocity", "Ranged Velocity", "远程速度");
        add("attribute.name.player.aggro", "Aggro", "仇恨");
        add("attribute.name.player.pickup_range", "Pickup Range", "拾取范围");
        add("attribute.name.player.mark_damage", "Mark Damage", "标记伤害");
        add("attribute.name.player.minion_capacity", "Minion Capacity", "仆从容量");
        add("attribute.name.player.sentry_capacity", "Sentry Capacity", "哨兵容量");
        add("attribute.name.player.summon_damage", "Summon Damage", "召唤伤害");
        add("attribute.name.player.summon_knockback", "Summon Knockback", "召唤物击退");
        add("attribute.name.player.whip_range", "Whip Range", "鞭范围");
        add("attribute.name.player.mob_spawn_count_multiplier", "Mob Spawn Count Multiplier", "生物生成数量系数");
        add("attribute.name.player.mob_spawn_speed_multiplier", "Mob Spawn Speed Multiplier", "生物生成速度系数");
        add("death.attack.star_cloak", "%1$s was squashed by a falling star", "%1%s 被坠星压扁了");
        add("death.attack.gun_bullet", "%1%s was shot by %2$s", "%1$s 被 %2$s 枪击");
        add("jei.tooltip.environment.biome", "Requires any biome:", "需要任意生物群系：");
        add("jei.tooltip.environment.block", "Requires any block nearby:", "需要任意方块在附近：");
        add("jei.tooltip.environment.block.inflate", "Search range: %s", "搜索范围：%s");
        add("jei.tooltip.environment.block.blocks", "Blocks:", "方块：");
        add("jei.tooltip.environment.block.predicates", "Block States:", "方块状态：");
        add("jei.tooltip.environment.block.predicates.property", "Property: %s", "属性：%s");
        add("jei.tooltip.environment.block.fluids", "Fluids:", "流体：");
        add("jei.tooltip.environment.graveyard", "Requires graveyard", "需要灵雾环境");
        add("message.confluence.boss_spawn", "%s Has Awoken!", "%s已苏醒！");
        add("message.confluence.boss_leave", "%s Has Been Defeated!", "%s已被打败！");
        add("message.confluence.boss_discard", "%s Has Been Discarded！", "%s已离开！");
        add("tooltip.confluence.work_in_progress", "Still Work In Progress!", "仍在开发中！");
        add("alarm.confluence_magic_lib.this_is_free_mod", "Confluence: Otherworld(Java Edition) is a §afree mod§f; please §cdo not distribute it§f. If you paid for it, please cut your losses promptly.", "汇流来世(Java版)是一个§a免费模组§f，§c请勿分发§f；如果你是付费受害者，请及时止损。");

        if (isEn) {
            LibEffects.EFFECTS.getEntries().forEach(effect -> add(effect.get().getDescriptionId(), LibUtils.toTitleCase(effect.getId().getPath())));
            addEffect(LibEffects.CEREBRAL_MINDTRICK.get(), "Increased critical chance");
            addEffect(LibEffects.HONEY.get(), "Life regeneration is increased");
            addEffect(LibEffects.CONFUSED.get(), "Movement is reversed");
            addEffect(LibEffects.GRAVITATION.get(), "Press UP to reverse gravity");
            addEffect(LibEffects.PALADINS_SHIELD.get(), "25% of damage taken will be redirected to another player");
        } else {
            addEffect(LibEffects.CEREBRAL_MINDTRICK.get(), "控脑术", "提高暴击率");
            addEffect(LibEffects.HONEY.get(), "蜂蜜", "生命再生速度提高");
            addEffect(LibEffects.CONFUSED.get(), "困惑", "移动方向逆转");
            addEffect(LibEffects.GRAVITATION.get(), "重力", "按[%s]可逆转重力");
            addEffect(LibEffects.PALADINS_SHIELD.get(), "圣骑士护盾", "所受伤害的25%将被转移到另一名玩家身上");
        }

        add("key.confluence_magic_lib.gameplay", "Confluence Magic Lib", "汇流魔法库");
        add("key.confluence_magic_lib.flip_gravitation", "Flip Gravitation", "反转重力");
    }

    public void add(String key, String en, String zh) {
        add(key, isEn ? en : zh);
    }

    private void addEffect(MobEffect effect, String name, String tooltip) {
        add(effect, name);
        addEffect(effect, tooltip);
    }

    private void addEffect(MobEffect effect, String tooltip) {
        add("tooltip." + effect.getDescriptionId() + ".0", tooltip);
    }
}
