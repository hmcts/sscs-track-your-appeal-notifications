package uk.gov.hmcts.reform.sscs.controller;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.CohEvent;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.AuthorisationService;
import uk.gov.hmcts.reform.sscs.service.NotificationService;

@ActiveProfiles("integration")
public class NotificationControllerTest {

    private static final String CASE_ID = "12345";
    private static final String eventType = "question_round_issued";

    private NotificationController notificationController;

    private String json;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthorisationService authorisationService;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    private SscsCaseCallbackDeserializer deserializer;

    private IdamTokens idamTokens;

    private ObjectMapper mapper;

    @Before
    public void setUp() throws IOException {
        initMocks(this);

        Jackson2ObjectMapperBuilder objectMapperBuilder =
                new Jackson2ObjectMapperBuilder()
                        .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                        .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                        .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                        .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());

        deserializer = new SscsCaseCallbackDeserializer(mapper);

        notificationController = new NotificationController(notificationService, authorisationService, ccdService, deserializer, idamService);
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void shouldCreateAndSendNotificationForSscsCaseData() {
        notificationController.sendNotification("", json);
        verify(notificationService).manageNotificationAndSubscription(new CcdNotificationWrapper(any()));
    }

    @Test
    public void shouldCreateAndSendNotificationForCohResponse() {
        long caseDetailsId = 123L;
        String onlineHearingId = "onlineHearingId";

        SscsCaseDetails caseDetails = SscsCaseDetails.builder().id(caseDetailsId).data(SscsCaseData.builder().region("My region").build()).build();

        when(ccdService.getByCaseId(Long.valueOf(CASE_ID), idamTokens)).thenReturn(caseDetails);

        CohEvent cohEvent = CohEvent.builder().caseId(CASE_ID).onlineHearingId(onlineHearingId).eventType(eventType).build();
        notificationController.sendCohNotification("", cohEvent);

        verify(notificationService).manageNotificationAndSubscription(argThat(argument ->
                argument instanceof CohNotificationWrapper
                        && ((CohNotificationWrapper) argument).getOnlineHearingId().equals(onlineHearingId)
                        && argument.getSscsCaseDataWrapper().getNewSscsCaseData().equals(caseDetails.getData())));
    }

    @Test
    public void handlesNotFindingCcdDetails() {
        when(ccdService.getByCaseId(eq(Long.valueOf(CASE_ID)), eq(idamTokens))).thenReturn(null);

        notificationController.sendCohNotification("", CohEvent.builder().caseId(CASE_ID).eventType(eventType).build());

        verifyNoInteractions(notificationService);
    }

    private JsonNode hasFields(String eventType, long caseDetailsId) {
        return argThat(argument -> {
            return argument.get("event_id").textValue().equals(eventType)
                    && argument.get("case_details").get("id").longValue() == caseDetailsId;
        });
    }
}