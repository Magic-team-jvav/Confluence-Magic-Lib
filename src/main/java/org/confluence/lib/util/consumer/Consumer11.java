package org.confluence.lib.util.consumer;

public interface Consumer11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> {
    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11);

    default Consumer11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> andThen(Consumer11<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? super T10, ? super T11> after) {
        return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) -> {
            accept(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
            after.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
        };
    }
}
