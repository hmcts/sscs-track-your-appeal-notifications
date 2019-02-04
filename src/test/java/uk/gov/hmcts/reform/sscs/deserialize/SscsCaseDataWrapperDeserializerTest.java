package uk.gov.hmcts.reform.sscs.deserialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.ESA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.LAPSED_REVISED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.WITHDRAWN;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.APPEAL_RECEIVED_NOTIFICATION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

public class SscsCaseDataWrapperDeserializerTest {

    private SscsCaseDataWrapperDeserializer ccdResponseDeserializer;
    private ObjectMapper mapper;

    @Before
    public void setup() {
        ccdResponseDeserializer = new SscsCaseDataWrapperDeserializer();
        mapper = new ObjectMapper();
    }

    @Test
    public void deserializeBenefitJson() throws IOException {

        String appealJson = "{\"benefitType\":{\"code\":\"ESA\"}}";

        BenefitType benefitType = ccdResponseDeserializer.deserializeBenefitDetailsJson(mapper.readTree(appealJson));

        assertEquals(ESA.name(), benefitType.getCode());
    }

    @Test
    public void deserializeAppellantJson() throws IOException {

        String subscriptionJson = "{\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"representativeSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}}";

        Subscriptions subscriptions = ccdResponseDeserializer.deserializeSubscriptionsJson(mapper.readTree(subscriptionJson));

        Subscription appellantSubscription = subscriptions.getAppellantSubscription();

        assertEquals("test@testing.com", appellantSubscription.getEmail());
        assertEquals("01234556634", appellantSubscription.getMobile());
        assertFalse(appellantSubscription.isSmsSubscribed());
        assertTrue(appellantSubscription.isEmailSubscribed());

        Subscription representativeSubscription = subscriptions.getRepresentativeSubscription();
        assertEquals("supporter@live.co.uk", representativeSubscription.getEmail());
        assertEquals("07925289702", representativeSubscription.getMobile());
        assertTrue(representativeSubscription.isSmsSubscribed());
        assertFalse(representativeSubscription.isEmailSubscribed());
    }

    @Test
    public void deserializeAppellantWithAppointeeJson() throws IOException {

        String subscriptionJson = "{\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
            + "\"appointeeSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}}";

        Subscriptions subscriptions = ccdResponseDeserializer.deserializeSubscriptionsJson(mapper.readTree(subscriptionJson));

        Subscription appellantSubscription = subscriptions.getAppellantSubscription();

        assertEquals("test@testing.com", appellantSubscription.getEmail());
        assertEquals("01234556634", appellantSubscription.getMobile());
        assertFalse(appellantSubscription.isSmsSubscribed());
        assertTrue(appellantSubscription.isEmailSubscribed());

        Subscription appointeeSubscription = subscriptions.getAppointeeSubscription();
        assertEquals("supporter@live.co.uk", appointeeSubscription.getEmail());
        assertEquals("07925289702", appointeeSubscription.getMobile());
        assertTrue(appointeeSubscription.isSmsSubscribed());
        assertFalse(appointeeSubscription.isEmailSubscribed());
    }

    @Test
    public void deserializeEventJson() throws IOException {
        String eventJson = "{\"events\": [{\"id\": \"bad54ab0-5d09-47ab-b9fd-c3d55cbaf56f\",\"value\": {\"date\": \"2018-01-19T12:54:12.000\",\"description\": null,\"type\": \"appealReceived\"}}]}";

        List<Event> events = ccdResponseDeserializer.deserializeEventDetailsJson(mapper.readTree(eventJson));

        assertEquals(1, events.size());
        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(12, 54, 12), ZoneId.of(AppConstants.ZONE_ID)), events.get(0).getValue().getDateTime());
        assertEquals(APPEAL_RECEIVED, events.get(0).getValue().getEventType());
    }

    @Test
    public void deserializeMultipleEventJsonInDescendingEventDateOrder() throws IOException {
        String eventJson = "{\"events\": [{\"id\": \"bad54ab0\",\"value\": {\"date\": \"2018-01-19T12:00:00.000\",\"description\": null,\"type\": \"appealReceived\"}},\n"
                + "{\"id\": \"12354ab0\",\"value\": {\"date\": \"2018-01-19T14:00:00.000\",\"description\": null,\"type\": \"appealWithdrawn\"}},\n"
                + "{\"id\": \"87564ab0\",\"value\": {\"date\": \"2018-01-19T13:00:00.000\",\"description\": null,\"type\": \"appealLapsed\"}}]}";

        List<Event> events = ccdResponseDeserializer.deserializeEventDetailsJson(mapper.readTree(eventJson));

        assertEquals(3, events.size());

        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(14, 0), ZoneId.of(AppConstants.ZONE_ID)), events.get(0).getValue().getDateTime());
        assertEquals(WITHDRAWN, events.get(0).getValue().getEventType());
        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(13, 0), ZoneId.of(AppConstants.ZONE_ID)), events.get(1).getValue().getDateTime());
        assertEquals(LAPSED_REVISED, events.get(1).getValue().getEventType());
        assertEquals(ZonedDateTime.of(LocalDate.of(2018, 1, 19), LocalTime.of(12, 0), ZoneId.of(AppConstants.ZONE_ID)), events.get(2).getValue().getDateTime());
        assertEquals(APPEAL_RECEIVED, events.get(2).getValue().getEventType());
    }

    @Test
    public void deserializeHearingJsonWithWinterHearingTime() throws IOException {
        String hearingJson = "{\"hearings\": [{\"id\": \"1234\",\"value\": {"
                + "\"hearingDate\": \"2018-01-12\",\"time\": \"11:00\",\"venue\": {"
                + "\"name\": \"Prudential House\",\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"\","
                + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                + "\"googleMapLink\": \"https://www.google.com/theAddress\"}}}]}";

        List<Hearing> hearings = ccdResponseDeserializer.deserializeHearingDetailsJson(mapper.readTree(hearingJson));

        assertEquals(1, hearings.size());

        Hearing hearing = hearings.get(0);
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

        List<Hearing> hearings = ccdResponseDeserializer.deserializeHearingDetailsJson(mapper.readTree(hearingJson));

        assertEquals(1, hearings.size());

        Hearing hearing = hearings.get(0);
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

        List<Hearing> hearings = ccdResponseDeserializer.deserializeHearingDetailsJson(mapper.readTree(hearingJson));

        assertEquals(3, hearings.size());

        assertEquals(LocalDateTime.of(LocalDate.of(2018, 1, 12), LocalTime.of(13, 0)), hearings.get(0).getValue().getHearingDateTime());
        assertEquals(LocalDateTime.of(LocalDate.of(2018, 1, 12), LocalTime.of(12, 0)), hearings.get(1).getValue().getHearingDateTime());
        assertEquals(LocalDateTime.of(LocalDate.of(2018, 1, 12), LocalTime.of(11, 0)), hearings.get(2).getValue().getHearingDateTime());
    }

    @Test
    public void deserializeAllSscsCaseDataJsonWithNewAndOldCcdData() throws IOException {

        String json = "{\"case_details\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"appointeeSubscription\":{\"tya\":\"23292924\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"},"
                + "\"representativeSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"caseReference\":\"SC022/14/12423\",\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"},"
                + "\"appointee\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Appointee\",\"firstName\":\"Appointee\",\"middleName\":\"Ab\"}}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}}},"
                + "\"id\": \"123456789\"},"
                + "\"case_details_before\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"123456\",\"email\":\"old@email.com\",\"mobile\":\"07543534345\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"appointeeSubscription\":{\"tya\":\"23292924\",\"email\":\"supporter@gmail.co.uk\",\"mobile\":\"07925267702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"},"
                + "\"representativeSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@gmail.co.uk\",\"mobile\":\"07925267702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"caseReference\":\"SC/5432/89\",\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Smith\",\"firstName\":\"Jeremy\",\"middleName\":\"Rupert\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Redknapp\",\"firstName\":\"Harry\",\"middleName\":\"Winston\"}}}},"
                + "\"id\": \"523456789\"},"
                + "\"event_id\": \"appealReceived\"\n}";

        SscsCaseDataWrapper wrapper = mapper.readValue(json, SscsCaseDataWrapper.class);

        assertEquals(APPEAL_RECEIVED_NOTIFICATION, wrapper.getNotificationEventType());

        SscsCaseData newSscsCaseData = wrapper.getNewSscsCaseData();
        Appellant newAppellant = newSscsCaseData.getAppeal().getAppellant();
        assertEquals("Dexter", newAppellant.getName().getFirstName());
        assertEquals("Vasquez", newAppellant.getName().getLastName());
        assertEquals("Mr", newAppellant.getName().getTitle());

        Appointee newAppointee = newSscsCaseData.getAppeal().getAppellant().getAppointee();
        assertEquals("Appointee", newAppointee.getName().getFirstName());
        assertEquals("Appointee", newAppointee.getName().getLastName());
        assertEquals("Mr", newAppointee.getName().getTitle());

        Subscription newAppellantSubscription = newSscsCaseData.getSubscriptions().getAppellantSubscription();
        assertEquals("test@testing.com", newAppellantSubscription.getEmail());
        assertEquals("01234556634", newAppellantSubscription.getMobile());
        assertFalse(newAppellantSubscription.isSmsSubscribed());
        assertTrue(newAppellantSubscription.isEmailSubscribed());

        Subscription newAppointeeSubscription = newSscsCaseData.getSubscriptions().getAppointeeSubscription();
        assertEquals("supporter@live.co.uk", newAppointeeSubscription.getEmail());
        assertEquals("07925289702", newAppointeeSubscription.getMobile());
        assertTrue(newAppointeeSubscription.isSmsSubscribed());
        assertFalse(newAppointeeSubscription.isEmailSubscribed());

        Subscription newRepresentativeSubscription = newSscsCaseData.getSubscriptions().getRepresentativeSubscription();
        assertEquals("supporter@live.co.uk", newRepresentativeSubscription.getEmail());
        assertEquals("07925289702", newRepresentativeSubscription.getMobile());
        assertTrue(newRepresentativeSubscription.isSmsSubscribed());
        assertFalse(newRepresentativeSubscription.isEmailSubscribed());
        assertEquals("SC022/14/12423", newSscsCaseData.getCaseReference());
        assertEquals("123456789", newSscsCaseData.getCcdCaseId());

        SscsCaseData oldSscsCaseData = wrapper.getOldSscsCaseData();

        Appellant oldAppellant = oldSscsCaseData.getAppeal().getAppellant();
        assertEquals("Jeremy", oldAppellant.getName().getFirstName());
        assertEquals("Smith", oldAppellant.getName().getLastName());
        assertEquals("Mr", oldAppellant.getName().getTitle());

        Subscription oldAppellantSubscription = oldSscsCaseData.getSubscriptions().getAppellantSubscription();
        assertEquals("old@email.com", oldAppellantSubscription.getEmail());
        assertEquals("07543534345", oldAppellantSubscription.getMobile());
        assertFalse(oldAppellantSubscription.isSmsSubscribed());
        assertTrue(oldAppellantSubscription.isEmailSubscribed());

        Subscription oldAppointeeSubscription = newSscsCaseData.getSubscriptions().getAppointeeSubscription();
        assertEquals("supporter@live.co.uk", oldAppointeeSubscription.getEmail());
        assertEquals("07925289702", oldAppointeeSubscription.getMobile());
        assertTrue(oldAppointeeSubscription.isSmsSubscribed());
        assertFalse(oldAppointeeSubscription.isEmailSubscribed());

        Subscription oldRepresentativeSubscription = oldSscsCaseData.getSubscriptions().getRepresentativeSubscription();
        assertEquals("supporter@gmail.co.uk", oldRepresentativeSubscription.getEmail());
        assertEquals("07925267702", oldRepresentativeSubscription.getMobile());
        assertTrue(oldRepresentativeSubscription.isSmsSubscribed());
        assertFalse(oldRepresentativeSubscription.isEmailSubscribed());
        assertEquals("SC/5432/89", oldSscsCaseData.getCaseReference());
        assertEquals("523456789", oldSscsCaseData.getCcdCaseId());
    }

    @Test
    public void deserializeWithMissingCaseReference() throws IOException {
        String json = "{\"case_details\":{\"case_data\":{\"subscriptions\":{"
                + "\"appellantSubscription\":{\"tya\":\"543212345\",\"email\":\"test@testing.com\",\"mobile\":\"01234556634\",\"reason\":null,\"subscribeSms\":\"No\",\"subscribeEmail\":\"Yes\"},"
                + "\"representativeSubscription\":{\"tya\":\"232929249492\",\"email\":\"supporter@live.co.uk\",\"mobile\":\"07925289702\",\"reason\":null,\"subscribeSms\":\"Yes\",\"subscribeEmail\":\"No\"}},"
                + "\"appeal\":{"
                + "\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\",\"middleName\":\"Ali Sosa\"}},"
                + "\"supporter\":{\"name\":{\"title\":\"Mrs\",\"lastName\":\"Wilder\",\"firstName\":\"Amber\",\"middleName\":\"Clark Eaton\"}}}}},\"event_id\": \"appealReceived\"\n}";

        SscsCaseDataWrapper wrapper = mapper.readValue(json, SscsCaseDataWrapper.class);

        assertNull(wrapper.getNewSscsCaseData().getCaseReference());
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
    public void returnNullWhenFieldTextIsNull() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;
        final ObjectNode child = factory.objectNode();

        child.put("message", "null");

        assertEquals(null, ccdResponseDeserializer.getField(null, "message"));
    }

    @Test
    public void shouldDeserializeOnlinePanel() throws Exception {
        String onlinePanelJson = "{\"onlinePanel\":{\"assignedTo\":\"Mr Smith\",\"disabilityQualifiedMember\":\"Mr Tall\","
                + "\"medicalMember\":\"Mr Small\"}}";

        OnlinePanel onlinePanel = ccdResponseDeserializer.deserializeOnlinePanelJson(mapper.readTree(onlinePanelJson));

        assertEquals("Mr Smith", onlinePanel.getAssignedTo());
        assertEquals("Mr Tall", onlinePanel.getDisabilityQualifiedMember());
        assertEquals("Mr Small", onlinePanel.getMedicalMember());
    }

    @Test
    public void shouldDeserializeRegionalProcessingCenterIfPresent() throws Exception {
        String rpcJson = "{\"regionalProcessingCenter\":{\"name\":\"CARDIFF\",\"address1\":\"HM Courts & Tribunals Service\","
                + "\"address2\":\"Social Security & Child Support Appeals\",\"address3\":\"Eastgate House\",\n"
                + "\"address4\":\"Newport Road\",\"city\":\"CARDIFF\",\"postcode\":\"CF24 0AB\",\"phoneNumber\":\"0300 123 1142\",\"faxNumber\":\"0870 739 4438\"}}";

        RegionalProcessingCenter regionalProcessingCenter = ccdResponseDeserializer.deserializeRegionalProcessingCenterJson(mapper.readTree(rpcJson));

        assertNotNull(regionalProcessingCenter);

        assertEquals("CARDIFF", regionalProcessingCenter.getName());
        assertEquals("HM Courts & Tribunals Service", regionalProcessingCenter.getAddress1());
        assertEquals("Social Security & Child Support Appeals", regionalProcessingCenter.getAddress2());
        assertEquals("Eastgate House", regionalProcessingCenter.getAddress3());
        assertEquals("Newport Road", regionalProcessingCenter.getAddress4());
        assertEquals("CARDIFF", regionalProcessingCenter.getCity());
        assertEquals("CF24 0AB", regionalProcessingCenter.getPostcode());
        assertEquals("0300 123 1142", regionalProcessingCenter.getPhoneNumber());
        assertEquals("0870 739 4438", regionalProcessingCenter.getFaxNumber());
    }

    @Test
    public void shouldNotDeserializeRegionalProcessingCenterIfItsNotPresent() throws Exception {
        String json = "{\"benefitType\":{\"code\":\"UNK\"}}";

        RegionalProcessingCenter rpc = ccdResponseDeserializer.deserializeRegionalProcessingCenterJson(mapper.readTree(json));

        assertNull(rpc);
    }

    @Test
    public void shouldDeserializeMrnDetails() throws Exception {
        String mrnJson = "{\"mrnDetails\":{\"dwpIssuingOffice\":\"Birmingham\",\"mrnDate\":\"2018-01-01\","
                + "\"mrnLateReason\":\"It is late\",\"mrnMissingReason\":\"It went missing\"}}";

        MrnDetails mrnDetails = ccdResponseDeserializer.deserializeMrnDetailsJson(mapper.readTree(mrnJson));

        assertEquals("Birmingham", mrnDetails.getDwpIssuingOffice());
        assertEquals("2018-01-01", mrnDetails.getMrnDate());
        assertEquals("It is late", mrnDetails.getMrnLateReason());
        assertEquals("It went missing", mrnDetails.getMrnMissingReason());
    }

    @Test
    public void shouldDeserializeAppellantDetails() throws Exception {
        String appellantJson = "{\"appellant\":{\"name\":{\"title\":\"Mr\",\"lastName\":\"Vasquez\",\"firstName\":\"Dexter\"},"
                + "\"address\": {\"line1\": \"36 Dale Street\",\"line2\": \"Village\","
                + "\"town\": \"Liverpool\",\"county\": \"Merseyside\",\"postcode\": \"L2 5UZ\"},"
                + "\"contact\": {\"email\": \"test@tester.com\", \"mobile\": \"07848484848\"},"
                + "\"identity\": {\"dob\": \"1998-07-01\", \"nino\": \"JT098230B\"}}}";

        Appellant appellant = ccdResponseDeserializer.deserializeAppellantDetailsJson(mapper.readTree(appellantJson));

        assertEquals("Mr", appellant.getName().getTitle());
        assertEquals("Dexter", appellant.getName().getFirstName());
        assertEquals("Vasquez", appellant.getName().getLastName());
        assertEquals("36 Dale Street", appellant.getAddress().getLine1());
        assertEquals("Village", appellant.getAddress().getLine2());
        assertEquals("Liverpool", appellant.getAddress().getTown());
        assertEquals("Merseyside", appellant.getAddress().getCounty());
        assertEquals("L2 5UZ", appellant.getAddress().getPostcode());
        assertEquals("test@tester.com", appellant.getContact().getEmail());
        assertEquals("07848484848", appellant.getContact().getPhone());
        assertEquals("1998-07-01", appellant.getIdentity().getDob());
        assertEquals("JT098230B", appellant.getIdentity().getNino());
    }

    @Test
    public void shouldDeserializeHearingOptionsDetails() throws Exception {
        String hearingOptionsDetailsJson = "{\"hearingOptions\":{\"wantsToAttend\":\"Yes\",\"wantsSupport\":\"No\",\"languageInterpreter\":\"Yes\","
                + "\"languages\": \"French\",\"scheduleHearing\": \"Yes\","
                + "\"other\": \"Bla\",\"arrangements\": [\"signLanguageInterpreter\",\"hearingLoop\"],"
                + "\"excludeDates\": [{\"value\": {\"start\": \"2018-04-04\",\"end\": \"2018-04-06\"}},"
                + "{\"value\": {\"start\": \"2018-04-10\"}}]}}";

        HearingOptions hearingOptions = ccdResponseDeserializer.deserializeHearingOptionsJson(mapper.readTree(hearingOptionsDetailsJson));

        List<String> arrangements = new ArrayList<>();
        arrangements.add("signLanguageInterpreter");
        arrangements.add("hearingLoop");

        assertEquals("Yes", hearingOptions.getWantsToAttend());
        assertEquals("No", hearingOptions.getWantsSupport());
        assertEquals("Yes", hearingOptions.getLanguageInterpreter());
        assertEquals("French", hearingOptions.getLanguages());
        assertEquals("Yes", hearingOptions.getScheduleHearing());
        assertEquals("Bla", hearingOptions.getOther());
        assertEquals(arrangements, hearingOptions.getArrangements());

        assertEquals("2018-04-04", hearingOptions.getExcludeDates().get(0).getValue().getStart());
        assertEquals("2018-04-06", hearingOptions.getExcludeDates().get(0).getValue().getEnd());
        assertEquals("2018-04-10", hearingOptions.getExcludeDates().get(1).getValue().getStart());
        assertNull(hearingOptions.getExcludeDates().get(1).getValue().getEnd());
    }

    @Test
    public void shouldDeserializeAppealReasonDetails() throws Exception {
        String appealReasonsJson = "{\"appealReasons\": {\"reasons\": [{\"value\": {\"reason\": \"reason1\",\"description\": \"description1\"}},"
                + "{\"value\": {\"reason\": \"reason2\",\"description\": \"description2\"}}],\"otherReasons\": \"Another reason\"}}";

        AppealReasons appealReasons = ccdResponseDeserializer.deserializeAppealReasonsJson(mapper.readTree(appealReasonsJson));

        assertEquals("reason1", appealReasons.getReasons().get(0).getValue().getReason());
        assertEquals("description1", appealReasons.getReasons().get(0).getValue().getDescription());
        assertEquals("reason2", appealReasons.getReasons().get(1).getValue().getReason());
        assertEquals("description2", appealReasons.getReasons().get(1).getValue().getDescription());
        assertEquals("Another reason", appealReasons.getOtherReasons());
    }

    @Test
    public void shouldDeserializeRepresentativeDetails() throws Exception {
        String appealReasonsJson = "{\"rep\": {\"hasRepresentative\": \"Yes\",\"name\": {"
                + "\"title\": \"Mr\",\"firstName\": \"Harry\",\"lastName\": \"Potter\"},\n"
                + "\"address\": {\"line1\": \"123 Hairy Lane\",\"line2\": \"Off Hairy Park\",\"town\": \"Town\",\n"
                + "\"county\": \"County\",\"postcode\": \"CM14 4LQ\"},\n"
                + "\"contact\": {\"email\": \"harry.potter@wizards.com\",\"mobile\": \"07411999999\"},"
                + "\"organisation\": \"HP Ltd\"}}}";

        Representative rep = ccdResponseDeserializer.deserializeRepresentativeReasons(mapper.readTree(appealReasonsJson));

        assertEquals("Mr", rep.getName().getTitle());
        assertEquals("Harry", rep.getName().getFirstName());
        assertEquals("Potter", rep.getName().getLastName());
        assertEquals("123 Hairy Lane", rep.getAddress().getLine1());
        assertEquals("Off Hairy Park", rep.getAddress().getLine2());
        assertEquals("Town", rep.getAddress().getTown());
        assertEquals("County", rep.getAddress().getCounty());
        assertEquals("CM14 4LQ", rep.getAddress().getPostcode());
        assertEquals("07411999999", rep.getContact().getPhone());
        assertEquals("harry.potter@wizards.com", rep.getContact().getEmail());
        assertEquals("HP Ltd", rep.getOrganisation());
    }

    @Test
    public void deserializeAllSscsCaseDataJson() throws IOException {

        String path = getClass().getClassLoader().getResource("json/ccdResponseWithCohFields.json").getFile();
        String json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        SscsCaseDataWrapper wrapper = mapper.readValue(json, SscsCaseDataWrapper.class);
        SscsCaseData ccdResponse = wrapper.getNewSscsCaseData();

        Subscription appellantSubscription = ccdResponse.getSubscriptions().getAppellantSubscription();

        assertEquals(APPEAL_RECEIVED_NOTIFICATION, wrapper.getNotificationEventType());
        assertEquals("sscstest@greencroftconsulting.com", appellantSubscription.getEmail());
        assertEquals("07985233301", appellantSubscription.getMobile());
        assertTrue(appellantSubscription.isSmsSubscribed());
        assertTrue(appellantSubscription.isEmailSubscribed());

        Appeal appeal = ccdResponse.getAppeal();
        assertEquals(ESA.name(), appeal.getBenefitType().getCode());

        MrnDetails mrnDetails = appeal.getMrnDetails();
        assertEquals("Birmingham", mrnDetails.getDwpIssuingOffice());
        assertEquals("2018-01-01", mrnDetails.getMrnDate());
        assertEquals("It is late", mrnDetails.getMrnLateReason());
        assertEquals("It went missing", mrnDetails.getMrnMissingReason());

        Appellant appellant = appeal.getAppellant();
        assertEquals("Dexter", appellant.getName().getFirstName());
        assertEquals("Vasquez", appellant.getName().getLastName());
        assertEquals("Mr", appellant.getName().getTitle());
        assertEquals("36 Dale Street", appellant.getAddress().getLine1());
        assertEquals("Village", appellant.getAddress().getLine2());
        assertEquals("Liverpool", appellant.getAddress().getTown());
        assertEquals("Merseyside", appellant.getAddress().getCounty());
        assertEquals("L2 5UZ", appellant.getAddress().getPostcode());
        assertEquals("test@tester.com", appellant.getContact().getEmail());
        assertEquals("07848484848", appellant.getContact().getPhone());
        assertEquals("JT098230B", appellant.getIdentity().getNino());
        assertEquals("1998-07-01", appellant.getIdentity().getDob());

        List<String> arrangements = new ArrayList<>();
        arrangements.add("signLanguageInterpreter");
        arrangements.add("hearingLoop");

        HearingOptions hearingOptions = appeal.getHearingOptions();
        assertEquals("Yes", hearingOptions.getWantsToAttend());
        assertEquals("No", hearingOptions.getWantsSupport());
        assertEquals("Yes", hearingOptions.getLanguageInterpreter());
        assertEquals("French", hearingOptions.getLanguages());
        assertEquals("Yes", hearingOptions.getScheduleHearing());
        assertEquals("Bla", hearingOptions.getOther());
        assertEquals(arrangements, hearingOptions.getArrangements());

        assertEquals("2018-04-04", hearingOptions.getExcludeDates().get(0).getValue().getStart());
        assertEquals("2018-04-06", hearingOptions.getExcludeDates().get(0).getValue().getEnd());
        assertEquals("2018-04-10", hearingOptions.getExcludeDates().get(1).getValue().getStart());
        assertNull(hearingOptions.getExcludeDates().get(1).getValue().getEnd());

        AppealReasons appealReasons = appeal.getAppealReasons();
        assertEquals("reason1", appealReasons.getReasons().get(0).getValue().getReason());
        assertEquals("description1", appealReasons.getReasons().get(0).getValue().getDescription());
        assertEquals("reason2", appealReasons.getReasons().get(1).getValue().getReason());
        assertEquals("description2", appealReasons.getReasons().get(1).getValue().getDescription());
        assertEquals("Another reason", appealReasons.getOtherReasons());

        Representative rep = appeal.getRep();

        assertEquals("Mr", rep.getName().getTitle());
        assertEquals("Harry", rep.getName().getFirstName());
        assertEquals("Potter", rep.getName().getLastName());
        assertEquals("123 Hairy Lane", rep.getAddress().getLine1());
        assertEquals("Off Hairy Park", rep.getAddress().getLine2());
        assertEquals("Town", rep.getAddress().getTown());
        assertEquals("County", rep.getAddress().getCounty());
        assertEquals("CM14 4LQ", rep.getAddress().getPostcode());
        assertEquals("07411999999", rep.getContact().getPhone());
        assertEquals("harry.potter@wizards.com", rep.getContact().getEmail());
        assertEquals("HP Ltd", rep.getOrganisation());

        assertEquals("Yes", ccdResponse.getAppeal().getSigner());

        Subscription representativeSubscription = ccdResponse.getSubscriptions().getRepresentativeSubscription();
        assertEquals("representative@hmcts.net", representativeSubscription.getEmail());
        assertEquals("07983469702", representativeSubscription.getMobile());
        assertTrue(representativeSubscription.isSmsSubscribed());
        assertFalse(representativeSubscription.isEmailSubscribed());
        assertEquals("SC022/14/12423", ccdResponse.getCaseReference());

        Hearing hearing = ccdResponse.getHearings().get(0);
        assertEquals("Prudential House", hearing.getValue().getVenue().getName());
        assertEquals("36 Dale Street", hearing.getValue().getVenue().getAddress().getLine1());
        assertEquals("Liverpool", hearing.getValue().getVenue().getAddress().getTown());
        assertEquals("Merseyside", hearing.getValue().getVenue().getAddress().getCounty());
        assertEquals("L2 5UZ", hearing.getValue().getVenue().getAddress().getPostcode());
        assertEquals("https://www.google.com/theAddress", hearing.getValue().getVenue().getGoogleMapLink());
        assertEquals("12345656789", ccdResponse.getCcdCaseId());
        assertNotNull(ccdResponse.getRegionalProcessingCenter());

        OnlinePanel onlinePanel = ccdResponse.getOnlinePanel();
        assertEquals("Mr Smith", onlinePanel.getAssignedTo());
        assertEquals("Mr Tall", onlinePanel.getDisabilityQualifiedMember());
        assertEquals("Mr Small", onlinePanel.getMedicalMember());
    }
}