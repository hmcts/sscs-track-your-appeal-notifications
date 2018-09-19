package uk.gov.hmcts.reform.sscs.factory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ONLINE;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.OnlinePanel;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

public class CcdNotificationWrapperTest {
    @Test
    public void hearingTypeIsOnline() {
        AppealHearingType hearingType = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .onlinePanel(OnlinePanel.builder().build())
                        .build())
                .build()).getHearingType();

        assertThat(hearingType, is(ONLINE));
    }

    @Test
    public void hearingTypeIsOral() {
        AppealHearingType hearingType = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .build())
                .build()).getHearingType();

        assertThat(hearingType, is(AppealHearingType.REGULAR));
    }
}