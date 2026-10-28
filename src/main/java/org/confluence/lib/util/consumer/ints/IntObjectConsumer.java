package org.confluence.lib.util.consumer.ints;

@FunctionalInterface
public interface IntObjectConsumer<T1> {
    void accept(int t, T1 t1);

    default IntObjectConsumer<T1> andThen(IntObjectConsumer<T1> after) {
        return (t, t1) -> {
            this.accept(t, t1);
            after.accept(t, t1);
        };
    }
}
