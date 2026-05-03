package org.confluence.lib.util;

import org.jetbrains.annotations.Nullable;

/// 用来在lambda循环中跳出循环
public class ReturnException extends RuntimeException {
    private final @Nullable Object value;

    public ReturnException(@Nullable Object value) {
        this.value = value;
    }

    public ReturnException() {
        this(null);
    }

    public @Nullable Object getValue() {
        return value;
    }
}
