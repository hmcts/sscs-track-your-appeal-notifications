package uk.gov.hmcts.reform.sscs.factory;

public class NotificationFactoryHelper {

    private NotificationFactoryHelper() {
        // Hidden
    }

    public static NotificationFactory create(PersonalisationFactory personalisationFactory) {
        return new NotificationFactory(personalisationFactory);
    }
}
