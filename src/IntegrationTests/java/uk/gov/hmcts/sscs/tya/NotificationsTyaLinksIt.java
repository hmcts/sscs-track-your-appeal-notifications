package uk.gov.hmcts.sscs.tya;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class NotificationsTyaLinksIt {

    @Value("${manage.emails.link}")
    private String manageEmailsLink;
    @Value("${track.appeal.link}")
    private String trackAppealLink;
    @Value("${evidence.submission.info.link}")
    private String evidenceSubmissionInfoLink;
    @Value("${claiming.expenses.link}")
    private String claimingExpensesLink;
    @Value("${hearing.info.link}")
    private String hearingInfoLink;

    @Test
    public void shouldVerifyTyaLinksAreInValidFormat() {
        assertThat(manageEmailsLink.endsWith("/manage-email-notifications/mac"), equalTo(true));
        assertThat(trackAppealLink.endsWith("/trackyourappeal/appeal_id"), equalTo(true));
        assertThat(evidenceSubmissionInfoLink.endsWith("/evidence/appeal_id"), equalTo(true));
        assertThat(claimingExpensesLink.endsWith("/expenses/appeal_id"), equalTo(true));
        assertThat(hearingInfoLink.endsWith("/abouthearing/appeal_id"), equalTo(true));
    }
}
