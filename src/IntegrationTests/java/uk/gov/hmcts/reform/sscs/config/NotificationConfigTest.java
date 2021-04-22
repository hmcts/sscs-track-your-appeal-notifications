package uk.gov.hmcts.reform.sscs.config;

import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.personalisation.Personalisation;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class NotificationConfigTest {
    public static final List<NotificationEventType> BUNDLED_LETTER_EVENT_TYPES = Arrays.asList(STRUCK_OUT, PROCESS_AUDIO_VIDEO, PROCESS_AUDIO_VIDEO_WELSH, DIRECTION_ISSUED, DECISION_ISSUED, ISSUE_FINAL_DECISION, ISSUE_FINAL_DECISION_WELSH, ISSUE_ADJOURNMENT_NOTICE, ISSUE_ADJOURNMENT_NOTICE_WELSH, JUDGE_DECISION_APPEAL_TO_PROCEED, TCW_DECISION_APPEAL_TO_PROCEED);

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
                                                                            List<String> expectedSmsTemplateId,
                                                                            String expectedLetterTemplateId,
                                                                            String expectedDocmosisTemplateId,
                                                                            AppealHearingType appealHearingType,
                                                                            String templateName,
                                                                            String createdInGapsFrom) {
        Template template = notificationConfig.getTemplate(templateName, templateName, templateName, templateName, Benefit.PIP, appealHearingType, createdInGapsFrom, LanguagePreference.ENGLISH);
        assertEquals(expectedEmailTemplateId, template.getEmailTemplateId());

        assertEquals(expectedSmsTemplateId, template.getSmsTemplateId());
        assertEquals(expectedLetterTemplateId, template.getLetterTemplateId());
        assertEquals(expectedDocmosisTemplateId, template.getDocmosisTemplateId());
    }

    @Test
    @Parameters(method = "templateIdsWithHearingAndEventTypesWelsh")
    public void given_templateNamesAndHearingType_should_getCorrectTemplate_Welsh(String expectedEmailTemplateId,
                                                                                  List<String> expectedSmsTemplateId,
                                                                                  String expectedLetterTemplateId,
                                                                                  String expectedDocmosisTemplateId,
                                                                                  AppealHearingType appealHearingType,
                                                                                  String templateName,
                                                                                  String createdInGapsFrom) {
        Template template = notificationConfig.getTemplate(templateName, templateName, templateName, templateName, Benefit.PIP, appealHearingType, createdInGapsFrom, LanguagePreference.WELSH);
        assertEquals(expectedEmailTemplateId, template.getEmailTemplateId());

        assertEquals(expectedSmsTemplateId, template.getSmsTemplateId());
        assertEquals(expectedLetterTemplateId, template.getLetterTemplateId());
        assertEquals(expectedDocmosisTemplateId, template.getDocmosisTemplateId());
    }

    @Test
    @Parameters(method = "bundledLetterTemplateNames")
    public void given_bundledLetters_should_notHaveTemplate(AppealHearingType appealHearingType, String templateName) {
        Template template = notificationConfig.getTemplate(templateName, templateName, templateName, templateName, Benefit.PIP, appealHearingType, null, LanguagePreference.ENGLISH);
        assertNull(template.getEmailTemplateId());
        assertTrue(template.getSmsTemplateId().isEmpty());
        assertNull(template.getLetterTemplateId());
        assertNull(template.getDocmosisTemplateId());
    }

    @Test
    @Parameters(method = "bundledLetterTemplateNames")
    public void given_bundledLetters_should_notHaveWelshTemplate(AppealHearingType appealHearingType,
                                                              String templateName) {
        Template template = notificationConfig.getTemplate(templateName, templateName, templateName, templateName,
                Benefit.PIP, appealHearingType, null, LanguagePreference.WELSH);
        assertNull(template.getEmailTemplateId());
        assertTrue(template.getSmsTemplateId().isEmpty());
        assertNull(template.getLetterTemplateId());
        if (!templateName.equals(String.format("%s.%s", STRUCK_OUT.getId(), lowerCase(REPRESENTATIVE.name())))) {
            assertNull(template.getDocmosisTemplateId());
        }
    }

    @Test
    @Parameters({
            "APPEAL_TO_PROCEED, TB-SCS-GNO-ENG-00551.docx, TB-SCS-GNO-ENG-00551.docx, TB-SCS-GNO-ENG-00551.docx",
            "PROVIDE_INFORMATION, TB-SCS-GNO-ENG-00067.docx, TB-SCS-GNO-ENG-00089.docx, TB-SCS-GNO-ENG-00067.docx",
            "GRANT_EXTENSION, TB-SCS-GNO-ENG-00556.docx, TB-SCS-GNO-ENG-00556.docx, TB-SCS-GNO-ENG-00556.docx",
            "REFUSE_EXTENSION, TB-SCS-GNO-ENG-00557.docx, TB-SCS-GNO-ENG-00557.docx, TB-SCS-GNO-ENG-00557.docx",
            "GRANT_REINSTATEMENT, TB-SCS-GNO-ENG-00584.docx, TB-SCS-GNO-ENG-00584.docx, null",
            "REFUSE_REINSTATEMENT, TB-SCS-GNO-ENG-00585.docx, TB-SCS-GNO-ENG-00585.docx, null",
    })
    public void shouldGiveCorrectDocmosisIdForDirectionIssued(DirectionType directionType, String configAppellantOrAppointee, String configRep, @Nullable String configJointParty) {

        NotificationWrapper wrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .directionTypeDl(new DynamicList(directionType.toString()))
                        .appeal(Appeal.builder()
                                .hearingType(HearingType.ONLINE.getValue())
                                .build())
                        .build())
                .notificationEventType(DIRECTION_ISSUED)
                .build());

        Personalisation personalisation = new Personalisation();
        ReflectionTestUtils.setField(personalisation, "config", notificationConfig);

        Template templateAppellant = personalisation.getTemplate(wrapper, PIP, APPELLANT);
        assertNull(templateAppellant.getEmailTemplateId());
        assertTrue(templateAppellant.getSmsTemplateId().isEmpty());
        assertNull(templateAppellant.getLetterTemplateId());
        assertEquals(configAppellantOrAppointee, templateAppellant.getDocmosisTemplateId());
        Template templateAppointee = personalisation.getTemplate(wrapper, PIP, APPOINTEE);
        assertEquals(configAppellantOrAppointee, templateAppointee.getDocmosisTemplateId());
        Template templateRep = personalisation.getTemplate(wrapper, PIP, REPRESENTATIVE);
        assertEquals(configRep, templateRep.getDocmosisTemplateId());
        Template templateJointParty = personalisation.getTemplate(wrapper, PIP, JOINT_PARTY);
        assertEquals(configJointParty, templateJointParty.getDocmosisTemplateId());
    }

    @Test
    @Parameters({
            "GRANT_EXTENSION, TB-SCS-GNO-WEL-00591.docx, TB-SCS-GNO-WEL-00591.docx, TB-SCS-GNO-WEL-00591.docx",
            "REFUSE_EXTENSION, TB-SCS-GNO-WEL-00592.docx, TB-SCS-GNO-WEL-00592.docx, TB-SCS-GNO-WEL-00592.docx",
            "APPEAL_TO_PROCEED, TB-SCS-GNO-WEL-00590.docx, TB-SCS-GNO-WEL-00590.docx, TB-SCS-GNO-WEL-00590.docx",
            "PROVIDE_INFORMATION, TB-SCS-GNO-WEL-00468.docx, TB-SCS-GNO-WEL-00472.docx, TB-SCS-GNO-WEL-00468.docx",
            "GRANT_REINSTATEMENT, TB-SCS-GNO-WEL-00586.docx, TB-SCS-GNO-WEL-00586.docx, null",
            "REFUSE_REINSTATEMENT, TB-SCS-GNO-WEL-00587.docx, TB-SCS-GNO-WEL-00587.docx, null",
    })
    public void shouldGiveCorrectDocmosisIdForDirectionIssuedWelsh(DirectionType directionType, String configAppellantOrAppointee, String configRep, @Nullable String configJointParty) {

        NotificationWrapper wrapper = new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .newSscsCaseData(SscsCaseData.builder()
                        .languagePreferenceWelsh("Yes")
                        .directionTypeDl(new DynamicList(directionType.toString()))
                        .appeal(Appeal.builder()
                                .hearingType(HearingType.ONLINE.getValue())
                                .build())
                        .build())
                .notificationEventType(DIRECTION_ISSUED_WELSH)
                .build());

        Personalisation personalisation = new Personalisation();
        ReflectionTestUtils.setField(personalisation, "config", notificationConfig);

        Template templateAppellant = personalisation.getTemplate(wrapper, PIP, APPELLANT);
        assertNull(templateAppellant.getEmailTemplateId());
        assertTrue(templateAppellant.getSmsTemplateId().isEmpty());
        assertNull(templateAppellant.getLetterTemplateId());
        assertEquals(configAppellantOrAppointee, templateAppellant.getDocmosisTemplateId());
        Template templateAppointee = personalisation.getTemplate(wrapper, PIP, APPOINTEE);
        assertEquals(configAppellantOrAppointee, templateAppointee.getDocmosisTemplateId());
        Template templateRep = personalisation.getTemplate(wrapper, PIP, REPRESENTATIVE);
        assertEquals(configRep, templateRep.getDocmosisTemplateId());
        Template templateJointParty = personalisation.getTemplate(wrapper, PIP, JOINT_PARTY);
        assertEquals(configJointParty, templateJointParty.getDocmosisTemplateId());
    }


    @SuppressWarnings({"Indentation", "unused"})
    private Object[] templateIdsWithHearingAndEventTypesWelsh() {
        return new Object[]{
                new Object[]{"46b8b76c-3205-49e3-9d9d-14f793fcae7c", Arrays.asList("8b23d743-3a40-418a-bb2f-39c440e922ce", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"0bb57ae1-a130-4d18-9826-ea50644c4aca", Arrays.asList("bfee6b55-9718-41c6-904c-50ecff324c3e", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"0bb57ae1-a130-4d18-9826-ea50644c4aca", Arrays.asList("bfee6b55-9718-41c6-904c-50ecff324c3e", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPOINTEE), null},
                new Object[]{"659f201a-f97f-4053-ac4c-d7a48d14895e", Arrays.asList("7c6980fe-fa12-46a4-a9aa-c7553a163fba", "f1076482-a76d-4389-b411-9865373cfc42"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, REPRESENTATIVE), null},

                new Object[]{"690ed91a-1b1b-4f0e-896d-4dd26b21f6b9", Arrays.asList("0dd9bf61-81da-4a07-9dd5-35bc1e7471b0", "7d36718b-1193-4b3d-86bd-db54612c5363"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"ebdb95f8-3362-4273-8148-6484af82d3c8", Arrays.asList("a2745961-214a-4138-95b0-89300b395a3e", "5f91012e-0d3f-465b-b301-ee3ee5a50100"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"d1a0ccb4-7051-4e3c-b7af-b1dbeeacc921", Arrays.asList("00c36e89-6cf0-42a1-b080-6eac4edfca25", "7d36718b-1193-4b3d-86bd-db54612c5363"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"d1a0ccb4-7051-4e3c-b7af-b1dbeeacc921", Arrays.asList("00c36e89-6cf0-42a1-b080-6eac4edfca25", "7d36718b-1193-4b3d-86bd-db54612c5363"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPOINTEE), null},

                new Object[]{"974a452f-e5a5-4072-a326-87ad8b0793fb", Arrays.asList("e4a574f8-dd1e-4c31-8826-88fff5427db3", "505be856-ceca-4bbc-ba70-29024585056f"), null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPELLANT), null},
                new Object[]{"974a452f-e5a5-4072-a326-87ad8b0793fb", Arrays.asList("e4a574f8-dd1e-4c31-8826-88fff5427db3", "505be856-ceca-4bbc-ba70-29024585056f"), null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"5abc83d8-f6b8-4385-805b-ffbb0f64b84b", Arrays.asList("7e068c25-fc93-4997-831c-717a319730c1", "b2d187cd-089b-4fe1-b460-a310c0af46fe"), null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},

                new Object[]{"c35a367c-ad53-4c6a-899b-554763945894", Arrays.asList("434b1974-6392-43d4-a7d7-8673b85fcde1", "f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null, ORAL, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, APPELLANT), null},
                new Object[]{"c35a367c-ad53-4c6a-899b-554763945894", Arrays.asList("434b1974-6392-43d4-a7d7-8673b85fcde1", "f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null, ORAL, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, APPOINTEE), null},
                new Object[]{"95351d56-4af3-4d54-9941-ab2987d66bf3", Arrays.asList("2406597b-636b-4f35-9a5c-03d25f03546c", "9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874"), null, null, ORAL, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"265d1671-ddf1-418f-bc8d-8bb9c758a6b6", Arrays.asList("8e616088-951f-49ed-810b-178a78f02232", "f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null, ORAL, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"974a452f-e5a5-4072-a326-87ad8b0793fb", Arrays.asList("e4a574f8-dd1e-4c31-8826-88fff5427db3", "5e5cfe8d-b893-4f87-817f-9d05d22d657a"), null, null, PAPER, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, APPELLANT), null},
                new Object[]{"974a452f-e5a5-4072-a326-87ad8b0793fb", Arrays.asList("e4a574f8-dd1e-4c31-8826-88fff5427db3", "5e5cfe8d-b893-4f87-817f-9d05d22d657a"), null, null, PAPER, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, APPOINTEE), null},
                new Object[]{"0b7ccdac-0b8e-4f94-8829-77f3a2874485", Arrays.asList("7e068c25-fc93-4997-831c-717a319730c1", "b2d187cd-089b-4fe1-b460-a310c0af46fe"), null, null, PAPER, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"253e775f-8324-4242-9dee-7ff15d0b67fc", Arrays.asList("cf36c940-b9e1-4d25-aaed-3ea4362706fc", "15cd6837-e998-4bf9-a815-af3e98922d19"), null, null, PAPER, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, JOINT_PARTY), null},

                new Object[]{"db9fd58c-f206-43d6-82d1-b6a9c0b359e9", Arrays.asList("8336c537-7cbe-4232-912e-64dd10d20ad1", "e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"6e9cfdcc-b4b4-4d72-af40-f676d9be36c4", Arrays.asList("8336c537-7cbe-4232-912e-64dd10d20ad1", "e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"862590f1-3b36-4537-84c5-173dccea5db3", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"862590f1-3b36-4537-84c5-173dccea5db3", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, JOINT_PARTY), null},

                new Object[]{"155a6c34-2efa-46b2-9fc7-7a626b77bdc4", Arrays.asList("1e59e3e7-5ff9-4a5a-85b1-e8fcc88bbcf1","ede384aa-0b6e-4311-9f01-ee547573a07b"), "830ba7f1-9757-4833-8520-2f872de7be44", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), null},
                new Object[]{"155a6c34-2efa-46b2-9fc7-7a626b77bdc4", Arrays.asList("1e59e3e7-5ff9-4a5a-85b1-e8fcc88bbcf1","ede384aa-0b6e-4311-9f01-ee547573a07b"), "830ba7f1-9757-4833-8520-2f872de7be44", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), "validAppeal"},
                new Object[]{"155a6c34-2efa-46b2-9fc7-7a626b77bdc4", Arrays.asList("1e59e3e7-5ff9-4a5a-85b1-e8fcc88bbcf1","ede384aa-0b6e-4311-9f01-ee547573a07b"), "830ba7f1-9757-4833-8520-2f872de7be44", "TB-SCS-GNO-WEL-00467.docx", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), "readyToList"},

                new Object[]{"e7993ed9-982a-44eb-8459-932d6d7653ea", Arrays.asList("d4353bf1-404e-434a-af04-75b014f6f5f4","99bd4a56-256c-4de8-b187-d43a8dde466f"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e7993ed9-982a-44eb-8459-932d6d7653ea", Arrays.asList("d4353bf1-404e-434a-af04-75b014f6f5f4","99bd4a56-256c-4de8-b187-d43a8dde466f"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), "validAppeal"},
                new Object[]{"e7993ed9-982a-44eb-8459-932d6d7653ea", Arrays.asList("d4353bf1-404e-434a-af04-75b014f6f5f4","99bd4a56-256c-4de8-b187-d43a8dde466f"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", "TB-SCS-GNO-WEL-00471.docx", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), "readyToList"},

                new Object[]{"d87e01c6-1e03-47a2-9387-9b06ac536cba", Arrays.asList("1e59e3e7-5ff9-4a5a-85b1-e8fcc88bbcf1","ede384aa-0b6e-4311-9f01-ee547573a07b"), "95b0b0f8-fdf6-450f-b805-6aab46d63bf7", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"d87e01c6-1e03-47a2-9387-9b06ac536cba", Arrays.asList("1e59e3e7-5ff9-4a5a-85b1-e8fcc88bbcf1","ede384aa-0b6e-4311-9f01-ee547573a07b"), "95b0b0f8-fdf6-450f-b805-6aab46d63bf7", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), "validAppeal"},
                new Object[]{"d87e01c6-1e03-47a2-9387-9b06ac536cba", Arrays.asList("1e59e3e7-5ff9-4a5a-85b1-e8fcc88bbcf1","ede384aa-0b6e-4311-9f01-ee547573a07b"), "95b0b0f8-fdf6-450f-b805-6aab46d63bf7", "TB-SCS-GNO-WEL-00467.docx", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), "readyToList"},

                new Object[]{"952af9ab-c73c-4ef7-90ba-32d67ee1cc5f", Arrays.asList("8245da79-cc7a-4953-b814-4b06cc322c0a","e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT), null},
                new Object[]{"da1fc8d6-fe58-4594-8f1e-aa204b091073", Arrays.asList("8245da79-cc7a-4953-b814-4b06cc322c0a","e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"952af9ab-c73c-4ef7-90ba-32d67ee1cc5f", Arrays.asList("8245da79-cc7a-4953-b814-4b06cc322c0a","e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},

                new Object[]{"7fd05871-f37b-410e-bcf4-1cb5f806ebd9", Arrays.asList("851f35a9-82f8-472f-aba4-c418210aefb5","345f802b-7089-4f46-a17f-bf534b272740"), null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"80ca3d0b-e650-46ad-899e-2ec35f5654ac", Arrays.asList("851f35a9-82f8-472f-aba4-c418210aefb5","345f802b-7089-4f46-a17f-bf534b272740"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"46b8b76c-3205-49e3-9d9d-14f793fcae7c", Arrays.asList("8b23d743-3a40-418a-bb2f-39c440e922ce", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, JOINT_PARTY), null},

                new Object[]{"06c91850-a81f-44bb-9577-1bc528913850", Arrays.asList("a26c0e22-b132-4528-86a5-e221cc9b6325","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT), null},
                new Object[]{"06c91850-a81f-44bb-9577-1bc528913850", Arrays.asList("a26c0e22-b132-4528-86a5-e221cc9b6325","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT), null},
                new Object[]{"6b1b836a-a7a2-4a0f-b5de-6988ac2a9e34", Arrays.asList("4d1caa6f-b0e8-4c07-aa76-84841880be9a","259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"6b1b836a-a7a2-4a0f-b5de-6988ac2a9e34", Arrays.asList("4d1caa6f-b0e8-4c07-aa76-84841880be9a","259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"20b4e9de-d80d-4e0d-9cc1-28093072833b", Arrays.asList("4490f1c9-0b5c-46a9-8b79-75f07dfef810","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"20b4e9de-d80d-4e0d-9cc1-28093072833b", Arrays.asList("4490f1c9-0b5c-46a9-8b79-75f07dfef810","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, JOINT_PARTY), null},

                new Object[]{"ab60afc3-7ad4-4de4-84da-e095e90d6f37", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT), null},
                new Object[]{"ab60afc3-7ad4-4de4-84da-e095e90d6f37", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT), null},
                new Object[]{"ab60afc3-7ad4-4de4-84da-e095e90d6f37", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE), null},
                new Object[]{"ab60afc3-7ad4-4de4-84da-e095e90d6f37", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE), null},

                new Object[]{"d5bc541b-78c8-4a65-8e55-cfb20ccfed43", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"d5bc541b-78c8-4a65-8e55-cfb20ccfed43", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"a72272a6-1eaf-41f5-84c8-ec00debbf958", Arrays.asList("37cddbe4-bd2e-4027-967b-840d1f11bea8", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"a72272a6-1eaf-41f5-84c8-ec00debbf958", Arrays.asList("37cddbe4-bd2e-4027-967b-840d1f11bea8", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"9baea173-35d7-4d7d-b781-5d3dfffbbdba", Arrays.asList("a3585b50-7a4e-4b84-9a3a-c1323ef26d3a","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx", ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"dc8164a8-b923-4896-9308-3bf8e74d5665", Arrays.asList("3cc24409-b7be-4fea-8cdb-0288d151c265","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"dc8164a8-b923-4896-9308-3bf8e74d5665", Arrays.asList("3cc24409-b7be-4fea-8cdb-0288d151c265","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"dc8164a8-b923-4896-9308-3bf8e74d5665", Arrays.asList("3cc24409-b7be-4fea-8cdb-0288d151c265","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx", PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"dc8164a8-b923-4896-9308-3bf8e74d5665", Arrays.asList("3cc24409-b7be-4fea-8cdb-0288d151c265","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx", ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE), null},
                new Object[]{"3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", Arrays.asList("904549fa-8054-4ece-91d9-5f57f1e40654", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", Arrays.asList("904549fa-8054-4ece-91d9-5f57f1e40654", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", Arrays.asList("904549fa-8054-4ece-91d9-5f57f1e40654", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, JOINT_PARTY), null},
                new Object[]{"3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", Arrays.asList("904549fa-8054-4ece-91d9-5f57f1e40654", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, JOINT_PARTY), null},
                new Object[]{"31ed3c24-0fc8-45a7-8071-4f27f4009634", Arrays.asList("def78942-c9e2-4feb-aa2a-863a2ee7c6c5","f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-WEL-00661.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE), null},
                new Object[]{"31ed3c24-0fc8-45a7-8071-4f27f4009634", Arrays.asList("def78942-c9e2-4feb-aa2a-863a2ee7c6c5","f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-WEL-00661.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPELLANT), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPELLANT), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPOINTEE), null},
                new Object[]{"45681209-46d6-4525-89d6-506611e131f9", Arrays.asList("ce1c3a63-7235-4b0a-960a-4b03b9a6abd4","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPOINTEE), null},

                new Object[]{"a0aa060b-78ab-4380-b922-f78b5878515f", Arrays.asList("25a321e7-088b-40ac-9313-4c4f6471e682", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"1b13d247-a866-4dd8-b1a8-157b60b77a18", Arrays.asList("847f22c9-2b19-41d4-9939-18d552834fb4", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT), null},
                new Object[]{"1b13d247-a866-4dd8-b1a8-157b60b77a18", Arrays.asList("847f22c9-2b19-41d4-9939-18d552834fb4", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT), null},
                new Object[]{"bca17772-cf41-47e4-aaab-ec839e6d947e", Arrays.asList("1b4cd26e-5219-44c5-ba6c-b21e33ddbeba","e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"bca17772-cf41-47e4-aaab-ec839e6d947e", Arrays.asList("1b4cd26e-5219-44c5-ba6c-b21e33ddbeba","e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"1b13d247-a866-4dd8-b1a8-157b60b77a18", Arrays.asList("847f22c9-2b19-41d4-9939-18d552834fb4", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"1b13d247-a866-4dd8-b1a8-157b60b77a18", Arrays.asList("847f22c9-2b19-41d4-9939-18d552834fb4", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE), null},

                new Object[]{"e3fa02fd-87b2-471c-9a99-7020b367eca9", Arrays.asList("21cb5f96-41c0-45cd-adb1-899ac8dd96b2","f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-WEL-00467.docx", PAPER, getTemplateName(VALID_APPEAL_CREATED, APPELLANT), null},
                new Object[]{"e3fa02fd-87b2-471c-9a99-7020b367eca9", Arrays.asList("21cb5f96-41c0-45cd-adb1-899ac8dd96b2","f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-WEL-00467.docx", ORAL, getTemplateName(VALID_APPEAL_CREATED, APPELLANT), null},
                new Object[]{"e0355abd-42a6-4f94-836b-ec7ee22631cc", Arrays.asList("21cb5f96-41c0-45cd-adb1-899ac8dd96b2","f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-WEL-00467.docx", PAPER, getTemplateName(VALID_APPEAL_CREATED, APPOINTEE), null},
                new Object[]{"e0355abd-42a6-4f94-836b-ec7ee22631cc", Arrays.asList("21cb5f96-41c0-45cd-adb1-899ac8dd96b2","f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-WEL-00467.docx", ORAL, getTemplateName(VALID_APPEAL_CREATED, APPOINTEE), null},
                new Object[]{"364bccc5-ffa6-495d-8035-e642d15114bf", Arrays.asList("c2654642-ad70-4ec9-949f-3160ca151d53","a6c09fad-6265-4c7c-8b95-36245ffa5352"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e3fa02fd-87b2-471c-9a99-7020b367eca9", Arrays.asList("21cb5f96-41c0-45cd-adb1-899ac8dd96b2","f41222ef-c05c-4682-9634-6b034a166368"), "830ba7f1-9757-4833-8520-2f872de7be44", null, PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},

                new Object[]{"7a652620-de50-47f6-9c53-2d442d083a29", Arrays.asList("bd7a6731-d807-4af8-a89d-4e075aa2b514", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"e503b3df-cc95-4d2d-9723-0a4759187eae", Arrays.asList("1c6eeac6-dbec-4e42-92b3-7a9dfeec0125", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"e503b3df-cc95-4d2d-9723-0a4759187eae", Arrays.asList("1c6eeac6-dbec-4e42-92b3-7a9dfeec0125", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"e503b3df-cc95-4d2d-9723-0a4759187eae", Arrays.asList("1c6eeac6-dbec-4e42-92b3-7a9dfeec0125", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPOINTEE), null},
                new Object[]{"e503b3df-cc95-4d2d-9723-0a4759187eae", Arrays.asList("1c6eeac6-dbec-4e42-92b3-7a9dfeec0125", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPOINTEE), null},
                new Object[]{"5637c48e-84a0-4c72-841f-c55505c5e703", Arrays.asList("c72e53f4-9226-48a4-ae9f-24f73e7d5bba","bb3df0ea-8259-43c4-95de-9eef96206575"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"5637c48e-84a0-4c72-841f-c55505c5e703", Arrays.asList("c72e53f4-9226-48a4-ae9f-24f73e7d5bba","bb3df0ea-8259-43c4-95de-9eef96206575"), null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, REPRESENTATIVE), null},

                new Object[]{"fd852f77-19e4-4513-be21-3eada4d90bcb", Arrays.asList("3b984e01-47be-40de-8c67-c4588bc8ea7d","8b459c7d-c7b9-4293-9734-26341a231695"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT), null},
                new Object[]{"fd852f77-19e4-4513-be21-3eada4d90bcb", Arrays.asList("3b984e01-47be-40de-8c67-c4588bc8ea7d","8b459c7d-c7b9-4293-9734-26341a231695"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPOINTEE), null},
                new Object[]{"24c897bf-58b1-4710-870c-7870b3c91d3f", Arrays.asList("4063a888-5d9d-4ef0-ae04-492e138ceb92","8b459c7d-c7b9-4293-9734-26341a231695"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"49ebfb21-4bd0-4739-a7b4-57e1f70762b3", Arrays.asList("e02056c6-cff2-4ab1-8445-7e42ece655d2","4562984e-2854-4191-81d9-cffbe5111015"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"7e26960c-40d3-4430-a720-9da1917199fe", Collections.emptyList(), null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT), null},
                new Object[]{"7e26960c-40d3-4430-a720-9da1917199fe", Collections.emptyList(), null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPOINTEE), null},
                new Object[]{"a3c3ace2-6b88-4557-b18a-7df1923a50f5", Collections.emptyList(), null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"49ebfb21-4bd0-4739-a7b4-57e1f70762b3", Collections.emptyList(), null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00589.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00589.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00589.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00589.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00629.docx", ORAL, getTemplateName(JOINT_PARTY_ADDED, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00629.docx", PAPER, getTemplateName(JOINT_PARTY_ADDED, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00663.docx", ORAL, getTemplateName(NON_COMPLIANT_NOTIFICATION), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00663.docx", PAPER, getTemplateName(NON_COMPLIANT_NOTIFICATION), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00649.docx", ORAL, getTemplateName(ISSUE_ADJOURNMENT_NOTICE_WELSH, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00649.docx", PAPER, getTemplateName(ISSUE_ADJOURNMENT_NOTICE_WELSH, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00649.docx", PAPER, getTemplateName(ISSUE_ADJOURNMENT_NOTICE_WELSH, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00649.docx", ORAL, getTemplateName(ISSUE_ADJOURNMENT_NOTICE_WELSH, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00649.docx", PAPER, getTemplateName(ISSUE_ADJOURNMENT_NOTICE_WELSH, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00681.docx", PAPER, getTemplateName(ISSUE_FINAL_DECISION_WELSH, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00681.docx", PAPER, getTemplateName(ISSUE_FINAL_DECISION_WELSH, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-WEL-00682.docx", PAPER, getTemplateName(ISSUE_FINAL_DECISION_WELSH, REPRESENTATIVE), null},
                new Object[]{"020c9611-6547-4a66-a05b-eca7e9a415ae", Arrays.asList("3baff32f-8253-409c-8ef1-147ed05676cf", "7397a76f-14cb-468c-b1a7-0570940ead91"), null, null, ORAL, getTemplateName(SUBSCRIPTION_UPDATED_NOTIFICATION), null}
        };
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] templateIdsWithHearingAndEventTypes() {
        return new Object[]{
                new Object[]{"e1084d78-5e2d-45d2-a54f-84339da141c1", Arrays.asList("505be856-ceca-4bbc-ba70-29024585056f"), null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPELLANT), null},
                new Object[]{"e1084d78-5e2d-45d2-a54f-84339da141c1", Arrays.asList("505be856-ceca-4bbc-ba70-29024585056f"), null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"5abc83d8-f6b8-4385-805b-ffbb0f64b84b", Arrays.asList("b2d187cd-089b-4fe1-b460-a310c0af46fe"), null, null, PAPER, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"2c5644db-1f7b-429b-b10a-8b23a80ed26a", Arrays.asList("f20ffcb1-c5f0-4bff-b2d1-a1094f8014e6"), "8b11f3f4-6452-4a35-93d8-a94996af6499", null, ORAL, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"0d844af4-b390-42d7-94d5-4fd1ae9388d9", Arrays.asList("9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874"), "419beb1c-4f26-45e7-8db3-69bfe5e9224d", null, ORAL, getTemplateName(DWP_RESPONSE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"ffa58120-24e4-44cb-8026-0becf1416684", Arrays.asList("f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null, ORAL, getTemplateName(DWP_UPLOAD_RESPONSE_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"c507a630-9e6a-43c9-8e39-dcabdcffaf53", Arrays.asList("56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"c507a630-9e6a-43c9-8e39-dcabdcffaf53", Arrays.asList("56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"c507a630-9e6a-43c9-8e39-dcabdcffaf53", Arrays.asList("56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPOINTEE), null},
                new Object[]{"81fa38cc-b7cc-469c-8109-67c801dc9c84", Arrays.asList("f1076482-a76d-4389-b411-9865373cfc42"), null, null, PAPER, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"d994236b-d7c4-44ef-9627-12372bb0434a", Arrays.asList("7d36718b-1193-4b3d-86bd-db54612c5363"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"df0803aa-f804-49fe-a2ac-c27adc4bb585", Arrays.asList("5f91012e-0d3f-465b-b301-ee3ee5a50100"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"d994236b-d7c4-44ef-9627-12372bb0434a", Arrays.asList("7d36718b-1193-4b3d-86bd-db54612c5363"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"d994236b-d7c4-44ef-9627-12372bb0434a", Arrays.asList("7d36718b-1193-4b3d-86bd-db54612c5363"), null, null, ORAL, getTemplateName(EVIDENCE_REMINDER_NOTIFICATION, APPOINTEE), null},
                new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", Arrays.asList("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, "TB-SCS-GNO-ENG-00079.doc", PAPER, getTemplateName(VALID_APPEAL_CREATED, REPRESENTATIVE), null},
                new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", Arrays.asList("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, "TB-SCS-GNO-ENG-00079.doc", ORAL, getTemplateName(VALID_APPEAL_CREATED, REPRESENTATIVE), null},
                new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(VALID_APPEAL_CREATED, APPELLANT), null},
                new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(VALID_APPEAL_CREATED, APPELLANT), null},
                new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(VALID_APPEAL_CREATED, APPOINTEE), null},
                new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(VALID_APPEAL_CREATED, APPOINTEE), null},
                new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", Arrays.asList("a6c09fad-6265-4c7c-8b95-36245ffa5352"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
                new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
                new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null, PAPER, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null, ORAL, getTemplateName(SYA_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", Arrays.asList("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", Arrays.asList("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", Arrays.asList("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", Arrays.asList("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(HMCTS_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", Arrays.asList("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e93dd744-84a1-4173-847a-6d023b55637f", Arrays.asList("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx", PAPER, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, APPELLANT), null},
                new Object[]{"8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", Arrays.asList("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx", ORAL, getTemplateName(DWP_APPEAL_LAPSED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", Arrays.asList("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", Arrays.asList("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPELLANT), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, APPOINTEE), null},
                new Object[]{"6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", Arrays.asList("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", Arrays.asList("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(APPEAL_WITHDRAWN_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", Arrays.asList("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE), null},
                new Object[]{"e29a2275-553f-4e70-97f4-2994c095f281", Arrays.asList("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, REPRESENTATIVE), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPELLANT), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPELLANT), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPOINTEE), null},
                new Object[]{"8620e023-f663-477e-a771-9cfad50ee30f", Arrays.asList("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, APPOINTEE), null},
                new Object[]{"6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", Arrays.asList("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx", PAPER, getTemplateName(ADMIN_APPEAL_WITHDRAWN, JOINT_PARTY), null},
                new Object[]{"6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", Arrays.asList("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx", ORAL, getTemplateName(ADMIN_APPEAL_WITHDRAWN, JOINT_PARTY), null},
                new Object[]{"ecf7db7d-a257-4496-a2bf-768e560c80e7", Arrays.asList("259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"ecf7db7d-a257-4496-a2bf-768e560c80e7", Arrays.asList("259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"77ea995b-9744-4167-9250-e627c85e5eda", Arrays.asList("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT), null},
                new Object[]{"77ea995b-9744-4167-9250-e627c85e5eda", Arrays.asList("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, APPELLANT), null},
                new Object[]{"77ea995b-9744-4167-9250-e627c85e5eda", Arrays.asList("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, PAPER, getTemplateName(ADJOURNED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"77ea995b-9744-4167-9250-e627c85e5eda", Arrays.asList("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null, ORAL, getTemplateName(ADJOURNED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"8509fb1b-eb15-449f-b4ee-15ce286ab404", Arrays.asList("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT), null},
                new Object[]{"bd78cbc4-27d3-4692-a491-6c1770df174e", Arrays.asList("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPELLANT), null},
                new Object[]{"7af36950-fc63-45d1-907d-f472fac7af06", Arrays.asList("345f802b-7089-4f46-a17f-bf534b272740"), null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"30260c0b-5575-4f4e-bce4-73cf3f245c2d", Arrays.asList("345f802b-7089-4f46-a17f-bf534b272740"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"8509fb1b-eb15-449f-b4ee-15ce286ab404", Arrays.asList("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"bd78cbc4-27d3-4692-a491-6c1770df174e", Arrays.asList("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"8509fb1b-eb15-449f-b4ee-15ce286ab404", Arrays.asList("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, PAPER, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"bd78cbc4-27d3-4692-a491-6c1770df174e", Arrays.asList("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null, ORAL, getTemplateName(EVIDENCE_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"07bebee4-f07a-4a0d-9c50-65be30dc72a5", Arrays.asList("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"07bebee4-f07a-4a0d-9c50-65be30dc72a5", Arrays.asList("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPELLANT), null},
                new Object[]{"07bebee4-f07a-4a0d-9c50-65be30dc72a5", Arrays.asList("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"07bebee4-f07a-4a0d-9c50-65be30dc72a5", Arrays.asList("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"07bebee4-f07a-4a0d-9c50-65be30dc72a5", Arrays.asList("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPOINTEE), null},
                new Object[]{"07bebee4-f07a-4a0d-9c50-65be30dc72a5", Arrays.asList("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, APPOINTEE), null},
                new Object[]{"97c58e23-c11f-40b3-b981-2d4cfa38b8fd", Arrays.asList("bb3df0ea-8259-43c4-95de-9eef96206575"), null, null, PAPER, getTemplateName(HEARING_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"97c58e23-c11f-40b3-b981-2d4cfa38b8fd", Arrays.asList("bb3df0ea-8259-43c4-95de-9eef96206575"), null, null, ORAL, getTemplateName(HEARING_REMINDER_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"aa0930a3-e1bd-4b50-ac6b-34df73ec8378", Arrays.asList("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT), null},
                new Object[]{"aa0930a3-e1bd-4b50-ac6b-34df73ec8378", Arrays.asList("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPELLANT), null},
                new Object[]{"aa0930a3-e1bd-4b50-ac6b-34df73ec8378", Arrays.asList("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"aa0930a3-e1bd-4b50-ac6b-34df73ec8378", Arrays.asList("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", Arrays.asList("e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", Arrays.asList("e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"aa0930a3-e1bd-4b50-ac6b-34df73ec8378", Arrays.asList("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, PAPER, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"aa0930a3-e1bd-4b50-ac6b-34df73ec8378", Arrays.asList("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, null, ORAL, getTemplateName(HEARING_BOOKED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"b74ea5d4-dba2-4148-b822-d102cedbea12", Arrays.asList("4562984e-2854-4191-81d9-cffbe5111015"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"976bdb6c-8a86-48cf-9e0f-7989acaec0c2", Arrays.asList("8b459c7d-c7b9-4293-9734-26341a231695"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT), null},
                new Object[]{"976bdb6c-8a86-48cf-9e0f-7989acaec0c2", Arrays.asList("8b459c7d-c7b9-4293-9734-26341a231695"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPOINTEE), null},
                new Object[]{"976bdb6c-8a86-48cf-9e0f-7989acaec0c2", Arrays.asList("8b459c7d-c7b9-4293-9734-26341a231695"), null, null, PAPER, getTemplateName(APPEAL_DORMANT_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", Arrays.asList("99bd4a56-256c-4de8-b187-d43a8dde466f"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", Arrays.asList("99bd4a56-256c-4de8-b187-d43a8dde466f"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), "validAppeal"},
                new Object[]{"4b1ee55b-abd1-4e7e-b0ed-693d8df1e741", Arrays.asList("99bd4a56-256c-4de8-b187-d43a8dde466f"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", "TB-SCS-GNO-ENG-00079.doc", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, REPRESENTATIVE), "readyToList"},
                new Object[]{"78cf9c9c-e2b8-44d7-bcf1-220311f114cb", Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"78cf9c9c-e2b8-44d7-bcf1-220311f114cb", Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), "validAppeal"},
                new Object[]{"78cf9c9c-e2b8-44d7-bcf1-220311f114cb", Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b"), "747d026e-1bec-4e96-8a34-28f36e30bba5", "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPOINTEE), "readyToList"},
                new Object[]{"d5fd9f65-1283-4533-a1be-10043dae7af6", Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, PAPER, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), null},
                new Object[]{"d5fd9f65-1283-4533-a1be-10043dae7af6", Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null, ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), "validAppeal"},
                new Object[]{"d5fd9f65-1283-4533-a1be-10043dae7af6", Arrays.asList("ede384aa-0b6e-4311-9f01-ee547573a07b"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(APPEAL_RECEIVED_NOTIFICATION, APPELLANT), "readyToList"},
                new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", Arrays.asList("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, "TB-SCS-GNO-ENG-00079.doc", PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"652753bf-59b4-46eb-9c24-bd762338a098", Arrays.asList("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, "TB-SCS-GNO-ENG-00079.doc", ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
                new Object[]{"01293b93-b23e-40a3-ad78-2c6cd01cd21c", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPELLANT), null},
                new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", PAPER, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"362d9a85-e0e4-412b-b874-020c0464e2b4", Arrays.asList("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-GNO-ENG-00060.doc", ORAL, getTemplateName(RESEND_APPEAL_CREATED_NOTIFICATION, APPOINTEE), null},
                new Object[]{"e07b7dba-f383-49ca-a0ba-b5b61be27da6", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"e07b7dba-f383-49ca-a0ba-b5b61be27da6", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"732ec1a2-243f-4047-b963-e8427cb007b8", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT), null},
                new Object[]{"732ec1a2-243f-4047-b963-e8427cb007b8", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPELLANT), null},
                new Object[]{"732ec1a2-243f-4047-b963-e8427cb007b8", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE), null},
                new Object[]{"732ec1a2-243f-4047-b963-e8427cb007b8", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, APPOINTEE), null},
                new Object[]{"732ec1a2-243f-4047-b963-e8427cb007b8", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(POSTPONEMENT_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"732ec1a2-243f-4047-b963-e8427cb007b8", Collections.EMPTY_LIST, null, null, PAPER, getTemplateName(POSTPONEMENT_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{"e2ee8609-7d56-4857-b3f8-79028e8960aa", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, REPRESENTATIVE), null},
                new Object[]{"1a2683d0-ca0f-4465-b25d-59d3d817750a", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPELLANT), null},
                new Object[]{"1a2683d0-ca0f-4465-b25d-59d3d817750a", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, APPOINTEE), null},
                new Object[]{"1a2683d0-ca0f-4465-b25d-59d3d817750a", Collections.EMPTY_LIST, null, null, ORAL, getTemplateName(APPEAL_DORMANT_NOTIFICATION, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00662.docx", PAPER, getTemplateName(NON_COMPLIANT_NOTIFICATION), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00662.docx", ORAL, getTemplateName(NON_COMPLIANT_NOTIFICATION), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", ORAL, getTemplateName(REQUEST_INFO_INCOMPLETE, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", ORAL, getTemplateName(REQUEST_INFO_INCOMPLETE, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", ORAL, getTemplateName(REQUEST_INFO_INCOMPLETE, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", ORAL, getTemplateName(REQUEST_INFO_INCOMPLETE, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00452.docx", PAPER, getTemplateName(REQUEST_INFO_INCOMPLETE, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00094.docx", ORAL, getTemplateName(DECISION_ISSUED, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00094.docx", PAPER, getTemplateName(DECISION_ISSUED, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00095.docx", ORAL, getTemplateName(DECISION_ISSUED, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00095.docx", PAPER, getTemplateName(DECISION_ISSUED, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00454.docx", ORAL, getTemplateName(ISSUE_FINAL_DECISION, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00454.docx", PAPER, getTemplateName(ISSUE_FINAL_DECISION, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00455.docx", ORAL, getTemplateName(ISSUE_FINAL_DECISION, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00455.docx", PAPER, getTemplateName(ISSUE_FINAL_DECISION, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00510.docx", ORAL, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00510.docx", PAPER, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, APPOINTEE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00512.docx", ORAL, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00512.docx", PAPER, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00579.docx", ORAL, getTemplateName(JOINT_PARTY_ADDED, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00579.docx", PAPER, getTemplateName(JOINT_PARTY_ADDED, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00510.docx", ORAL, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, APPELLANT), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00510.docx", PAPER, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, JOINT_PARTY), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00512.docx", ORAL, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, REPRESENTATIVE), null},
                new Object[]{null, Collections.EMPTY_LIST, null, "TB-SCS-GNO-ENG-00510.docx", ORAL, getTemplateName(ISSUE_ADJOURNMENT_NOTICE, APPOINTEE), null},
                new Object[]{"b8b2904f-629d-42cf-acea-1b74bde5b2ff", Arrays.asList("7397a76f-14cb-468c-b1a7-0570940ead91"), null, null, ORAL, getTemplateName(SUBSCRIPTION_UPDATED_NOTIFICATION), null}

        };
    }


    @SuppressWarnings({"Indentation", "unused"})
    private Object[] bundledLetterTemplateNames() {
        List<SubscriptionType> subscriptionTypes = Arrays.asList(APPELLANT, APPOINTEE, REPRESENTATIVE, JOINT_PARTY);
        Object[] result = new Object[(BUNDLED_LETTER_EVENT_TYPES.size()) * subscriptionTypes.size() * 2];

        int i = 0;
        for (NotificationEventType eventType : BUNDLED_LETTER_EVENT_TYPES.stream()
                .filter(f -> !f.equals(DIRECTION_ISSUED))
                .filter(f -> !f.equals(DECISION_ISSUED))
                .filter(f -> !f.equals(ISSUE_FINAL_DECISION))
                .filter(f -> !f.equals(ISSUE_FINAL_DECISION_WELSH))
                .filter(f -> !f.equals(ISSUE_ADJOURNMENT_NOTICE))
                .filter(f -> !f.equals(ISSUE_ADJOURNMENT_NOTICE_WELSH))
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
