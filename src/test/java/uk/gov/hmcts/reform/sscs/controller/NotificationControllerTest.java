package uk.gov.hmcts.reform.sscs.controller;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

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
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
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

    @Before
    public void setUp() throws IOException {
        openMocks(this);

        Jackson2ObjectMapperBuilder objectMapperBuilder =
                new Jackson2ObjectMapperBuilder()
                        .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                        .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                        .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                        .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        ObjectMapper mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());

        SscsCaseCallbackDeserializer deserializer = new SscsCaseCallbackDeserializer(mapper);

        notificationController = new NotificationController(notificationService, authorisationService, ccdService, deserializer, idamService);
        String path = getClass().getClassLoader().getResource("json/ccdResponse.json").getFile();
        json = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8.name());

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void shouldCreateAndSendNotificationForSscsCaseData() {
        notificationController.sendNotification("", json);
        verify(notificationService).manageNotificationAndSubscription(any(CcdNotificationWrapper.class), eq(false));
    }

    private JsonNode hasFields(String eventType, long caseDetailsId) {
        return argThat(argument -> {
            return argument.get("event_id").textValue().equals(eventType)
                    && argument.get("case_details").get("id").longValue() == caseDetailsId;
        });
    }
}
