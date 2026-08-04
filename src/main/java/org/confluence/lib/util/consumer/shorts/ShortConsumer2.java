package org.confluence.lib.util.consumer.shorts;

@FunctionalInterface
public interface ShortConsumer2<T1> {
    void accept(short t, T1 t1);

    default ShortConsumer2<T1> andThen(ShortConsumer2<T1> after) {
        return (t, t1) -> {
            this.accept(t, t1);
            after.accept(t, t1);
        };
    }
}
