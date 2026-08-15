package org.confluence.lib.common.item;

import PortLib.extensions.net.minecraft.resources.ResourceLocation.PortResourceLocationExtension;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.api.event.AddGroupInvalidCreativeModeTabEvent;
import org.confluence.lib.api.event.CustomGroupItemIconEvent;
import org.confluence.lib.mixed.ILibClientItemStack;
import org.confluence.lib.util.LibUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupItem extends Item {
    private static GroupItem instance;
    private static final Set<ResourceKey<CreativeModeTab>> INVALID_CREATIVE_MODE_TABS = new ReferenceOpenHashSet<>();

    @ApiStatus.Internal
    public GroupItem() {
        super(new Properties().stacksTo(1));
        if (instance != null) {
            throw new UnsupportedOperationException("Group Item must be singleton instance");
        }
        instance = this;
        INVALID_CREATIVE_MODE_TABS.add(CreativeModeTabs.SEARCH);
        PortEventHandler.postEvent(new AddGroupInvalidCreativeModeTabEvent(INVALID_CREATIVE_MODE_TABS::add));
    }

    public static GroupItem getInstance() {
        return instance;
    }

    public static boolean isInvalidCreativeModeTab(ResourceKey<CreativeModeTab> tabKey) {
        return INVALID_CREATIVE_MODE_TABS.contains(tabKey);
    }

    public static boolean isInvalidCreativeModeTab(CreativeModeTab tab) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(tab).map(GroupItem::isInvalidCreativeModeTab).orElse(false);
    }

    public static ItemStack of(ResourceLocation name, ItemStack... stacks) {
        return of(name, Arrays.asList(stacks));
    }

    public static ItemStack of(ResourceLocation name, List<ItemStack> stacks) {
        ItemStack stack = getInstance().getDefaultInstance();
        stack.set(ConfluenceMagicLib.GROUP_STACKS, Stacks.of(name, false, stacks));
        stack.setCustomName(Component.translatable("itemGroup." + name.getNamespace() + "." + name.getPath()));
        return stack;
    }

    @ApiStatus.Internal
    public static void toggleVisibility(ItemStack group) {
        Stacks stacks = group.get(ConfluenceMagicLib.GROUP_STACKS);
        if (stacks == null) throw new NullPointerException("Group item stacks must not be null");
        group.set(ConfluenceMagicLib.GROUP_STACKS, stacks.toggleVisibility());
    }

    public static class Stacks {
        public static final Stacks EMPTY = new Stacks(ConfluenceMagicLib.asResource("empty"), false, new ItemStack[0], -1);
        public static final Codec<Stacks> CODEC = Codec.unit(EMPTY);
        public static final PortStreamCodec<ByteBuf, Stacks> STREAM_CODEC = PortStreamCodec.unit(EMPTY);
        private static final AtomicInteger cachedId = new AtomicInteger();

        private transient ObjectLinkedOpenCustomHashSet<ItemStack> duplicateChecker;
        private transient int lastIndex;
        private transient long lastAdvanceTime;
        private transient ItemStack iconStack;

        private transient final ResourceLocation name;
        private transient final boolean visible;
        private transient final ItemStack[] values;
        private transient final int id;

        private Stacks(ResourceLocation name, boolean visible, ItemStack[] values, int id) {
            this.name = name;
            this.visible = visible;
            this.values = values;
            this.id = id;
            if (LibUtils.isPhysicalClient()) {
                for (ItemStack stack : values) {
                    ILibClientItemStack.of(stack).confluence$clientSetGroupId(id);
                }
            }
        }

        public ResourceLocation getName() {
            return name;
        }

        public boolean isVisible() {
            return visible;
        }

        @Unmodifiable
        public ItemStack[] getValues() {
            return values;
        }

        @ApiStatus.Internal
        public ItemStack getCurrentRendered(long currentAdvanceTime) {
            if (iconStack != null) {
                return iconStack;
            }
            int length = values.length;
            if (length == 0) {
                return ItemStack.EMPTY;
            }
            if (length == 1) {
                return values[0];
            }
            if (currentAdvanceTime != lastAdvanceTime) {
                this.lastAdvanceTime = currentAdvanceTime;
                if (++lastIndex >= length) {
                    this.lastIndex = 0;
                }
            }
            return values[lastIndex];
        }

        public int getId() {
            return id;
        }

        @Override
        public final boolean equals(Object o) {
            return o == this || (o instanceof Stacks stacks && stacks.visible == visible && stacks.id == id && stacks.name.equals(name) && Arrays.equals(stacks.values, values));
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + Arrays.hashCode(values);
            result = 31 * result + Boolean.hashCode(visible);
            result = 31 * result + id;
            return result;
        }

        @Override
        public String toString() {
            return "Stacks{" +
                    "name=" + name +
                    ", visible=" + visible +
                    ", values=" + Arrays.toString(values) +
                    ", id=" + id +
                    '}';
        }

        @ApiStatus.Internal
        public Stacks toggleVisibility() {
            Stacks data = new Stacks(name, !visible, values, id == -1 ? cachedId.getAndIncrement() : id);
            if (iconStack != null) {
                data.iconStack = iconStack.copy();
            }
            return data;
        }

        @ApiStatus.Internal
        public Stacks withValues(ResourceKey<CreativeModeTab> tabKey, ItemStack... stacks) {
            if (stacks.length == 0) return this;
            ItemStack[] src = new ItemStack[values.length + stacks.length];
            System.arraycopy(values, 0, src, 0, values.length);
            int i = 0;
            for (ItemStack stack : stacks) {
                if (duplicateChecker == null) {
                    this.duplicateChecker = new ObjectLinkedOpenCustomHashSet<>(values, ItemStackLinkedSet.TYPE_AND_TAG);
                }
                if (duplicateChecker.contains(stack)) continue;
                src[values.length + i] = stack;
                duplicateChecker.add(stack);
                ++i;
            }
            ItemStack[] dest = new ItemStack[values.length + i];
            System.arraycopy(src, 0, dest, 0, dest.length);
            Stacks data = new Stacks(name, visible, dest, id == -1 ? cachedId.getAndIncrement() : id);
            data.duplicateChecker = duplicateChecker.clone();
            data.iconStack = iconStack == null ? CustomGroupItemIconEvent.getIcon(tabKey, name) : iconStack.copy();
            return data;
        }

        public static Stacks of(ResourceLocation name, boolean visible, ItemStack... stacks) {
            return of(name, visible, Arrays.asList(stacks));
        }

        public static Stacks of(ResourceLocation name, boolean visible, List<ItemStack> stacks) {
            int id = cachedId.getAndIncrement();
            Set<ItemStack> set = ItemStackLinkedSet.createTypeAndTagSet();
            for (ItemStack stack : stacks) {
                if (LibUtils.isPhysicalClient()) {
                    ILibClientItemStack.of(stack).confluence$clientSetGroupId(id);
                }
                set.add(stack);
            }
            return new Stacks(name, visible, set.toArray(ItemStack[]::new), id);
        }
    }

    public static BelongsTo belongsTo(String path) {
        return new BelongsTo(ConfluenceMagicLib.asConfluenceResource(path));
    }

    public static CreativeModeTab.Output belongsTo(String path, CreativeModeTab.Output output) {
        if (LibStartupConfig.itemGroups()) {
            return belongsTo(belongsTo(path), output);
        }
        return output;
    }

    public static CreativeModeTab.Output belongsTo(BelongsTo belongsTo, CreativeModeTab.Output output) {
        if (LibStartupConfig.itemGroups()) {
            return (stack, tabVisibility) -> {
                stack.set(ConfluenceMagicLib.BELONGS_TO_GROUP, belongsTo);
                output.accept(stack, tabVisibility);
            };
        }
        return output;
    }

    public record BelongsTo(ResourceLocation name) {
        public static final Codec<BelongsTo> CODEC = ResourceLocation.CODEC.xmap(BelongsTo::new, BelongsTo::name);
        public static final PortStreamCodec<ByteBuf, BelongsTo> STREAM_CODEC = PortResourceLocationExtension.streamCodec().map(BelongsTo::new, BelongsTo::name);

        @Override
        public boolean equals(Object o) {
            return o == this || (o instanceof BelongsTo b && b.name().equals(name));
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    @ApiStatus.Internal
    public record DisplayItems(Collection<ItemStack> delegate) implements Collection<ItemStack> {
        @Override
        public int size() {
            int size = delegate.size();
            for (ItemStack stack : delegate) {
                if (stack.is(getInstance())) {
                    Stacks stacks = stack.getOrDefault(ConfluenceMagicLib.GROUP_STACKS, Stacks.EMPTY);
                    if (stacks.visible) size += stacks.values.length;
                }
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            for (ItemStack stack : this) {
                if (Objects.equals(stack, o)) return true;
            }
            return false;
        }

        @Override
        public Iterator<ItemStack> iterator() {
            Iterator<ItemStack> groups = delegate.iterator();
            return new Iterator<>() {
                private Iterator<ItemStack> children = Collections.emptyIterator();

                @Override
                public boolean hasNext() {
                    return children.hasNext() || groups.hasNext();
                }

                @Override
                public ItemStack next() {
                    if (children.hasNext()) return children.next();
                    ItemStack stack = groups.next();
                    if (stack.is(getInstance())) {
                        Stacks stacks = stack.getOrDefault(ConfluenceMagicLib.GROUP_STACKS, Stacks.EMPTY);
                        if (stacks.visible) children = Arrays.asList(stacks.values).iterator();
                    }
                    return stack;
                }
            };
        }

        @Override
        public Object[] toArray() {
            return expandedList().toArray();
        }

        @Override
        public <T> T[] toArray(T[] ts) {
            return expandedList().toArray(ts);
        }

        @Override
        public boolean add(ItemStack e) {
            return delegate.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            return collection.stream().allMatch(this::contains);
        }

        @Override
        public boolean addAll(Collection<? extends ItemStack> collection) {
            return delegate.addAll(collection);
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return delegate.removeAll(collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return delegate.retainAll(collection);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        /**
         * 构建创造栏真正应该看到的物品序列。
         *
         * <p>原版创造栏会同时使用 {@link #size()}、{@link #iterator()} 和 {@link #toArray()} 来刷新槽位与滚动范围。
         * 如果这些入口返回的数量不一致，展开分组后就会出现槽位错位或滚动范围异常。</p>
         */
        private List<ItemStack> expandedList() {
            List<ItemStack> items = new ArrayList<>(size());
            forEach(items::add);
            return items;
        }
    }
}
