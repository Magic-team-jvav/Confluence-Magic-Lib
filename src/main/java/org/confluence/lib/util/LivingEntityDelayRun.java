package org.confluence.lib.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.ConfluenceMagicLib;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class LivingEntityDelayRun {
    private final LivingEntity livingEntity;
    private final Map<ResourceLocation, Run> runList = new LinkedHashMap<>();

    public LivingEntityDelayRun(LivingEntity livingEntity) {
        this.livingEntity = livingEntity;
    }

    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    public Map<ResourceLocation, Run> getRunList() {
        return runList;
    }

    public void tick() {
        Iterator<Run> iterator = runList.values().iterator();
        while (iterator.hasNext()) {
            Run consumer = iterator.next();
            if (consumer.isRemoved) {
                iterator.remove();
            }
            consumer.run(this);
        }
    }

    public void addTimingRun(ResourceLocation id, Run run) {
        runList.put(id, run);
    }

    public void addTimingRun(EquipmentSlot slot, Run run) {
        addTimingRun(ConfluenceMagicLib.asResource(slot.getName()), run);
    }

    public void addTimingRun(InteractionHand handUsed, Run run) {
        addTimingRun(handUsed == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, run);
    }

    public void removeTimingRun(ResourceLocation id) {
        runList.remove(id);
    }

    public void removeTimingRun(EquipmentSlot slot) {
        removeTimingRun(ConfluenceMagicLib.asResource(slot.getName()));
    }

    public void removeTimingRun(InteractionHand handUsed) {
        removeTimingRun(handUsed == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    public void removeAllTimingRun() {
        runList.clear();
    }

    public static LivingEntityDelayRun getInstance(LivingEntity entity) {
        return entity.getData(ConfluenceMagicLib.LIVING_ENTITY_DELAY_RUN);
    }

    public static Run createTimingRun(Run.ResultRun resultRun, int tick) {
        return createTimingRunBilder().build(resultRun, tick);
    }

    public static Run.Builder createTimingRunBilder() {
        return Run.Builder.create();
    }

    public static class Run {
        private final TickRun tickRun;
        private final ResultRun resultRun;
        private int remainingTick;
        private boolean isRemoved;
        private final int maxTick;

        private Run(TickRun tickRun, ResultRun resultRun, int tick) {
            this.tickRun = tickRun;
            this.resultRun = resultRun;
            this.remainingTick = tick;
            this.maxTick = tick;
        }

        public void run(LivingEntityDelayRun livingEntityDelayRun) {
            if (isRemoved) {
                return;
            }

            remainingTick = tickRun.run(remainingTick, maxTick, livingEntityDelayRun.getLivingEntity());
            if (remainingTick <= 0) {
                remainingTick = resultRun.run(livingEntityDelayRun.getLivingEntity());
            }

            if (remainingTick <= 0) {
                isRemoved = true;
            }
        }

        public static class Builder {
            private TickRun tickRun = TickRun.DEFAULT;

            private Builder() {
            }

            public static Builder create() {
                return new Builder();
            }

            public Builder tickRun(TickRun tickRun) {
                this.tickRun = tickRun;
                return this;
            }

            public Run build(ResultRun resultRun, int maxTick) {
                return new Run(tickRun, resultRun, maxTick);
            }

            public Run build(int maxTick) {
                return build(ResultRun.DEFAULT, maxTick);
            }
        }

        @FunctionalInterface
        public interface TickRun {
            TickRun DEFAULT = (tick, maxTick, playerTimingRun) -> tick - 1;

            int run(int tick, int maxTick, LivingEntity livingEntity);
        }

        @FunctionalInterface
        public interface ResultRun {
            ResultRun DEFAULT = (playerTimingRun) -> 0;

            int run(LivingEntity livingEntity);
        }
    }
}
