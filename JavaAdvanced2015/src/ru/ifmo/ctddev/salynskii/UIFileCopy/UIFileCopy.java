package ru.ifmo.ctddev.salynskii.UIFileCopy;

import ru.ifmo.ctddev.salynskii.UIFileCopy.Scanner.ScanDirectory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Alimantu on 03/03/16.
 */
public class UIFileCopy {

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected input format: <path of the copied file/dir> <path to the copy dir>");
        }
//        File file1 = new File(args[0]);
//        File file2 = new File(args[1]);
//        if (!file2.isDirectory()) {
//            throw new IllegalArgumentException("Expected directory name as second argument, but found file name - " + args[1]);
//        }
        Path path1 = Paths.get(args[0]);
        Path path2 = Paths.get(args[1]);
        if (Files.isRegularFile(path2)) {
            throw new IllegalArgumentException("Expected directory name as second argument, but found file name - " + args[1]);
        }
        AtomicLong bytesCount = new AtomicLong(0);
        ConcurrentMap<String, Map<String, Path>> from = new ConcurrentHashMap<>();
        ConcurrentMap<String, Map<String, Path>> to = new ConcurrentHashMap<>();
//        ConcurrentMap<String, Map<String, File>> from = new ConcurrentHashMap<>();
//        ConcurrentMap<String, Map<String, File>> to = new ConcurrentHashMap<>();
//        ConcurrentSkipListSet<String> fromDirs = new ConcurrentSkipListSet<>();
//        ConcurrentSkipListSet<String> toDirs = new ConcurrentSkipListSet<>();
        Thread t1 = new Thread(() -> {
//            (new ScanDirectory(file1, "", bytesCount, from, fromDirs)).scan();
            (new ScanDirectory(path1, "", bytesCount, from)).scan();
        });
        t1.start();
        Thread t2 = new Thread(() -> {
//            (new ScanDirectory(file2, "", to, toDirs)).scan();
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



        // TODO comment this debug messages
        printInfo(from, to, file1, file2, bytesCount, toDirs, fromDirs);
        //
    }

    private static void printInfo(ConcurrentMap<String, Map<String, File>> from, ConcurrentMap<String, Map<String, File>> to,
                                  File file1, File file2, AtomicLong bytesCount, ConcurrentSkipListSet<String> toDirs,
                                  ConcurrentSkipListSet<String> fromDirs) {
        System.out.println("File name: " + file1.getName());
        System.out.println("Absolute path: " + file1.getAbsolutePath());
        System.out.println("Parent name: " + file1.getParent());
        System.out.println("Size: " + bytesCount.get());
        System.out.println("------------------------------------------");
//        System.out.println("Files :");
//        for (Map.Entry<String, Map<String, File>> e : from.entrySet()) {
//            System.out.println(e.getKey());
//        }
        from.forEach((k, v) -> {
            System.out.println("Dir " + k + " contains files:");
//            v.forEach((k2, v2) -> System.out.println(k2));
            System.out.println(v.size());
//            System.out.println(v.isEmpty());
//            for (String s : v.keySet()) {
//                System.out.println(s);
//            }
        });
//        System.out.println("Dirs :");
//        fromDirs.forEach(System.out::println);
        System.out.println("==========================================");
        System.out.println("File name: " + file2.getName());
        System.out.println("Absolute path: " + file2.getAbsolutePath());
        System.out.println("Parent name: " + file2.getParent());
        System.out.println("------------------------------------------");
//        System.out.println("Files :");
//        for (Map.Entry<String, Map<String, File>> e : to.entrySet()) {
//            System.out.println(e.getKey());
//        }
//        System.out.println("Dirs :");
//        toDirs.forEach(System.out::println);
//        toDirs.forEach(s -> {
//            System.out.println("Dir " + s + " contains files:");
//            to.get(s).forEach((k, v) -> System.out.println(k));
//        });
        to.forEach((k, v) -> {
            System.out.println("Dir " + k + " contains files:");
//            v.forEach((k2, v2) -> System.out.println(k2));
            System.out.println(v.size());
//            System.out.println(v.isEmpty());
        });
    }
}
