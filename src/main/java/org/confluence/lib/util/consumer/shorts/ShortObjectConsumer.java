package org.confluence.lib.util.consumer.shorts;

@FunctionalInterface
public interface ShortObjectConsumer<T1> {
    void accept(short t, T1 t1);

    default ShortObjectConsumer<T1> andThen(ShortObjectConsumer<T1> after) {
        return (t, t1) -> {
            this.accept(t, t1);
            after.accept(t, t1);
        };
    }
}
