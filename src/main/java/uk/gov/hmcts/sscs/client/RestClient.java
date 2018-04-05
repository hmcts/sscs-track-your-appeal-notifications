package uk.gov.hmcts.sscs.client;

import static org.slf4j.LoggerFactory.getLogger;

import com.sun.jersey.api.client.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.sscs.exception.ReminderException;

public class RestClient {

    private static final org.slf4j.Logger LOG = getLogger(RestClient.class);

    private Client jerseyClient;

    @Value("${job.scheduler.url}")
    private String url;

    public RestClient(final Client client) {
        this.jerseyClient = client;
    }

    public void post(JSONObject json, String endpoint) {

        WebResource webResource = jerseyClient.resource(url + "/" + endpoint);

        LOG.info("Attempting to post to reminder service with json: ", json);

        try {
            ClientResponse response = webResource.type("application/json")
                    .header("ServiceAuthorization", "sscs")
                    .post(ClientResponse.class, json);

            if (response.getStatus() != 201) {
                logReminderError(new Exception("Failed reminder response: " + response));
            }
        } catch (UniformInterfaceException | ClientHandlerException e) {
            logReminderError(e);
        }
    }

    private void logReminderError(Exception e) {

        ReminderException reminderException = new ReminderException(e);
        LOG.error("Failed with HTTP error", reminderException);
        throw reminderException;
    }
}