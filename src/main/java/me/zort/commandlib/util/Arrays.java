package me.zort.commandlib.util;

import java.util.function.Function;

public final class Arrays {

    public static <O, T> T[] map(O[] array, Function<O, T> function) {
        T[] result = (T[]) new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = function.apply(array[i]);
        }
        return result;
    }

}
