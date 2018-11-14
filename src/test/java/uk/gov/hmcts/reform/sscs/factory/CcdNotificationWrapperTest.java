package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

@RunWith(JUnitParamsRunner.class)
public class CcdNotificationWrapperTest {

    @Test
    @Parameters({"paper, PAPER", "null, REGULAR", "oral, ORAL", "online, ONLINE"})
    public void should_returnAccordingAppealHearingType_when_hearingTypeIsPresent(String hearingType,
                                                                                  AppealHearingType expected) {
        CcdNotificationWrapper ccdNotificationWrapper = buildCcdNotificationWrapper(hearingType);

        assertThat(ccdNotificationWrapper.getHearingType(), is(expected));
    }

    private CcdNotificationWrapper buildCcdNotificationWrapper(String hearingType) {
        if ("online".equals(hearingType)) {
            return new CcdNotificationWrapper(
                    SscsCaseDataWrapper.builder()
                            .newSscsCaseData(SscsCaseData.builder()
                                    .appeal(Appeal.builder()
                                            .hearingType("cor")
                                            .build())
                                    .build())
                            .build()
            );
        }
        return new CcdNotificationWrapper(
                SscsCaseDataWrapper.builder()
                        .newSscsCaseData(SscsCaseData.builder()
                                .appeal(Appeal.builder()
                                        .hearingType(hearingType)
                                        .build())
                                        .build())
                        .build()
        );
    }
}