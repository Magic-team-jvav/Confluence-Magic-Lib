package org.confluence.lib.util.range;

public record IntegerRange(int min, int max) {
    public IntegerRange {
        if (min > max) {
            throw new IllegalArgumentException("min value must smaller than max value");
        }
    }

    public static IntegerRange of(int min, int max) {
        return new IntegerRange(min, max);
    }
}
