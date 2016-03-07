package ru.ifmo.ctddev.salynskii.UIFileCopy.Utils;

/**
 * Created by Alimantu on 06/03/16.
 */
public enum MessageType {
    CORRELATIONS_MAP(0), CORRELATION_RESOLUTIONS(1000);
    private int value;

    private MessageType(int value) {
        this.value = value;
    }
}
