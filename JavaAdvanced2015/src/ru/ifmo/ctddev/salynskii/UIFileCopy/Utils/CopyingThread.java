package ru.ifmo.ctddev.salynskii.UIFileCopy.Utils;

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
    private final ConcurrentLinkedQueue<String> messages;
    AtomicLong counter;

    public CopyingThread(Map<String, Map<String, Path>> pathsMap, Path destination,
                         ConcurrentMap<String, CopyValues> correlationsResolutions, AtomicLong counter,
                         ConcurrentLinkedQueue<String> messages) {
        this.pathsMap = pathsMap;
        this.destination = destination;
        this.correlationsResolutions = correlationsResolutions;
        this.messages = messages;
        this.counter = counter;
    }

    @Override
    public void run() {
        pathsMap.forEach((k, v) -> {
            new File(destination + k).mkdirs();
            System.err.println("Dir: " + destination + k);
            v.forEach((innerKey, innerValue) -> {
                Path tmpPath = Paths.get(destination.toAbsolutePath() + k + File.separator + innerKey);
                String completeKey = k + File.separator + innerKey;
                switch (correlationsResolutions.getOrDefault(completeKey, CopyValues.REPLACE_MODE)) {
                        case IGNORE_MODE:
                            break;
                        case COPYING_WITH_MARKER:
                            System.err.println("Here we are!");
                            tmpPath = getNewName(destination, k, innerKey);
                            System.err.println(tmpPath);
                        case REPLACE_MODE:
                            try {
                                Files.copy(innerValue.toAbsolutePath(), tmpPath, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                }
                updateStats(innerValue);
//                if (correlationsResolutions.containsKey(completeKey)) {
//                    switch (correlationsResolutions.get(completeKey)) {
//                        case IGNORE_MODE:
//                            break;
//                        case COPYING_WITH_MARKER:
//                            System.err.println("Here we are!");
//                            tmpPath = getNewName(destination, k, innerKey);
//                            System.err.println(tmpPath);
//                        case REPLACE_MODE:
//                            copyFile(innerValue.toAbsolutePath(), tmpPath);
////                            try {
////                                Files.copy(innerValue.toAbsolutePath(), tmpPath, StandardCopyOption.REPLACE_EXISTING);
////                            } catch (IOException e) {
////                                e.printStackTrace();
////                            }
//                    }
//                } else {
//                    copyFile(innerValue.toAbsolutePath(), tmpPath);
////                    try {
////                        Files.copy(innerValue.toAbsolutePath(), tmpPath, StandardCopyOption.REPLACE_EXISTING);
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
//                }
            });
        });
    }

    private void updateStats(Path path) {
        messages.add("File " + path.toAbsolutePath() + " successfully copied.");
        try {
            counter.getAndAdd(Files.size(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Path getNewName(Path destination, String k, String innerKey) {
        Path newPath = Paths.get(destination.toAbsolutePath() + k + File.separator + innerKey);
        do {
            newPath = Paths.get(newPath.getParent().toAbsolutePath() +
                    File.separator + "new_" + newPath.getName(newPath.getNameCount() - 1));
        } while (Files.exists(newPath));
        return newPath;
    }
}
