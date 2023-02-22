package uk.gov.hmcts.reform.sscs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.ORAL;
import static uk.gov.hmcts.reform.sscs.config.AppealHearingType.PAPER;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.Collections;
import java.util.List;
import junitparams.Parameters;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.domain.notify.Template;

public class NotificationConfigTestIt extends AbstractNotificationConfigTest {
    @Test
    @Parameters(method = "templateIdsWithHearingAndEventTypes")
    public void given_templateNamesAndHearingType_should_getCorrectTemplate(
        NotificationEventType eventType, boolean welsh,
        SubscriptionType subscriptionType, AppealHearingType hearingType, String createdInGapsFrom,
        String expectedEmailTemplateId, List<String> expectedSmsTemplateId,
        String expectedLetterTemplateId, String expectedDocmosisTemplateId) {

        Template template = getTemplate(eventType, subscriptionType, hearingType, welsh, createdInGapsFrom);

        assertThat(expectedEmailTemplateId).isEqualTo(template.getEmailTemplateId());
        assertThat(expectedSmsTemplateId).isEqualTo(template.getSmsTemplateId());
        assertThat(expectedLetterTemplateId).isEqualTo(template.getLetterTemplateId());
        assertThat(expectedDocmosisTemplateId).isEqualTo(template.getDocmosisTemplateId());
    }

    @SuppressWarnings({"unused"})
    private Object[] templateIdsWithHearingAndEventTypes() {
        return new Object[]{
            new Object[]{ADJOURNED, false, APPELLANT, ORAL, null, "77ea995b-9744-4167-9250-e627c85e5eda", List.of("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, false, APPELLANT, PAPER, null, "77ea995b-9744-4167-9250-e627c85e5eda", List.of("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, false, JOINT_PARTY, ORAL, null, "77ea995b-9744-4167-9250-e627c85e5eda", List.of("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, false, JOINT_PARTY, PAPER, null, "77ea995b-9744-4167-9250-e627c85e5eda", List.of("7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, false, REPRESENTATIVE, ORAL, null, "ecf7db7d-a257-4496-a2bf-768e560c80e7", List.of("259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null},
            new Object[]{ADJOURNED, false, REPRESENTATIVE, PAPER, null, "ecf7db7d-a257-4496-a2bf-768e560c80e7", List.of("259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null},
            new Object[]{ADJOURNED, true, APPELLANT, ORAL, null, "06c91850-a81f-44bb-9577-1bc528913850", List.of("1a6dba94-15b2-4251-9cba-8fb82f29308f","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, true, APPELLANT, PAPER, null, "06c91850-a81f-44bb-9577-1bc528913850", List.of("1a6dba94-15b2-4251-9cba-8fb82f29308f","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, true, JOINT_PARTY, ORAL, null, "20b4e9de-d80d-4e0d-9cc1-28093072833b", List.of("71d5714f-3467-482f-9615-3a68bf968c4e","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, true, JOINT_PARTY, PAPER, null, "20b4e9de-d80d-4e0d-9cc1-28093072833b", List.of("71d5714f-3467-482f-9615-3a68bf968c4e","7455de19-aa3b-48f0-b765-ab2757ba6a88"), null, null},
            new Object[]{ADJOURNED, true, REPRESENTATIVE, ORAL, null, "6b1b836a-a7a2-4a0f-b5de-6988ac2a9e34", List.of("5afa93bf-8c72-45da-a7db-e23f8e32906e","259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null},
            new Object[]{ADJOURNED, true, REPRESENTATIVE, PAPER, null, "6b1b836a-a7a2-4a0f-b5de-6988ac2a9e34", List.of("5afa93bf-8c72-45da-a7db-e23f8e32906e","259b8e81-b44a-4271-a57b-ba7f8bdfcb33"), null, null},

            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, APPELLANT, ORAL, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, APPELLANT, PAPER, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, APPOINTEE, ORAL, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, APPOINTEE, PAPER, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, JOINT_PARTY, ORAL, null, "6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", List.of("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, JOINT_PARTY, PAPER, null, "6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", List.of("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, REPRESENTATIVE, ORAL, null, "e29a2275-553f-4e70-97f4-2994c095f281", List.of("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, false, REPRESENTATIVE, PAPER, null, "e29a2275-553f-4e70-97f4-2994c095f281", List.of("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, APPELLANT, ORAL, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, APPELLANT, PAPER, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, APPOINTEE, ORAL, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, APPOINTEE, PAPER, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, JOINT_PARTY, ORAL, null, "3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", List.of("d43edb04-7563-406d-9d5a-a82ac983c065", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, JOINT_PARTY, PAPER, null, "3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", List.of("d43edb04-7563-406d-9d5a-a82ac983c065", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, REPRESENTATIVE, ORAL, null, "31ed3c24-0fc8-45a7-8071-4f27f4009634", List.of("66ff7a2c-7e22-4a2b-9120-00aea140db75","f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{ADMIN_APPEAL_WITHDRAWN, true, REPRESENTATIVE, PAPER, null, "31ed3c24-0fc8-45a7-8071-4f27f4009634", List.of("66ff7a2c-7e22-4a2b-9120-00aea140db75","f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-WEL-00661.docx"},

            new Object[]{APPEAL_DORMANT, false, APPELLANT, ORAL, null, "1a2683d0-ca0f-4465-b25d-59d3d817750a", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, false, APPELLANT, PAPER, null, "976bdb6c-8a86-48cf-9e0f-7989acaec0c2", List.of("8b459c7d-c7b9-4293-9734-26341a231695"), null, null},
            new Object[]{APPEAL_DORMANT, false, APPOINTEE, ORAL, null, "1a2683d0-ca0f-4465-b25d-59d3d817750a", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, false, APPOINTEE, PAPER, null, "976bdb6c-8a86-48cf-9e0f-7989acaec0c2", List.of("8b459c7d-c7b9-4293-9734-26341a231695"), null, null},
            new Object[]{APPEAL_DORMANT, false, JOINT_PARTY, ORAL, null, "1a2683d0-ca0f-4465-b25d-59d3d817750a", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, false, JOINT_PARTY, PAPER, null, "976bdb6c-8a86-48cf-9e0f-7989acaec0c2", List.of("8b459c7d-c7b9-4293-9734-26341a231695"), null, null},
            new Object[]{APPEAL_DORMANT, false, REPRESENTATIVE, ORAL, null, "e2ee8609-7d56-4857-b3f8-79028e8960aa", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, false, REPRESENTATIVE, PAPER, null, "b74ea5d4-dba2-4148-b822-d102cedbea12", List.of("4562984e-2854-4191-81d9-cffbe5111015"), null, null},
            new Object[]{APPEAL_DORMANT, true, APPELLANT, ORAL, null, "7e26960c-40d3-4430-a720-9da1917199fe", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, true, APPELLANT, PAPER, null, "fd852f77-19e4-4513-be21-3eada4d90bcb", List.of("f2dc22ab-632d-4b3b-8f8b-4f596103e5f7","8b459c7d-c7b9-4293-9734-26341a231695"), null, null},
            new Object[]{APPEAL_DORMANT, true, APPOINTEE, ORAL, null, "7e26960c-40d3-4430-a720-9da1917199fe", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, true, APPOINTEE, PAPER, null, "fd852f77-19e4-4513-be21-3eada4d90bcb", List.of("f2dc22ab-632d-4b3b-8f8b-4f596103e5f7","8b459c7d-c7b9-4293-9734-26341a231695"), null, null},
            new Object[]{APPEAL_DORMANT, true, JOINT_PARTY, ORAL, null, "a3c3ace2-6b88-4557-b18a-7df1923a50f5", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, true, JOINT_PARTY, PAPER, null, "24c897bf-58b1-4710-870c-7870b3c91d3f", List.of("579e7575-2519-425c-9158-ebc6f9ae404d","8b459c7d-c7b9-4293-9734-26341a231695"), null, null},
            new Object[]{APPEAL_DORMANT, true, REPRESENTATIVE, ORAL, null, "10e958ff-4ff4-4a38-b79c-298e89aa8079", Collections.emptyList(), null, null},
            new Object[]{APPEAL_DORMANT, true, REPRESENTATIVE, PAPER, null, "49ebfb21-4bd0-4739-a7b4-57e1f70762b3", List.of("62196001-5076-413d-a6da-208b61fdcc58","4562984e-2854-4191-81d9-cffbe5111015"), null, null},

            new Object[]{APPEAL_LAPSED, false, APPELLANT, ORAL, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, false, APPELLANT, PAPER, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, false, APPOINTEE, ORAL, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, false, APPOINTEE, PAPER, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, false, JOINT_PARTY, ORAL, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, false, JOINT_PARTY, PAPER, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, false, REPRESENTATIVE, ORAL, null, "e93dd744-84a1-4173-847a-6d023b55637f", List.of("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, false, REPRESENTATIVE, PAPER, null, "e93dd744-84a1-4173-847a-6d023b55637f", List.of("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{APPEAL_LAPSED, true, APPELLANT, ORAL, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{APPEAL_LAPSED, true, APPELLANT, PAPER, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{APPEAL_LAPSED, true, APPOINTEE, ORAL, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{APPEAL_LAPSED, true, APPOINTEE, PAPER, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{APPEAL_LAPSED, true, JOINT_PARTY, ORAL, null, "73ff24ca-847c-407c-bbce-e615bd51ff98", List.of("7cb4a413-520e-47c2-9cba-4fdcfe987faf", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{APPEAL_LAPSED, true, JOINT_PARTY, PAPER, null, "73ff24ca-847c-407c-bbce-e615bd51ff98", List.of("7cb4a413-520e-47c2-9cba-4fdcfe987faf", "d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{APPEAL_LAPSED, true, REPRESENTATIVE, ORAL, null, "dc8164a8-b923-4896-9308-3bf8e74d5665", List.of("77180ecb-c7e8-407c-8d74-618ae37b47b6","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{APPEAL_LAPSED, true, REPRESENTATIVE, PAPER, null, "dc8164a8-b923-4896-9308-3bf8e74d5665", List.of("77180ecb-c7e8-407c-8d74-618ae37b47b6","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx"},

            new Object[]{APPEAL_RECEIVED, false, APPELLANT, ORAL, "readyToList", "d5fd9f65-1283-4533-a1be-10043dae7af6", List.of("ede384aa-0b6e-4311-9f01-ee547573a07b"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", "TB-SCS-LET-ENG-Appeal-Lodged-Appellant.docx"},
            new Object[]{APPEAL_RECEIVED, false, APPELLANT, ORAL, "validAppeal", "d5fd9f65-1283-4533-a1be-10043dae7af6", List.of("ede384aa-0b6e-4311-9f01-ee547573a07b"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null},
            new Object[]{APPEAL_RECEIVED, false, APPELLANT, PAPER, null, "d5fd9f65-1283-4533-a1be-10043dae7af6", List.of("ede384aa-0b6e-4311-9f01-ee547573a07b"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null},
            new Object[]{APPEAL_RECEIVED, false, APPOINTEE, ORAL, "readyToList", "78cf9c9c-e2b8-44d7-bcf1-220311f114cb", List.of("ede384aa-0b6e-4311-9f01-ee547573a07b"), "747d026e-1bec-4e96-8a34-28f36e30bba5", "TB-SCS-LET-ENG-Appeal-Lodged-Appellant.docx"},
            new Object[]{APPEAL_RECEIVED, false, APPOINTEE, ORAL, "validAppeal", "78cf9c9c-e2b8-44d7-bcf1-220311f114cb", List.of("ede384aa-0b6e-4311-9f01-ee547573a07b"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null},
            new Object[]{APPEAL_RECEIVED, false, APPOINTEE, PAPER, null, "78cf9c9c-e2b8-44d7-bcf1-220311f114cb", List.of("ede384aa-0b6e-4311-9f01-ee547573a07b"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null},
            new Object[]{APPEAL_RECEIVED, false, REPRESENTATIVE, ORAL, "readyToList", "3d05393a-e593-400a-963f-a26893a0b672", List.of("99bd4a56-256c-4de8-b187-d43a8dde466f"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", "TB-SCS-LET-ENG-Appeal-Lodged-Representative.docx"},
            new Object[]{APPEAL_RECEIVED, false, REPRESENTATIVE, ORAL, "validAppeal", "3d05393a-e593-400a-963f-a26893a0b672", List.of("99bd4a56-256c-4de8-b187-d43a8dde466f"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null},
            new Object[]{APPEAL_RECEIVED, false, REPRESENTATIVE, PAPER, null, "3d05393a-e593-400a-963f-a26893a0b672", List.of("99bd4a56-256c-4de8-b187-d43a8dde466f"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null},
            new Object[]{APPEAL_RECEIVED, true, APPELLANT, ORAL, "readyToList", "ba2654d8-3e76-43df-bf7d-f457d4d6819a", List.of("53369ca5-75a8-43a7-8e5e-4eda9df04001","ede384aa-0b6e-4311-9f01-ee547573a07b"), "830ba7f1-9757-4833-8520-2f872de7be44", "TB-SCS-LET-WEL-Appeal-Lodged-Appellant.docx"},
            new Object[]{APPEAL_RECEIVED, true, APPELLANT, ORAL, "validAppeal", "ba2654d8-3e76-43df-bf7d-f457d4d6819a", List.of("53369ca5-75a8-43a7-8e5e-4eda9df04001","ede384aa-0b6e-4311-9f01-ee547573a07b"), "830ba7f1-9757-4833-8520-2f872de7be44", null},
            new Object[]{APPEAL_RECEIVED, true, APPELLANT, PAPER, null, "ba2654d8-3e76-43df-bf7d-f457d4d6819a", List.of("53369ca5-75a8-43a7-8e5e-4eda9df04001","ede384aa-0b6e-4311-9f01-ee547573a07b"), "830ba7f1-9757-4833-8520-2f872de7be44", null},
            new Object[]{APPEAL_RECEIVED, true, APPOINTEE, ORAL, "readyToList", "c77d9768-5343-42a5-9333-7b399e3d7199", List.of("53369ca5-75a8-43a7-8e5e-4eda9df04001","ede384aa-0b6e-4311-9f01-ee547573a07b"), "95b0b0f8-fdf6-450f-b805-6aab46d63bf7", "TB-SCS-LET-WEL-Appeal-Lodged-Appellant.docx"},
            new Object[]{APPEAL_RECEIVED, true, APPOINTEE, ORAL, "validAppeal", "c77d9768-5343-42a5-9333-7b399e3d7199", List.of("53369ca5-75a8-43a7-8e5e-4eda9df04001","ede384aa-0b6e-4311-9f01-ee547573a07b"), "95b0b0f8-fdf6-450f-b805-6aab46d63bf7", null},
            new Object[]{APPEAL_RECEIVED, true, APPOINTEE, PAPER, null, "c77d9768-5343-42a5-9333-7b399e3d7199", List.of("53369ca5-75a8-43a7-8e5e-4eda9df04001","ede384aa-0b6e-4311-9f01-ee547573a07b"), "95b0b0f8-fdf6-450f-b805-6aab46d63bf7", null},
            new Object[]{APPEAL_RECEIVED, true, REPRESENTATIVE, ORAL, "readyToList", "e7993ed9-982a-44eb-8459-932d6d7653ea", List.of("6970bdda-7293-46d6-8876-7e1a7c4295fc","99bd4a56-256c-4de8-b187-d43a8dde466f"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", "TB-SCS-LET-WEL-Appeal-Lodged-Representative.docx"},
            new Object[]{APPEAL_RECEIVED, true, REPRESENTATIVE, ORAL, "validAppeal", "e7993ed9-982a-44eb-8459-932d6d7653ea", List.of("6970bdda-7293-46d6-8876-7e1a7c4295fc","99bd4a56-256c-4de8-b187-d43a8dde466f"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", null},
            new Object[]{APPEAL_RECEIVED, true, REPRESENTATIVE, PAPER, null, "e7993ed9-982a-44eb-8459-932d6d7653ea", List.of("6970bdda-7293-46d6-8876-7e1a7c4295fc","99bd4a56-256c-4de8-b187-d43a8dde466f"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", null},

            new Object[]{APPEAL_WITHDRAWN, false, APPELLANT, ORAL, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, false, APPELLANT, PAPER, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, false, APPOINTEE, ORAL, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, false, APPOINTEE, PAPER, null, "8620e023-f663-477e-a771-9cfad50ee30f", List.of("446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, false, JOINT_PARTY, ORAL, null, "6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", List.of("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, false, JOINT_PARTY, PAPER, null, "6ce5e7b0-b94f-4f6e-878b-012ec0ee17d1", List.of("c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, false, REPRESENTATIVE, ORAL, null, "e29a2275-553f-4e70-97f4-2994c095f281", List.of("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, false, REPRESENTATIVE, PAPER, null, "e29a2275-553f-4e70-97f4-2994c095f281", List.of("f59440ee-19ca-4d47-a702-13e9cecaccbd"), null, "TB-SCS-GNO-ENG-00659.docx"},
            new Object[]{APPEAL_WITHDRAWN, true, APPELLANT, ORAL, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{APPEAL_WITHDRAWN, true, APPELLANT, PAPER, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{APPEAL_WITHDRAWN, true, APPOINTEE, ORAL, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{APPEAL_WITHDRAWN, true, APPOINTEE, PAPER, null, "45681209-46d6-4525-89d6-506611e131f9", List.of("0a269295-bf72-4ce1-ad99-9ff616fca0ae","446c7b23-7342-42e1-adff-b4c367e951cb"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{APPEAL_WITHDRAWN, true, JOINT_PARTY, ORAL, null, "3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", List.of("d43edb04-7563-406d-9d5a-a82ac983c065", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx"},
            new Object[]{APPEAL_WITHDRAWN, true, JOINT_PARTY, PAPER, null, "3a43dc4c-80d8-4bce-8290-26c08bfc0ef8", List.of("d43edb04-7563-406d-9d5a-a82ac983c065", "c4db4fca-6876-4130-b4eb-09e900ae45a8"), null, "TB-SCS-GNO-WEL-00661.docx"},

            new Object[]{DECISION_ISSUED, false, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00094.docx"},
            new Object[]{DECISION_ISSUED, false, APPOINTEE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00094.docx"},
            new Object[]{DECISION_ISSUED, false, REPRESENTATIVE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00094.docx"},
            new Object[]{DECISION_ISSUED, false, REPRESENTATIVE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00094.docx"},

            new Object[]{DECISION_ISSUED_WELSH, true, REPRESENTATIVE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00474.docx"},

            new Object[]{DWP_APPEAL_LAPSED, false, APPELLANT, ORAL, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{DWP_APPEAL_LAPSED, false, APPELLANT, PAPER, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{DWP_APPEAL_LAPSED, false, APPOINTEE, ORAL, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{DWP_APPEAL_LAPSED, false, REPRESENTATIVE, ORAL, null, "e93dd744-84a1-4173-847a-6d023b55637f", List.of("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{DWP_APPEAL_LAPSED, false, REPRESENTATIVE, PAPER, null, "e93dd744-84a1-4173-847a-6d023b55637f", List.of("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx"},

            new Object[]{DWP_RESPONSE_RECEIVED, false, APPELLANT, PAPER, null, "e1084d78-5e2d-45d2-a54f-84339da141c1", List.of("505be856-ceca-4bbc-ba70-29024585056f"), null, null},
            new Object[]{DWP_RESPONSE_RECEIVED, false, APPOINTEE, ORAL, null, "2c5644db-1f7b-429b-b10a-8b23a80ed26a", List.of("f20ffcb1-c5f0-4bff-b2d1-a1094f8014e6"), "8b11f3f4-6452-4a35-93d8-a94996af6499", null},
            new Object[]{DWP_RESPONSE_RECEIVED, false, APPOINTEE, PAPER, null, "e1084d78-5e2d-45d2-a54f-84339da141c1", List.of("505be856-ceca-4bbc-ba70-29024585056f"), null, null},
            new Object[]{DWP_RESPONSE_RECEIVED, false, REPRESENTATIVE, ORAL, null, "0d844af4-b390-42d7-94d5-4fd1ae9388d9", List.of("9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874"), "419beb1c-4f26-45e7-8db3-69bfe5e9224d", null},
            new Object[]{DWP_RESPONSE_RECEIVED, false, REPRESENTATIVE, PAPER, null, "5abc83d8-f6b8-4385-805b-ffbb0f64b84b", List.of("b2d187cd-089b-4fe1-b460-a310c0af46fe"), null, null},
            new Object[]{DWP_RESPONSE_RECEIVED, true, APPELLANT, PAPER, null, "974a452f-e5a5-4072-a326-87ad8b0793fb", List.of("7257266e-b02f-4091-a360-70e4b231124f", "505be856-ceca-4bbc-ba70-29024585056f"), null, null},
            new Object[]{DWP_RESPONSE_RECEIVED, true, APPOINTEE, PAPER, null, "974a452f-e5a5-4072-a326-87ad8b0793fb", List.of("7257266e-b02f-4091-a360-70e4b231124f", "505be856-ceca-4bbc-ba70-29024585056f"), null, null},
            new Object[]{DWP_RESPONSE_RECEIVED, true, REPRESENTATIVE, PAPER, null, "5abc83d8-f6b8-4385-805b-ffbb0f64b84b", List.of("cca7c565-c907-405f-b778-735b31947b85", "b2d187cd-089b-4fe1-b460-a310c0af46fe"), null, null},

            new Object[]{DWP_UPLOAD_RESPONSE, false, JOINT_PARTY, ORAL, null, "ffa58120-24e4-44cb-8026-0becf1416684", List.of("f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, false, OTHER_PARTY, ORAL, null, "0d844af4-b390-42d7-94d5-4fd1ae9388d9", List.of("9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, APPELLANT, ORAL, null, "c35a367c-ad53-4c6a-899b-554763945894", List.of("4cd7a59c-fd8f-464a-86a5-0c2a701b88f0", "f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, APPELLANT, PAPER, null, "974a452f-e5a5-4072-a326-87ad8b0793fb", List.of("7257266e-b02f-4091-a360-70e4b231124f", "5e5cfe8d-b893-4f87-817f-9d05d22d657a"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, APPOINTEE, ORAL, null, "c35a367c-ad53-4c6a-899b-554763945894", List.of("4cd7a59c-fd8f-464a-86a5-0c2a701b88f0", "f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, APPOINTEE, PAPER, null, "974a452f-e5a5-4072-a326-87ad8b0793fb", List.of("7257266e-b02f-4091-a360-70e4b231124f", "5e5cfe8d-b893-4f87-817f-9d05d22d657a"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, JOINT_PARTY, ORAL, null, "265d1671-ddf1-418f-bc8d-8bb9c758a6b6", List.of("36f94562-df64-420d-93ce-3bb1d4f1de4a", "f0444380-a8a4-4805-b9c2-563d1bd199cd"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, JOINT_PARTY, PAPER, null, "253e775f-8324-4242-9dee-7ff15d0b67fc", List.of("71f667c7-561e-4c06-befd-2a3246f10dcc", "15cd6837-e998-4bf9-a815-af3e98922d19"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, OTHER_PARTY, ORAL, null, "95351d56-4af3-4d54-9941-ab2987d66bf3", List.of("0f3f501a-8f7b-427a-af61-f2f4ca301a0b", "9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, OTHER_PARTY, ORAL, null, "95351d56-4af3-4d54-9941-ab2987d66bf3", List.of("0f3f501a-8f7b-427a-af61-f2f4ca301a0b", "9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, REPRESENTATIVE, ORAL, null, "95351d56-4af3-4d54-9941-ab2987d66bf3", List.of("0f3f501a-8f7b-427a-af61-f2f4ca301a0b", "9fe3a4f1-b8e2-4aed-aafb-6360d6ba2874"), null, null},
            new Object[]{DWP_UPLOAD_RESPONSE, true, REPRESENTATIVE, PAPER, null, "0b7ccdac-0b8e-4f94-8829-77f3a2874485", List.of("cca7c565-c907-405f-b778-735b31947b85", "b2d187cd-089b-4fe1-b460-a310c0af46fe"), null, null},

            new Object[]{EVIDENCE_RECEIVED, false, APPELLANT, ORAL, null, "bd78cbc4-27d3-4692-a491-6c1770df174e", List.of("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, false, APPELLANT, PAPER, null, "8509fb1b-eb15-449f-b4ee-15ce286ab404", List.of("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, false, APPOINTEE, ORAL, null, "bd78cbc4-27d3-4692-a491-6c1770df174e", List.of("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, false, APPOINTEE, ORAL, null, "bd78cbc4-27d3-4692-a491-6c1770df174e", List.of("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, false, APPOINTEE, PAPER, null, "8509fb1b-eb15-449f-b4ee-15ce286ab404", List.of("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, false, APPOINTEE, PAPER, null, "8509fb1b-eb15-449f-b4ee-15ce286ab404", List.of("e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, false, REPRESENTATIVE, ORAL, null, "30260c0b-5575-4f4e-bce4-73cf3f245c2d", List.of("345f802b-7089-4f46-a17f-bf534b272740"), null, null},
            new Object[]{EVIDENCE_RECEIVED, false, REPRESENTATIVE, PAPER, null, "7af36950-fc63-45d1-907d-f472fac7af06", List.of("345f802b-7089-4f46-a17f-bf534b272740"), null, null},
            new Object[]{EVIDENCE_RECEIVED, true, APPELLANT, ORAL, null, "952af9ab-c73c-4ef7-90ba-32d67ee1cc5f", List.of("399dc6fe-9f62-4180-ac92-7331411d1d16","e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, true, APPOINTEE, ORAL, null, "952af9ab-c73c-4ef7-90ba-32d67ee1cc5f", List.of("399dc6fe-9f62-4180-ac92-7331411d1d16","e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, true, APPOINTEE, PAPER, null, "da1fc8d6-fe58-4594-8f1e-aa204b091073", List.of("399dc6fe-9f62-4180-ac92-7331411d1d16","e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, true, JOINT_PARTY, ORAL, null, "db9fd58c-f206-43d6-82d1-b6a9c0b359e9", List.of("40f2669c-4389-4ac9-82cf-e98a4a9dd0e2", "e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, true, JOINT_PARTY, PAPER, null, "6e9cfdcc-b4b4-4d72-af40-f676d9be36c4", List.of("40f2669c-4389-4ac9-82cf-e98a4a9dd0e2", "e7868511-3a1f-4b8e-8bb3-b36c2bd99799"), null, null},
            new Object[]{EVIDENCE_RECEIVED, true, REPRESENTATIVE, ORAL, null, "80ca3d0b-e650-46ad-899e-2ec35f5654ac", List.of("e163e9c2-10a9-4426-9eb8-f83df490d6d4","345f802b-7089-4f46-a17f-bf534b272740"), null, null},
            new Object[]{EVIDENCE_RECEIVED, true, REPRESENTATIVE, PAPER, null, "7fd05871-f37b-410e-bcf4-1cb5f806ebd9", List.of("e163e9c2-10a9-4426-9eb8-f83df490d6d4","345f802b-7089-4f46-a17f-bf534b272740"), null, null},

            new Object[]{EVIDENCE_REMINDER, false, APPELLANT, ORAL, null, "d994236b-d7c4-44ef-9627-12372bb0434a", List.of("7d36718b-1193-4b3d-86bd-db54612c5363"), null, null},
            new Object[]{EVIDENCE_REMINDER, false, APPELLANT, PAPER, null, "c507a630-9e6a-43c9-8e39-dcabdcffaf53", List.of("56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null},
            new Object[]{EVIDENCE_REMINDER, false, APPOINTEE, ORAL, null, "d994236b-d7c4-44ef-9627-12372bb0434a", List.of("7d36718b-1193-4b3d-86bd-db54612c5363"), null, null},
            new Object[]{EVIDENCE_REMINDER, false, APPOINTEE, PAPER, null, "c507a630-9e6a-43c9-8e39-dcabdcffaf53", List.of("56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null},
            new Object[]{EVIDENCE_REMINDER, false, JOINT_PARTY, ORAL, null, "d994236b-d7c4-44ef-9627-12372bb0434a", List.of("7d36718b-1193-4b3d-86bd-db54612c5363"), null, null},
            new Object[]{EVIDENCE_REMINDER, false, JOINT_PARTY, PAPER, null, "c507a630-9e6a-43c9-8e39-dcabdcffaf53", List.of("56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null},
            new Object[]{EVIDENCE_REMINDER, false, REPRESENTATIVE, ORAL, null, "df0803aa-f804-49fe-a2ac-c27adc4bb585", List.of("5f91012e-0d3f-465b-b301-ee3ee5a50100"), null, null},
            new Object[]{EVIDENCE_REMINDER, false, REPRESENTATIVE, PAPER, null, "81fa38cc-b7cc-469c-8109-67c801dc9c84", List.of("f1076482-a76d-4389-b411-9865373cfc42"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, APPELLANT, ORAL, null, "d1a0ccb4-7051-4e3c-b7af-b1dbeeacc921", List.of("7be3e078-d602-485a-8963-0c7ef28476c6", "7d36718b-1193-4b3d-86bd-db54612c5363"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, APPELLANT, PAPER, null, "0bb57ae1-a130-4d18-9826-ea50644c4aca", List.of("594abfc5-39da-4255-aecf-fe043e3ee9a1", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, APPOINTEE, ORAL, null, "d1a0ccb4-7051-4e3c-b7af-b1dbeeacc921", List.of("7be3e078-d602-485a-8963-0c7ef28476c6", "7d36718b-1193-4b3d-86bd-db54612c5363"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, APPOINTEE, PAPER, null, "0bb57ae1-a130-4d18-9826-ea50644c4aca", List.of("594abfc5-39da-4255-aecf-fe043e3ee9a1", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, JOINT_PARTY, ORAL, null, "690ed91a-1b1b-4f0e-896d-4dd26b21f6b9", List.of("b6609bf1-1d9b-4f5f-bf24-a45dfbd7373f", "7d36718b-1193-4b3d-86bd-db54612c5363"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, JOINT_PARTY, PAPER, null, "46b8b76c-3205-49e3-9d9d-14f793fcae7c", List.of("d03f993e-8eae-4212-8a6b-168f132953f1", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, JOINT_PARTY, PAPER, null, "46b8b76c-3205-49e3-9d9d-14f793fcae7c", List.of("d03f993e-8eae-4212-8a6b-168f132953f1", "56a6c0c8-a251-482d-be83-95a7a1bf528c"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, REPRESENTATIVE, ORAL, null, "ebdb95f8-3362-4273-8148-6484af82d3c8", List.of("56d5f947-c4a1-4bb8-af9f-ab0e886f73c2", "5f91012e-0d3f-465b-b301-ee3ee5a50100"), null, null},
            new Object[]{EVIDENCE_REMINDER, true, REPRESENTATIVE, PAPER, null, "659f201a-f97f-4053-ac4c-d7a48d14895e", List.of("6537a9a1-1b9c-483f-bc6a-b492acd572a0", "f1076482-a76d-4389-b411-9865373cfc42"), null, null},

            new Object[]{HEARING_BOOKED, false, APPELLANT, ORAL, null, "aa0930a3-e1bd-4b50-ac6b-34df73ec8378", List.of("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, false, APPELLANT, PAPER, null, "aa0930a3-e1bd-4b50-ac6b-34df73ec8378", List.of("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, false, APPOINTEE, ORAL, null, "aa0930a3-e1bd-4b50-ac6b-34df73ec8378", List.of("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, false, APPOINTEE, PAPER, null, "aa0930a3-e1bd-4b50-ac6b-34df73ec8378", List.of("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, false, JOINT_PARTY, ORAL, null, "aa0930a3-e1bd-4b50-ac6b-34df73ec8378", List.of("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, false, JOINT_PARTY, PAPER, null, "aa0930a3-e1bd-4b50-ac6b-34df73ec8378", List.of("8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, false, REPRESENTATIVE, ORAL, null, "a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", List.of("e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, false, REPRESENTATIVE, PAPER, null, "a56e67cb-6b4b-41e3-8f4c-cd1cdb6809c1", List.of("e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, "TB-SCS-LET-ENG-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, true, APPELLANT, ORAL, null, "aca83c9c-05c0-49d5-8ded-e581a0107408", List.of("a51a7435-dffe-4455-9183-5c6f4f13bee3", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-WEL-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, true, APPELLANT, PAPER, null, "aca83c9c-05c0-49d5-8ded-e581a0107408", List.of("a51a7435-dffe-4455-9183-5c6f4f13bee3", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-WEL-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, true, APPOINTEE, ORAL, null, "aca83c9c-05c0-49d5-8ded-e581a0107408", List.of("a51a7435-dffe-4455-9183-5c6f4f13bee3", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-WEL-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, true, APPOINTEE, PAPER, null, "aca83c9c-05c0-49d5-8ded-e581a0107408", List.of("a51a7435-dffe-4455-9183-5c6f4f13bee3", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-WEL-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, true, JOINT_PARTY, ORAL, null, "1df83ea1-bdd5-4ce0-901d-769c3a8509cb", List.of("a11f7576-c44c-40f5-96cf-2920dce41bd5", "8aa77a9c-9bc6-424d-8716-1c948681270e"), null, "TB-SCS-LET-WEL-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, true, REPRESENTATIVE, ORAL, null, "c7047e95-42ac-4d6a-91d9-c8f31f2685b8", List.of("5e7444fe-860c-4623-b5c5-b23af1ddbb0b","e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, "TB-SCS-LET-WEL-Hearing-Booked.docx"},
            new Object[]{HEARING_BOOKED, true, REPRESENTATIVE, PAPER, null, "c7047e95-42ac-4d6a-91d9-c8f31f2685b8", List.of("5e7444fe-860c-4623-b5c5-b23af1ddbb0b","e04c548d-1ba9-40b5-bf9b-ea5e7bbadbac"), null, "TB-SCS-LET-WEL-Hearing-Booked.docx"},

            new Object[]{HEARING_REMINDER, false, APPELLANT, ORAL, null, "07bebee4-f07a-4a0d-9c50-65be30dc72a5", List.of("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, false, APPELLANT, PAPER, null, "07bebee4-f07a-4a0d-9c50-65be30dc72a5", List.of("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, false, APPOINTEE, ORAL, null, "07bebee4-f07a-4a0d-9c50-65be30dc72a5", List.of("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, false, APPOINTEE, PAPER, null, "07bebee4-f07a-4a0d-9c50-65be30dc72a5", List.of("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, false, JOINT_PARTY, ORAL, null, "07bebee4-f07a-4a0d-9c50-65be30dc72a5", List.of("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, false, JOINT_PARTY, PAPER, null, "07bebee4-f07a-4a0d-9c50-65be30dc72a5", List.of("18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, false, REPRESENTATIVE, ORAL, null, "97c58e23-c11f-40b3-b981-2d4cfa38b8fd", List.of("bb3df0ea-8259-43c4-95de-9eef96206575"), null, null},
            new Object[]{HEARING_REMINDER, false, REPRESENTATIVE, PAPER, null, "97c58e23-c11f-40b3-b981-2d4cfa38b8fd", List.of("bb3df0ea-8259-43c4-95de-9eef96206575"), null, null},
            new Object[]{HEARING_REMINDER, true, APPELLANT, ORAL, null, "07eb8f5f-3436-4c2e-9676-7ee213239cc5", List.of("adf60ff5-f83d-4931-9f35-fda2609afb7e", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, true, APPELLANT, PAPER, null, "07eb8f5f-3436-4c2e-9676-7ee213239cc5", List.of("adf60ff5-f83d-4931-9f35-fda2609afb7e", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, true, APPOINTEE, ORAL, null, "07eb8f5f-3436-4c2e-9676-7ee213239cc5", List.of("adf60ff5-f83d-4931-9f35-fda2609afb7e", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, true, APPOINTEE, PAPER, null, "07eb8f5f-3436-4c2e-9676-7ee213239cc5", List.of("adf60ff5-f83d-4931-9f35-fda2609afb7e", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, true, JOINT_PARTY, PAPER, null, "b2f8f063-bb06-494d-a59e-cc037106a9c3", List.of("534e7537-3012-42de-a6c3-9036b133c525", "18960596-1983-4da8-8b5c-dc1c851bb19b"), null, null},
            new Object[]{HEARING_REMINDER, true, REPRESENTATIVE, ORAL, null, "351c3d34-c436-4700-92ff-ff2954f5e17c", List.of("54f9c139-1c3d-4f4c-ae79-d75e4eafcfec","bb3df0ea-8259-43c4-95de-9eef96206575"), null, null},
            new Object[]{HEARING_REMINDER, true, REPRESENTATIVE, PAPER, null, "351c3d34-c436-4700-92ff-ff2954f5e17c", List.of("54f9c139-1c3d-4f4c-ae79-d75e4eafcfec","bb3df0ea-8259-43c4-95de-9eef96206575"), null, null},

            new Object[]{HMCTS_APPEAL_LAPSED, false, APPELLANT, ORAL, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, false, APPELLANT, PAPER, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null,"TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, false, APPOINTEE, ORAL, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, false, APPOINTEE, PAPER, null, "8ce8d794-75e8-49a0-b4d2-0c6cd2061c11", List.of("d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, false, REPRESENTATIVE, ORAL, null, "e93dd744-84a1-4173-847a-6d023b55637f", List.of("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, false, REPRESENTATIVE, PAPER, null, "e93dd744-84a1-4173-847a-6d023b55637f", List.of("ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-ENG-00656.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, true, APPELLANT, ORAL, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, true, APPELLANT, PAPER, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, true, APPOINTEE, ORAL, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, true, APPOINTEE, PAPER, null, "542a6346-7039-4eab-8b20-61fa77db33f0", List.of("4ab5141a-d6d6-43fe-bdf7-c9a7f99f3338","d2b4394b-d1c9-4d5c-a44e-b382e41c67e5"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, true, REPRESENTATIVE, ORAL, null, "dc8164a8-b923-4896-9308-3bf8e74d5665", List.of("77180ecb-c7e8-407c-8d74-618ae37b47b6","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx"},
            new Object[]{HMCTS_APPEAL_LAPSED, true, REPRESENTATIVE, PAPER, null, "dc8164a8-b923-4896-9308-3bf8e74d5665", List.of("77180ecb-c7e8-407c-8d74-618ae37b47b6","ee58f7d0-8de7-4bee-acd4-252213db6b7b"), null, "TB-SCS-GNO-WEL-00658.docx"},

            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00510.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00510.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, APPOINTEE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00510.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, APPOINTEE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00510.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, JOINT_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00510.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, REPRESENTATIVE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00512.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, REPRESENTATIVE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00512.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE, false, REPRESENTATIVE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00512.docx"},

            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, true, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00649.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, true, APPOINTEE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00649.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, true, JOINT_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00649.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, true, REPRESENTATIVE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00649.docx"},
            new Object[]{ISSUE_ADJOURNMENT_NOTICE_WELSH, true, REPRESENTATIVE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00649.docx"},

            new Object[]{ISSUE_FINAL_DECISION, false, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00454.docx"},
            new Object[]{ISSUE_FINAL_DECISION, false, APPOINTEE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00454.docx"},
            new Object[]{ISSUE_FINAL_DECISION, false, REPRESENTATIVE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00455.docx"},
            new Object[]{ISSUE_FINAL_DECISION, false, REPRESENTATIVE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00455.docx"},

            new Object[]{ISSUE_FINAL_DECISION_WELSH, true, APPELLANT, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00681.docx"},
            new Object[]{ISSUE_FINAL_DECISION_WELSH, true, APPOINTEE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00681.docx"},
            new Object[]{ISSUE_FINAL_DECISION_WELSH, true, REPRESENTATIVE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00682.docx"},

            new Object[]{JOINT_PARTY_ADDED, false, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00579.docx"},
            new Object[]{JOINT_PARTY_ADDED, false, JOINT_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00579.docx"},
            new Object[]{JOINT_PARTY_ADDED, true, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00629.docx"},
            new Object[]{JOINT_PARTY_ADDED, true, JOINT_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00629.docx"},

            new Object[]{NON_COMPLIANT, false, null, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00662.docx"},
            new Object[]{NON_COMPLIANT, false, null, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00662.docx"},
            new Object[]{NON_COMPLIANT, true, null, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00663.docx"},
            new Object[]{NON_COMPLIANT, true, null, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00663.docx"},

            new Object[]{REQUEST_INFO_INCOMPLETE, false, APPELLANT, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, false, APPELLANT, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, false, APPOINTEE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, false, APPOINTEE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, false, JOINT_PARTY, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, false, JOINT_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, false, REPRESENTATIVE, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, false, REPRESENTATIVE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-LET-ENG-Request-for-Information.docx"},
            
            new Object[]{REQUEST_INFO_INCOMPLETE, true, APPELLANT, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00589.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, true, APPOINTEE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00589.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, true, JOINT_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00589.docx"},
            new Object[]{REQUEST_INFO_INCOMPLETE, true, REPRESENTATIVE, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00589.docx"},

            new Object[]{RESEND_APPEAL_CREATED, false, APPELLANT, ORAL, null, "01293b93-b23e-40a3-ad78-2c6cd01cd21c", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-LET-ENG-Appeal-Lodged-Appellant.docx"},
            new Object[]{RESEND_APPEAL_CREATED, false, APPELLANT, PAPER, null, "01293b93-b23e-40a3-ad78-2c6cd01cd21c", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-LET-ENG-Appeal-Lodged-Appellant.docx"},
            new Object[]{RESEND_APPEAL_CREATED, false, APPOINTEE, ORAL, null, "362d9a85-e0e4-412b-b874-020c0464e2b4", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-LET-ENG-Appeal-Lodged-Appellant.docx"},
            new Object[]{RESEND_APPEAL_CREATED, false, APPOINTEE, PAPER, null, "362d9a85-e0e4-412b-b874-020c0464e2b4", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, "TB-SCS-LET-ENG-Appeal-Lodged-Appellant.docx"},
            new Object[]{RESEND_APPEAL_CREATED, false, REPRESENTATIVE, ORAL, null, "652753bf-59b4-46eb-9c24-bd762338a098", List.of("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, "TB-SCS-LET-ENG-Appeal-Lodged-Representative.docx"},
            new Object[]{RESEND_APPEAL_CREATED, false, REPRESENTATIVE, PAPER, null, "652753bf-59b4-46eb-9c24-bd762338a098", List.of("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, "TB-SCS-LET-ENG-Appeal-Lodged-Representative.docx"},

            new Object[]{SUBSCRIPTION_UPDATED, false, null, ORAL, null, "b8b2904f-629d-42cf-acea-1b74bde5b2ff", List.of("7397a76f-14cb-468c-b1a7-0570940ead91"), null, null},
            new Object[]{SUBSCRIPTION_UPDATED, true, null, ORAL, null, "020c9611-6547-4a66-a05b-eca7e9a415ae", List.of("c4ecf5b9-2190-4256-a525-0e5f5bee27d0", "7397a76f-14cb-468c-b1a7-0570940ead91"), null, null},

            new Object[]{SYA_APPEAL_CREATED, false, APPELLANT, ORAL, null, "01293b93-b23e-40a3-ad78-2c6cd01cd21c", List.of("f41222ef-c05c-4682-9634-6b034a166368"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null},
            new Object[]{SYA_APPEAL_CREATED, false, APPELLANT, PAPER, null, "01293b93-b23e-40a3-ad78-2c6cd01cd21c", List.of("f41222ef-c05c-4682-9634-6b034a166368"), "91143b85-dd9d-430c-ba23-e42ec90f44f8", null},
            new Object[]{SYA_APPEAL_CREATED, false, APPOINTEE, ORAL, null, "362d9a85-e0e4-412b-b874-020c0464e2b4", List.of("f41222ef-c05c-4682-9634-6b034a166368"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null},
            new Object[]{SYA_APPEAL_CREATED, false, APPOINTEE, ORAL, null, "362d9a85-e0e4-412b-b874-020c0464e2b4", List.of("f41222ef-c05c-4682-9634-6b034a166368"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null},
            new Object[]{SYA_APPEAL_CREATED, false, APPOINTEE, PAPER, null, "362d9a85-e0e4-412b-b874-020c0464e2b4", List.of("f41222ef-c05c-4682-9634-6b034a166368"), "747d026e-1bec-4e96-8a34-28f36e30bba5", null},
            new Object[]{SYA_APPEAL_CREATED, false, REPRESENTATIVE, ORAL, null, "652753bf-59b4-46eb-9c24-bd762338a098", List.of("a6c09fad-6265-4c7c-8b95-36245ffa5352"), "77ea8a2f-06df-4279-9c1f-0f23cb2d9bbf", null},
            new Object[]{SYA_APPEAL_CREATED, true, APPELLANT, PAPER, null, "e3fa02fd-87b2-471c-9a99-7020b367eca9", List.of("6a39acc9-7a3f-4ed7-a0b1-d30f6594fe82","f41222ef-c05c-4682-9634-6b034a166368"), "830ba7f1-9757-4833-8520-2f872de7be44", null},
            new Object[]{SYA_APPEAL_CREATED, true, REPRESENTATIVE, ORAL, null, "364bccc5-ffa6-495d-8035-e642d15114bf", List.of("f8391d2b-4d80-480e-87a9-816710f5650b","a6c09fad-6265-4c7c-8b95-36245ffa5352"), "89cbb9d8-3b7d-4766-b481-585832e8bd90", null},

            new Object[]{UPDATE_OTHER_PARTY_DATA, false, OTHER_PARTY, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00784.docx"},
            new Object[]{UPDATE_OTHER_PARTY_DATA, false, OTHER_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-ENG-00784.docx"},
            new Object[]{UPDATE_OTHER_PARTY_DATA, true, OTHER_PARTY, ORAL, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00787.docx"},
            new Object[]{UPDATE_OTHER_PARTY_DATA, true, OTHER_PARTY, PAPER, null, null, Collections.emptyList(), null, "TB-SCS-GNO-WEL-00787.docx"},

            new Object[]{VALID_APPEAL_CREATED, false, APPELLANT, ORAL, null, "9b5a6ac7-a1ad-4fc2-8e83-b0f32af9ff32", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, null},
            new Object[]{VALID_APPEAL_CREATED, false, APPELLANT, PAPER, null, "9b5a6ac7-a1ad-4fc2-8e83-b0f32af9ff32", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, null},
            new Object[]{VALID_APPEAL_CREATED, false, APPOINTEE, ORAL, null, "362d9a85-e0e4-412b-b874-020c0464e2b4", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, null},
            new Object[]{VALID_APPEAL_CREATED, false, APPOINTEE, PAPER, null, "362d9a85-e0e4-412b-b874-020c0464e2b4", List.of("f41222ef-c05c-4682-9634-6b034a166368"), null, null},
            new Object[]{VALID_APPEAL_CREATED, false, REPRESENTATIVE, ORAL, null, "652753bf-59b4-46eb-9c24-bd762338a098", List.of("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, null},
            new Object[]{VALID_APPEAL_CREATED, false, REPRESENTATIVE, PAPER, null, "652753bf-59b4-46eb-9c24-bd762338a098", List.of("a6c09fad-6265-4c7c-8b95-36245ffa5352"), null, null},
            new Object[]{VALID_APPEAL_CREATED, true, APPELLANT, ORAL, null, "e3fa02fd-87b2-471c-9a99-7020b367eca9", List.of("6a39acc9-7a3f-4ed7-a0b1-d30f6594fe82","f41222ef-c05c-4682-9634-6b034a166368"), null, null},
            new Object[]{VALID_APPEAL_CREATED, true, APPELLANT, PAPER, null, "e3fa02fd-87b2-471c-9a99-7020b367eca9", List.of("6a39acc9-7a3f-4ed7-a0b1-d30f6594fe82","f41222ef-c05c-4682-9634-6b034a166368"), null, null},
            new Object[]{VALID_APPEAL_CREATED, true, APPOINTEE, ORAL, null, "e0355abd-42a6-4f94-836b-ec7ee22631cc", List.of("6a39acc9-7a3f-4ed7-a0b1-d30f6594fe82","f41222ef-c05c-4682-9634-6b034a166368"), null, null},
            new Object[]{VALID_APPEAL_CREATED, true, APPOINTEE, PAPER, null, "e0355abd-42a6-4f94-836b-ec7ee22631cc", List.of("6a39acc9-7a3f-4ed7-a0b1-d30f6594fe82","f41222ef-c05c-4682-9634-6b034a166368"), null, null},
        };
    }
}
