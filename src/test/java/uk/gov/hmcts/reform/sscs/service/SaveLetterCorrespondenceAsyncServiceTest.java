package uk.gov.hmcts.reform.sscs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@RunWith(JUnitParamsRunner.class)
public class SaveLetterCorrespondenceAsyncServiceTest {
    private static final String NOTIFICATION_ID = "123";
    private static final String CCD_ID = "82828";

    private SaveLetterCorrespondenceAsyncService service;
    private Correspondence correspondence;

    @Mock
    private CcdNotificationsPdfService ccdNotificationsPdfService;

    @Mock
    private NotificationClient notificationClient;


    @Before
    public void setup() throws NotificationClientException {
        initMocks(this);
        service = new SaveLetterCorrespondenceAsyncService(ccdNotificationsPdfService);
        correspondence = Correspondence.builder().value(CorrespondenceDetails.builder().to("Mr Blobby").build()).build();
        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        byte[] bytes = "%PDF bytes".getBytes();
        when(notificationClient.getPdfForLetter(eq(NOTIFICATION_ID))).thenReturn(bytes);
        when(ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcd(eq(bytes), eq(Long.valueOf(CCD_ID)), eq(correspondence))).thenReturn(sscsCaseData);
    }

    @Test
    public void willGetLetterFromNotifyAndUploadIntoCcd() throws NotificationClientException {
        service.saveLetter(notificationClient, NOTIFICATION_ID, correspondence, CCD_ID);

        verify(notificationClient).getPdfForLetter(eq(NOTIFICATION_ID));
        verify(ccdNotificationsPdfService).mergeLetterCorrespondenceIntoCcd(any(), eq(Long.valueOf(CCD_ID)), eq(correspondence));
    }

    @Test(expected = NotificationClientException.class)
    @Parameters({"400 PDFNotReadyError", "400 BadRequestError"})
    public void notificationClientExceptionIsReThrown(String message) throws NotificationClientException {
        when(notificationClient.getPdfForLetter(eq(NOTIFICATION_ID))).thenThrow(new NotificationClientException(message));
        service.saveLetter(notificationClient, NOTIFICATION_ID, correspondence, CCD_ID);
    }

    @Test
    public void recoverWillConsumeThrowable() {

        service.getBackendResponseFallback(new NotificationClientException("400 BadRequestError"));
    }

}
