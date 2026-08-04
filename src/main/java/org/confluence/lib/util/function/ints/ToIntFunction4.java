package org.confluence.lib.util.function.ints;

@FunctionalInterface
public interface ToIntFunction4<T1, T2, T3, T4> {
    int applyAsInt(T1 t1, T2 t2, T3 t3, T4 t4);
}
