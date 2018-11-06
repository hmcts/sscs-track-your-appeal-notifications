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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class NotificationConfigTest {
    // Below rules are needed to use the junitParamsRunner together with SpringRunner
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private NotificationConfig notificationConfig;

    @Test
    @Parameters({"a64bce9a-9162-47ca-b3e7-cf5f85ca7bdc, f5b61f94-0b2b-4e8e-9c25-56e9830df7d4, PAPER, responseReceived",
            "1afd89f9-9935-4acb-b4f6-ba708b03a0d3, 4bba0b5d-a3f3-4fd9-a845-26af5eda042e, ORAL, responseReceived",
            "90f0ed29-a616-4ce0-b4ef-108391f5d90e, e2e166c4-3600-443d-8feb-39f2c28e8732, ONLINE, responseReceived",
            "976bdb6c-8a86-48cf-9e0f-7989acaec0c2, 1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b, PAPER, appealDormant",
            "e88cb0ca-d295-4f51-8041-c3ef1f4321c2, 69793118-1032-4e9b-b2de-c23a25869842, PAPER, appealCreated.representative",
            "e88cb0ca-d295-4f51-8041-c3ef1f4321c2, 69793118-1032-4e9b-b2de-c23a25869842, ORAL, appealCreated.representative",
            "01293b93-b23e-40a3-ad78-2c6cd01cd21c, f41222ef-c05c-4682-9634-6b034a166368, ORAL, appealCreated.appellant",
            "01293b93-b23e-40a3-ad78-2c6cd01cd21c, f41222ef-c05c-4682-9634-6b034a166368, ORAL, appealCreated.appellant"
    })
    public void given_templateNamesAndHearingType_should_getCorrectTemplate(String expectedEmailTemplateId,
                                                                            String expectedSmsTemplateId,
                                                                            AppealHearingType appealHearingType,
                                                                            String templateName) {
        Template template = notificationConfig.getTemplate(templateName, templateName, Benefit.PIP, appealHearingType);
        assertEquals(expectedEmailTemplateId, template.getEmailTemplateId());
        assertEquals(expectedSmsTemplateId, template.getSmsTemplateId());
    }
}
