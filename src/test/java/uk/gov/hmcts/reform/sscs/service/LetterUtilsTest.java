package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.config.AppConstants.REP_SALUTATION;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPOINTEE;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType.*;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.addBlankPageAtTheEndIfOddPage;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.buildBundledLetter;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.getAddressToUseForLetter;
import static uk.gov.hmcts.reform.sscs.service.LetterUtils.getNameToUseForLetter;
import static uk.gov.hmcts.reform.sscs.service.NotificationServiceTest.APPELLANT_WITH_ADDRESS;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.APPELLANT_WITH_ADDRESS_AND_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.service.SendNotificationServiceTest.REP_WITH_ADDRESS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

@RunWith(JUnitParamsRunner.class)
public class LetterUtilsTest {
    private static final Subscription EMPTY_SUBSCRIPTION = Subscription.builder().build();

    static Appellant APPELLANT_WITHOUT_ID = Appellant.builder()
            .name(Name.builder().firstName("Tom").lastName("Cat").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

    static  Representative REPRESENTATIVE_WITH_ID = Representative.builder()
            .id("REP123456")
            .name(Name.builder()
                    .title("Mr.")
                    .firstName("Representative")
                    .lastName("Appellant")
                    .build())
            .build();
    static Appellant APPELLANT_WITH_ID = Appellant.builder()
            .id("APP123456")
            .name(Name.builder().firstName("Tom").lastName("Cat").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

    static JointParty JOINT_PARTY_WITH_ID = JointParty.builder()
            .id("JP123456")
            .name(Name.builder().firstName("Joint").lastName("Party").build())
            .address(Address.builder().line1("Appellant Line 1").town("Appellant Town").county("Appellant County").postcode("AP9 3LL").build())
            .build();

    @Test
    public void useAppellantAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(APPELLANT, APPELLANT_WITH_ADDRESS, APPELLANT_WITH_ADDRESS)));
    }

    @NotNull
    private SubscriptionWithType getSubscriptionWithType(SubscriptionType subscriptionType, Party party, Entity entity) {
        return new SubscriptionWithType(Subscription.builder().build(), subscriptionType, party, entity);
    }

    @Test
    public void useAppointeeAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getAddress(),
            getAddressToUseForLetter(wrapper, getSubscriptionWithType(APPOINTEE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee())));
    }

    @Test
    public void useRepAddressForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            REP_WITH_ADDRESS,
            null
        );

        assertEquals(REP_WITH_ADDRESS.getAddress(), getAddressToUseForLetter(wrapper,
            getSubscriptionWithType(REPRESENTATIVE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE, REP_WITH_ADDRESS)));
    }

    @Test
    public void useAppellantNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS.getName().getFullNameNoTitle(),
            getNameToUseForLetter(wrapper, getSubscriptionWithType(APPELLANT, APPELLANT_WITH_ADDRESS,
                APPELLANT_WITH_ADDRESS)));
    }

    @Test
    public void useAppointeeNameForLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            null,
            null
        );

        assertEquals(APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee().getName().getFullNameNoTitle(),
            getNameToUseForLetter(wrapper,
                getSubscriptionWithType(APPOINTEE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                    APPELLANT_WITH_ADDRESS_AND_APPOINTEE.getAppointee())));
    }

    @Test
    public void useJointPartyAddressForLetter() {
        Address jointPartyAddress = Address.builder().county("county").line1("line1").line2("line2").postcode("EN1 1AF").build();
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(
            SYA_APPEAL_CREATED,
                APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                Name.builder().title("Mr").firstName("Joint").lastName("Party").build(),
                jointPartyAddress,
                null
        );
        assertEquals(jointPartyAddress, getAddressToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
        assertEquals("Joint Party", getNameToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
    }

    @Test
    public void useAppellantAddressForJointPartyIfSameAsAppellantLetter() {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperJointParty(
            SYA_APPEAL_CREATED,
                APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
                Name.builder()
                        .title("Mrs")
                        .firstName("Betty")
                        .lastName("Bloom")
                        .build(),
                null,
                null
        );
        Address appellantAddress = wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        assertEquals(appellantAddress, getAddressToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
        assertEquals("Betty Bloom", getNameToUseForLetter(wrapper, getSubscriptionWithType(JOINT_PARTY,
            wrapper.getNewSscsCaseData().getJointParty(), wrapper.getNewSscsCaseData().getJointParty())));
    }

    @Test
    @Parameters(method = "repNamesForLetters")
    public void useRepNameForLetter(Name name, String expectedResult) {
        Representative rep = Representative.builder()
                .name(name)
                .address(Address.builder().line1("Rep Line 1").town("Rep Town").county("Rep County").postcode("RE9 3LL").build())
                .build();

        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapper(
            SYA_APPEAL_CREATED,
            APPELLANT_WITH_ADDRESS_AND_APPOINTEE,
            rep,
            null
        );

        assertEquals(expectedResult, getNameToUseForLetter(wrapper,
            getSubscriptionWithType(REPRESENTATIVE, APPELLANT_WITH_ADDRESS_AND_APPOINTEE, rep)));
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
        assertTrue(LetterUtils.isAlternativeLetterFormatRequired(wrapper, new SubscriptionWithType(EMPTY_SUBSCRIPTION,
                subscriptionType, null, null)));
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
                .notificationEventType(APPEAL_RECEIVED)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue());
        subscriptionWithType.setPartyId("1");
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
                .notificationEventType(APPEAL_RECEIVED)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue().getAppointee());
        subscriptionWithType.setPartyId("2");
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
                .notificationEventType(APPEAL_RECEIVED)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue().getAppointee());
        subscriptionWithType.setPartyId("3");
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
                .notificationEventType(APPEAL_RECEIVED)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue());
        subscriptionWithType.setPartyId("2");
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
                .notificationEventType(APPEAL_RECEIVED)
                .build();
        NotificationWrapper wrapper = new CcdNotificationWrapper(caseDataWrapper);
        SubscriptionWithType subscriptionWithType = new SubscriptionWithType(EMPTY_SUBSCRIPTION, OTHER_PARTY,
            ccdValue.getValue(), ccdValue.getValue());
        subscriptionWithType.setPartyId("1");
        assertFalse(LetterUtils.isAlternativeLetterFormatRequired(wrapper, subscriptionWithType));
    }

    @Test
    @Parameters({"OTHER_PARTY, 4", "OTHER_PARTY, 3", "OTHER_PARTY, 2"})
    public void useOtherPartyLetterNameAndAddress(SubscriptionType subscriptionType, String otherPartyId) {
        NotificationWrapper wrapper = NotificationServiceTest.buildBaseWrapperOtherParty(SYA_APPEAL_CREATED, Appellant.builder().build(), null);
        final Address expectedAddress = getExpectedAddress(otherPartyId, wrapper);

        assertThat(LetterUtils.getAddressToUseForLetter(wrapper,
                new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, null, null, otherPartyId)),
            is(expectedAddress));

        final String expectedName = getExpectedName(otherPartyId, wrapper);
        assertThat(LetterUtils.getNameToUseForLetter(wrapper,
                new SubscriptionWithType(EMPTY_SUBSCRIPTION, subscriptionType, null, null, otherPartyId)),
            is(expectedName));

    }

    private Address getExpectedAddress(final String otherPartyId, final NotificationWrapper wrapper) {
        return requireNonNull(wrapper.getNewSscsCaseData().getOtherParties().stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getAddress()) : Pair.of(op.getId(), op.getAddress()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getAddress()) : null))
                .filter(Objects::nonNull)
                .filter(p -> p.getRight() != null && p.getLeft() != null)
                .filter(pair -> pair.getLeft().equals(String.valueOf(otherPartyId)))
                .findFirst()
                .map(Pair::getRight).orElse(null));
    }

    private String getExpectedName(final String otherPartyId, final NotificationWrapper wrapper) {
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

    private SscsCaseData setupTestData(DynamicList sender) {
        return SscsCaseData.builder()
                .originalSender(sender)
                .jointParty(JOINT_PARTY_WITH_ID)
                .otherParties(buildOtherPartyData())
                .appeal(Appeal.builder()
                    .appellant(APPELLANT_WITH_ID)
                    .rep(REPRESENTATIVE_WITH_ID)
                    .build())
                .build();
    }

    private List<CcdValue<OtherParty>> buildOtherPartyData() {
        return List.of(CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id("OP123456")
                .name(Name.builder()
                    .firstName("Other")
                    .lastName("Party")
                    .build())
                .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                .rep(Representative.builder()
                    .id("OPREP123456")
                    .name(Name.builder()
                        .firstName("OtherParty")
                        .lastName("Representative")
                        .build())
                    .hasRepresentative(YesNo.YES.getValue())
                    .build())
                .build())
            .build(),
            CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                    .id("OP7890123")
                    .name(Name.builder()
                        .firstName("Other 1")
                        .lastName("Party 1")
                        .build())
                    .otherPartySubscription(Subscription.builder().email("other@party").subscribeEmail("Yes").build())
                    .rep(Representative.builder()
                        .hasRepresentative(YesNo.NO.getValue())
                        .build())
                    .build())
                .build()
            );
    }

    @DisplayName("When sender is appellant then return name of case Appellant")
    @Test
    public void testGetNameForSenderAppellant() {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals("Tom Cat", LetterUtils.getNameForSender(caseData));
    }

    @DisplayName("When sender is representative then return name of representative")
    @Test
    public void testGetNameForSenderRepresentative() {
        DynamicList sender = new DynamicList(new DynamicListItem("representative", "Representative"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals("Representative Appellant", LetterUtils.getNameForSender(caseData));
    }

    @DisplayName("When sender is Joint Party then return name of the joint party")
    @Test
    public void testGetNameForSenderJointParty() {
        DynamicList sender = new DynamicList(new DynamicListItem("jointParty", "jointParty"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals("Joint Party", LetterUtils.getNameForSender(caseData));
    }

    @DisplayName("When sender is Joint Party then return name of the joint party")
    @Test
    public void testGetNameForSenderOtherParty() {
        DynamicList sender = new DynamicList(new DynamicListItem("jointParty1", "jointParty1"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals("", LetterUtils.getNameForSender(caseData));
    }

    @DisplayName("When sender is appellant then return name of case Appellant")
    @Test
    public void testGetNameForSenderEmpty() {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(null);
        assertEquals("", LetterUtils.getNameForSender(caseData));
    }

    @DisplayName("When sender is an Other Party then return name of Other Party")
    @Test
    public void testGetOtherPartyName() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOP123456", "Other Party 1"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals(Optional.of(Name.builder()
            .firstName("Other")
            .lastName("Party")
            .build()), LetterUtils.getOtherPartyName(caseData));
    }

    @DisplayName("When sender is an invalid Other Party then return empty.")
    @Test
    public void testGetOtherPartyNameWithInvalidOtherParty() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyded", "Other Party 1"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals(Optional.empty(), LetterUtils.getOtherPartyName(caseData));
    }

    @DisplayName("When sender is an Other Party Representative then return detail of other party representative")
    @Test
    public void testGetOtherPartyNameRE() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyRepOPREP123456", "Other party 1 - Representative - R Basker R Nadar"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals(Optional.of(Name.builder()
            .firstName("OtherParty")
            .lastName("Representative")
            .build()), LetterUtils.getOtherPartyName(caseData));
    }

    @DisplayName("When sender is an Other Party Representative is not found then return empty")
    @Test
    public void testGetOtherPartyNameRE1() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyRep34554", "Other party 1 - Representative - R Basker R Nadar"), new ArrayList<>());
        SscsCaseData caseData = setupTestData(sender);
        assertEquals(Optional.empty(), LetterUtils.getOtherPartyName(caseData));
    }

    @DisplayName("When sender is an Other Party Representative is not found then return empty")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence() {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Other party 1 - Representative - R Basker R Nadar"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender),
                SYA_APPEAL_CREATED);

        assertEquals("", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, SubscriptionWithType.builder().build()));
    }

    @DisplayName("When sender is an appellant and subscriber is appellant then return confirmation")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence1() {
        DynamicList sender = new DynamicList(new DynamicListItem("appellant", "Appellant"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().party(APPELLANT_WITH_ID).build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @Test
    @Parameters({"appellant", "representative", "jointParty"})
    public void testGetNotificationTypeForActionFurtherEvidence2(String requester) {
        DynamicList sender = new DynamicList(new DynamicListItem(requester, requester), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().party(APPELLANT_WITHOUT_ID).partyId(APPELLANT_WITHOUT_ID.getId()).build();
        assertEquals("notice", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is an representative, subscriber is appellant and appellant is not the represented "
            + "then return notification.")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence3() {
        DynamicList sender = new DynamicList(new DynamicListItem("representative", "Representative"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().party(APPELLANT_WITHOUT_ID).partyId(APPELLANT_WITH_ID.getId()).build();
        assertEquals("notice", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is a Joint party and subscriber is joint party then return confirmation.")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence6() {
        DynamicList sender = new DynamicList(new DynamicListItem("jointParty", "jointParty"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().party(JOINT_PARTY_WITH_ID).build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is an other party and subscriber is other than sender then return notification.")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence7() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherParty", "Other party 1 - Representative - R Basker R Nadar"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("asdfsd").build();

        assertEquals("notice", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender and subscriber are same other party then return confirmation.")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence8() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOP123456", "OP123456"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("OP123456").build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender and subscriber is an other party representative then return confirmation")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence9() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOPREP123456", "OPREP123456"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("OPREP123456").build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is an Other party representative and subscriber is an other party of the representative"
            + " then return confirmation")
    @Test
    public void testGetNotificationTypeForActionFurtherEvidence10() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOPREP123456", "OPREP123456"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("OP123456").build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is an other party and subscriber is his representative then return confirmation")
    @Test
    public void testIsValidOtherPartyWithRep2() {
        DynamicList sender = new DynamicList(new DynamicListItem("otherPartyOP123456", "OPREP123456"), new ArrayList<>());
        NotificationWrapper wrapper = buildBaseWrapperWithCaseData(setupTestData(sender), ACTION_FURTHER_EVIDENCE);
        SubscriptionWithType type = SubscriptionWithType.builder().partyId("otherPartyOPREP123456").build();
        assertEquals("confirmation", LetterUtils.getNotificationTypeForActionFurtherEvidence(wrapper, type));
    }

    @DisplayName("When sender is an other party and subscriber is his representative then return true")
    @Test
    public void testisValidOtherPartyRepresentative() {
        String senderId = "OPREP123456";
        String subscriptionId = "OPREP123456";
        List<CcdValue<OtherParty>>  otherParties = buildOtherPartyData();
        assertTrue(LetterUtils.isValidOtherPartyRepresentative(subscriptionId,
                senderId, otherParties.get(0)));
    }

    @DisplayName("When sender is an other party representative and subscriber is party then return confirmation")
    @Test
    public void testisValidOtherPartyRepresentative1() {
        String senderId = "OPREP123456";
        String subscriptionId = "OP123456";
        List<CcdValue<OtherParty>>  otherParties = buildOtherPartyData();
        assertTrue(LetterUtils.isValidOtherPartyRepresentative(subscriptionId,
                senderId, otherParties.get(0)));
    }

    public CcdNotificationWrapper buildBaseWrapperWithCaseData(SscsCaseData caseData, NotificationEventType type) {
        SscsCaseDataWrapper caseDataWrapper = SscsCaseDataWrapper.builder()
                .newSscsCaseData(caseData)
                .notificationEventType(type)
                .build();
        return new CcdNotificationWrapper(caseDataWrapper);
    }
}
