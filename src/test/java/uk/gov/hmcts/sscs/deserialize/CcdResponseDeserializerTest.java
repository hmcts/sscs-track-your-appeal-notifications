package uk.gov.hmcts.sscs.deserialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.sscs.domain.notify.NotificationType.APPEAL_RECEIVED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.CcdResponse;

public class CcdResponseDeserializerTest {

    private CcdResponseDeserializer ccdResponseDeserializer;
    private ObjectMapper mapper;

    @Before
    public void setup() {
        ccdResponseDeserializer = new CcdResponseDeserializer();
        mapper = new ObjectMapper();
    }

    @Test
    public void deserializeAppellantJson() throws IOException {

        String json = "{\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\"}}}";

        CcdResponse ccdResponse = ccdResponseDeserializer.deserializeAppellantJson(mapper.readTree(json), new CcdResponse());

        assertEquals("J", ccdResponse.getAppellantFirstName());
        assertEquals("Maloney", ccdResponse.getAppellantSurname());
        assertEquals("Mr", ccdResponse.getAppellantTitle());
        assertEquals("test@testing.com", ccdResponse.getEmail());
        assertEquals("01234556634", ccdResponse.getMobileNumber());
    }

    @Test
    public void deserializeCaseIdJson() throws IOException {

        String json = "{\"id\":{\"tya\":\"755TY68876\"}}";

        CcdResponse ccdResponse = ccdResponseDeserializer.deserializeCaseIdJson(mapper.readTree(json), new CcdResponse());

        assertEquals("755TY68876", ccdResponse.getAppealNumber());
    }

    @Test
    public void deserializeAllCcdResponseJson() throws IOException {

        String json = "{\"state\":\"appealReceivedNotification\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\",\"gaps2\":\"SC001/0000/0000\"},\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertEquals(APPEAL_RECEIVED, ccdResponse.getNotificationType());
        assertEquals("J", ccdResponse.getAppellantFirstName());
        assertEquals("Maloney", ccdResponse.getAppellantSurname());
        assertEquals("Mr", ccdResponse.getAppellantTitle());
        assertEquals("test@testing.com", ccdResponse.getEmail());
        assertEquals("01234556634", ccdResponse.getMobileNumber());
        assertEquals("755TY68876", ccdResponse.getAppealNumber());
        assertEquals("SC001/0000/0000", ccdResponse.getCaseReference());
    }

    @Test
    public void deserializeWithMissingAppellant() throws IOException {
        String json = "{\"state\":\"appealReceivedNotification\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getAppellantSurname());
    }

    @Test
    public void deserializeWithMissingAppellantName() throws IOException {
        String json = "{\"state\":\"appealReceivedNotification\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"},\"appellant\":{\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getAppellantSurname());
    }

    @Test
    public void deserializeWithMissingAppellantContact() throws IOException {
        String json = "{\"state\":\"appealReceivedNotification\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"},\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getEmail());
    }

    @Test
    public void deserializeWithMissingCaseId() throws IOException {
        String json = "{\"state\":\"appealReceivedNotification\",\"case_data\":{\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getAppealNumber());

    }
}