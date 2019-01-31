package uk.gov.hmcts.reform.sscs.service;

import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;

public class NotificationUtils {
    private NotificationUtils() {
        // empty
    }

    /* Sometimes the data for the appointee comes in with null populated objects */
    public static boolean hasAppointee(SscsCaseDataWrapper wrapper) {
        return (wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName() != null
            && wrapper.getNewSscsCaseData().getAppeal().getAppellant().getAppointee().getName().getFirstName() != null);
    }

    public static boolean hasRepresentative(SscsCaseDataWrapper wrapper) {
        return (wrapper.getNewSscsCaseData().getAppeal() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative() != null
            && wrapper.getNewSscsCaseData().getAppeal().getRep().getHasRepresentative().equalsIgnoreCase("yes"));
    }

    public static boolean hasAppointeeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getAppointeeSubscription();
    }

    public static boolean hasRepresentativeSubscription(SscsCaseDataWrapper wrapper) {
        return null != wrapper.getNewSscsCaseData().getSubscriptions().getRepresentativeSubscription();
    }
}
