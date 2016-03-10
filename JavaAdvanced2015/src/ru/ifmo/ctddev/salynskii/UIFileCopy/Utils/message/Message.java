package ru.ifmo.ctddev.salynskii.UIFileCopy.utils.message;

/**
 * Created by Alimantu on 06/03/16.
 */
public class Message {
    private final MessageType messageType;
    private final Object value;

    public Message(MessageType messageType, Object value) {
        this.messageType = messageType;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public MessageType getMessageType() {
        return messageType;
    }

}
