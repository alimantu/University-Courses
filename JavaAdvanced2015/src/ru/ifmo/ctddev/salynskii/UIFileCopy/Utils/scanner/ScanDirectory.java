package ru.ifmo.ctddev.salynskii.UIFileCopy.utils.scanner;

import ru.ifmo.ctddev.salynskii.UIFileCopy.utils.exception.BreakException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 03/03/16.
 */
public class ScanDirectory implements Runnable{
    private final Path path;
    private ConcurrentMap<String, Map<String, Path>> pathMap;
    private final String prefix;
    private AtomicLong accumulator;
    private final boolean accumulatorUsage;

    public ScanDirectory(Path path, String prefix, AtomicLong accumulator,
                         ConcurrentMap<String, Map<String, Path>> pathMap) {
        this.path = path;
        this.pathMap = pathMap;
        this.prefix = prefix;
        this.accumulator = accumulator;
        this.accumulatorUsage = true;
    }

    public ScanDirectory(Path path, String prefix,
                     ConcurrentMap<String, Map<String, Path>> pathMap) {
        this.path = path;
        this.pathMap = pathMap;
        this.prefix = prefix;
        this.accumulatorUsage = false;
    }

    private ScanDirectory(Path path, String prefix, AtomicLong accumulator, boolean accumulatorUsage,
                          ConcurrentMap<String, Map<String, Path>> pathMap) {
        this.path = path;
        this.pathMap = pathMap;
        this.prefix = prefix;
        this.accumulator = accumulator;
        this.accumulatorUsage = accumulatorUsage;
    }

    public void scan() {
        if (Files.isDirectory(path)) {
            pathMap.put(prefix, new HashMap<>());
            scanDirectory();
        } else {
            throw new IllegalArgumentException("Expected directory name, but found file name " + path.toAbsolutePath());
        }
    }

    private void putPath(String key, Path value) {
        if (Files.isRegularFile(value)) {
            pathMap.get(prefix).put(key, value);
            if (accumulatorUsage) {
                try {
                    accumulator.getAndAdd(Files.size(value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            pathMap.put(prefix + File.separator + key, new HashMap<>());
        }
    }

    private void scanDirectory() {
        ArrayList<Thread> threads = new ArrayList<>();
        try {
            Files.walk(path, 1).forEach(innerPath -> {
                if (Thread.interrupted()) {
                    threads.forEach(Thread::interrupt);
                    throw new BreakException();
                }
                if (!innerPath.equals(path) && !isSystemFile(innerPath)) {
                    putPath(innerPath.getName(innerPath.getNameCount() - 1).toString(), innerPath);
                    if (Files.isDirectory(innerPath)) {
                        Thread t = new Thread(new ScanDirectory(innerPath,
                                prefix + File.separator + innerPath.getName(innerPath.getNameCount() - 1).toString(),
                                accumulator, accumulatorUsage, pathMap));
                        threads.add(t);
                        t.start();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BreakException ignore) {}

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isSystemFile(Path tmpPath) {
        return tmpPath.toString().endsWith(".DS_Store");
    }

    @Override
    public void run() {
        scanDirectory();
    }
}
