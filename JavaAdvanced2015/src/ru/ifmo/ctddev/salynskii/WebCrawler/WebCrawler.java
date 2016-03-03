package ru.ifmo.ctddev.salynskii.WebCrawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * This implementation of the {@link Crawler} imterface should be used for recursive crawling of the links
 * on the specified web-page.
 * @author Alimantu
 */
public class WebCrawler implements Crawler {

    private final static int DEFAULT_DEPTH = 1;
    private final static int TIME_AWAIT = 1000;
    private final static int MAX_THREADS = Integer.MAX_VALUE / 2;
    private final Downloader downloader;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final Semaphore sem;
    private final int semSize;


    /**
     * Constructor receives the initial parameters such as specified web-page downloader, max number
     * of downloaders to use and max number of threads could be used for links extraction simultaneously.
     * @param downloader page downloader should be used for downloading the web-pages
     * @param downloaders max threads could be used for downloading web-pages
     * @param extractors max threads could be used for extraction links from downloaded web-pages
     * @param perHost max pages could be downloaded from one host simultaneously
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        int downCnt = downloaders > MAX_THREADS ? MAX_THREADS : downloaders;
        int extrCnt = extractors > MAX_THREADS ? MAX_THREADS : extractors;
        this.downloadersPool = Executors.newFixedThreadPool(downCnt);
        this.extractorsPool = Executors.newFixedThreadPool(extrCnt);
        this.semSize = downCnt + extrCnt;
        this.sem = new Semaphore(semSize);
//        System.out.println("Semaphore size: " + semSize);
//        System.out.println("Downloaders count: " + downCnt);
//        System.out.println("Extractors cout: " + extrCnt);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Expected arguments format: url [downloads [extractors [perHost]]]");
        }
        String[] init = {"", "10", "10", "10"};
        System.arraycopy(args, 0, init, 0, args.length);
        WebCrawler wc = new WebCrawler(new CachingDownloader(),
                Integer.parseInt(init[1]), Integer.parseInt(init[2]), Integer.parseInt(init[2]));
        Result r = wc.download(args[0], DEFAULT_DEPTH);
        //
        for (String s : r.getDownloaded()) {
            System.out.println(s);
        }
        System.out.println("----------------------------------------");
        for (String s : r.getErrors().keySet()) {
            System.out.println(s);
        }
        //
//        wc.close();
    }

    /**
     * Recursively extracts all the links from 0 to <code>depth</code> level of the specified web-site.
     * So 1 means to use only current web-page, 2 - current and all children of it etc.
     * @param url address of the start web-page
     * @param depth max depth of the recursive extraction
     * @return data structure contains list of the successfully extracted pages and map with
     * problem pages url's - received exception as key - value.
     */
    @Override
    public Result download(String url, int depth) {
        ConcurrentSkipListSet<String> urls = new ConcurrentSkipListSet<>();
        ConcurrentMap<String, IOException> badUrls = new ConcurrentHashMap<>();
        downloadersPool.submit(() -> downloadHandler(url, depth, urls, badUrls));
        try {
            downloadersPool.awaitTermination(TIME_AWAIT, TimeUnit.MILLISECONDS);
            sem.acquire(semSize);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        close();
        return new Result(urls.stream().collect(Collectors.toList()), badUrls);
    }

    /**
     * Downloads the web-page using the specified url, writes the successfully read pages to the <code>url</code>
     * set and to the <code>badUrls</> map otherwise.
     * @param url url of the page should be downloaded
     * @param depth max depth of the current recursive extraction
     * @param urls set with the previously successfully read pages
     * @param badUrls map with problem pages
     */
    private void downloadHandler(String url, int depth, ConcurrentSkipListSet<String> urls,
                                 ConcurrentMap<String, IOException> badUrls) {
        try {
            sem.acquire();
            if (!urls.contains(url) && !badUrls.containsKey(url)) {
                urls.add(url);
                Document doc = downloader.download(url);
                extractorsPool.submit(() -> {
                    try {
                        extractorHandler(doc, depth, urls, badUrls);
                    } catch (IOException e) {
                        urls.remove(url);
                        badUrls.put(url, e);
                    }
                });
            }
        } catch (IOException e) {
            urls.remove(url);
            badUrls.put(url, e);
        } catch (InterruptedException ignore) {
        } finally {
            sem.release();
        }
    }

    /**
     * Uses for extraction all the links from the specified document.
     * @param doc document for the link extraction
     * @param depth max depth of the current recursive extraction
     * @param urls set with the previously successfully read pages
     * @param badUrls map with problem pages
     * @throws IOException
     */
    private void extractorHandler(Document doc, int depth, ConcurrentSkipListSet<String> urls,
                                  ConcurrentMap<String, IOException> badUrls) throws IOException {
        try {
            sem.acquire();
            List<String> links = doc.extractLinks();
            for (String link : links) {
                if (depth > 1 && !urls.contains(link) && !badUrls.containsKey(link)) {
                    downloadersPool.submit(() -> downloadHandler(link, depth - 1, urls, badUrls));
                }
            }
        } catch (InterruptedException ignore) {
        } finally {
            sem.release();
        }
    }

    /**
     * Kill all the downloader and extraction threads.
     */
    @Override
    public void close() {
        downloadersPool.shutdownNow();
        extractorsPool.shutdownNow();
    }
}
