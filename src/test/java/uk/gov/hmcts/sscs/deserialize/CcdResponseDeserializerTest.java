package uk.gov.hmcts.sscs.deserialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

        String json = "{\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"phone\":\"07998445858\"}}}";

        CcdResponse ccdResponse = ccdResponseDeserializer.deserializeAppellantJson(mapper.readTree(json), new CcdResponse());

        assertEquals(ccdResponse.getAppellantFirstName(), "J");
        assertEquals(ccdResponse.getAppellantSurname(), "Maloney");
        assertEquals(ccdResponse.getAppellantTitle(), "Mr");
        assertEquals(ccdResponse.getEmail(), "test@testing.com");
        assertEquals(ccdResponse.getMobileNumber(), "01234556634");
        assertEquals(ccdResponse.getPhoneNumber(), "07998445858");
    }

    @Test
    public void deserializeCaseIdJson() throws IOException {

        String json = "{\"id\":{\"tya\":\"755TY68876\"}}";

        CcdResponse ccdResponse = ccdResponseDeserializer.deserializeCaseIdJson(mapper.readTree(json), new CcdResponse());

        assertEquals(ccdResponse.getAppealNumber(), "755TY68876");
    }




    @Test
    public void deserializeAllCcdResponseJson() throws IOException {

        String json = "{\"state\":\"ResponseRequested\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"},\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"phone\":\"07998445858\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertEquals(ccdResponse.getAppealStatus(), "ResponseRequested");
        assertEquals(ccdResponse.getAppellantFirstName(), "J");
        assertEquals(ccdResponse.getAppellantSurname(), "Maloney");
        assertEquals(ccdResponse.getAppellantTitle(), "Mr");
        assertEquals(ccdResponse.getEmail(), "test@testing.com");
        assertEquals(ccdResponse.getMobileNumber(), "01234556634");
        assertEquals(ccdResponse.getPhoneNumber(), "07998445858");
        assertEquals(ccdResponse.getAppealNumber(), "755TY68876");
    }

    @Test
    public void deserializeWithMissingAppellant() throws IOException {
        String json = "{\"state\":\"ResponseRequested\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getAppellantSurname());
    }

    @Test
    public void deserializeWithMissingAppellantName() throws IOException {
        String json = "{\"state\":\"ResponseRequested\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"},\"appellant\":{\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"phone\":\"07998445858\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getAppellantSurname());
    }

    @Test
    public void deserializeWithMissingAppellantContact() throws IOException {
        String json = "{\"state\":\"ResponseRequested\",\"case_data\":{\"id\":{\"tya\":\"755TY68876\"},\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getEmail());
    }

    @Test
    public void deserializeWithMissingCaseId() throws IOException {
        String json = "{\"state\":\"ResponseRequested\",\"case_data\":{\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Maloney\",\"firstName\":\"J\"},\"contact\":{\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"phone\":\"07998445858\"}}}}";

        CcdResponse ccdResponse = mapper.readValue(json, CcdResponse.class);

        assertNull(ccdResponse.getAppealNumber());

    }
}