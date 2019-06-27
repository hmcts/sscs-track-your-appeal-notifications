package uk.gov.hmcts.reform.sscs.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;

public class BundledLetterTemplateUtilTest {

    private String strikeoutTemplate;
    private String strikeoutRepTemplate;
    private String directionTemplate;
    private String directionRepTemplate;
    private String validAppealCreatedLetterTemplate;
    private String validAppealCreatedLetterTemplateRep;
    private BundledLetterTemplateUtil bundledLetterTemplateUtil;
    private SscsCaseData sscsCaseDataWithDocument;
    private SscsCaseData sscsCaseDataWithoutDocument;

    @Before
    public void setUp() {
        strikeoutTemplate = "strikeoutTemplate";
        strikeoutRepTemplate = "strikeoutRepTemplate";
        directionTemplate = "directionTemplate";
        directionRepTemplate = "directionRepTemplate";
        validAppealCreatedLetterTemplate = "validAppealCreatedLetterTemplate";
        validAppealCreatedLetterTemplateRep = "validAppealCreatedLetterTemplateRep";


        bundledLetterTemplateUtil = new BundledLetterTemplateUtil(
                strikeoutTemplate, strikeoutRepTemplate, directionTemplate, directionRepTemplate, validAppealCreatedLetterTemplate, validAppealCreatedLetterTemplateRep
        );

        sscsCaseDataWithDocument = SscsCaseData.builder().sscsDocument(Collections.singletonList(SscsDocument.builder().build())).build();
        sscsCaseDataWithoutDocument = SscsCaseData.builder().build();
    }

    @Test
    public void getStruckOutTemplateWhenAppellantStruckOutAndHaveDocumentTemplate() {
        check(STRUCK_OUT, APPELLANT, strikeoutTemplate);
    }

    @Test
    public void getStruckOutTemplateWhenAppointeeStruckOutAndHaveDocumentTemplate() {
        check(STRUCK_OUT, APPOINTEE, strikeoutTemplate);
    }

    @Test
    public void getStruckOutRepTemplateWhenRepStruckOutAndHaveDocumentTemplate() {
        check(STRUCK_OUT, REPRESENTATIVE, strikeoutRepTemplate);
    }

    @Test
    public void noTemplateWhenStruckOutAndDoNotHaveDocumentTemplate() {
        String bundledLetterTemplate = bundledLetterTemplateUtil.getBundledLetterTemplate(STRUCK_OUT, sscsCaseDataWithoutDocument, APPELLANT);

        assertThat(bundledLetterTemplate, is(nullValue()));
    }

    @Test
    public void getDirectionTemplateWhenAppellantDirectionIssuedHaveDocumentTemplate() {
        check(DIRECTION_ISSUED, APPELLANT, directionTemplate);
    }

    @Test
    public void getDirectionTemplateWhenAppointeeDirectionIssuedHaveDocumentTemplate() {
        check(DIRECTION_ISSUED, APPOINTEE, directionTemplate);
    }

    @Test
    public void getDirectionTemplateWhenRepDirectionIssuedHaveDocumentTemplate() {
        check(DIRECTION_ISSUED, REPRESENTATIVE, directionRepTemplate);
    }

    @Test
    public void noTemplateWhenDirectionIssuedAndDoNotHaveDocumentTemplate() {
        String bundledLetterTemplate = bundledLetterTemplateUtil.getBundledLetterTemplate(DIRECTION_ISSUED, sscsCaseDataWithoutDocument, APPELLANT);

        assertThat(bundledLetterTemplate, is(nullValue()));
    }

    @Test
    public void getValidAppealTemplateWhenAppellantJudgeDecisionToProceedAndHaveDocumentTemplate() {
        check(JUDGE_DECISION_APPEAL_TO_PROCEED, APPELLANT, validAppealCreatedLetterTemplate);
    }

    @Test
    public void getValidAppealTemplateWhenAppointeeJudgeDecisionToProceedHaveDocumentTemplate() {
        check(JUDGE_DECISION_APPEAL_TO_PROCEED, APPOINTEE, validAppealCreatedLetterTemplate);
    }

    @Test
    public void getValidAppealTemplateWhenRepJudgeDecisionToProceedHaveDocumentTemplate() {
        check(JUDGE_DECISION_APPEAL_TO_PROCEED, REPRESENTATIVE, validAppealCreatedLetterTemplateRep);
    }

    @Test
    public void noTemplateWhenJudgeDecisionToProceedAndDoNotHaveDocumentTemplate() {
        String bundledLetterTemplate = bundledLetterTemplateUtil.getBundledLetterTemplate(JUDGE_DECISION_APPEAL_TO_PROCEED, sscsCaseDataWithoutDocument, APPELLANT);

        assertThat(bundledLetterTemplate, is(nullValue()));
    }

    @Test
    public void getValidAppealTemplateWhenAppellantTcwDecisionToProceedAndHaveDocumentTemplate() {
        check(TCW_DECISION_APPEAL_TO_PROCEED, APPELLANT, validAppealCreatedLetterTemplate);
    }

    @Test
    public void getValidAppealTemplateWhenAppointeeTcsDecisionToProceedHaveDocumentTemplate() {
        check(TCW_DECISION_APPEAL_TO_PROCEED, APPOINTEE, validAppealCreatedLetterTemplate);
    }

    @Test
    public void getValidAppealTemplateWhenRepTcsDecisionToProceedHaveDocumentTemplate() {
        check(TCW_DECISION_APPEAL_TO_PROCEED, REPRESENTATIVE, validAppealCreatedLetterTemplateRep);
    }

    @Test
    public void noTemplateWhenTcsDecisionToProceedAndDoNotHaveDocumentTemplate() {
        String bundledLetterTemplate = bundledLetterTemplateUtil.getBundledLetterTemplate(TCW_DECISION_APPEAL_TO_PROCEED, sscsCaseDataWithoutDocument, APPELLANT);

        assertThat(bundledLetterTemplate, is(nullValue()));
    }

    private void check(NotificationEventType notificationEventType, SubscriptionType subscriptionType, String expectedTemplate) {
        String bundledLetterTemplate = bundledLetterTemplateUtil.getBundledLetterTemplate(notificationEventType, sscsCaseDataWithDocument, subscriptionType);
        assertThat(bundledLetterTemplate, is(expectedTemplate));
    }


}