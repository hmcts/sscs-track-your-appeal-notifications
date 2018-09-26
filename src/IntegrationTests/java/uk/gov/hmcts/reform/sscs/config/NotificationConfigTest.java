package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.assertEquals;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
public class NotificationConfigTest {
    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private NotificationConfig notificationConfig;

    @Test
    @Parameters({"a64bce9a-9162-47ca-b3e7-cf5f85ca7bdc, f5b61f94-0b2b-4e8e-9c25-56e9830df7d4, PIP",
            "a64bce9a-9162-47ca-b3e7-cf5f85ca7bdc, f5b61f94-0b2b-4e8e-9c25-56e9830df7d4, ESA"})
    public void should_getDwpResponseTemplate_when_aDwpResponseNotificationPaperCaseHappens(
            String expectedEmailTemplateId, String expectedSmsTemplateId, Benefit benefit) {
        Template template = notificationConfig.getTemplate("responseReceived",
                "responseReceived", benefit, AppealHearingType.PAPER);
        assertEquals(expectedEmailTemplateId, template.getEmailTemplateId());
        assertEquals(expectedSmsTemplateId, template.getSmsTemplateId());
    }
}
