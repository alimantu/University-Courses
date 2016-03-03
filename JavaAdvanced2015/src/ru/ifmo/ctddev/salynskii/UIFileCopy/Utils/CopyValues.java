package ru.ifmo.ctddev.salynskii.UIFileCopy.Utils;

/**
 * Created by Alimantu on 03/03/16.
 */
public enum CopyValues {
    IGNORE_MODE(0), COPYING_WITH_MARKER(1), REPLACE_MODE(2);
    private int value;

    private CopyValues(int value) {
        this.value = value;
    }


}
