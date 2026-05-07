package org.confluence.lib.util.function.ints;

@FunctionalInterface
public interface ToIntFunction3<T1, T2, T3> {
    int applyAsInt(T1 t1, T2 t2, T3 t3);
}
