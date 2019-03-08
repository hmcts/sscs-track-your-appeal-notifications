package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
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
                                                                            String expectedLetterTemplateId,
                                                                            AppealHearingType appealHearingType,
                                                                            String templateName) {
        Template template = notificationConfig.getTemplate(templateName, templateName, templateName, Benefit.PIP, appealHearingType);
        assertEquals(expectedEmailTemplateId, template.getEmailTemplateId());
        assertEquals(expectedSmsTemplateId, template.getSmsTemplateId());
        assertEquals(expectedLetterTemplateId, template.getLetterTemplateId());
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] templateIdsWithHearingAndEventTypes() {
        return new Object[]{
            new Object[]{"a64bce9a-9162-47ca-b3e7-cf5f85ca7bdc", "f5b61f94-0b2b-4e8e-9c25-56e9830df7d4", null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION)},
            new Object[]{"01caec0c-191b-4a32-882a-6fded2546ce6", "317a121e-d08c-4890-b3b3-4652f741771f", null, ORAL, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION)},
            new Object[]{"90f0ed29-a616-4ce0-b4ef-108391f5d90e", null, null, ONLINE, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION)},
            new Object[]{"a3b22e07-e90b-4b52-a293-30823802c209", "aaa1aad4-7abc-4a7a-b8fb-8b0567c09365", null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION)},
            new Object[]{"b9e47ec4-3b58-4b8d-9304-f77ac27fb7f2", "e3f71440-d1ac-43c8-a8cc-a088c4f3c959", null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION)},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", "91143b85-dd9d-430c-ba23-e42ec90f44f8", PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT)},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", "91143b85-dd9d-430c-ba23-e42ec90f44f8", ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT)},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", "747d026e-1bec-4e96-8a34-28f36e30bba5", PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE)},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", "747d026e-1bec-4e96-8a34-28f36e30bba5", ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE)},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", null, PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT)},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", null, ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT)},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", null, ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPOINTEE)},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", null, PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", null, ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", null, PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT)},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", null, ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT)},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", null, PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE)},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", null, ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE)},
            new Object[]{"75357eb8-bba7-4bdf-b879-b535bc3fb50a", "a170d63e-b04e-4da5-ad89-d93644b6c1e9", null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"75357eb8-bba7-4bdf-b879-b535bc3fb50a", "a170d63e-b04e-4da5-ad89-d93644b6c1e9", null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"bff02237-9bcb-49fa-bbf7-11725b97132a", "46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT)},
            new Object[]{"bff02237-9bcb-49fa-bbf7-11725b97132a", "46c6bf06-33dd-4e5a-9b6b-8bd6d0eb33b1", null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT)},
            new Object[]{"cab48431-a4f0-41f5-b753-2cecf20ab5d4", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", "6af62d46-98e5-4ade-aa72-e4a11c56286e", PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT)},
            new Object[]{"c5654134-2e13-4541-ac73-334a5b5cdbb6", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", "6af62d46-98e5-4ade-aa72-e4a11c56286e", ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT)},
            new Object[]{"7af36950-fc63-45d1-907d-f472fac7af06", "345f802b-7089-4f46-a17f-bf534b272740", "8eb75404-a442-47aa-bab2-c4ba83a70900", PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"30260c0b-5575-4f4e-bce4-73cf3f245c2d", "345f802b-7089-4f46-a17f-bf534b272740", "8eb75404-a442-47aa-bab2-c4ba83a70900", ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"c5654134-2e13-4541-ac73-334a5b5cdbb6", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", "6af62d46-98e5-4ade-aa72-e4a11c56286e", PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE)},
            new Object[]{"c5654134-2e13-4541-ac73-334a5b5cdbb6", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", "6af62d46-98e5-4ade-aa72-e4a11c56286e", ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE)},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "f900174a-a556-43b2-8042-bbf3e6090071", null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT)},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "f900174a-a556-43b2-8042-bbf3e6090071", null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT)},
            new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", "e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac", null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", "e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac", null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "f900174a-a556-43b2-8042-bbf3e6090071", null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE)},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "f900174a-a556-43b2-8042-bbf3e6090071", null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE)},
            new Object[]{"b74ea5d4-dba2-4148-b822-d102cedbea12", "4562984e-2854-4191-81d9-cffbe5111015", null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"976bdb6c-8a86-48cf-9e0f-7989acaec0c2", "1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b", null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT)},
            new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", "99bd4a56-256c-4de8-b187-d43a8dde466f", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", "99bd4a56-256c-4de8-b187-d43a8dde466f", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"08365e91-9e07-4a5c-bf96-ef56fd0ada63", "ede384aa-0b6e-4311-9f01-ee547573a07b", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE)},
            new Object[]{"08365e91-9e07-4a5c-bf96-ef56fd0ada63", "ede384aa-0b6e-4311-9f01-ee547573a07b", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE)},
            new Object[]{"b90df52f-c628-409c-8875-4b0b9663a053", "ede384aa-0b6e-4311-9f01-ee547573a07b", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT)},
            new Object[]{"b90df52f-c628-409c-8875-4b0b9663a053", "ede384aa-0b6e-4311-9f01-ee547573a07b", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT)},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", null, PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT)},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", null, ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT)},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", null, PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", null, ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"0a48bd48-f79c-4863-b6e3-e8fa69019c34", null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"0a48bd48-f79c-4863-b6e3-e8fa69019c34", null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"08959288-e09a-472d-80b8-af79bfcbb437", null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT)},
            new Object[]{"08959288-e09a-472d-80b8-af79bfcbb437", null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT)},
            new Object[]{"08959288-e09a-472d-80b8-af79bfcbb437", null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE)},
            new Object[]{"08959288-e09a-472d-80b8-af79bfcbb437", null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE)},
            new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", "99bd4a56-256c-4de8-b187-d43a8dde466f", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", PAPER, getTemplateName(APPEAL_LODGED,REPRESENTATIVE)},
            new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", "99bd4a56-256c-4de8-b187-d43a8dde466f", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", ORAL, getTemplateName(APPEAL_LODGED,REPRESENTATIVE)},
            new Object[]{"b90df52f-c628-409c-8875-4b0b9663a053", "ede384aa-0b6e-4311-9f01-ee547573a07b", "91143b85-dd9d-430c-ba23-e42ec90f44f8", PAPER, getTemplateName(APPEAL_LODGED,APPELLANT)},
            new Object[]{"b90df52f-c628-409c-8875-4b0b9663a053", "ede384aa-0b6e-4311-9f01-ee547573a07b", "91143b85-dd9d-430c-ba23-e42ec90f44f8", ORAL, getTemplateName(APPEAL_LODGED,APPELLANT)},
            new Object[]{"08365e91-9e07-4a5c-bf96-ef56fd0ada63", "ede384aa-0b6e-4311-9f01-ee547573a07b", "747d026e-1bec-4e96-8a34-28f36e30bba5", PAPER, getTemplateName(APPEAL_LODGED,APPOINTEE)},
            new Object[]{"08365e91-9e07-4a5c-bf96-ef56fd0ada63", "ede384aa-0b6e-4311-9f01-ee547573a07b", "747d026e-1bec-4e96-8a34-28f36e30bba5", ORAL, getTemplateName(APPEAL_LODGED,APPOINTEE)},
            new Object[]{"e2ee8609-7d56-4857-b3f8-79028e8960aa", null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE)},
            new Object[]{"fc9d0618-68c4-48ec-9481-a84b225a57a9", null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT)}
        };
    }

    private String getTemplateName(NotificationEventType notificationEventType, SubscriptionType subscriptionType) {
        return notificationEventType.getId() + "." + subscriptionType.name().toLowerCase();
    }

    private String getTemplateName(NotificationEventType notificationEventType) {
        return notificationEventType.getId();
    }

}
