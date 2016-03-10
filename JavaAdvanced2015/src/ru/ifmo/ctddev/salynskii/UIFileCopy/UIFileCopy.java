package ru.ifmo.ctddev.salynskii.UIFileCopy;

import javafx.util.Pair;
import ru.ifmo.ctddev.salynskii.UIFileCopy.UI.SwingRunnable;
import ru.ifmo.ctddev.salynskii.UIFileCopy.utils.*;
import ru.ifmo.ctddev.salynskii.UIFileCopy.utils.message.Message;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 03/03/16.
 */
public class UIFileCopy {

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected input format: <path of the copied file/dir> <path to the copy dir>");
        }
        Path path1 = Paths.get(args[0]);
        Path path2 = Paths.get(args[1]);
        if (Files.isRegularFile(path2)) {
            throw new IllegalArgumentException("Expected directory name as second argument, but found file name - " + args[1]);
        }
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicLong copiedBytes = new AtomicLong(0);
        ConcurrentLinkedQueue<String> messagesLog = new ConcurrentLinkedQueue<>();
        Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> uIMessagesChanel =
                new Pair<>(new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
        SwingRunnable sr = new SwingRunnable(copiedBytes, totalBytes, uIMessagesChanel);
        sr.setMessagesLog(messagesLog);
        Thread srThread = new Thread(sr);
        srThread.start();

        FileCopy fc = new FileCopy(path1, path2);
        fc.setCounters(totalBytes, copiedBytes);
        fc.setMessagesLog(messagesLog);
        Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> fCMessagesChanel =
                new Pair<>(new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
        fc.setMessagesChanel(fCMessagesChanel);
        Thread fcThread = new Thread(fc);
        fcThread.start();

        checkMessages(uIMessagesChanel, fCMessagesChanel, fcThread, srThread);
    }

    private static void checkMessages(Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> uIMessagesChanel,
                                      Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> fCMessagesChanel,
                                      Thread fcThread, Thread srThread) {
        boolean alive = true;
        while (alive) {
            alive = fcThread.isAlive();
            if (Thread.interrupted()) {
                fcThread.interrupt();
                srThread.interrupt();
                alive = false;
            }
            while (!uIMessagesChanel.getValue().isEmpty()) {
                Message income = uIMessagesChanel.getValue().poll();
                switch (income.getMessageType()) {
                    case CLOSE_APP:
                        fcThread.interrupt();
                        alive = false;
                    case CORRELATION_RESOLUTIONS:
                        fCMessagesChanel.getKey().add(income);
                        break;
                }
            }
            while (!fCMessagesChanel.getValue().isEmpty()) {
                Message income = fCMessagesChanel.getValue().poll();
                switch (income.getMessageType()) {
                    case SCANNING_COMPLETED:
                    case CORRELATIONS_MAP:
                        uIMessagesChanel.getKey().add(income);
                        break;
                    case COPY_COMPLETED:
                        uIMessagesChanel.getKey().add(income);
                        alive = false;
                }
            }
            Thread.yield();
        }
    }
}
