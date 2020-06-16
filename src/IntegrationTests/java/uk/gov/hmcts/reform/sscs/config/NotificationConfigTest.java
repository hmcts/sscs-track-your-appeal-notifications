package uk.gov.hmcts.reform.sscs.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.*;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
    public static final List<NotificationEventType> BUNDLED_LETTER_EVENT_TYPES = Arrays.asList(STRUCK_OUT, DIRECTION_ISSUED, DECISION_ISSUED, ISSUE_FINAL_DECISION, JUDGE_DECISION_APPEAL_TO_PROCEED, TCW_DECISION_APPEAL_TO_PROCEED);

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
                                                                            String expectedDocmosisTemplateId,
                                                                            AppealHearingType appealHearingType,
                                                                            String templateName,
                                                                            String createdInGapsFrom) {
        Template template = notificationConfig.getTemplate(templateName, templateName, templateName, templateName, Benefit.PIP, appealHearingType, createdInGapsFrom);
        assertEquals(expectedEmailTemplateId, template.getEmailTemplateId());
        assertEquals(expectedSmsTemplateId, template.getSmsTemplateId());
        assertEquals(expectedLetterTemplateId, template.getLetterTemplateId());
        assertEquals(expectedDocmosisTemplateId, template.getDocmosisTemplateId());
    }

    @Test
    @Parameters(method = "bundledLetterTemplateNames")
    public void given_bundledLetters_should_notHaveTemplate(AppealHearingType appealHearingType, String templateName) {
        Template template = notificationConfig.getTemplate(templateName, templateName, templateName, templateName, Benefit.PIP, appealHearingType, null);
        assertNull(template.getEmailTemplateId());
        assertNull(template.getSmsTemplateId());
        assertNull(template.getLetterTemplateId());
        assertNull(template.getDocmosisTemplateId());
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] templateIdsWithHearingAndEventTypes() {
        return new Object[]{
            new Object[]{"e1084d78-5e2d-45d2-a54f-84339da141c1", "505be856-ceca-4bbc-ba70-29024585056f", null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPELLANT), null},
            new Object[]{"e1084d78-5e2d-45d2-a54f-84339da141c1", "505be856-ceca-4bbc-ba70-29024585056f", null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"5abc83d8-f6b8-4385-805b-ffbb0f64b84b", "b2d187cd-089b-4fe1-b460-a310c0af46fe", null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"2c5644db-1f7b-429b-b10a-8b23a80ed26a", "f20ffcb1-c5f0-4bff-b2d1-a1094f8014e6", "8b11f3f4-6452-4a35-93d8-a94996af6499", null, ORAL, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"0d844af4-b390-42d7-94d5-4fd1ae9388d9", "9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874", "419beb1c-4f26-45e7-8db3-69bfe5e9224d", null, ORAL, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"90f0ed29-a616-4ce0-b4ef-108391f5d90e", null, null, null, ONLINE, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION), null},
            new Object[]{"a3b22e07-e90b-4b52-a293-30823802c209", "aaa1aad4-7abc-4a7a-b8fb-8b0567c09365", null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPELLANT), null},
            new Object[]{"b9e47ec4-3b58-4b8d-9304-f77ac27fb7f2", "e3f71440-d1ac-43c8-a8cc-a088c4f3c959", null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPELLANT), null},
            new Object[]{"a3b22e07-e90b-4b52-a293-30823802c209", "aaa1aad4-7abc-4a7a-b8fb-8b0567c09365", null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPOINTEE), null},
            new Object[]{"b9e47ec4-3b58-4b8d-9304-f77ac27fb7f2", "e3f71440-d1ac-43c8-a8cc-a088c4f3c959", null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPOINTEE), null},
            new Object[]{"81fa38cc-b7cc-469c-8109-67c801dc9c84", "f1076482-a76d-4389-b411-9865373cfc42", null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"df0803aa-f804-49fe-a2ac-c27adc4bb585", "5f91012e-0d3f-465b-b301-ee3ee5a50100", null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", "747d026e-1bec-4e96-8a34-28f36e30bba5", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", null, "TB-SCS-GNO-ENG-00079.doc", PAPER, getTemplateName(VALID_APPEAL_CREATED, REPRESENTATIVE), null},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", null, "TB-SCS-GNO-ENG-00079.doc", ORAL, getTemplateName(VALID_APPEAL_CREATED, REPRESENTATIVE), null},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(VALID_APPEAL_CREATED, APPELLANT), null},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(VALID_APPEAL_CREATED, APPELLANT), null},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(VALID_APPEAL_CREATED, APPOINTEE), null},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(VALID_APPEAL_CREATED, APPOINTEE), null},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", "747d026e-1bec-4e96-8a34-28f36e30bba5", null, PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", "747d026e-1bec-4e96-8a34-28f36e30bba5", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, null, PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, null, ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, null, PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, null, ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, null, PAPER, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", "ee58f7d0-8de7-4bee-acd4-252213db6b7b", null, null, ORAL, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, PAPER, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, ORAL, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
            new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5", "85a81e3c-a59f-4ae9-9c99-266dfd5ca95f", null, ORAL, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", "8c719552-6b6b-4df3-9edb-9fe1635755b9", null, PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", "8c719552-6b6b-4df3-9edb-9fe1635755b9", null, ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE), null},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", "8c719552-6b6b-4df3-9edb-9fe1635755b9", null, PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE), null},
            new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", "f59440ee-19ca-4d47-a702-13e9cecaccbd", "8c719552-6b6b-4df3-9edb-9fe1635755b9", null, ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPELLANT), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPELLANT), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPOINTEE), null},
            new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", "446c7b23-7342-42e1-adff-b4c367e951cb", "d4ca58d1-8b48-44eb-9af9-0bfc14a0d72d", null, ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPOINTEE), null},
            new Object[]{"ecf7db7d-a257-4496-a2bf-768e560c80e7", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33", null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"ecf7db7d-a257-4496-a2bf-768e560c80e7", "259b8e81-b44a-4271-a57b-ba7f8bdfcb33", null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"cff1be5f-20cf-4cfa-9a90-4a75d3341ba8", "f71772b1-ae1d-49d6-87c6-a41da97a4039", null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT), null},
            new Object[]{"cff1be5f-20cf-4cfa-9a90-4a75d3341ba8", "f71772b1-ae1d-49d6-87c6-a41da97a4039", null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT), null},
            new Object[]{"cab48431-a4f0-41f5-b753-2cecf20ab5d4", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT), null},
            new Object[]{"c5654134-2e13-4541-ac73-334a5b5cdbb6", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT), null},
            new Object[]{"7af36950-fc63-45d1-907d-f472fac7af06", "345f802b-7089-4f46-a17f-bf534b272740", null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"30260c0b-5575-4f4e-bce4-73cf3f245c2d", "345f802b-7089-4f46-a17f-bf534b272740", null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"c5654134-2e13-4541-ac73-334a5b5cdbb6", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"c5654134-2e13-4541-ac73-334a5b5cdbb6", "74bda35f-040b-4355-bda3-faf0e4f5ae6e", null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"774a5cba-fab6-4b8c-a9d9-03f913ed2dca", "fcb93790-8308-4f4f-9674-65b777b7fe8c", null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPELLANT), null},
            new Object[]{"774a5cba-fab6-4b8c-a9d9-03f913ed2dca", "fcb93790-8308-4f4f-9674-65b777b7fe8c", null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPELLANT), null},
            new Object[]{"774a5cba-fab6-4b8c-a9d9-03f913ed2dca", "fcb93790-8308-4f4f-9674-65b777b7fe8c", null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPOINTEE), null},
            new Object[]{"774a5cba-fab6-4b8c-a9d9-03f913ed2dca", "fcb93790-8308-4f4f-9674-65b777b7fe8c", null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPOINTEE), null},
            new Object[]{"97c58e23-c11f-40b3-b981-2d4cfa38b8fd", "bb3df0ea-8259-43c4-95de-9eef96206575", null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"97c58e23-c11f-40b3-b981-2d4cfa38b8fd", "bb3df0ea-8259-43c4-95de-9eef96206575", null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "693c9bfb-151e-4add-a6f2-af1cbc94eef5", null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT), null},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "693c9bfb-151e-4add-a6f2-af1cbc94eef5", null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT), null},
            new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", "e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac", null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", "e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac", null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "693c9bfb-151e-4add-a6f2-af1cbc94eef5", null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"fee16753-0bdb-43f1-9abb-b14b826e3b26", "693c9bfb-151e-4add-a6f2-af1cbc94eef5", null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"b74ea5d4-dba2-4148-b822-d102cedbea12", "4562984e-2854-4191-81d9-cffbe5111015", null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"976bdb6c-8a86-48cf-9e0f-7989acaec0c2", "1aa60c8a-1b6f-4ee1-88ae-51c1cef0ea2b", null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT), null},
            new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", "99bd4a56-256c-4de8-b187-d43a8dde466f", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", "99bd4a56-256c-4de8-b187-d43a8dde466f", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), "validAppeal"},
            new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", "99bd4a56-256c-4de8-b187-d43a8dde466f", "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", "TB-SCS-GNO-ENG-00079.doc", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), "readyToList"},
            new Object[]{"08365e91-9e07-4a5c-bf96-ef56fd0ada63", "ede384aa-0b6e-4311-9f01-ee547573a07b", "747d026e-1bec-4e96-8a34-28f36e30bba5", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"08365e91-9e07-4a5c-bf96-ef56fd0ada63", "ede384aa-0b6e-4311-9f01-ee547573a07b", "747d026e-1bec-4e96-8a34-28f36e30bba5", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), "validAppeal"},
            new Object[]{"08365e91-9e07-4a5c-bf96-ef56fd0ada63", "ede384aa-0b6e-4311-9f01-ee547573a07b", "747d026e-1bec-4e96-8a34-28f36e30bba5", "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), "readyToList"},
            new Object[]{"b90df52f-c628-409c-8875-4b0b9663a053", "ede384aa-0b6e-4311-9f01-ee547573a07b", "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), null},
            new Object[]{"b90df52f-c628-409c-8875-4b0b9663a053", "ede384aa-0b6e-4311-9f01-ee547573a07b", "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), "validAppeal"},
            new Object[]{"b90df52f-c628-409c-8875-4b0b9663a053", "ede384aa-0b6e-4311-9f01-ee547573a07b", "91143b85-dd9d-430c-ba23-e42ec90f44f8", "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), "readyToList"},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", null, "TB-SCS-GNO-ENG-00079.doc", PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", "a6c09fad-6265-4c7c-8b95-36245ffa5352", null, "TB-SCS-GNO-ENG-00079.doc", ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
            new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", "f41222ef-c05c-4682-9634-6b034a166368", null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
            new Object[]{"e07b7dba-f383-49ca-a0ba-b5b61be27da6", null, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"e07b7dba-f383-49ca-a0ba-b5b61be27da6", null, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"221095a2-aee8-466b-a7ab-beee516cc6cc", null, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT), null},
            new Object[]{"221095a2-aee8-466b-a7ab-beee516cc6cc", null, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT), null},
            new Object[]{"221095a2-aee8-466b-a7ab-beee516cc6cc", null, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE), null},
            new Object[]{"221095a2-aee8-466b-a7ab-beee516cc6cc", null, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE), null},
            new Object[]{"e2ee8609-7d56-4857-b3f8-79028e8960aa", null, null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{"fc9d0618-68c4-48ec-9481-a84b225a57a9", null, null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT), null},
            new Object[]{null, null, "5745f77c-6512-4082-8c34-63851f24eab1", null, PAPER, getTemplateName(NON_COMPLIANT_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{null, null, "5745f77c-6512-4082-8c34-63851f24eab1", null, ORAL, getTemplateName(NON_COMPLIANT_NOTIFICATION, REPRESENTATIVE), null},
            new Object[]{null, null, "114cadaa-7760-4699-9add-d3a252f68909", null, PAPER, getTemplateName(NON_COMPLIANT_NOTIFICATION, APPELLANT), null},
            new Object[]{null, null, "114cadaa-7760-4699-9add-d3a252f68909", null, ORAL, getTemplateName(NON_COMPLIANT_NOTIFICATION, APPELLANT), null},
            new Object[]{null, null, "114cadaa-7760-4699-9add-d3a252f68909", null, ORAL, getTemplateName(NON_COMPLIANT_NOTIFICATION, APPOINTEE), null},
            new Object[]{null, null, "114cadaa-7760-4699-9add-d3a252f68909", null, PAPER, getTemplateName(NON_COMPLIANT_NOTIFICATION, APPOINTEE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00452.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, REPRESENTATIVE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00452.docx", ORAL, getTemplateName(REQUEST_INFO_INCOMPLETE, REPRESENTATIVE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00452.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, APPELLANT), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00452.docx", ORAL, getTemplateName(REQUEST_INFO_INCOMPLETE, APPELLANT), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00452.docx", ORAL, getTemplateName(REQUEST_INFO_INCOMPLETE, APPOINTEE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00452.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, APPOINTEE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00067.docx", ORAL, getTemplateName(DIRECTION_ISSUED, APPELLANT), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00067.docx", PAPER, getTemplateName(DIRECTION_ISSUED, APPOINTEE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00089.docx", ORAL, getTemplateName(DIRECTION_ISSUED, REPRESENTATIVE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00089.docx", PAPER, getTemplateName(DIRECTION_ISSUED, REPRESENTATIVE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00094.docx", ORAL, getTemplateName(DECISION_ISSUED, APPELLANT), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00094.docx", PAPER, getTemplateName(DECISION_ISSUED, APPOINTEE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00095.docx", ORAL, getTemplateName(DECISION_ISSUED, REPRESENTATIVE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00095.docx", PAPER, getTemplateName(DECISION_ISSUED, REPRESENTATIVE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00454.docx", ORAL, getTemplateName(ISSUE_FINAL_DECISION, APPELLANT), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00454.docx", PAPER, getTemplateName(ISSUE_FINAL_DECISION, APPOINTEE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00455.docx", ORAL, getTemplateName(ISSUE_FINAL_DECISION, REPRESENTATIVE), null},
            new Object[]{null, null, null, "TB-SCS-GNO-ENG-00455.docx", PAPER, getTemplateName(ISSUE_FINAL_DECISION, REPRESENTATIVE), null}
        };
    }


    @SuppressWarnings({"Indentation", "unused"})
    private Object[] bundledLetterTemplateNames() {
        List<SubscriptionType> subscriptionTypes = Arrays.asList(APPELLANT, APPOINTEE, REPRESENTATIVE);
        Object[] result = new Object[(BUNDLED_LETTER_EVENT_TYPES.size()) * subscriptionTypes.size() * 2];

        int i = 0;
        for (NotificationEventType eventType : BUNDLED_LETTER_EVENT_TYPES.stream()
                .filter(f -> !f.equals(DIRECTION_ISSUED))
                .filter(f -> !f.equals(DECISION_ISSUED))
                .filter(f -> !f.equals(ISSUE_FINAL_DECISION))
                .collect(Collectors.toList())) {
            for (SubscriptionType subscriptionType : subscriptionTypes) {
                result[i++] = new Object[]{PAPER, getTemplateName(eventType, subscriptionType)};
                result[i++] = new Object[]{ORAL, getTemplateName(eventType, subscriptionType)};
            }
        }

        return Arrays.stream(result)
                .filter(Objects::nonNull)
                .toArray();
    }

    private String getTemplateName(NotificationEventType notificationEventType, SubscriptionType subscriptionType) {
        return notificationEventType.getId() + "." + subscriptionType.name().toLowerCase();
    }

    private String getTemplateName(NotificationEventType notificationEventType) {
        return notificationEventType.getId();
    }

}
