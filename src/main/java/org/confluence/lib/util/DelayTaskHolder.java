package org.confluence.lib.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 延迟任务
 * <p>
 * 更倾向与玩家或实体的自身任务的类以{@link AttachmentType}的形式存储
 * <p>
 * 目前仅有{@link LivingEntity}能正常运行
 * <p>
 * 像有前摇的攻击就时候使用该类
 * <p>
 * 像肉山生成的时候要破坏一大堆方块。之类的任务请使用{@link TaskScheduler}
 * <p>
 * 注：实体死亡时会移除所有任务
 */
public class DelayTaskHolder {
    private final AttachmentHolder attachmentHolder;
    private final Map<ResourceLocation, ITask> runList = new LinkedHashMap<>();

    public DelayTaskHolder(AttachmentHolder attachmentHolder) {
        this.attachmentHolder = attachmentHolder;
    }

    public AttachmentHolder getAttachmentHolder() {
        return attachmentHolder;
    }

    public Map<ResourceLocation, ITask> getRunList() {
        return runList;
    }

    public void tick() {
        if (runList.isEmpty()) {
            return;
        }
        Iterator<ITask> iterator = runList.values().iterator();
        while (iterator.hasNext()) {
            ITask consumer = iterator.next();
            if (consumer.isRemoved()) {
                iterator.remove();
            }
            consumer.run(this);
        }
    }

    public void addTimingRun(ResourceLocation id, ITask task) {
        runList.put(id, task);
    }

    /**
     * 通过该方法添加的任务会在对应槽位的物品更替时移除
     */
    public void addTimingRun(EquipmentSlot slot, ITask task) {
        addTimingRun(ConfluenceMagicLib.asResource(slot.getName()), task);
    }

    /**
     * 通过该方法添加的任务会在对应手的物品更替时移除
     */
    public void addTimingRun(InteractionHand handUsed, ITask task) {
        addTimingRun(handUsed == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND, task);
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

    public static DelayTaskHolder getInstance(AttachmentHolder attachmentHolder) {
        return attachmentHolder.getData(ConfluenceMagicLib.LIVING_ENTITY_DELAY_RUN);
    }

    public static ITask.Builder createTaskBilder() {
        return ITask.Builder.create();
    }

    public interface ITask {
        void run(DelayTaskHolder delayTaskHolder);

        boolean isRemoved();

        /**
         * 运行任务类，isRemoved为true时将在下一刻移除该任务
         */
        class BaseTask implements ITask {
            protected final ResultRun resultRun;
            protected int tick = 0;
            protected final int maxTick;
            protected int repeatCount = 0;
            protected final int maxRepeatCount;
            protected boolean isRemoved;

            private BaseTask(ResultRun resultRun, int removedTick, int maxRepeatCount) {
                this.resultRun = resultRun;
                this.maxTick = removedTick;
                this.maxRepeatCount = maxRepeatCount;
            }

            @Override
            public void run(DelayTaskHolder delayTaskHolder) {
                if (repeatCount == maxRepeatCount) {
                    isRemoved = true;
                    return;
                }

                if (tick >= maxTick) {
                    resultRun.run(delayTaskHolder.getAttachmentHolder());
                    repeatCount++;
                    tick = 0;
                }
                tick++;
            }

            @Override
            public boolean isRemoved() {
                return isRemoved;
            }
        }

        class TickTask extends BaseTask {
            private final TickRun tickRun;

            private TickTask(TickRun tickRun, ResultRun resultRun, int removedTick, int maxRepeatCount) {
                super(resultRun, removedTick, maxRepeatCount);
                this.tickRun = tickRun;
            }

            @Override
            public void run(DelayTaskHolder delayTaskHolder) {
                if (repeatCount == maxRepeatCount) {
                    isRemoved = true;
                    return;
                }

                if (tick >= maxTick) {
                    resultRun.run(delayTaskHolder.getAttachmentHolder());
                    repeatCount++;
                    tick = 0;
                }

                tick = tickRun.run(tick, maxTick, delayTaskHolder.getAttachmentHolder());
            }
        }

        /**
         * 每一tick执行一次可通过修改返回值来自定义结束的时间之类的逻辑
         */
        @FunctionalInterface
        interface TickRun {
            TickRun DEFAULT = (tick, maxTick, playerTimingRun) -> tick - 1;

            int run(int tick, int maxTick, AttachmentHolder attachmentHolder);
        }

        /**
         * 当剩余时间为0时执行，通过修改返回值来修改剩余时间
         */
        @FunctionalInterface
        interface ResultRun {
            void run(AttachmentHolder attachmentHolder);
        }

        class Builder {
            private @Nullable TickRun tickRun;
            private ResultRun resultRun;
            private int removedTick;
            private int repeatCount = 1;

            private Builder() {
            }

            public static Builder create() {
                return new Builder();
            }

            public Builder tickRun(TickRun tickRun) {
                this.tickRun = tickRun;
                return this;
            }

            public Builder resultRun(ResultRun resultRun) {
                this.resultRun = resultRun;
                return this;
            }

            public Builder removedTick(int removedTick) {
                this.removedTick = removedTick;
                return this;
            }

            public Builder repeatCount(int repeatCount) {
                this.repeatCount = repeatCount;
                return this;
            }

            public ITask build() {
                assert resultRun != null : "resultRun can not be null";
                assert repeatCount > 0 : "repeatCount can not be less than 1";
                return tickRun == null ?
                        new BaseTask(resultRun, removedTick, repeatCount) :
                        new TickTask(tickRun, resultRun, removedTick, repeatCount);
            }
        }
    }
}
