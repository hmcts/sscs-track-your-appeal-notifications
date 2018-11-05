package uk.gov.hmcts.reform.sscs.personalisation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasonDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.AppealReasons;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.config.AppConstants;

@RunWith(JUnitParamsRunner.class)
public class SyaAppealCreatedPersonalisationTest {

    private static final String CASE_ID = "54321";

    private SscsCaseData response;

    private SyaAppealCreatedPersonalisation syaAppealCreatedPersonalisation =
            new SyaAppealCreatedPersonalisation();

    @Test
    @Parameters(method = "generateSscsCaseDataForTest")
    public void givenSyaAppealCreated_shouldSetRepresentativeNameIfPresent(
            SscsCaseData sscsCaseData, String expected) {
        Map<String, String> personalisation = syaAppealCreatedPersonalisation.setRepresentativeName(
                new HashMap<>(), sscsCaseData);
        assertEquals(expected, personalisation.get(AppConstants.REPRESENTATIVE_NAME));
    }

    @SuppressWarnings("Indentation")
    private Object[] generateSscsCaseDataForTest() {
        SscsCaseData sscsCaseDataWithReps = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .rep(Representative.builder()
                                .name(Name.builder()
                                        .firstName("Manish")
                                        .lastName("Sharma")
                                        .title("Mrs")
                                        .build())
                                .build())
                        .build())
                .build();
        SscsCaseData sscsCaseDataWithNoReps = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .rep(null)
                        .build())
                .build();
        SscsCaseData sscsCaseDataWithEmptyReps = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .rep(Representative.builder().build())
                        .build())
                .build();
        return new Object[]{
                new Object[]{sscsCaseDataWithReps, "Manish Sharma"},
                new Object[]{sscsCaseDataWithNoReps, null},
                new Object[]{sscsCaseDataWithEmptyReps, null}
        };
    }

    @Test
    public void givenASyaAppealCreated_setMrnDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .mrnDetails(MrnDetails.builder()
                                .mrnDate("3 May 2018")
                                .mrnLateReason("My train was cancelled.")
                                .mrnMissingReason("My dog ate my homework.")
                                .build())
                        .build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n"
                        + "\nReason for late appeal: My train was cancelled.\n"
                        + "\nReason for no MRN: My dog ate my homework.",
                result.get(AppConstants.MRN_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreated_setMrnDetailsForTemplateWhenReasonForNoMrnMissing() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .mrnDetails(MrnDetails.builder().mrnDate("3 May 2018").mrnLateReason("My train was cancelled.").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n"
                        + "\nReason for late appeal: My train was cancelled.",
                result.get(AppConstants.MRN_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreated_setYourDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                                .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                .contact(Contact.builder().email("manish.sharma@gmail.com").phone("0797 243 8179").build())
                                .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                        + "\nDate of birth: 12 March 1971\n"
                        + "\nNational Insurance number: NP 27 28 67 B\n"
                        + "\nAddress: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n"
                        + "\nEmail: manish.sharma@gmail.com\n"
                        + "\nPhone: 0797 243 8179",
                result.get(AppConstants.YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithNoEmailOrPhoneProvided_setYourDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                                .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                .contact(Contact.builder().build())
                                .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                        + "\nDate of birth: 12 March 1971\n"
                        + "\nNational Insurance number: NP 27 28 67 B\n"
                        + "\nAddress: 122 Breach Street, My town, Cardiff, CF11 2HB\n"
                        + "\nEmail: Not provided\n"
                        + "\nPhone: Not provided",
                result.get(AppConstants.YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .subscribeSms("Yes")
                                .mobile("07955555708").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response);

        assertEquals("Receive text message reminders: yes\n"
                        + "\nMobile number: 07955555708",
                result.get(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithNoTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .subscribeSms("No").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response);

        assertEquals("Receive text message reminders: no",
                result.get(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithRepresentative_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().rep(Representative.builder()
                        .name(Name.builder().firstName("Peter").lastName("Smith").build())
                        .organisation("Citizens Advice")
                        .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                        .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                        + "\nName: Peter Smith\n"
                        + "\nOrganisation: Citizens Advice\n"
                        + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                        + "\nEmail: peter.smith@cab.org.uk\n"
                        + "\nPhone: 03444 77 1010",
                result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithRepresentativeAndNoEmailOrPhoneOrOrganisationProvided_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().rep(Representative.builder()
                        .name(Name.builder().firstName("Peter").lastName("Smith").build())
                        .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                        .contact(Contact.builder().build())
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                        + "\nName: Peter Smith\n"
                        + "\nOrganisation: Not provided\n"
                        + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                        + "\nEmail: Not provided\n"
                        + "\nPhone: Not provided",
                result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithNoRepresentative_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder()
                        .build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: no",
                result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithOneReasonForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason = AppealReason.builder().value(AppealReasonDetails.builder().description("I want to appeal").reason("Because I do").build()).build();
        appealReasonList.add(reason);

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                        .build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n"
                        + "\nWhy you disagree with it: Because I do\n"
                        + "\nAnything else you want to tell the tribunal: Some other reason",
                result.get(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithMultipleReasonsForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason1 = AppealReason.builder().value(AppealReasonDetails.builder().description("I want to appeal").reason("Because I do").build()).build();
        AppealReason reason2 = AppealReason.builder().value(AppealReasonDetails.builder().description("I want to appeal again").reason("I'm in the mood").build()).build();
        appealReasonList.add(reason1);
        appealReasonList.add(reason2);

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                        .build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n"
                        + "\nWhy you disagree with it: Because I do\n"
                        + "\nWhat you disagree with: I want to appeal again\n"
                        + "\nWhy you disagree with it: I'm in the mood\n"
                        + "\nAnything else you want to tell the tribunal: Some other reason",
                result.get(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoAppealReasons_setReasonForAppealingDetailsForTemplate() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().build())
                        .build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("Anything else you want to tell the tribunal: Not provided",
                result.get(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithNoExcludedDates_setHearingDetailsForTemplate() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes",
                result.get(AppConstants.HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithOneExcludedDate_setHearingDetailsForTemplate() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-03").build()).build());

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                        .excludeDates(excludeDates)
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n"
                        + "\nDates you can't attend: 3 January 2018",
                result.get(AppConstants.HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithMultipleExcludedDates_setHearingDetailsForTemplateAndIgnoreEndDateRange() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-03").build()).build());
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-05").end("2018-01-07").build()).build());

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                        .excludeDates(excludeDates)
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n"
                        + "\nDates you can't attend: 3 January 2018, 5 January 2018",
                result.get(AppConstants.HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithHearingArrangements_setHearingArrangementsForTemplate() {

        List<String> arrangementList = new ArrayList<>();

        arrangementList.add("signLanguageInterpreter");
        arrangementList.add("hearingLoop");
        arrangementList.add("disabledAccess");

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                        .arrangements(arrangementList)
                        .languageInterpreter("Yes")
                        .other("Other")
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Required\n"
                        + "\nSign interpreter: Required\n"
                        + "\nHearing loop: Required\n"
                        + "\nDisabled access: Required\n"
                        + "\nAny other arrangements: Other",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoLanguageInterpreter_setHearingArrangementsForTemplate() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                        .languageInterpreter("No")
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                        + "\nSign interpreter: Not required\n"
                        + "\nHearing loop: Not required\n"
                        + "\nDisabled access: Not required\n"
                        + "\nAny other arrangements: Not required",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoHearingArrangements_setHearingArrangementsForTemplate() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                        + "\nSign interpreter: Not required\n"
                        + "\nHearing loop: Not required\n"
                        + "\nDisabled access: Not required\n"
                        + "\nAny other arrangements: Not required",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));
    }

}
