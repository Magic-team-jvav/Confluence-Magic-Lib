package org.confluence.lib.util.consumer.ints;

@FunctionalInterface
public interface IntConsumer2<T1> {
    void accept(int t, T1 t1);

    default IntConsumer2<T1> andThen(IntConsumer2<T1> after) {
        return (t, t1) -> {
            this.accept(t, t1);
            after.accept(t, t1);
        };
    }
}
