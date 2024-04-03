package multiThreadedWebCrawler;

import java.util.List;

public class App {

    public static void main(String[] args) {
        WebCrawler wc = new WebCrawler();
        List<String> crawlList = wc.crawl("https://en.wikipedia.org/wiki/Dune%3A_Part_Two", 2);

        System.out.println("\n\nFinished Crawl.\nURLs crawled: " + crawlList.size());
    }
}
