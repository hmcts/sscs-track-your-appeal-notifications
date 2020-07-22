package uk.gov.hmcts.reform.sscs.personalisation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.AppConstants;
import uk.gov.hmcts.reform.sscs.config.PersonalisationConfiguration;

public class SyaAppealCreatedAndReceivedPersonalisationTest {

    private static final String CASE_ID = "54321";
    private static final String YES = "yes";

    SscsCaseData response;

    @InjectMocks
    @Resource
    SyaAppealCreatedAndReceivedPersonalisation syaAppealCreatedAndReceivedPersonalisation;

    @Spy
    private PersonalisationConfiguration personalisationConfiguration;

    @Before
    public void setup() {
        initMocks(this);
        Map<PersonalisationKey, String> englishMap = new HashMap<>();
        englishMap.put(PersonalisationKey.ATTENDING_HEARING, "Attending the hearing: ");
        englishMap.put(PersonalisationKey.YES, "yes");
        englishMap.put(PersonalisationKey.NO, "no");
        englishMap.put(PersonalisationKey.DATES_NOT_ATTENDING, "Dates you can't attend: ");
        englishMap.put(PersonalisationKey.DATE_OF_MRN, "Date of MRN: ");
        englishMap.put(PersonalisationKey.REASON_FOR_LATE_APPEAL, "Reason for late appeal: ");
        englishMap.put(PersonalisationKey.REASON_FOR_NO_MRN, "Reason for no MRN: ");
        englishMap.put(PersonalisationKey.NAME, "Name: ");
        englishMap.put(PersonalisationKey.DATE_OF_BIRTH, "Date of birth: ");
        englishMap.put(PersonalisationKey.NINO, "National Insurance number: ");
        englishMap.put(PersonalisationKey.ADDRESS, "Address: ");
        englishMap.put(PersonalisationKey.EMAIL, "Email: ");
        englishMap.put(PersonalisationKey.PHONE, "Phone: ");
        englishMap.put(PersonalisationKey.RECEIVE_TEXT_MESSAGE_REMINDER, "Receive text message reminders: ");
        englishMap.put(PersonalisationKey.MOBILE, "Mobile number: ");
        englishMap.put(PersonalisationKey.HAVE_AN_APPOINTEE, "Have an appointee: ");
        englishMap.put(PersonalisationKey.NOT_PROVIDED, "Not provided");
        englishMap.put(PersonalisationKey.HAVE_A_REPRESENTATIVE, "Have a representative: ");
        englishMap.put(PersonalisationKey.ORGANISATION, "Organisation: ");
        englishMap.put(PersonalisationKey.WHAT_DISAGREE_WITH, "What you disagree with: ");
        englishMap.put(PersonalisationKey.WHY_DISAGREE_WITH, "Why you disagree with it: ");
        englishMap.put(PersonalisationKey.ANYTHING, "Anything else you want to tell the tribunal: ");
        englishMap.put(PersonalisationKey.LANGUAGE_INTERPRETER, "Language interpreter: ");
        englishMap.put(PersonalisationKey.SIGN_INTERPRETER, "Sign interpreter: ");
        englishMap.put(PersonalisationKey.HEARING_LOOP, "Hearing loop: ");
        englishMap.put(PersonalisationKey.DISABLED_ACCESS, "Disabled access: ");
        englishMap.put(PersonalisationKey.OTHER_ARRANGEMENTS, "Any other arrangements: ");
        englishMap.put(PersonalisationKey.REQUIRED, "Required");
        englishMap.put(PersonalisationKey.NOT_REQUIRED, "Not required");
        englishMap.put(PersonalisationKey.OTHER, "Other");

        Map<PersonalisationKey, String> welshMap = new HashMap<>();
        welshMap.put(PersonalisationKey.ATTENDING_HEARING, "Ydych chi'n bwriadu mynychu'r gwrandawiad: ");
        welshMap.put(PersonalisationKey.YES, "ydw");
        welshMap.put(PersonalisationKey.NO, "nac ydw");
        welshMap.put(PersonalisationKey.DATES_NOT_ATTENDING, "Dyddiadau na allwch fynychu: ");
        welshMap.put(PersonalisationKey.DATE_OF_MRN, "Dyddiad yr MRN: ");
        welshMap.put(PersonalisationKey.REASON_FOR_LATE_APPEAL, "Rheswm dros apêl hwyr: ");
        welshMap.put(PersonalisationKey.REASON_FOR_NO_MRN, "Rheswm dros ddim MRN: ");
        welshMap.put(PersonalisationKey.NAME, "Enw: ");
        welshMap.put(PersonalisationKey.DATE_OF_BIRTH, "Dyddiad geni: ");
        welshMap.put(PersonalisationKey.NINO, "Rhif Yswiriant Gwladol: ");
        welshMap.put(PersonalisationKey.ADDRESS, "Cyfeiriad: ");
        welshMap.put(PersonalisationKey.EMAIL, "E-bost: ");
        welshMap.put(PersonalisationKey.PHONE, "Rhif ffôn: ");
        welshMap.put(PersonalisationKey.RECEIVE_TEXT_MESSAGE_REMINDER, "Eisiau negeseuon testun atgoffa: ");
        welshMap.put(PersonalisationKey.MOBILE, "Rhif ffôn symudol: ");
        welshMap.put(PersonalisationKey.HAVE_AN_APPOINTEE, "Have an appointee: ");
        welshMap.put(PersonalisationKey.NOT_PROVIDED, "Nis ddarparwyd");
        welshMap.put(PersonalisationKey.HAVE_A_REPRESENTATIVE, "A oes gennych chi gynrychiolydd: ");
        welshMap.put(PersonalisationKey.ORGANISATION, "Sefydliad: ");
        welshMap.put(PersonalisationKey.WHAT_DISAGREE_WITH, "Beth ydych chi’n anghytuno ag o: ");
        welshMap.put(PersonalisationKey.WHY_DISAGREE_WITH, "Pam ydych chi’n anghytuno ag o: ");
        welshMap.put(PersonalisationKey.ANYTHING, "Unrhyw beth arall yr hoffech ddweud wrth y tribiwnlys: ");
        welshMap.put(PersonalisationKey.LANGUAGE_INTERPRETER, "Dehonglydd iaith arwyddion: ");
        welshMap.put(PersonalisationKey.SIGN_INTERPRETER, "Dehonglydd iaith arwyddion: ");
        welshMap.put(PersonalisationKey.HEARING_LOOP, "Dolen glyw: ");
        welshMap.put(PersonalisationKey.DISABLED_ACCESS, "Mynediad i bobl anab: ");
        welshMap.put(PersonalisationKey.OTHER_ARRANGEMENTS, "Unrhyw drefniadau eraill: ");
        welshMap.put(PersonalisationKey.REQUIRED, "Gofynnol");
        welshMap.put(PersonalisationKey.NOT_REQUIRED, "Dim yn ofynnol");
        welshMap.put(PersonalisationKey.OTHER, "Other");

        Map<LanguagePreference, Map<PersonalisationKey, String>> personalisations = new HashMap<>();
        personalisations.put(LanguagePreference.ENGLISH, englishMap);
        personalisations.put(LanguagePreference.WELSH, welshMap);

        personalisationConfiguration.setPersonalisation(personalisations);
    }

    @Test
    public void givenAnAppeal_setMrnDetailsForTemplate() {
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

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n"
                        + "\nReason for late appeal: My train was cancelled.\n"
                        + "\nReason for no MRN: My dog ate my homework.",
                result.get(AppConstants.MRN_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppeal_setMrnDetailsForTemplateWhenReasonForNoMrnMissing() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .mrnDetails(MrnDetails.builder().mrnDate("3 May 2018").mrnLateReason("My train was cancelled.").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n"
                        + "\nReason for late appeal: My train was cancelled.",
                result.get(AppConstants.MRN_DETAILS_LITERAL));
        assertNull("Welsh mrn details should be set", result.get(AppConstants.WELSH_HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppeal_setMrnDetailsForTemplateWhenReasonForNoMrnMissing_welsh() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .languagePreferenceWelsh("Yes")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .mrnDetails(MrnDetails.builder().mrnDate("3 May 2018").mrnLateReason("My train was cancelled.").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setMrnDetails(new HashMap<>(), response);

        assertEquals("Date of MRN: 3 May 2018\n"
                        + "\nReason for late appeal: My train was cancelled.",
                result.get(AppConstants.MRN_DETAILS_LITERAL));

        assertEquals("Dyddiad yr MRN: 3 Mai 2018\n"
                        + "\nRheswm dros apêl hwyr: My train was cancelled.",
                result.get(AppConstants.WELSH_MRN_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppeal_setYourDetailsForTemplate() {
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

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                        + "\nDate of birth: 12 March 1971\n"
                        + "\nNational Insurance number: NP 27 28 67 B\n"
                        + "\nAddress: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n"
                        + "\nEmail: manish.sharma@gmail.com\n"
                        + "\nPhone: 0797 243 8179",
                result.get(AppConstants.YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppeal_setYourDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                                .address(Address.builder().line1("122 Breach Street").line2("The Village").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                .contact(Contact.builder().email("manish.sharma@gmail.com").phone("0797 243 8179").build())
                                .build()).build())
                .languagePreferenceWelsh("Yes")
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                      + "\nDate of birth: 12 March 1971\n"
                      + "\nNational Insurance number: NP 27 28 67 B\n"
                      + "\nAddress: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n"
                      + "\nEmail: manish.sharma@gmail.com\n"
                      + "\nPhone: 0797 243 8179",
              result.get(AppConstants.YOUR_DETAILS_LITERAL));

        assertEquals("Enw: Manish Sharma\n"
                        + "\nDyddiad geni: 12 Mawrth 1971\n"
                        + "\nRhif Yswiriant Gwladol: NP 27 28 67 B\n"
                        + "\nCyfeiriad: 122 Breach Street, The Village, My town, Cardiff, CF11 2HB\n"
                        + "\nE-bost: manish.sharma@gmail.com\n"
                        + "\nRhif ffôn: 0797 243 8179",
                result.get(AppConstants.WELSH_YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoEmailOrPhoneProvided_setYourDetailsForTemplate() {
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

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                        + "\nDate of birth: 12 March 1971\n"
                        + "\nNational Insurance number: NP 27 28 67 B\n"
                        + "\nAddress: 122 Breach Street, My town, Cardiff, CF11 2HB\n"
                        + "\nEmail: Not provided\n"
                        + "\nPhone: Not provided",
                result.get(AppConstants.YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoEmailOrPhoneProvided_setYourDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("PIP").build())
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                                .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                                .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                                .contact(Contact.builder().build())
                                .build()).build())
                .languagePreferenceWelsh("yes")
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setYourDetails(new HashMap<>(), response);

        assertEquals("Name: Manish Sharma\n"
                        + "\nDate of birth: 12 March 1971\n"
                        + "\nNational Insurance number: NP 27 28 67 B\n"
                        + "\nAddress: 122 Breach Street, My town, Cardiff, CF11 2HB\n"
                        + "\nEmail: Not provided\n"
                        + "\nPhone: Not provided",
                result.get(AppConstants.YOUR_DETAILS_LITERAL));

        assertEquals("Enw: Manish Sharma\n"
                        + "\nDyddiad geni: 12 Mawrth 1971\n"
                        + "\nRhif Yswiriant Gwladol: NP 27 28 67 B\n"
                        + "\nCyfeiriad: 122 Breach Street, My town, Cardiff, CF11 2HB\n"
                        + "\nE-bost: Nis ddarparwyd\n"
                        + "\nRhif ffôn: Nis ddarparwyd",
                result.get(AppConstants.WELSH_YOUR_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .subscribeSms("Yes").wantSmsNotifications("Yes")
                                .mobile("07955555708").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: yes\n"
                        + "\nMobile number: 07955555708",
                result.get(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithTextMessageReminders_setTextMessageReminderDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .subscribeSms("Yes").wantSmsNotifications("Yes")
                                .mobile("07955555708").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: yes\n"
                        + "\nMobile number: 07955555708",
                result.get(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));

        assertEquals("Eisiau negeseuon testun atgoffa: ydw\n"
                        + "\nRhif ffôn symudol: 07955555708",
                result.get(AppConstants.WELSH_TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoTextMessageReminders_setTextMessageReminderDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .subscribeSms("No").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: no",
                result.get(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoTextMessageReminders_setTextMessageReminderDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .subscriptions(Subscriptions.builder()
                        .appellantSubscription(Subscription.builder()
                                .subscribeSms("No").build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setTextMessageReminderDetails(new HashMap<>(), response.getSubscriptions().getAppellantSubscription());

        assertEquals("Receive text message reminders: no",
                result.get(AppConstants.TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));

        assertEquals("Eisiau negeseuon testun atgoffa: nac ydw",
                result.get(AppConstants.WELSH_TEXT_MESSAGE_REMINDER_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithAppointee_setAppointeeDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                    .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .appointee(Appointee.builder().name(Name.builder().firstName("Peter").lastName("Smith").build())
                        .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                        .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                        .identity(Identity.builder().dob("12 March 1981").build())
                        .build())
                    .contact(Contact.builder().build()).build()).build()).build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: yes\n"
                        + "\nName: Peter Smith\n"
                        + "\nDate of birth: 12 March 1981\n"
                        + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                        + "\nEmail: peter.smith@cab.org.uk\n"
                        + "\nPhone: 03444 77 1010",
                result.get(AppConstants.APPOINTEE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithAppointee_setAppointeeDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .languagePreferenceWelsh("yes")
                .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .identity(Identity.builder().nino("NP 27 28 67 B").dob("12 March 1971").build())
                    .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .appointee(Appointee.builder().name(Name.builder().firstName("Peter").lastName("Smith").build())
                        .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                        .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                        .identity(Identity.builder().dob("12 March 1981").build())
                        .build())
                    .contact(Contact.builder().build()).build()).build()).build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: yes\n"
                        + "\nName: Peter Smith\n"
                        + "\nDate of birth: 12 March 1981\n"
                        + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                        + "\nEmail: peter.smith@cab.org.uk\n"
                        + "\nPhone: 03444 77 1010",
                result.get(AppConstants.APPOINTEE_DETAILS_LITERAL));

        assertEquals("Have an appointee: ydw\n"
                        + "\nEnw: Peter Smith\n"
                        + "\nDyddiad geni: 12 Mawrth 1981\n"
                        + "\nCyfeiriad: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                        + "\nE-bost: peter.smith@cab.org.uk\n"
                        + "\nRhif ffôn: 03444 77 1010",
                result.get(AppConstants.WELSH_APPOINTEE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithAppointeeAndNoEmailOrPhone_setAppointeeDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appellant(Appellant.builder().appointee(Appointee.builder()
                        .name(Name.builder().firstName("Peter").lastName("Smith").build())
                        .identity(Identity.builder().dob("12 March 1981").build())
                        .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                        .contact(Contact.builder().build())
                        .build()).build())
                    .build()).build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: yes\n"
                        + "\nName: Peter Smith\n"
                        + "\nDate of birth: 12 March 1981\n"
                        + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                        + "\nEmail: Not provided\n"
                        + "\nPhone: Not provided",
                result.get(AppConstants.APPOINTEE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithNoAppointee_setAppointeeDetailsForTemplate() {
        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appellant(Appellant.builder()
                    .name(Name.builder().firstName("Manish").lastName("Sharma").title("Mrs").build())
                    .address(Address.builder().line1("122 Breach Street").town("My town").county("Cardiff").postcode("CF11 2HB").build())
                    .build())
                .build()).build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setAppointeeDetails(new HashMap<>(), response);

        assertEquals("Have an appointee: no",
                result.get(AppConstants.APPOINTEE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithRepresentative_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .organisation("Citizens Advice")
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                .build()).build())
            .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                + "\nName: Peter Smith\n"
                + "\nOrganisation: Citizens Advice\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: peter.smith@cab.org.uk\n"
                + "\nPhone: 03444 77 1010",
            result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithRepresentative_setRepresentativeDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .organisation("Citizens Advice")
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().email("peter.smith@cab.org.uk").phone("03444 77 1010").build())
                .build()).build())
            .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                + "\nName: Peter Smith\n"
                + "\nOrganisation: Citizens Advice\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: peter.smith@cab.org.uk\n"
                + "\nPhone: 03444 77 1010",
            result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));

        assertEquals("A oes gennych chi gynrychiolydd: ydw\n"
                + "\nEnw: Peter Smith\n"
                + "\nSefydliad: Citizens Advice\n"
                + "\nCyfeiriad: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nE-bost: peter.smith@cab.org.uk\n"
                + "\nRhif ffôn: 03444 77 1010",
            result.get(AppConstants.WELSH_REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithRepresentativeAndNoEmailOrPhoneOrOrganisationProvided_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().build())
                .build()).build())
            .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                + "\nName: Peter Smith\n"
                + "\nOrganisation: Not provided\n"
                + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nEmail: Not provided\n"
                + "\nPhone: Not provided",
            result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenAnAppealWithRepresentativeAndNoEmailOrPhoneOrOrganisationProvided_setRepresentativeDetailsForTemplate_Welsh() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .languagePreferenceWelsh("yes")
            .appeal(Appeal.builder().rep(Representative.builder().hasRepresentative(YES)
                .name(Name.builder().firstName("Peter").lastName("Smith").build())
                .address(Address.builder().line1("Ground Floor").line2("Gazette Buildings").town("168 Corporation Street").county("Cardiff").postcode("CF11 6TF").build())
                .contact(Contact.builder().build())
                .build()).build())
            .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: yes\n"
                        + "\nName: Peter Smith\n"
                        + "\nOrganisation: Not provided\n"
                        + "\nAddress: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                        + "\nEmail: Not provided\n"
                        + "\nPhone: Not provided",
                result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));

        assertEquals("A oes gennych chi gynrychiolydd: ydw\n"
                + "\nEnw: Peter Smith\n"
                + "\nSefydliad: Nis ddarparwyd\n"
                + "\nCyfeiriad: Ground Floor, Gazette Buildings, 168 Corporation Street, Cardiff, CF11 6TF\n"
                + "\nE-bost: Nis ddarparwyd\n"
                + "\nRhif ffôn: Nis ddarparwyd",
            result.get(AppConstants.WELSH_REPRESENTATIVE_DETAILS_LITERAL));


    }

    @Test
    public void givenAnAppealWithNoRepresentative_setRepresentativeDetailsForTemplate() {
        response = SscsCaseData.builder()
            .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
            .appeal(Appeal.builder()
                .build())
            .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setRepresentativeDetails(new HashMap<>(), response);

        assertEquals("Have a representative: no",
            result.get(AppConstants.REPRESENTATIVE_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithOneReasonForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        appealReasonList.add(reason);

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                        .build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n"
                        + "\nWhy you disagree with it: Because I do\n"
                        + "\nAnything else you want to tell the tribunal: Some other reason",
                result.get(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithOneReasonForAppealing_setReasonForAppealingDetailsForTemplate_Welsh() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        appealReasonList.add(reason);

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                        .build())
                .languagePreferenceWelsh("yes")
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("What you disagree with: I want to appeal\n"
                        + "\nWhy you disagree with it: Because I do\n"
                        + "\nAnything else you want to tell the tribunal: Some other reason",
                result.get(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL));

        assertEquals("Beth ydych chi’n anghytuno ag o: I want to appeal\n"
                        + "\nPam ydych chi’n anghytuno ag o: Because I do\n"
                        + "\nUnrhyw beth arall yr hoffech ddweud wrth y tribiwnlys: Some other reason",
                result.get(AppConstants.WELSH_REASONS_FOR_APPEALING_DETAILS_LITERAL));

    }

    @Test
    public void givenASyaAppealWithMultipleReasonsForAppealing_setReasonForAppealingDetailsForTemplate() {
        List<AppealReason> appealReasonList = new ArrayList<>();
        AppealReason reason1 = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal").description("Because I do").build()).build();
        AppealReason reason2 = AppealReason.builder().value(AppealReasonDetails.builder().reason("I want to appeal again").description("I'm in the mood").build()).build();
        appealReasonList.add(reason1);
        appealReasonList.add(reason2);

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().reasons(appealReasonList).otherReasons("Some other reason").build())
                        .build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

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

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("Anything else you want to tell the tribunal: Not provided",
                result.get(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoAppealReasons_setReasonForAppealingDetailsForTemplate_Welsh() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().appealReasons(AppealReasons.builder().build())
                        .build())
                .languagePreferenceWelsh("yes")
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setReasonsForAppealingDetails(new HashMap<>(), response);

        assertEquals("Anything else you want to tell the tribunal: Not provided",
                result.get(AppConstants.REASONS_FOR_APPEALING_DETAILS_LITERAL));

        assertEquals("Unrhyw beth arall yr hoffech ddweud wrth y tribiwnlys: Nis ddarparwyd",
                result.get(AppConstants.WELSH_REASONS_FOR_APPEALING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithNoExcludedDates_setHearingDetailsForTemplate() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

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

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

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

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n"
                        + "\nDates you can't attend: 3 January 2018, 5 January 2018",
                result.get(AppConstants.HEARING_DETAILS_LITERAL));
        assertNull("Welsh details not be set ", result.get(AppConstants.WELSH_HEARING_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealAttendingHearingWithMultipleExcludedDates_setHearingDetailsForTemplateAndIgnoreEndDateRange_Welsh() {

        List<ExcludeDate> excludeDates = new ArrayList<>();

        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-03").build()).build());
        excludeDates.add(ExcludeDate.builder().value(DateRange.builder().start("2018-01-05").end("2018-01-07").build()).build());

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .languagePreferenceWelsh("Yes")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().wantsToAttend("yes")
                        .excludeDates(excludeDates)
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingDetails(new HashMap<>(), response);

        assertEquals("Attending the hearing: yes\n"
                        + "\nDates you can't attend: 3 January 2018, 5 January 2018",
                result.get(AppConstants.HEARING_DETAILS_LITERAL));

        assertEquals("Ydych chi'n bwriadu mynychu'r gwrandawiad: ydw\n"
                        + "\nDyddiadau na allwch fynychu: 3 Ionawr 2018, 5 Ionawr 2018",
                result.get(AppConstants.WELSH_HEARING_DETAILS_LITERAL));
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

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Required\n"
                        + "\nSign interpreter: Required\n"
                        + "\nHearing loop: Required\n"
                        + "\nDisabled access: Required\n"
                        + "\nAny other arrangements: Other",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));

    }

    @Test
    public void givenASyaAppealWithHearingArrangements_setHearingArrangementsForTemplate_Welsh() {

        List<String> arrangementList = new ArrayList<>();

        arrangementList.add("signLanguageInterpreter");
        arrangementList.add("hearingLoop");
        arrangementList.add("disabledAccess");

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .languagePreferenceWelsh("yes")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                        .arrangements(arrangementList)
                        .languageInterpreter("Yes")
                        .other("Other")
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Required\n"
                        + "\nSign interpreter: Required\n"
                        + "\nHearing loop: Required\n"
                        + "\nDisabled access: Required\n"
                        + "\nAny other arrangements: Other",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));

        assertEquals("Dehonglydd iaith arwyddion: Gofynnol\n"
                        + "\nDehonglydd iaith arwyddion: Gofynnol\n"
                        + "\nDolen glyw: Gofynnol\n"
                        + "\nMynediad i bobl anab: Gofynnol\n"
                        + "\nUnrhyw drefniadau eraill: Other",
                result.get(AppConstants.WELSH_HEARING_ARRANGEMENT_DETAILS_LITERAL));

    }

    @Test
    public void givenASyaAppealWithNoLanguageInterpreter_setHearingArrangementsForTemplate() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                        .languageInterpreter("No")
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                        + "\nSign interpreter: Not required\n"
                        + "\nHearing loop: Not required\n"
                        + "\nDisabled access: Not required\n"
                        + "\nAny other arrangements: Not required",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));
    }

    @Test
    public void givenASyaAppealWithNoLanguageInterpreter_setHearingArrangementsForTemplate_Welsh() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .languagePreferenceWelsh("yes")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                        .languageInterpreter("No")
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                        + "\nSign interpreter: Not required\n"
                        + "\nHearing loop: Not required\n"
                        + "\nDisabled access: Not required\n"
                        + "\nAny other arrangements: Not required",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));

        assertEquals("Dehonglydd iaith arwyddion: Dim yn ofynnol\n"
                        + "\nDehonglydd iaith arwyddion: Dim yn ofynnol\n"
                        + "\nDolen glyw: Dim yn ofynnol\n"
                        + "\nMynediad i bobl anab: Dim yn ofynnol\n"
                        + "\nUnrhyw drefniadau eraill: Dim yn ofynnol",
                result.get(AppConstants.WELSH_HEARING_ARRANGEMENT_DETAILS_LITERAL));

    }

    @Test
    public void givenASyaAppealWithNoHearingArrangements_setHearingArrangementsForTemplate() {

        response = SscsCaseData.builder()
                .ccdCaseId(CASE_ID).caseReference("SC/1234/5")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder()
                        .build()).build())
                .build();

        Map<String, String> result = syaAppealCreatedAndReceivedPersonalisation.setHearingArrangementDetails(new HashMap<>(), response);

        assertEquals("Language interpreter: Not required\n"
                        + "\nSign interpreter: Not required\n"
                        + "\nHearing loop: Not required\n"
                        + "\nDisabled access: Not required\n"
                        + "\nAny other arrangements: Not required",
                result.get(AppConstants.HEARING_ARRANGEMENT_DETAILS_LITERAL));
    }

    @Test public void getOptionalFieldTest() {

        final Name emptyName = Name.builder().build();
        final Name firstName = Name.builder().firstName("FIRST").build();
        final Name lastName = Name.builder().lastName("LAST").build();
        final Name bothName = Name.builder().firstName("FIRST").lastName("LAST").build();
        final Name allName = Name.builder().title("MX").firstName("FIRST").lastName("LAST").build();

        assertEquals("expected", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(null, "expected"));
        assertEquals("expected", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(emptyName.getFullName(), "expected"));
        assertEquals("expected", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(emptyName.getFullNameNoTitle(), "expected"));
        assertEquals("null FIRST null", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(firstName.getFullName(), "expected"));
        assertEquals("FIRST null", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(firstName.getFullNameNoTitle(), "expected"));
        assertEquals("null null LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(lastName.getFullName(), "expected"));
        assertEquals("null LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(lastName.getFullNameNoTitle(), "expected"));
        assertEquals("null FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(bothName.getFullName(), "expected"));
        assertEquals("FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(bothName.getFullNameNoTitle(), "expected"));
        assertEquals("MX FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(allName.getFullName(), "expected"));
        assertEquals("FIRST LAST", SyaAppealCreatedAndReceivedPersonalisation.getOptionalField(allName.getFullNameNoTitle(), "expected"));
    }

}
