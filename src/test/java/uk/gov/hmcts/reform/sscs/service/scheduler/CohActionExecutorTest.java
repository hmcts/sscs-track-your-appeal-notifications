package uk.gov.hmcts.reform.sscs.service.scheduler;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.EVIDENCE_REMINDER_NOTIFICATION;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.factory.CohNotificationWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.NotificationService;
import uk.gov.hmcts.reform.sscs.service.RetryNotificationService;

public class CohActionExecutorTest {

    private static final int MAX_RETRY = 3;
    @Mock
    private NotificationService notificationService;
    @Mock
    private IdamService idamService;
    @Mock
    private CcdService ccdService;
    @Mock
    private RetryNotificationService retryNotificationService;
    private CohActionExecutor cohActionExecutor;
    private SscsCaseDetails caseDetails;
    private SscsCaseDataWrapper wrapper;
    private IdamTokens idamTokens;

    @Before
    public void setup() {
        initMocks(this);

        Jackson2ObjectMapperBuilder objectMapperBuilder =
                new Jackson2ObjectMapperBuilder()
                        .featuresToEnable(READ_ENUMS_USING_TO_STRING)
                        .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                        .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
                        .serializationInclusion(JsonInclude.Include.NON_ABSENT);

        final ObjectMapper mapper = objectMapperBuilder.createXmlMapper(false).build();
        mapper.registerModule(new JavaTimeModule());

        final SscsCaseCallbackDeserializer deserializer = new SscsCaseCallbackDeserializer(mapper);

        cohActionExecutor = new CohActionExecutor(notificationService, retryNotificationService, MAX_RETRY, ccdService, idamService, deserializer);

        caseDetails = SscsCaseDetails.builder().state("appealCreated").id(456L)
                .caseTypeId("123").createdDate(LocalDateTime.now().minusMinutes(10)).build();

        SscsCaseData newSscsCaseData = SscsCaseData.builder().ccdCaseId("456").build();
        caseDetails.setData(newSscsCaseData);

        wrapper = SscsCaseDataWrapper.builder().state(State.APPEAL_CREATED)
                .newSscsCaseData(newSscsCaseData)
                .notificationEventType(EVIDENCE_REMINDER_NOTIFICATION)
                .createdDate(caseDetails.getCreatedDate())
                .build();

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    public void givenAReminderIsTriggered_thenActionExecutorShouldProcessTheJob() {
        when(ccdService.getByCaseId(eq(123456L), eq(idamTokens))).thenReturn(caseDetails);

        String onlineHearingId = UUID.randomUUID().toString();
        cohActionExecutor.execute("1", "group", EVIDENCE_REMINDER_NOTIFICATION.getId(), new CohJobPayload(123456L, onlineHearingId));

        verify(notificationService, times(1)).manageNotificationAndSubscription(new CohNotificationWrapper(onlineHearingId, wrapper));
    }

    @Test
    public void shouldGiveTheMaxRetryWhenReturningTheRetryValue() {
        int retry = cohActionExecutor.getRetry(new CohJobPayload(12L, "1"));
        assertThat(retry, is(equalTo(MAX_RETRY)));
    }
}