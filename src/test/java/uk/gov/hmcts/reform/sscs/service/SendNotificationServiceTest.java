package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class SendNotificationServiceTest {

    private static final String YES = "Yes";
    private static final String CASE_REFERENCE = "ABC123";
    private static final String CASE_ID = "1000001";

    private static Appellant APPELLANT_WITH_ADDRESS = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

    private static Appointee APPOINTEE_WITH_ADDRESS = Appointee.builder()
            .address(Address.builder().line1("Appointee Line 1").town("Appointee Town").county("Appointee County").postcode("AP9 0IN").build())
            .name(Name.builder().firstName("Ap").lastName("Pointee").build())
            .build();

    private static Appellant APPELLANT_WITH_ADDRESS_AND_APPOINTEE = Appellant.builder()
            .name(Name.builder().firstName("Ap").lastName("Pellant").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .appointee(APPOINTEE_WITH_ADDRESS)
            .build();

    @Test
    public void getAppellantAddressToUseForLetter() {
        Address expectedAddress = APPELLANT_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS);

        Address actualAddress = SendNotificationService.getAddressToUseForLetter(wrapper);
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    @Test
    public void getAppointeeAddressToUseForLetter() {
        Address expectedAddress = APPOINTEE_WITH_ADDRESS.getAddress();
        CcdNotificationWrapper wrapper = buildBaseWrapper(APPELLANT_WITH_ADDRESS_AND_APPOINTEE);

        Address actualAddress = SendNotificationService.getAddressToUseForLetter(wrapper);
        assertEquals(expectedAddress.getLine1(), actualAddress.getLine1());
        assertEquals(expectedAddress.getLine2(), actualAddress.getLine2());
        assertEquals(expectedAddress.getTown(), actualAddress.getTown());
        assertEquals(expectedAddress.getCounty(), actualAddress.getCounty());
        assertEquals(expectedAddress.getPostcode(), actualAddress.getPostcode());
    }

    private CcdNotificationWrapper buildBaseWrapper(Appellant appellant) {
        SscsCaseData sscsCaseDataWithDocuments = SscsCaseData.builder()
                .appeal(
                        Appeal
                                .builder()
                                .hearingType(AppealHearingType.ORAL.name())
                                .hearingOptions(HearingOptions.builder().wantsToAttend(YES).build())
                                .appellant(appellant)
                                .rep(null)
                                .build())
                .subscriptions(Subscriptions.builder().appellantSubscription(Subscription.builder()
                        .tya("GLSCRR")
                        .email("Email")
                        .mobile("07983495065")
                        .subscribeEmail(YES)
                        .subscribeSms(YES)
                        .build()).build())
                .caseReference(CASE_REFERENCE)
                .ccdCaseId(CASE_ID)
                .sscsDocument(new ArrayList<>(Collections.singletonList(null)))
                .build();

        SscsCaseDataWrapper struckOutSscsCaseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(sscsCaseDataWithDocuments)
                .oldSscsCaseData(sscsCaseDataWithDocuments)
                .notificationEventType(NotificationEventType.STRUCK_OUT)
                .build();
        return new CcdNotificationWrapper(struckOutSscsCaseDataWrapper);
    }

}
