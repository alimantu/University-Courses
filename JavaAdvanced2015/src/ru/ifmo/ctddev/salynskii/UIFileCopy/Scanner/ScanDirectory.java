package ru.ifmo.ctddev.salynskii.UIFileCopy.Scanner;

import com.sun.source.util.TreeScanner;
import com.sun.xml.internal.bind.unmarshaller.InfosetScanner;
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 03/03/16.
 */
public class ScanDirectory implements Runnable{
    //
    private final Path path;
    private ConcurrentMap<String, Map<String, Path>> pathMap;
//    private ConcurrentSkipListSet<String> dirsSet;
    //
//    private final File file;
    private final String prefix;
//    private ConcurrentMap<String, Map<String, File>> filesMap;
//    private ConcurrentSkipListSet<String> dirsSet;
    private AtomicLong counter;
    private final boolean accumulatorUsage;

//    public ScanDirectory(File file, String prefix, AtomicLong counter,
//                        ConcurrentMap<String, Map<String, File>> filesMap, ConcurrentSkipListSet<String> dirsSet) {
    public ScanDirectory(Path path, String prefix, AtomicLong counter,
                         ConcurrentMap<String, Map<String, Path>> pathMap) {
        //
        this.path = path;
        this.pathMap = pathMap;
        //
//        this.filesMap = filesMap;
//        this.dirsSet = dirsSet;
//        this.file = file;
        this.prefix = prefix;
        this.counter = counter;
        this.accumulatorUsage = true;
    }

//    public ScanDirectory(File file, String prefix,
//                         ConcurrentMap<String, Map<String, File>> filesMap, ConcurrentSkipListSet<String> dirsSet) {
    public ScanDirectory(Path path, String prefix,
                     ConcurrentMap<String, Map<String, Path>> pathMap) {
        //
        this.path = path;
        this.pathMap = pathMap;
        //
//        this.filesMap = filesMap;
//        this.dirsSet = dirsSet;
//        this.file = file;
        this.prefix = prefix;
        this.accumulatorUsage = false;
    }

//    private ScanDirectory(File file, String prefix, AtomicLong counter, boolean accumulatorUsage,
//                          ConcurrentMap<String, Map<String, File>> filesMap, ConcurrentSkipListSet<String> dirsSet) {
    private ScanDirectory(Path path, String prefix, AtomicLong counter, boolean accumulatorUsage,
                      ConcurrentMap<String, Map<String, Path>> pathMap) {
        //
        this.path = path;
        this.pathMap = pathMap;
        //
//        this.filesMap = filesMap;
//        this.dirsSet = dirsSet;
//        this.file = file;
        this.prefix = prefix;
        this.counter = counter;
        this.accumulatorUsage = accumulatorUsage;
    }

    public void scan() {
//        if (file.isDirectory()) {
        if (Files.isDirectory(path)) {
//            filesMap.put(prefix, new HashMap<>());
            pathMap.put(prefix, new HashMap<>());
            scanDiectory();
        } else {
//            throw new IllegalArgumentException("Expected directory name, but found file name " + file.getAbsolutePath());
            throw new IllegalArgumentException("Expected directory name, but found file name " + path.toAbsolutePath());
        }
    }

//    private void putFile(String key, File value) {
    private void putPath(String key, Path value) {
//        if (value.isFile()) {
        if (Files.isRegularFile(value)) {
//            filesMap.get(prefix).put(key, value);
            pathMap.get(prefix).put(key, value);
            if (accumulatorUsage) {
//                counter.getAndAdd(value.length());
                try {
                    counter.getAndAdd(Files.size(value));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
//            dirsSet.add(key);
//            filesMap.put(prefix + '/' + key, new HashMap<String, File>());
            pathMap.put(prefix + '/' + key, new HashMap<String, Path>());
        }
    }

    private void scanDiectory() {
        ArrayList<Thread> threads = new ArrayList<>();
//        try {
//            Files.walk(Paths.get(file.getAbsolutePath())/*.toPath()*/, 1).forEach(innerFile -> {
//                if (!innerFile.equals(file.toPath())) {
//                    putFile(innerFile.getName(innerFile.getNameCount() - 1).toString(), innerFile.toFile());
//                    if (Files.isDirectory(innerFile)) {
//                        Thread t = new Thread(new ScanDirectory(innerFile.toFile(),
//                                prefix + '/' + innerFile.getName(innerFile.getNameCount() - 1).toString(),
//                                counter, accumulatorUsage, filesMap, dirsSet));
//                        threads.add(t);
//                        t.start();
//                    }
//                }
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
            Files.walk(path, 1).forEach(innerPath -> {
                if (!innerPath.equals(path)) {
                    putPath(innerPath.getName(innerPath.getNameCount() - 1).toString(), innerPath);
                    if (Files.isDirectory(innerPath)) {
                        Thread t = new Thread(new ScanDirectory(innerPath,
                                prefix + '/' + innerPath.getName(innerPath.getNameCount() - 1).toString(),
                                counter, accumulatorUsage, pathMap));
                        threads.add(t);
                        t.start();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Bad Idea!
//        if (file.length() == 0) {
//            return;
//        }
//        for (File inerFile : file.listFiles()) {
////            putFile(prefix + '/' + inerFile.getName(), inerFile);
//            System.err.println(inerFile.getAbsolutePath());
//            putFile(inerFile.getName(), inerFile);
//            if (inerFile.isDirectory()) {
//                Thread t = new Thread(new ScanDirectory(inerFile, prefix + '/' + inerFile.getName(),
//                        counter, accumulatorUsage, filesMap, dirsSet));
//                threads.add(t);
//                t.start();
//            }
//        }
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
