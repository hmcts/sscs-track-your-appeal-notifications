package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.REP_WITH_ADDRESS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class LetterUtilsTest {
    private static final Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    @Test
    public void useAppellantAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper, getSubscriptionWithType(APPELLANT)));
    }

    @NotNull
    private SubscriptionWithType getSubscriptionWithType(SubscriptionType subscriptionType) {
        return new SubscriptionWithType(Subscription.builder().build(), subscriptionType);
    }

    @Test
    public void useAppointeeAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getAddress(), getAddressToUseForLetter(wrapper, getSubscriptionWithType(APPOINTEE)));
    }

    @Test
    public void useRepAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            REP_WITH_ADDRESS,
            null
        );

        assertEquals(REP_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper, getSubscriptionWithType(REPRESENTATIVE)));
    }

    @Test
    public void useAppellantNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getName().getFullNameNoTitle(), getNameToUseForLetter(wrapper, getSubscriptionWithType(APPELLANT)));
    }

    @Test
    public void useAppointeeNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getName().getFullNameNoTitle(), getNameToUseForLetter(wrapper, getSubscriptionWithType(APPOINTEE)));
    }

    @Test
    public void useJointPartyAddressForLetter() {
        Address jointPartyAddress = Address.builder().county("county").line1("line1").line2("line2").postcode("EN1 1AF").build();
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(
                SYA_APPEAL_CREATED_NOTIFICATION,
                APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                JointPartyName.builder().title("Mr").firstName("Joint").lastName("Party").build(),
                jointPartyAddress,
                null
        );
        assertEquals(jointPartyAddress, getAddressToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY)));
        assertEquals("Joint Party", getNameToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY)));
    }

    @Test
    public void useAppellantAddressForJointPartyIfSameAsAppellantLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(
                SYA_APPEAL_CREATED_NOTIFICATION,
                APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                JointPartyName.builder().title("Mrs").firstName("Betty").lastName("Bloom").build(),
                null,
                null
        );
        Address appellantAddress = wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        assertEquals(appellantAddress, getAddressToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY)));
        assertEquals("Betty Bloom", getNameToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY)));
    }

    @Test
    @Parameters(method = "repNamesForLetters")
    public void useRepNameForLetter(Name name, String expectedResult) {
        Representative rep = Representative.builder()
                .name(name)
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
                .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED_NOTIFICATION,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            rep,
            null
        );

        assertEquals(expectedResult, getNameToUseForLetter(wrapper, getSubscriptionWithType(REPRESENTATIVE)));
    }

    private Object[] repNamesForLetters() {

        return new Object[] {
            new Object[]{Name.builder().firstName("Re").lastName("Presentative").build(), "Re Presentative"},
            new Object[]{Name.builder().build(), REP_SALUTATION},
            new Object[]{Name.builder().firstName("undefined").lastName("undefined").build(), REP_SALUTATION}
        };
    }

    @Test
    public void successfulBundleLetter() throws IOException {
        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));

        assertNotNull(buildBundledLetter(sampleDirectionCoversheet, sampleDirectionText));
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldNotBundleLetterWhenCoverSheetIsNull() throws IOException {
        byte[] sampleDirectionText = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-text.pdf"));

        buildBundledLetter(null, sampleDirectionText);
    }

    @Test(expected = NotificationClientRuntimeException.class)
    public void shouldNotBundleLetterWhenAttachmentIsNull() throws IOException {
        byte[] sampleDirectionCoversheet = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("pdfs/direction-notice-coversheet-sample.pdf"));

        buildBundledLetter(sampleDirectionCoversheet, null);
    }

    @Test
    @Parameters({"1", "2", "3", "4"})
    public void willAddABlankPageAtTheEndIfAnOddPageIsGiven(int pages) throws IOException {
        PDDocument originalDocument = new PDDocument();

        // Create a new blank page and add it to the originalDocument
        PDPage blankPage = new PDPage();
        for (int i = 1; i <= pages; i++) {
            originalDocument.addPage(blankPage);
        }
        assertEquals(pages, originalDocument.getNumberOfPages());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        originalDocument.save(baos);
        originalDocument.close();
        byte[] bytes = baos.toByteArray();
        baos.close();

        byte[] newBytes = addBlankPageAtTheEndIfOddPage(bytes);
        PDDocument newDocument = PDDocument.load(newBytes);
        int expectedPages = (pages % 2 == 0) ? pages : pages + 1;
        assertEquals(expectedPages, newDocument.getNumberOfPages());
    }

    @Test
    @Parameters({"APPELLANT", "JOINT_PARTY", "APPOINTEE", "REPRESENTATIVE"})
    public void isAlternativeLetterFormatRequired(SubscriptionType subscriptionType) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperWithReasonableAdjustment();
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType)));
    }

    @Test
    public void givenAnOtherParty_thenIsAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
                .id("1")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
                .otherParties(otherPartyList).build();
        SscsCaseDataWrapper caseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseData)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY);
        subscriptionWithType.setPartyId(1);
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyWithAppointeeThatWantsReasonableAdjustment_thenIsAlternativeLetterFormatRequiredForAppointee() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
                .id("1")
                .appointee(Appointee.builder().id("2").build())
                .isAppointee("Yes")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
                .appointeeReasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
                .otherParties(otherPartyList).build();
        SscsCaseDataWrapper caseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseData)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY);
        subscriptionWithType.setPartyId(2);
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyWithRepThatWantsReasonableAdjustment_thenIsAlternativeLetterFormatRequiredForOtherPartyRep() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
                .id("1")
                .rep(Representative.builder().id("3").hasRepresentative("Yes").build())
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
                .repReasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
                .otherParties(otherPartyList).build();
        SscsCaseDataWrapper caseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseData)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY);
        subscriptionWithType.setPartyId(3);
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyWithReasonableAdjustmentAndSubscriptionIsSearchingForDifferentPartyId_thenNoAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
                .id("1")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.YES).build())
                .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
                .otherParties(otherPartyList).build();
        SscsCaseDataWrapper caseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseData)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY);
        subscriptionWithType.setPartyId(2);
        assertFalse(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    public void givenAnOtherPartyNoReasonableAdjustmentRequired_thenNoAlternativeLetterFormatRequired() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder()
                .id("1")
                .reasonableAdjustment(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YesNo.NO).build())
                .build()).build();
        otherPartyList.add(ccdValue);

        SscsCaseData caseData = SscsCaseData.builder()
                .otherParties(otherPartyList).build();
        SscsCaseDataWrapper caseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .oldSscsCaseData(caseData)
                .notificationEventType(APPEAL_RECEIVED_NOTIFICATION)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY);
        subscriptionWithType.setPartyId(1);
        assertFalse(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    @Parameters({"OTHER_PARTY, 4", "OTHER_PARTY, 3", "OTHER_PARTY, 2"})
    public void useOtherPartyLetterNameAndAddress(SubscriptionType subscriptionType, int otherPartyId) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperOtherParty(SYA_APPEAL_CREATED_NOTIFICATION, Appellant.builder().build(), null);
        final Address expectedAddress = getExpectedAddress(otherPartyId, wrapper);

        assertThat(LetterUtils.getAddressToUseForLetter(wrapper,
                new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, otherPartyId)), is(expectedAddress));

        final String expectedName = getExpectedName(otherPartyId, wrapper);
        assertThat(LetterUtils.getNameToUseForLetter(wrapper,
                new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, otherPartyId)), is(expectedName));

    }

    private Address getExpectedAddress(final int otherPartyId, final NotificationWrapper wrapper) {
        return requireNonNull(wrapper.getNewSscsCaseData().getOtherParties().stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getAddress()) : Pair.of(op.getId(), op.getAddress()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getAddress()) : null))
                .filter(Objects::nonNull)
                .filter(p -> p.getRight() != null && p.getLeft() != null)
                .filter(pair -> pair.getLeft().equals(String.valueOf(otherPartyId)))
                .findFirst()
                .map(Pair::getRight).orElse(null));
    }

    private String getExpectedName(final int otherPartyId, final NotificationWrapper wrapper) {
        return requireNonNull(wrapper.getNewSscsCaseData().getOtherParties().stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getName()) : Pair.of(op.getId(), op.getName()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getName()) : null))
                .filter(Objects::nonNull)
                .filter(p -> p.getRight() != null && p.getLeft() != null)
                .filter(pair -> pair.getLeft().equals(String.valueOf(otherPartyId)))
                .findFirst()
                .map(Pair::getRight)
                .map(Name::getFullNameNoTitle)
                .orElse(""));
    }

}
