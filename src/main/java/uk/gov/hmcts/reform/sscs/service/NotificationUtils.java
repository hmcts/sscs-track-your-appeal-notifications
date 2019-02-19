package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.APPELLANT;
import static uk.gov.hmcts.reform.sscs.config.SubscriptionType.REPRESENTATIVE;

import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

public class NotificationUtils {
    private NotificationUtils() {
        // empty
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasAppointee(SscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant() != null
            && hasAppointee(wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee());
    }

    public static boolean hasAppointee(Appointee appointee) {
        return appointee != null && appointee.getName() != null && appointee.getName().getFirstName() != null
                && appointee.getName().getLastName() != null;
    }

    public static boolean hasRepresentative(SscsCaseDataWrapper wrapper) {
        return wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative().equalsIgnoreCase("yes");
    }

    public static boolean hasAppointeeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
    }

    public static boolean hasRepresentativeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
    }

    public static Subscription getSubscription(SscsCaseData sscsCaseData, SubscriptionType subscriptionType) {
        if (REPRESENTATIVE.equals(subscriptionType)) {
            return sscsCaseData.getSubscriptions().getRepresentativeSubscription();
        } else if (APPELLANT.equals(subscriptionType)) {
            return sscsCaseData.getSubscriptions().getAppellantSubscription();
        } else {
            return sscsCaseData.getSubscriptions().getAppointeeSubscription();
        }
    }
}
