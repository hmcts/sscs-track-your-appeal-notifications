package uk.gov.hmcts.sscs.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.CcdResponseWrapper;
import uk.gov.hmcts.sscs.service.AuthorisationService;
import uk.gov.hmcts.sscs.service.NotificationService;

@ActiveProfiles("integration")
public class NotificationControllerTest {

    private NotificationController notificationController;

    private CcdResponseWrapper ccdResponseWrapper;

    @Mock
    NotificationService notificationService;

    @Mock
    AuthorisationService authorisationService;

    @Before
    public void setUp() {
        initMocks(this);

        notificationController = new NotificationController(notificationService, authorisationService);
        ccdResponseWrapper = CcdResponseWrapper.builder().newCcdResponse(CcdResponse.builder().build()).oldCcdResponse(CcdResponse.builder().build()).build();
    }

    @Test
    public void shouldCreateAndSendNotificationForCcdResponse() {
        notificationController.sendNotification("", ccdResponseWrapper);
        verify(notificationService).createAndSendNotification(ccdResponseWrapper);
    }
}