package ru.ifmo.ctddev.salynskii.UIFileCopy.Utils;

import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 03/03/16.
 */
public class CopyingThread implements Runnable {
    private final Map<String, Map<String, Path>> pathsMap;
    private final Path destination;
    private final ConcurrentMap<String, CopyValues> correlationsResolutions;
    private ConcurrentLinkedQueue<String> messagesLog;
    private boolean messagesLogSet = false;
    private AtomicLong accumulator;
    private boolean accumulatorSet = false;


    public CopyingThread(Map<String, Map<String, Path>> pathsMap, Path destination,
                         ConcurrentMap<String, CopyValues> correlationsResolutions) {
        checkNull(new Pair<>("Map<String, Map<String, Path>> pathsMap", pathsMap),
                new Pair<>("Path destination", destination),
                new Pair<>("ConcurrentMap<String, CopyValues> correlationsResolutions", correlationsResolutions)
                );
        this.pathsMap = pathsMap;
        this.destination = destination;
        this.correlationsResolutions = correlationsResolutions;
    }

    public void setAccumulator(AtomicLong accumulator) {
        checkNull(new Pair<>("AtomicLong accumulator", accumulator));
        this.accumulatorSet = true;
        this.accumulator = accumulator;
    }

    public void setMessagesLog(ConcurrentLinkedQueue<String> messagesLog) {
        checkNull(new Pair<>("ConcurrentLinkedQueue<String> messagesLog", messagesLog));
        this.messagesLogSet = true;
        this.messagesLog = messagesLog;
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
            pathsMap.forEach((k, v) -> {
                if (Thread.interrupted()) {
                    throw new BreakException();
                }
                new File(destination + k).mkdirs();
                v.forEach((innerKey, innerValue) -> {
                    if (Thread.interrupted()) {
                        throw new BreakException();
                    }
                    Path tmpPath = Paths.get(destination.toAbsolutePath() + k + File.separator + innerKey);
                    String completeKey = k + File.separator + innerKey;
                    switch (correlationsResolutions.getOrDefault(completeKey, CopyValues.REPLACE_MODE)) {
                        case IGNORE_MODE:
                            break;
                        case COPYING_WITH_MARKER:
                            tmpPath = getNewName(destination, k, innerKey);
                        case REPLACE_MODE:
                            copyFile(innerValue.toAbsolutePath(), tmpPath);
                    }
                });
            });
        } catch (BreakException ignore) {}
    }

    private void copyFile(Path path, Path tmpPath) {
        try{
            Files.copy(path, tmpPath, StandardCopyOption.REPLACE_EXISTING);
            if (accumulatorSet) {
                    accumulator.getAndAdd(Files.size(path));
            }
            addLogMessage(path.toString() + "\n    copied to " + tmpPath);
        } catch (IOException e) {
            addLogMessage("Troubles with copying " + path.toString() + " file!");
            e.printStackTrace();
        }
    }

    private void addLogMessage(String message) {
        if (messagesLogSet) {
            messagesLog.add(message);
        }
    }

    private Path getNewName(Path destination, String k, String innerKey) {
        Path newPath = Paths.get(destination.toAbsolutePath() + k + File.separator + innerKey);
        String name = newPath.getName(newPath.getNameCount() - 1).toString();
        int extIndex = name.lastIndexOf('.') == -1 ? name.length() : name.lastIndexOf('.');
        String extension = name.substring(extIndex);
        name = name.substring(0, extIndex);
        int count = 1;
        if (name.endsWith(")") && name.contains("(")) {
            try {
                count += Integer.parseInt(name.substring(name.lastIndexOf("(") + 1, name.lastIndexOf(")")));
                name = name.substring(0, name.lastIndexOf("(") > 0 ? name.lastIndexOf("(") - 1 : 0);
            } catch (NumberFormatException ignore) {}
        }
        do {
            newPath = Paths.get(newPath.getParent().toAbsolutePath() +
                    File.separator + (name.equals("") ? "" : name + " ")
                    + "(" + count++ + ")" + extension);
        } while (Files.exists(newPath));
        return newPath;
    }
}
