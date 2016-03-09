package ru.ifmo.ctddev.salynskii.UIFileCopy.Utils;

import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 04/03/16.
 */
public class FileCopy implements Runnable {
    private static final int MIN_FILES_COUNT = 30;
    private static final CopyValues DEFAULT_MODE = CopyValues.COPYING_WITH_MARKER;
    private final Path path1;
    private final Path path2;
    private final ConcurrentMap<String, Map<String, Path>> from;
    private final ConcurrentMap<String, Map<String, Path>> to;
    private ConcurrentLinkedQueue<String> messagesLog;
    private boolean loggingMode = false;
    private AtomicLong totalCount;
    private AtomicLong currentCount;
    private boolean countingMode = false;
    private Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> messagesChanel;
    private boolean messagesChanelSet = false;
    private boolean isInterrupted = false;

    public FileCopy(Path path1, Path path2) {
        checkNull(new Pair<>("Path path1", path1),
                new Pair<>("Path path2", path2));
        this.path1 = path1;
        this.path2 = path2;
        this.from = new ConcurrentHashMap<>();
        this.to = new ConcurrentHashMap<>();
    }

    public void setMessagesLog(ConcurrentLinkedQueue<String> messagesLog) {
        checkNull(new Pair<>("ConcurrentLinkedQueue<String>", messagesLog));
        this.loggingMode = true;
        this.messagesLog = messagesLog;
    }

    public void setCounters(AtomicLong totalCount, AtomicLong currentCount) {
        checkNull(new Pair<>("AtomicLong totalCount", totalCount),
                new Pair<>("AtomicLong currentCount", currentCount));
        this.countingMode = true;
        this.totalCount = totalCount;
        this.currentCount = currentCount;
    }

    public void setMessagesChanel(Pair<ConcurrentLinkedQueue<Message>, ConcurrentLinkedQueue<Message>> messagesChanel) {
        checkNull(new Pair<>("ConcurrentLinkedQueue<Message> messagesChanel", messagesChanel));
        this.messagesChanel = messagesChanel;
        this.messagesChanelSet = true;
    }

    @SafeVarargs
    private final void checkNull(Pair<String, Object>... pairs) {
        for (Pair<String, Object> p : pairs) {
            if (p.getValue() == null) {
                throw new IllegalArgumentException("Expected " + p.getKey() + ", but found null");
            }
        }
    }

    @Override
    public void run() {
        try {
            walkDirs();
            Map<String, Pair<Path, Path>> correlationsMap = checkForCorrelations();
            ConcurrentMap<String, CopyValues> correlationResolutions = solveCorrelations(correlationsMap);
            copyFiles(correlationResolutions);
        } catch (BreakException ignore) {}
        sendMessage(new Message(MessageType.COPY_COMPLETED, null));
    }

    private ConcurrentMap<String, CopyValues> solveCorrelations(Map<String, Pair<Path, Path>> correlationsMap) {
        ConcurrentMap<String, CopyValues> result;
        if (!correlationsMap.isEmpty()) {
            if (messagesChanelSet) {
                sendMessage(new Message(MessageType.CORRELATIONS_MAP, correlationsMap));
                result = (ConcurrentMap<String, CopyValues>) getMessage(MessageType.CORRELATION_RESOLUTIONS);
            } else {
                result = new ConcurrentHashMap<>();
                correlationsMap.forEach((k, v) -> result.put(k, DEFAULT_MODE));
            }
        } else {
            result = new ConcurrentHashMap<>();
        }
        // Hint for avoid yellow blocks of code
        assert result != null;
        result.forEach((k, v) -> {
            try {
                if (v == CopyValues.IGNORE_MODE) {
                    totalCount.getAndAdd(-Files.size(Paths.get(path1.toAbsolutePath() + k)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    private Object getMessage(MessageType messageType) {
        while (!isInterrupted) {
            isInterrupted |= Thread.interrupted();
            // I don't like it at all, but in current situation another solutions will cost us much more time
            // and we will not use the received functionality in anyway.
            while (!messagesChanel.getValue().isEmpty()) {
                Message tmp = messagesChanel.getValue().poll();
                if (tmp.getMessageType() == messageType) {
                    return tmp.getValue();
                }
            }
            Thread.yield();
        }
        return null;
    }

    private void copyFiles(ConcurrentMap<String, CopyValues> correlationResolutions) {
        Map<String, Map<String, Path>> pathsMap = new HashMap<>();
        ArrayList<Thread> threads = new ArrayList<>();
        final int[] cnt = {0};
        from.forEach((k, v) -> {
            if (v.size() > MIN_FILES_COUNT) {
                // TODO here we need some additional thread generation for large folders
                Map<String, Map<String, Path>> tmp = new HashMap<>();
                tmp.put(k, v);
                threads.add(copyingThread(tmp, correlationResolutions));
            } else {
                pathsMap.put(k, v);
                cnt[0] += v.size();
                if (cnt[0] > MIN_FILES_COUNT) {
                    threads.add(copyingThread(new HashMap<>(pathsMap), correlationResolutions));
                    pathsMap.clear();
                    cnt[0] = 0;
                }
            }
        });
        if (!pathsMap.isEmpty()) {
            threads.add(copyingThread(pathsMap, correlationResolutions));
        }
        boolean alive = true;
        while (alive) {
            alive = false;
            isInterrupted |= Thread.interrupted();
            if (isInterrupted) {
                threads.forEach(Thread::interrupt);
            }
            for (Thread t : threads) {
                alive |= t.isAlive();
            }
            Thread.yield();
        }
        sendMessage(new Message(MessageType.SCANNING_COMPLETED, null));
        checkInterrupted();
    }

    private void sendMessage(Message message) {
        if (messagesChanelSet) {
            messagesChanel.getValue().add(message);
        }
    }

    private void checkInterrupted() {
        if (isInterrupted) {
            throw new BreakException();
        }
    }

    private Thread copyingThread(Map<String, Map<String, Path>> pathsMap, ConcurrentMap<String, CopyValues> correlationResolutions) {
        CopyingThread ct = new CopyingThread(pathsMap, path2, correlationResolutions);
        ct.setAccumulator(currentCount);
        ct.setMessagesLog(messagesLog);
        Thread t = new Thread(ct);
        t.start();
        return t;
    }

    private void walkDirs() {
        Thread t1 = new Thread(() -> {
            if (countingMode) {
                (new ScanDirectory(path1, "", totalCount, from)).scan();
            } else {
                (new ScanDirectory(path1, "", from)).scan();
            }
        });
        t1.start();
        Thread t2 = new Thread(() -> {
            (new ScanDirectory(path2, "", to)).scan();
        });
        t2.start();
        boolean alive = true;
        while (alive) {
            isInterrupted |= Thread.interrupted();
            if (isInterrupted) {
                t1.interrupt();
                t2.interrupt();
            }
            alive = t1.isAlive() || t2.isAlive();
            Thread.yield();
        }
    }

    private Map<String, Pair<Path, Path>> checkForCorrelations() {
        final ConcurrentMap<String, Map<String, Path>> map1;
        final ConcurrentMap<String, Map<String, Path>> map2;
        Map<String, Pair<Path, Path>> result = new HashMap<>();
        if (from.size() > to.size()) {
            map1 = to;
            map2 = from;
        } else {
            map1 = from;
            map2 = to;
        }
        map1.forEach((k, v) -> {
            if (map2.containsKey(k)) {
                map1.get(k).forEach((internalKey, internalValue) -> {
                    if (map2.get(k).containsKey(internalKey)) {
                        result.put(k + File.separator + internalKey, new Pair<>(internalValue, map2.get(k).get(internalKey)));
                    }
                });
            }
        });
        checkInterrupted();
        return result;
    }
}
