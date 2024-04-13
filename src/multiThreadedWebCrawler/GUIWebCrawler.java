package multiThreadedWebCrawler;

/* General Imports */
import java.io.IOException;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;

/* GUI Imports */
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.FlowLayout;

/* Multi-Threading Imports */
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/* Jsoup Webpage Interface Imports */
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class GUIWebCrawler extends JFrame {
    private ExecutorService executor;
    private final BlockingQueue<SimpleEntry<String, Integer>> urlQueue;
    private final Set<String> visited;
    private JTextField urlField, depthField;
    private JButton startButton, pauseButton, resumeButton, endButton;
    private int maxDepth;

    public GUIWebCrawler() {
        executor = Executors.newCachedThreadPool();
        urlQueue = new LinkedBlockingQueue<>();
        visited = ConcurrentHashMap.newKeySet();

        initializeUI();
    }

    private void initializeUI() {
        setTitle("Web Crawler");
        setSize(500, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        urlField = new JTextField(20);
        add(urlField);

        depthField = new JTextField(5);
        add(new JLabel("Max Depth:"));
        add(depthField);

        startButton = new JButton("Start");
        startButton.addActionListener(e -> {
            try {
                maxDepth = Integer.parseInt(depthField.getText());
                startCrawling(urlField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for max depth.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        add(startButton);

        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> pause());
        pauseButton.setVisible(false);
        add(pauseButton);

        resumeButton = new JButton("Resume");
        resumeButton.addActionListener(e -> resume());
        resumeButton.setVisible(false);
        add(resumeButton);

        endButton = new JButton("End");
        endButton.addActionListener(e -> end());
        endButton.setVisible(false);
        add(endButton);

        setVisible(true);
    }

    private void startCrawling(String startUrl) {
        visited.add(startUrl);
        urlQueue.offer(new SimpleEntry<>(startUrl, 0));
        processQueue();
        startButton.setVisible(false);
        depthField.setVisible(false);
        pauseButton.setVisible(true);
    }

    private void processQueue() {
        new Thread(() -> {
            while (!urlQueue.isEmpty() && !Thread.currentThread().isInterrupted()) {
                SimpleEntry<String, Integer> entry = urlQueue.poll();
                if (entry != null && entry.getValue() <= maxDepth) {
                    Future<?> future = executor.submit(() -> crawl(entry.getKey(), entry.getValue()));
                    try {
                        future.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    private void crawl(String url, int currentDepth) {
        Document doc = request(url);
        if (currentDepth < maxDepth) {
            if(doc != null) {
                for (Element link : doc.select("a[href]")) {
                    String absUrl = link.absUrl("href");
                    if(visited.add(absUrl)) {
                        urlQueue.offer(new SimpleEntry<>(absUrl, currentDepth + 1));
                    }
                }
            }
        }
    }

    /* Webpage Retrival */
    private Document request(String url) {
        try {
            Connection con = Jsoup.connect(url);
            Document doc = con.get();

            if(con.response().statusCode() == 200) {
                System.out.println("\n**Bot: " + Thread.currentThread().threadId() + ", Received Webpage at: " + url);

                String title = doc.title();
                System.out.println("Title: " + title);

                return doc;
            }

            return null;
        } catch(IOException e) {
            System.err.println("Error crawling " + url + ": " + e.getMessage());
            return null;
        }
    }

    private void pause() {
        executor.shutdownNow();
        pauseButton.setVisible(false);
        resumeButton.setVisible(true);
        endButton.setVisible(true);
    }

    private void resume() {
        executor = Executors.newCachedThreadPool();
        processQueue();
        resumeButton.setVisible(false);
        endButton.setVisible(false);
        pauseButton.setVisible(true);
    }

    private void end() {
        executor.shutdownNow();
        urlQueue.clear();
        visited.clear();
        pauseButton.setVisible(false);
        resumeButton.setVisible(false);
        endButton.setVisible(false);
        startButton.setVisible(true);
        depthField.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUIWebCrawler::new);
    }
}
