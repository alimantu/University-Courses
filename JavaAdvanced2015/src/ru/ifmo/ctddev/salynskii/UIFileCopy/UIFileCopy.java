package ru.ifmo.ctddev.salynskii.UIFileCopy;

import javafx.util.Pair;
import ru.ifmo.ctddev.salynskii.UIFileCopy.Utils.CopyValues;
import ru.ifmo.ctddev.salynskii.UIFileCopy.Utils.CopyingThread;
import ru.ifmo.ctddev.salynskii.UIFileCopy.Utils.ScanDirectory;

import java.io.File;
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
 * Created by Alimantu on 03/03/16.
 */
public class UIFileCopy {
    private static final int MIN_FILES_COUNT = 30;
    private static final CopyValues DEFAULT_MODE = CopyValues.COPYING_WITH_MARKER;

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected input format: <path of the copied file/dir> <path to the copy dir>");
        }
        Path path1 = Paths.get(args[0]);
        Path path2 = Paths.get(args[1]);
        if (Files.isRegularFile(path2)) {
            throw new IllegalArgumentException("Expected directory name as second argument, but found file name - " + args[1]);
        }
        AtomicLong bytesCount = new AtomicLong(0);
        ConcurrentMap<String, Map<String, Path>> from = new ConcurrentHashMap<>();
        ConcurrentMap<String, Map<String, Path>> to = new ConcurrentHashMap<>();
        walkDirs(path1, from, path2, to, bytesCount);

        Map<String, Pair<Path, Path>> correlationsMap = checkForCorrelations(from, to);
        // TODO Hey friend, here is some problems with this files, what I need to do with them?
        correlationsMap.forEach((k, v) -> {
            System.out.println("Key used: " + k + "\nFiles found in directories:\nFrom dir: "
                    + v.getKey() + "\nTo dir: " + v.getValue());
            System.out.println("-----------------------------------");
        });

        ConcurrentMap<String, CopyValues> correlationResolutions = new ConcurrentHashMap<>();
        correlationsMap.forEach((k, v) -> correlationResolutions.put(k, DEFAULT_MODE));
        //

        ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

        AtomicLong copied = new AtomicLong(0);
        copyFiles(path1, from, path2, correlationResolutions, copied, messages);


        // TODO comment this debug messages
        printInfo(from, to, path1, path2, bytesCount);
        //
    }

    private static void copyFiles(Path sourceDir, ConcurrentMap<String, Map<String, Path>> from, Path destination,
                                  ConcurrentMap<String, CopyValues> correlationsResolutions, AtomicLong counter,
                                  ConcurrentLinkedQueue<String> messages) {
        Map<String, Map<String, Path>> pathsMap = new HashMap<>();
        ArrayList<Thread> threads = new ArrayList<>();
        from.forEach((k, v) -> {
            if (v.size() > MIN_FILES_COUNT) {
                Map<String, Map<String, Path>> tmp = new HashMap<>();
                tmp.put(k, v);
                threads.add(copyingThread(sourceDir, tmp, destination, correlationsResolutions, counter, messages));
            } else {
                pathsMap.put(k, v);
                if (pathsMap.size() > MIN_FILES_COUNT) {
                    threads.add(copyingThread(sourceDir, new HashMap<>(pathsMap), destination, correlationsResolutions, counter, messages));
                    pathsMap.clear();
                }
            }
        });
        if (!pathsMap.isEmpty()) {
            threads.add(copyingThread(sourceDir, pathsMap, destination, correlationsResolutions, counter, messages));
        }

        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private static Thread copyingThread(Path sourceDir, Map<String, Map<String, Path>> pathsMap, Path destination,
                                        ConcurrentMap<String, CopyValues> correlationsResolutions, AtomicLong counter,
                                        ConcurrentLinkedQueue<String> messages) {
        Thread t = new Thread(new CopyingThread(sourceDir, pathsMap, destination, correlationsResolutions, counter, messages));
        t.start();
        return t;
    }

    private static void walkDirs(Path path1, ConcurrentMap<String, Map<String, Path>> from,
                                 Path path2, ConcurrentMap<String, Map<String, Path>> to, AtomicLong bytesCount) {
        Thread t1 = new Thread(() -> {
            (new ScanDirectory(path1, "", bytesCount, from)).scan();
        });
        t1.start();
        Thread t2 = new Thread(() -> {
            (new ScanDirectory(path2, "", to)).scan();
        });
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.err.println("Unexpected main thread interruption!");
//            e.printStackTrace();
        }
    }

    private static Map<String, Pair<Path, Path>> checkForCorrelations(
            ConcurrentMap<String, Map<String, Path>> initMap1, ConcurrentMap<String, Map<String, Path>> initMap2) {

        final ConcurrentMap<String, Map<String, Path>> map1;
        final ConcurrentMap<String, Map<String, Path>> map2;
        Map<String, Pair<Path, Path>> result = new HashMap<>();
        if (initMap1.size() > initMap2.size()) {
            map1 = initMap2;
            map2 = initMap1;
        } else {
            map1 = initMap1;
            map2 = initMap2;
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
        return result;
    }

    private static void printInfo(ConcurrentMap<String, Map<String, Path>> from,
                                  ConcurrentMap<String, Map<String, Path>> to,
                                  Path path1, Path path2, AtomicLong bytesCount) {

        System.out.println("File name: " + path1.getName(path1.getNameCount() - 1));
        System.out.println("Absolute path: " + path1.toAbsolutePath());
        System.out.println("Parent name: " + path1.getParent());
        System.out.println("Size: " + bytesCount.get());
        System.out.println("------------------------------------------");
        from.forEach((k, v) -> {
            System.out.println("Dir " + k + " contains files:");
            System.out.println(v.size());
        });
        System.out.println("==========================================");
        System.out.println("File name: " + path2.getName(path2.getNameCount() - 1));
        System.out.println("Absolute path: " + path2.toAbsolutePath());
        System.out.println("Parent name: " + path2.getParent());
        System.out.println("------------------------------------------");
        to.forEach((k, v) -> {
            System.out.println("Dir " + k + " contains files:");
            System.out.println(v.size());
        });
    }
}
