package uk.gov.hmcts.reform.sscs.service;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.*;
import static uk.gov.hmcts.reform.sscs.service.NotificationUtils.hasAppointee;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.ReasonableAdjustments;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.domain.SubscriptionWithType;
import uk.gov.hmcts.reform.sscs.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.factory.NotificationWrapper;

public class LetterUtils {

    private LetterUtils() {
        // Hiding utility class constructor
    }

    public static Address getAddressToUseForLetter(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        if (REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
            return wrapper.getNewSscsCaseData().getAppeal().getRep().getAddress();
        } else if (JOINT_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
            if (equalsIgnoreCase("yes",
                    wrapper.getNewSscsCaseData().getJointPartyAddressSameAsAppellant())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
            }
            return wrapper.getNewSscsCaseData().getJointPartyAddress();
        } else if (OTHER_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
            return getAddressForOtherParty(wrapper.getNewSscsCaseData(), subscriptionWithType.getPartyId());
        } else {
            if (hasAppointee(wrapper.getSscsCaseDataWrapper())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getAddress();
            }

            return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAddress();
        }
    }

    private static Address getAddressForOtherParty(final SscsCaseData sscsCaseData, final int partyId) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getAddress()) : Pair.of(op.getId(), op.getAddress()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getAddress()) : null))
                .filter(Objects::nonNull)
                .filter(p -> p.getLeft() != null && p.getRight() != null)
                .filter(p -> p.getLeft().equals(String.valueOf(partyId)))
                .map(Pair::getRight)
                .findFirst()
                .orElse(null);
    }

    public static Optional<Name> getNameForOtherParty(SscsCaseData sscsCaseData, final int partyId) {
        return emptyIfNull(sscsCaseData.getOtherParties()).stream()
                .map(CcdValue::getValue)
                .flatMap(op -> Stream.of((op.hasAppointee()) ? Pair.of(op.getAppointee().getId(), op.getAppointee().getName()) : Pair.of(op.getId(), op.getName()), (op.hasRepresentative()) ? Pair.of(op.getRep().getId(), op.getRep().getName()) : null))
                .filter(Objects::nonNull)
                .filter(p -> p.getLeft() != null && p.getRight() != null)
                .filter(p -> p.getLeft().equals(String.valueOf(partyId)))
                .map(Pair::getRight)
                .findFirst();
    }

    public static String getNameToUseForLetter(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        if (REPRESENTATIVE.equals(subscriptionWithType.getSubscriptionType())) {
            return SendNotificationHelper.getRepSalutation(wrapper.getNewSscsCaseData().getAppeal().getRep(), false);
        } else if (JOINT_PARTY.equals(subscriptionWithType.getSubscriptionType())) {
            return format("%s %s",wrapper.getNewSscsCaseData().getJointPartyName().getFirstName(), wrapper.getNewSscsCaseData().getJointPartyName().getLastName());
        } else {
            if (subscriptionWithType.getPartyId() > 0 && isNotEmpty(wrapper.getNewSscsCaseData().getOtherParties())) {
                return getNameForOtherParty(wrapper.getNewSscsCaseData(), subscriptionWithType.getPartyId()).map(Name::getFullNameNoTitle).orElse("");
            }
            if (hasAppointee(wrapper.getSscsCaseDataWrapper())) {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle();
            } else {
                return wrapper.getNewSscsCaseData().getAppeal().getAppellant().getName().getFullNameNoTitle();
            }
        }
    }

    public static byte[] addBlankPageAtTheEndIfOddPage(byte[] letter) throws IOException {
        if (ArrayUtils.isNotEmpty(letter)) {
            PDDocument loadDoc = PDDocument.load(letter);
            if (loadDoc.getNumberOfPages() % 2 != 0) {
                final PDPage blankPage = new PDPage(PDRectangle.A4);
                // need to add PDPageContentStream here to pass gov notify validation!
                PDPageContentStream contents = new PDPageContentStream(loadDoc, blankPage);
                contents.beginText();
                contents.endText();
                contents.close();
                loadDoc.addPage(blankPage);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                loadDoc.save(baos);
                loadDoc.close();
                byte[] bytes = baos.toByteArray();
                baos.close();
                return bytes;
            }
        }
        return letter;
    }

    public static byte[] buildBundledLetter(byte[] coveringLetter, byte[] directionText) throws IOException {
        if (coveringLetter != null && directionText != null) {
            PDDocument bundledLetter = PDDocument.load(coveringLetter);

            PDDocument loadDoc = PDDocument.load(directionText);

            final PDFMergerUtility merger = new PDFMergerUtility();
            merger.appendDocument(bundledLetter, loadDoc);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bundledLetter.save(baos);
            bundledLetter.close();

            return baos.toByteArray();
        } else {
            throw new NotificationClientRuntimeException("Can not bundle empty documents");
        }
    }

    public static boolean isAlternativeLetterFormatRequired(NotificationWrapper wrapper, SubscriptionWithType subscriptionWithType) {
        YesNo wantsReasonableAdjustment = YesNo.NO;
        ReasonableAdjustments resAdj = wrapper.getNewSscsCaseData().getReasonableAdjustments();

        if (resAdj != null) {
            switch (subscriptionWithType.getSubscriptionType()) {
                case APPELLANT:
                    wantsReasonableAdjustment = resAdj.getAppellant() != null && resAdj.getAppellant().getWantsReasonableAdjustment() != null ? resAdj.getAppellant().getWantsReasonableAdjustment() : YesNo.NO;
                    break;
                case JOINT_PARTY:
                    wantsReasonableAdjustment = resAdj.getJointParty() != null && resAdj.getJointParty().getWantsReasonableAdjustment() != null ? resAdj.getJointParty().getWantsReasonableAdjustment() : YesNo.NO;
                    break;
                case APPOINTEE:
                    wantsReasonableAdjustment = resAdj.getAppointee() != null && resAdj.getAppointee().getWantsReasonableAdjustment() != null ? resAdj.getAppointee().getWantsReasonableAdjustment() : YesNo.NO;
                    break;
                case REPRESENTATIVE:
                    wantsReasonableAdjustment = resAdj.getRepresentative() != null && resAdj.getRepresentative().getWantsReasonableAdjustment() != null ? resAdj.getRepresentative().getWantsReasonableAdjustment() : YesNo.NO;
                    break;
                default:
                    wantsReasonableAdjustment = YesNo.NO;
            }
        }
        return wantsReasonableAdjustment.equals(YesNo.YES);
    }
}
