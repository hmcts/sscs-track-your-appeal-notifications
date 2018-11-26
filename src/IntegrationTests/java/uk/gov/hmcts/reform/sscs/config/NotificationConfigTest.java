package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

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
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
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
    @Parameters(method = "templateIdsWithHearingAndEventTypes")
    public void given_templateNamesAndHearingType_should_getCorrectTemplate(String expectedEmailTemplateId,
                                                                            String expectedSmsTemplateId,
                                                                            AppealHearingType appealHearingType,
                                                                            String templateName) {
        Template template = notificationConfig.getTemplate(templateName, templateName, Benefit.PIP, appealHearingType);
        assertEquals(expectedEmailTemplateId, template.getEmailTemplateId());
        assertEquals(expectedSmsTemplateId, template.getSmsTemplateId());
    }

    private Object[] templateIdsWithHearingAndEventTypes() {
        return new Object[]{
            new Object[]{"a64bce9a-9162-47ca-b3e7-cf5f85ca7bdc", "f5b61f94-0b2b-4e8e-9c25-56e9830df7d4", PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION)},
            new Object[]{"1afd89f9-9935-4acb-b4f6-ba708b03a0d3", "4bba0b5d-a3f3-4fd9-a845-26af5eda042e", ORAL, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION)},
            new Object[]{"90f0ed29-a616-4ce0-b4ef-108391f5d90e", "e2e166c4-3600-443d-8feb-39f2c28e8732", ONLINE, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION)},
            new Object[]{"976bdb6c-8a86-48cf-9e0f-7989acaec0c2", "1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b", PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION)},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION)},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION,APPELLANT)},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION,APPELLANT)},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION,REPRESENTATIVE)},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION,REPRESENTATIVE)},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION,APPELLANT)},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION,APPELLANT)},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "f900174a-a556-43b2-8042-bbf3e6090071", PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION,APPELLANT)},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "f900174a-a556-43b2-8042-bbf3e6090071", ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION,APPELLANT)},
            new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", "e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac", PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION,REPRESENTATIVE)},
            new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", "e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac", ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION,REPRESENTATIVE)},
            new Object[]{"0a48bd48-f79c-4863-b6e3-e8fa69019c34", null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION,REPRESENTATIVE)},
            new Object[]{"0a48bd48-f79c-4863-b6e3-e8fa69019c34", null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION,REPRESENTATIVE)},
            new Object[]{"08959288-e09a-472d-80b8-af79bfcbb437", null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION,APPELLANT)},
            new Object[]{"08959288-e09a-472d-80b8-af79bfcbb437", null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION,APPELLANT)}
        };
    }

    private String getTemplateName(NotificationEventType notificationEventType, SubscriptionType subscriptionType) {
        return notificationEventType.getId() + "." + subscriptionType.name().toLowerCase();
    }

    private String getTemplateName(NotificationEventType notificationEventType) {
        return notificationEventType.getId();
    }

}
