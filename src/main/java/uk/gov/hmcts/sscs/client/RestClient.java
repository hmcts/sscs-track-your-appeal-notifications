package uk.gov.hmcts.sscs.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.sscs.exception.ReminderException;

public class RestClient {

    private Client jerseyClient;

    @Value("${job.scheduler.url}")
    private String url;

    public RestClient(final Client client) {
        this.jerseyClient = client;
    }

    public void post(JSONObject json, String endpoint) throws ReminderException {

        WebResource webResource = jerseyClient.resource(url + "/" + endpoint);

        ClientResponse response = webResource.type("application/json")
                .header("ServiceAuthorization", "sscs")
                .post(ClientResponse.class, json);

        if (response.getStatus() != 201) {
            throw new ReminderException(new Exception("Failed : HTTP error : " + response));
        }
    }
}