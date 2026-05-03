package org.confluence.lib.util.consumer;

public interface Consumer10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> {
    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10);

    default Consumer10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> andThen(Consumer10<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10> after) {
        return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) -> {
            accept(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
            after.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
        };
    }
}
