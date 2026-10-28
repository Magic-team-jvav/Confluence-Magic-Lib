package org.confluence.lib.util.consumer.floats;

@FunctionalInterface
public interface FloatFloatConsumer {
    void accept(float t, float t2);

    default FloatFloatConsumer andThen(FloatFloatConsumer after) {
        return (t, t1) -> {
            this.accept(t, t1);
            after.accept(t, t1);
        };
    }
}
