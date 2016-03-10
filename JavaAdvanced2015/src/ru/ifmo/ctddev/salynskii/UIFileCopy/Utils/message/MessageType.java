package ru.ifmo.ctddev.salynskii.UIFileCopy.utils.message;

/**
 * Created by Alimantu on 06/03/16.
 */
public enum MessageType {
    CORRELATIONS_MAP(0), CORRELATION_RESOLUTIONS(1), SCANNING_COMPLETED(2), CLOSE_APP(4), COPY_COMPLETED(5);
    private int value;

    private MessageType(int value) {
        this.value = value;
    }
}
