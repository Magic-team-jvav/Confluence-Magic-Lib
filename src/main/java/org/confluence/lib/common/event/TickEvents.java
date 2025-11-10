package org.confluence.lib.common.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.util.NaturalSpawnerUtil;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID)
public final class TickEvents {
    @SubscribeEvent
    public static void levelTick$Post(LevelTickEvent.Pre event) {
        // TODO 临时值
        if (!(event.getLevel() instanceof ServerLevel serverLevel)/* || serverLevel.getGameTime() % (1 * 20) != 0*/) {
            return;
        }
        NaturalSpawnerUtil.initOrUpdate(serverLevel);
    }
}
