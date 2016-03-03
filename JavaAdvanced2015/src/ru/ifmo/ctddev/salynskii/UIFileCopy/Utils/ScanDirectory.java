package ru.ifmo.ctddev.salynskii.UIFileCopy.Utils;

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
    private AtomicLong counter;
    private final boolean accumulatorUsage;

    public ScanDirectory(Path path, String prefix, AtomicLong counter,
                         ConcurrentMap<String, Map<String, Path>> pathMap) {
        this.path = path;
        this.pathMap = pathMap;
        this.prefix = prefix;
        this.counter = counter;
        this.accumulatorUsage = true;
    }

    public ScanDirectory(Path path, String prefix,
                     ConcurrentMap<String, Map<String, Path>> pathMap) {
        this.path = path;
        this.pathMap = pathMap;
        this.prefix = prefix;
        this.accumulatorUsage = false;
    }

    private ScanDirectory(Path path, String prefix, AtomicLong counter, boolean accumulatorUsage,
                      ConcurrentMap<String, Map<String, Path>> pathMap) {
        this.path = path;
        this.pathMap = pathMap;
        this.prefix = prefix;
        this.counter = counter;
        this.accumulatorUsage = accumulatorUsage;
    }

    public void scan() {
        if (Files.isDirectory(path)) {
            pathMap.put(prefix, new HashMap<>());
            scanDiectory();
        } else {
            throw new IllegalArgumentException("Expected directory name, but found file name " + path.toAbsolutePath());
        }
    }

    private void putPath(String key, Path value) {
        if (Files.isRegularFile(value)) {
            pathMap.get(prefix).put(key, value);
            if (accumulatorUsage) {
                try {
                    counter.getAndAdd(Files.size(value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            pathMap.put(prefix + File.separator + key, new HashMap<>());
        }
    }

    private void scanDiectory() {
        ArrayList<Thread> threads = new ArrayList<>();

        try {
            Files.walk(path, 1).forEach(innerPath -> {
                if (!innerPath.equals(path)) {
                    putPath(innerPath.getName(innerPath.getNameCount() - 1).toString(), innerPath);
                    if (Files.isDirectory(innerPath)) {
                        Thread t = new Thread(new ScanDirectory(innerPath,
                                prefix + File.separator + innerPath.getName(innerPath.getNameCount() - 1).toString(),
                                counter, accumulatorUsage, pathMap));
                        threads.add(t);
                        t.start();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        scanDiectory();
    }
}
