package multiThreadedWebCrawler;

/* General Imports */
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

/* Multi-Threading Imports */
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/* Jsoup Webpage Interface Imports */
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WebCrawler {
    
    /* Initial Url Crawl */
    public List<String> crawl(String startUrl, int maxDepth) {
        int currentDepth = 0;
        Set<String> visited = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newCachedThreadPool();
        visited.add(startUrl);
        crawl(visited, startUrl, executor, currentDepth, maxDepth);
        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new ArrayList<>(visited);
    }

    /* Recursive Crawl Function */
    private void crawl(Set<String> visited, String start, ExecutorService executor, int currentDepth, int maxDepth) {
        if(currentDepth < maxDepth) {
            List<Future<?>> futures = new ArrayList<>();
            long id = Thread.currentThread().threadId();
            Document doc = request(start, id);

            if(doc != null) {
                for (Element link : doc.select("a[href]")) {
                    String url = link.absUrl("href");
                    if(visited.add(url)) {
                        int nextDepth = currentDepth + 1;
                        Future<?> future = executor.submit(() -> crawl(visited, url, executor, nextDepth, maxDepth));
                        futures.add(future);
                    }
                }
                for (Future<?> f : futures) {
                    try {
                        f.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /* Webpage Retrival */
    private Document request(String url, long id) {
        try {
            Connection con = Jsoup.connect(url);
            Document doc = con.get();

            if(con.response().statusCode() == 200) {
                System.out.println("\n**Bot: " + id + ", Received Webpage at: " + url);

                String title = doc.title();
                System.out.println("Title: " + title);

                return doc;
            }

            return null;
        }
        catch(IOException e) {
            return null;
        }
    }
    
}