package uk.gov.hmcts.sscs.personalisation;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.sscs.config.AppConstants.*;
import static uk.gov.hmcts.sscs.domain.Benefit.PIP;
import static uk.gov.hmcts.sscs.domain.notify.EventType.SYA_APPEAL_CREATED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import uk.gov.hmcts.sscs.domain.*;

public class SyaAppealCreatedPersonalisationTest {

    private static final String CASE_ID = "54321";

    CcdResponse response;

    @InjectMocks
    @Resource
    SyaAppealCreatedPersonalisation personalisation;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void givenASyaAppealCreated_setMrnDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().mrnDate("3 May 2018").mrnLateReason("My train was cancelled.").mrnMissingReason("My dog ate my homework.").build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n" +
                "\nReason for late appeal: My train was cancelled.\n" +
                "\nReason for no MRN: My dog ate my homework.",
                result.get(MRN_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreated_setYourDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appellant(Appellant.builder()
                        .isAppointee("No")
                        .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                        .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                        .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                        .contact(Contact.builder().email("manish.sharma@gmail.com").phone("0797 243 8179").build())
                    .build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Appointee: No\n" +
                        "\nName: Manish Sharma\n" +
                        "\nDate of birth: 12 March 1971\n" +
                        "\nNational Insurance number: NP 27 28 67 B\n" +
                        "\nAddress: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n" +
                        "\nEmail: manish.sharma@gmail.com\n" +
                        "\nPhone: 0797 243 8179",
                result.get(YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithNoEmailOrPhoneProvided_setYourDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appellant(Appellant.builder()
                        .isAppointee("No")
                        .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                        .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                        .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                        .contact(Contact.builder().build())
                        .build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Appointee: No\n" +
                        "\nName: Manish Sharma\n" +
                        "\nDate of birth: 12 March 1971\n" +
                        "\nNational Insurance number: NP 27 28 67 B\n" +
                        "\nAddress: 122 Breach Street, My town, Cardiff, CF11 2HB\n" +
                        "\nEmail: Not provided\n" +
                        "\nPhone: Not provided",
                result.get(YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                        .subscribeSms("Yes")
                        .mobile("07955555708").build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setTextMessageReminderDetails(new HashMap<>(), response);

        assertEquals("Receive text message reminders: yes\n" +
                        "\nMobile number: 07955555708",
                result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithNoTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .subscribeSms("No").build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setTextMessageReminderDetails(new HashMap<>(), response);

        assertEquals("Receive text message reminders: no",
                result.get(TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithRepresentative_setRepresentativeDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().rep(Representative.builder()
                        .name(Name.builder().firstName("Peter").lastName("Smith").build())
                        .organisation("Citizens Advice")
                        .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                        .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                        .build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n" +
                        "\nName: Peter Smith\n" +
                        "\nOrganisation: Citizens Advice\n" +
                        "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n" +
                        "\nEmail: peter.smith@cab.org.uk\n" +
                        "\nPhone: 03444 77 1010",
                result.get(REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithRepresentativeAndNoEmailOrPhoneOrOrganisationProvided_setRepresentativeDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().rep(Representative.builder()
                        .name(Name.builder().firstName("Peter").lastName("Smith").build())
                        .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                        .contact(Contact.builder().build())
                        .build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n" +
                        "\nName: Peter Smith\n" +
                        "\nOrganisation: Not provided\n" +
                        "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n" +
                        "\nEmail: Not provided\n" +
                        "\nPhone: Not provided",
                result.get(REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealCreatedWithNoRepresentative_setRepresentativeDetailsForTemplate() {
        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder()
                        .build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: no",
                result.get(REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithOneReasonForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        appealReasonList.add(reason);

        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                        .build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n" +
                        "\nWhy you disagree with it: Because I do\n" +
                        "\nAnything else you want to tell the tribunal: Some other reason",
                result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithMultipleReasonsForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason1 = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        AppealReason reason2 = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal again").description("I'm in the mood").build()).build();
        appealReasonList.add(reason1);
        appealReasonList.add(reason2);

        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                        .build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n" +
                        "\nWhy you disagree with it: Because I do\n" +
                        "\nWhat you disagree with: I want to appeal again\n" +
                        "\nWhy you disagree with it: I'm in the mood\n" +
                        "\nAnything else you want to tell the tribunal: Some other reason",
                result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoAppealReasons_setReasonForAppealingDetailsForTemplate() {

        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().build())
                        .build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("Anything else you want to tell the tribunal: Not provided",
                result.get(REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithNoExcludedDates_setHearingDetailsForTemplate() {

        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().attendingHearing("yes")
                        .build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes",
                result.get(HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithOneExcludedDate_setHearingDetailsForTemplate() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("3 January 2018").build()).build());

        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().attendingHearing("yes")
                        .excludeDates(excludeDates)
                        .build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n" +
                "\nDates you can't attend: 3 January 2018",
                result.get(HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithMultipleExcludedDates_setHearingDetailsForTemplate() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("3 January 2018").build()).build());
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("5 January 2018").end("7 January 2018").build()).build());

        response = CcdResponse.builder()
                .caseId(CASE_ID).benefitType(PIP).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().attendingHearing("yes")
                        .excludeDates(excludeDates)
                        .build()).build())
                .notificationType(SYA_APPEAL_CREATED)
                .build();

        Map<String, String> result = personalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n" +
                        "\nDates you can't attend: 3 January 2018, 5 January 2018 to 7 January 2018",
                result.get(HEARING_DETAILS_LITERAL));
    }


}
