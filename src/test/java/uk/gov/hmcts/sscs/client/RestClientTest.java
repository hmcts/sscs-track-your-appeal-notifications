package uk.gov.hmcts.sscs.client;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.sscs.exception.ReminderException;

public class RestClientTest {

    @Mock
    private Client jerseyClient;

    @Mock
    private WebResource webResource;

    @Mock
    private WebResource.Builder builder;

    @Mock
    private ClientResponse response;

    private RestClient restClient;

    private String url = "http://test.com";
    private JSONObject json =  new JSONObject("{\"test\":\"name\"}");

    @Before
    public void setup() {
        initMocks(this);

        restClient = new RestClient(jerseyClient);

        doReturn(webResource).when(jerseyClient).resource(url + "/jobs");
        doReturn(builder).when(webResource).type("application/json");
        doReturn(builder).when(builder).header("ServiceAuthorization", "sscs");
        doReturn(response).when(builder).post(ClientResponse.class, json);
    }

    @Test
    public void checkCorrectlyHandles201ResponseFromJobScheduler() throws ReminderException {
        doReturn(201).when(response).getStatus();

        restClient.post(json, "jobs");

        verify(builder, times(1)).post(ClientResponse.class, json);
    }

    @Test(expected = ReminderException.class)
    public void checkThrowsJobSchedulerExceptionWhenErrorResponseFromJobScheduler() throws ReminderException {
        doReturn(400).when(response).getStatus();

        restClient.post(json, "jobs");
    }
}
