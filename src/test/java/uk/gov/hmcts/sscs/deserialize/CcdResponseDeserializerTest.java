package uk.gov.hmcts.sscs.deserialize;

import static org.junit.Assert.*;
import static uk.gov.hmcts.sscs.config.AppConstants.ZONE_ID;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.*;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.domain.Hearing;
import uk.gov.hmcts.sscs.domain.RegionalProcessingCenter;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.exception.BenefitMappingException;

public class CcdResponseDeserializerTest {

    private CcdResponseDeserializer ccdResponseDeserializer;
    private ObjectMapper mapper;

    @Before
    public void setup() {
        ccdResponseDeserializer = new CcdResponseDeserializer();
        mapper = new ObjectMapper();
    }

    @Test
    public void deserializeBenefitJson() throws IOException {

        String appealJson = "{\"benefitType\":{\"code\":\"PIP\"}}";

        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeBenefitDetailsJson(mapper.readTree(appealJson), ccdResponse);

        assertEquals(PIP, ccdResponse.getBenefitType());
    }

    @Test(expected = BenefitMappingException.class)
    public void throwBenefitMappingExceptionWhenBenefitTypeUnknown() throws IOException {

        String appealJson = "{\"benefitType\":{\"code\":\"UNK\"}}";

        ccdResponseDeserializer.deserializeBenefitDetailsJson(mapper.readTree(appealJson), CcdResponse.builder().build());
    }

    @Test
    public void deserializeAppellantJson() throws IOException {

        String appealJson = "{\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Eaton\"}}}";
        String subscriptionJson = "{\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}}";

        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeSubscriptionJson(mapper.readTree(appealJson), mapper.readTree(subscriptionJson), ccdResponse);

        Subscription appellantSubscription = ccdResponse.getSubscriptions().getAppellantSubscription();

        assertEquals("Dexter", appellantSubscription.getFirstName());
        assertEquals("Vasquez", appellantSubscription.getSurname());
        assertEquals("Mr", appellantSubscription.getTitle());
        assertEquals("test@testing.com", appellantSubscription.getEmail());
        assertEquals("01234556634", appellantSubscription.getMobile());
        assertFalse(appellantSubscription.isSubscribeSms());
        assertTrue(appellantSubscription.isSubscribeEmail());

        Subscription supporterSubscription = ccdResponse.getSubscriptions().getSupporterSubscription();
        assertEquals("Amber", supporterSubscription.getFirstName());
        assertEquals("Wilder", supporterSubscription.getSurname());
        assertEquals("Mrs", supporterSubscription.getTitle());
        assertEquals("supporter@live.co.uk", supporterSubscription.getEmail());
        assertEquals("07925289702", supporterSubscription.getMobile());
        assertTrue(supporterSubscription.isSubscribeSms());
        assertFalse(supporterSubscription.isSubscribeEmail());
    }

    @Test
    public void deserializeEventJson() throws IOException {
        String eventJson = "{\"events\": [{\"id\": \"bad54ab0-5d09-47ab-b9fd-c3d55cbaf56f\",\"value\": {\"date\": \"2018-01-19T12:54:12.000\",\"description\": null,\"type\": \"appealReceived\"}}]}";

        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeEventDetailsJson(mapper.readTree(eventJson), ccdResponse);

        assertEquals(1, ccdResponse.getEvents().size());
        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(12, 54, 12), ZoneId.of(ZONE_ID)), ccdResponse.getEvents().get(0).getValue().getDateTime());
        assertEquals(APPEAL_RECEIVED, ccdResponse.getEvents().get(0).getValue().getEventType());
    }

    @Test
    public void deserializeMultipleEventJsonInDescendingEventDateOrder() throws IOException {
        String eventJson = "{\"events\": [{\"id\": \"bad54ab0\",\"value\": {\"date\": \"2018-01-19T12:00:00.000\",\"description\": null,\"type\": \"appealReceived\"}},\n"
                + "{\"id\": \"12354ab0\",\"value\": {\"date\": \"2018-01-19T14:00:00.000\",\"description\": null,\"type\": \"appealWithdrawn\"}},\n"
                + "{\"id\": \"87564ab0\",\"value\": {\"date\": \"2018-01-19T13:00:00.000\",\"description\": null,\"type\": \"appealLapsed\"}}]}";

        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeEventDetailsJson(mapper.readTree(eventJson), ccdResponse);

        assertEquals(3, ccdResponse.getEvents().size());

        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(14, 0), ZoneId.of(ZONE_ID)), ccdResponse.getEvents().get(0).getValue().getDateTime());
        assertEquals(APPEAL_WITHDRAWN, ccdResponse.getEvents().get(0).getValue().getEventType());
        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(13, 0), ZoneId.of(ZONE_ID)), ccdResponse.getEvents().get(1).getValue().getDateTime());
        assertEquals(APPEAL_LAPSED, ccdResponse.getEvents().get(1).getValue().getEventType());
        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(12, 0), ZoneId.of(ZONE_ID)), ccdResponse.getEvents().get(2).getValue().getDateTime());
        assertEquals(APPEAL_RECEIVED, ccdResponse.getEvents().get(2).getValue().getEventType());
    }

    @Test
    public void deserializeHearingJsonWithWinterHearingTime() throws IOException {
        String hearingJson = "{\"hearings\": [{\"id\": \"1234\",\"value\": {"
                + "\"hearingDate\": \"2018-01-12\",\"time\": \"11:00\",\"venue\": {"
                + "\"name\": \"Prudential House\",\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"\","
                + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                + "\"googleMapLink\": \"https://www.google.com/theAddress\"}}}]}";

        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeHearingDetailsJson(mapper.readTree(hearingJson), ccdResponse);

        assertEquals(1, ccdResponse.getHearings().size());

        Hearing hearing = ccdResponse.getHearings().get(0);
        assertEquals(LocalDateTime.of(LocalDate.of(2018, 1, 12), LocalTime.of(11, 00, 00)), hearing.getValue().getHearingDateTime());
        assertEquals("Prudential House", hearing.getValue().getVenue().getName());
        assertEquals("36 Dale Street", hearing.getValue().getVenue().getAddress().getLine1());
        assertEquals("Liverpool", hearing.getValue().getVenue().getAddress().getTown());
        assertEquals("Merseyside", hearing.getValue().getVenue().getAddress().getCounty());
        assertEquals("L2 5UZ", hearing.getValue().getVenue().getAddress().getPostcode());
        assertEquals("https://www.google.com/theAddress", hearing.getValue().getVenue().getGoogleMapLink());
    }

    @Test
    public void deserializeHearingJsonWithSummerHearingTime() throws IOException {
        String hearingJson = "{\"hearings\": [{\"id\": \"1234\",\"value\": {"
                + "\"hearingDate\": \"2018-07-12\",\"time\": \"11:00\",\"venue\": {"
                + "\"name\": \"Prudential House\",\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"\","
                + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                + "\"googleMapLink\": \"https://www.google.com/theAddress\"}}}]}";

        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeHearingDetailsJson(mapper.readTree(hearingJson), ccdResponse);

        assertEquals(1, ccdResponse.getHearings().size());

        Hearing hearing = ccdResponse.getHearings().get(0);
        assertEquals(LocalDateTime.of(LocalDate.of(2018, 7, 12), LocalTime.of(11, 00, 00)), hearing.getValue().getHearingDateTime());
    }

    @Test
    public void deserializeMultipleHearingJsonInDescendingHearingDateOrder() throws IOException {
        String hearingJson = "{\"hearings\": [{\"id\": \"1234\",\"value\": {"
                + "\"hearingDate\": \"2018-01-12\",\"time\": \"11:00\",\"venue\": {"
                + "\"name\": \"Prudential House\",\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"\","
                + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                + "\"googleMapLink\": \"https://www.google.com/theAddress\"}}},"
                + "{\"id\": \"4567\",\"value\": {"
                + "\"hearingDate\": \"2018-01-12\",\"time\": \"13:00\",\"venue\": {"
                + "\"name\": \"Prudential House\",\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"\","
                + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                + "\"googleMapLink\": \"https://www.google.com/theAddress\"}}},"
                + "{\"id\": \"9875\",\"value\": {"
                + "\"hearingDate\": \"2018-01-12\",\"time\": \"12:00\",\"venue\": {"
                + "\"name\": \"Prudential House\",\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"\","
                + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                + "\"googleMapLink\": \"https://www.google.com/theAddress\"}}}"
                + "]}";
        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeHearingDetailsJson(mapper.readTree(hearingJson), ccdResponse);

        assertEquals(3, ccdResponse.getHearings().size());

        assertEquals(LocalDateTime.of(LocalDate.of(2018, 1, 12), LocalTime.of(13, 0)), ccdResponse.getHearings().get(0).getValue().getHearingDateTime());
        assertEquals(LocalDateTime.of(LocalDate.of(2018, 1, 12), LocalTime.of(12, 0)), ccdResponse.getHearings().get(1).getValue().getHearingDateTime());
        assertEquals(LocalDateTime.of(LocalDate.of(2018, 1, 12), LocalTime.of(11, 0)), ccdResponse.getHearings().get(2).getValue().getHearingDateTime());
    }

    @Test
    public void deserializeAllCcdResponseJson() throws IOException {

        String json =
            "{\"case_details\":{"
                + "\"case_data\":{"
                    + "\"subscriptions\":{"
                        + "\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                        + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}"
                    + "},"
                    + "\"caseReference\":\"SC/1234/23\","
                    + "\"appeal\":{"
                        + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"}},"
                        + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}"
                    + "},"
                    + "\"regionalProcessingCenter\":{\"name\":\"CARDIFF\",\"address1\":\"HM Courts & Tribunals Service\",\"address2\":\"Social Security & Child Support Appeals\",\"address3\":\"Eastgate House\",\n"
                    + "\"address4\":\"Newport Road\",\"city\":\"CARDIFF\",\"postcode\":\"CF24 0AB\",\"phoneNumber\":\"0300 123 1142\",\"faxNumber\":\"0870 739 4438\"},"
                    + "\"hearings\": [{\"id\": \"1234\",\"value\": {"
                        + "\"hearingDate\": \"2018-01-12\",\"time\": \"11:00\",\"venue\": {"
                            + "\"name\": \"Prudential House\",\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"\","
                            + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                        + "\"googleMapLink\": \"https://www.google.com/theAddress\"}}}]"
                + "},"
                + "\"id\": \"123456789\""
            + "},"
            + "\"event_id\": \"appealReceived\"}";

        CcdResponseWrapper wrapper = mapper.readValue(json, CcdResponseWrapper.class);
        CcdResponse ccdResponse = wrapper.getNewCcdResponse();

        Subscription appellantSubscription = ccdResponse.getSubscriptions().getAppellantSubscription();

        assertEquals(APPEAL_RECEIVED, ccdResponse.getNotificationType());
        assertEquals("Dexter", appellantSubscription.getFirstName());
        assertEquals("Vasquez", appellantSubscription.getSurname());
        assertEquals("Mr", appellantSubscription.getTitle());
        assertEquals("test@testing.com", appellantSubscription.getEmail());
        assertEquals("01234556634", appellantSubscription.getMobile());
        assertFalse(appellantSubscription.isSubscribeSms());
        assertTrue(appellantSubscription.isSubscribeEmail());

        Subscription supporterSubscription = ccdResponse.getSubscriptions().getSupporterSubscription();
        assertEquals("Amber", supporterSubscription.getFirstName());
        assertEquals("Wilder", supporterSubscription.getSurname());
        assertEquals("Mrs", supporterSubscription.getTitle());
        assertEquals("supporter@live.co.uk", supporterSubscription.getEmail());
        assertEquals("07925289702", supporterSubscription.getMobile());
        assertTrue(supporterSubscription.isSubscribeSms());
        assertFalse(supporterSubscription.isSubscribeEmail());
        assertEquals("SC/1234/23", ccdResponse.getCaseReference());
        Hearing hearing = ccdResponse.getHearings().get(0);
        assertEquals("Prudential House", hearing.getValue().getVenue().getName());
        assertEquals("36 Dale Street", hearing.getValue().getVenue().getAddress().getLine1());
        assertEquals("Liverpool", hearing.getValue().getVenue().getAddress().getTown());
        assertEquals("Merseyside", hearing.getValue().getVenue().getAddress().getCounty());
        assertEquals("L2 5UZ", hearing.getValue().getVenue().getAddress().getPostcode());
        assertEquals("https://www.google.com/theAddress", hearing.getValue().getVenue().getGoogleMapLink());
        assertEquals("123456789", ccdResponse.getCaseId());
        assertNotNull(ccdResponse.getRegionalProcessingCenter());
    }

    @Test
    public void deserializeAllCcdResponseJsonWithNewAndOldCcdData() throws IOException {

        String json = "{\"case_details\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"caseReference\":\"SC/1234/23\",\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}}},"
                + "\"id\": \"123456789\"},"
                + "\"case_details_before\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"123456\",\"email\":\"old@email.com\",\"mobile\":\"07543534345\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@gmail.co.uk\",\"mobile\":\"07925267702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"caseReference\":\"SC/5432/89\",\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Smith\",\"firstName\":\"Jeremy\",\"middleName\":\"Rupert\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Redknapp\",\"firstName\":\"Harry\",\"middleName\":\"Winston\"}}}},"
                + "\"id\": \"523456789\"},"
                + "\"event_id\": \"appealReceived\"\n}";

        CcdResponseWrapper wrapper = mapper.readValue(json, CcdResponseWrapper.class);
        CcdResponse newCcdResponse = wrapper.getNewCcdResponse();
        Subscription newAppellantSubscription = newCcdResponse.getSubscriptions().getAppellantSubscription();

        assertEquals(APPEAL_RECEIVED, newCcdResponse.getNotificationType());
        assertEquals("Dexter", newAppellantSubscription.getFirstName());
        assertEquals("Vasquez", newAppellantSubscription.getSurname());
        assertEquals("Mr", newAppellantSubscription.getTitle());
        assertEquals("test@testing.com", newAppellantSubscription.getEmail());
        assertEquals("01234556634", newAppellantSubscription.getMobile());
        assertFalse(newAppellantSubscription.isSubscribeSms());
        assertTrue(newAppellantSubscription.isSubscribeEmail());

        Subscription newSupporterSubscription = newCcdResponse.getSubscriptions().getSupporterSubscription();
        assertEquals("Amber", newSupporterSubscription.getFirstName());
        assertEquals("Wilder", newSupporterSubscription.getSurname());
        assertEquals("Mrs", newSupporterSubscription.getTitle());
        assertEquals("supporter@live.co.uk", newSupporterSubscription.getEmail());
        assertEquals("07925289702", newSupporterSubscription.getMobile());
        assertTrue(newSupporterSubscription.isSubscribeSms());
        assertFalse(newSupporterSubscription.isSubscribeEmail());
        assertEquals("SC/1234/23", newCcdResponse.getCaseReference());
        assertEquals("123456789", newCcdResponse.getCaseId());

        CcdResponse oldCcdResponse = wrapper.getOldCcdResponse();
        Subscription oldAppellantSubscription = oldCcdResponse.getSubscriptions().getAppellantSubscription();

        assertEquals("Jeremy", oldAppellantSubscription.getFirstName());
        assertEquals("Smith", oldAppellantSubscription.getSurname());
        assertEquals("Mr", oldAppellantSubscription.getTitle());
        assertEquals("old@email.com", oldAppellantSubscription.getEmail());
        assertEquals("07543534345", oldAppellantSubscription.getMobile());
        assertFalse(oldAppellantSubscription.isSubscribeSms());
        assertTrue(oldAppellantSubscription.isSubscribeEmail());

        Subscription oldSupporterSubscription = oldCcdResponse.getSubscriptions().getSupporterSubscription();
        assertEquals("Harry", oldSupporterSubscription.getFirstName());
        assertEquals("Redknapp", oldSupporterSubscription.getSurname());
        assertEquals("Mr", oldSupporterSubscription.getTitle());
        assertEquals("supporter@gmail.co.uk", oldSupporterSubscription.getEmail());
        assertEquals("07925267702", oldSupporterSubscription.getMobile());
        assertTrue(oldSupporterSubscription.isSubscribeSms());
        assertFalse(oldSupporterSubscription.isSubscribeEmail());
        assertEquals("SC/5432/89", oldCcdResponse.getCaseReference());
        assertEquals("523456789", oldCcdResponse.getCaseId());
    }

    @Test
    public void deserializeWithMissingAppellantName() throws IOException {
        String json = "{\"case_details\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"caseReference\":\"SC/1234/23\",\"appeal\":{"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}}}},\"event_id\": \"appealReceived\"\n}";

        CcdResponseWrapper wrapper = mapper.readValue(json, CcdResponseWrapper.class);

        assertNull(wrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().getSurname());
        assertEquals("test@testing.com", wrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().getEmail());
    }

    @Test
    public void deserializeWithMissingAppellantSubscription() throws IOException {
        String json = "{\"case_details\":{\"case_data\":{\"subscriptions\":{"
                + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"caseReference\":\"SC/1234/23\",\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}}}},\"event_id\": \"appealReceived\"\n}";

        CcdResponseWrapper wrapper = mapper.readValue(json, CcdResponseWrapper.class);

        assertNull(wrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().getEmail());
        assertEquals("Vasquez", wrapper.getNewCcdResponse().getSubscriptions().getAppellantSubscription().getSurname());
    }

    @Test
    public void deserializeWithMissingCaseReference() throws IOException {
        String json = "{\"case_details\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"supporterSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}}}},\"event_id\": \"appealReceived\"\n}";

        CcdResponseWrapper wrapper = mapper.readValue(json, CcdResponseWrapper.class);

        assertNull(wrapper.getNewCcdResponse().getCaseReference());
    }

    @Test
    public void returnNodeWhenNodeIsPresent() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        final ObjectNode node = factory.objectNode();
        final ObjectNode child = factory.objectNode();

        node.put("message", "test");
        child.set("child", node);
        assertEquals(node, ccdResponseDeserializer.getNode(child, "child"));
    }

    @Test
    public void returnNullWhenNodeIsNotPresent() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        final ObjectNode child = factory.objectNode();

        child.put("message", "test");
        assertEquals(null, ccdResponseDeserializer.getNode(child, "somethingelse"));
    }

    @Test
    public void returnNullWhenNodeIsNull() {
        assertEquals(null, ccdResponseDeserializer.getNode(null, "somethingelse"));
    }

    @Test
    public void returnTextWhenFieldIsPresent() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        final ObjectNode node = factory.objectNode();

        node.put("message", "test");
        assertEquals("test", ccdResponseDeserializer.getField(node, "message"));
    }

    @Test
    public void returnNullWhenFieldIsNotPresent() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        final ObjectNode child = factory.objectNode();

        child.put("message", "test");
        assertEquals(null, ccdResponseDeserializer.getField(child, "somethingelse"));
    }

    @Test
    public void returnNullWhenFieldIsNull() {
        assertEquals(null, ccdResponseDeserializer.getField(null, "somethingelse"));
    }

    @Test
    public void shouldDeseriliazeRegionalProcessingCenterIfPresent() throws Exception {
        String rpcJson = "{\"regionalProcessingCenter\":{\"name\":\"CARDIFF\",\"address1\":\"HM Courts & Tribunals Service\","
                + "\"address2\":\"Social Security & Child Support Appeals\",\"address3\":\"Eastgate House\",\n"
                + "\"address4\":\"Newport Road\",\"city\":\"CARDIFF\",\"postcode\":\"CF24 0AB\",\"phoneNumber\":\"0300 123 1142\",\"faxNumber\":\"0870 739 4438\"}}";

        CcdResponse ccdResponse = CcdResponse.builder().build();
        ccdResponseDeserializer.deserializeRegionalProcessingCenterJson(mapper.readTree(rpcJson), ccdResponse);


        assertNotNull(ccdResponse.getRegionalProcessingCenter());

        RegionalProcessingCenter regionalProcessingCenter = ccdResponse.getRegionalProcessingCenter();

        assertEquals(regionalProcessingCenter.getName(), "CARDIFF");
        assertEquals(regionalProcessingCenter.getAddress1(), "HM Courts & Tribunals Service");
        assertEquals(regionalProcessingCenter.getAddress2(), "Social Security & Child Support Appeals");
        assertEquals(regionalProcessingCenter.getAddress3(), "Eastgate House");
        assertEquals(regionalProcessingCenter.getAddress4(), "Newport Road");
        assertEquals(regionalProcessingCenter.getCity(), "CARDIFF");
        assertEquals(regionalProcessingCenter.getPostcode(), "CF24 0AB");
        assertEquals(regionalProcessingCenter.getPhoneNumber(), "0300 123 1142");
        assertEquals(regionalProcessingCenter.getFaxNumber(), "0870 739 4438");

    }

    @Test
    public void shouldNotDeserializeRegionalProcessingCenterIfItsNotPresent() throws Exception {
        String json = "{\"benefitType\":{\"code\":\"UNK\"}}";
        CcdResponse ccdResponse = CcdResponse.builder().build();

        ccdResponseDeserializer.deserializeRegionalProcessingCenterJson(mapper.readTree(json), ccdResponse);

        assertNull(ccdResponse.getRegionalProcessingCenter());

    }
}