package uk.gov.hmcts.reform.sscs;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.SscsCaseDataWrapper;
import uk.gov.hmcts.reform.sscs.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.factory.CcdNotificationWrapper;

public final class SscsCaseDataUtils {

    public static final String CASE_ID = "1234";

    private SscsCaseDataUtils() {
    }

    public static SscsCaseData buildSscsCaseData(String caseReference, String subscribeEmail, String subscribeSms) {
        return buildSscsCaseData(
                caseReference,
                subscribeEmail,
                subscribeSms,
                EventType.APPEAL_RECEIVED, "oral"
        );
    }

    public static SscsCaseData buildSscsCaseData(
            String caseReference,
            String subscribeEmail,
            String subscribeSms,
            EventType eventType,
            String hearingType
    ) {
        return builderSscsCaseData(caseReference, subscribeEmail, subscribeSms, eventType, hearingType).build();
    }

    public static SscsCaseData.SscsCaseDataBuilder builderSscsCaseData(
            String caseReference,
            String subscribeEmail,
            String subscribeSms,
            EventType eventType,
            String hearingType
    ) {
        Name name = Name.builder()
                .title("Mr")
                .firstName("User")
                .lastName("Test")
                .build();
        Contact contact = Contact.builder()
                .email("test-mail@hmcts.net")
                .phone("01234567890")
                .build();
        Identity identity = Identity.builder()
                .dob("1904-03-10")
                .nino("AB 22 55 66 B")
                .build();
        Appellant appellant = Appellant.builder()
                .name(name)
                .contact(contact)
                .identity(identity)
                .build();

        HearingOptions hearingOptions = HearingOptions.builder()
                .wantsToAttend("Yes")
                .wantsSupport("Yes")
                .languageInterpreter("Yes")
                .other("No")
                .build();

        Representative rep = Representative.builder()
                .name(Name.builder().firstName("Harry").lastName("Potter").build())
                .hasRepresentative("Yes").build();

        final Appeal appeal = Appeal.builder()
                .appellant(appellant)
                .rep(rep)
                .benefitType(BenefitType.builder().code("ESA").build())
                .hearingOptions(hearingOptions)
                .hearingType(hearingType)
                .build();

        Event events = Event.builder()
                .value(EventDetails.builder()
                        .type(eventType.getCcdType())
                        .description("Some Events")
                        .date("2017-05-24T14:01:18.243")
                        .build())
                .build();

        Subscription appellantSubscription = Subscription.builder()
                .tya("")
                .email("sscstest+notify@greencroftconsulting.com")
                .mobile("07398785050")
                .subscribeEmail(subscribeEmail)
                .subscribeSms(subscribeSms)
                .build();
        Subscription representativeSubscription = Subscription.builder()
                .tya("")
                .email("sscstest+notify@greencroftconsulting.com")
                .mobile("07398785050")
                .subscribeEmail("Yes")
                .subscribeSms("Yes")
                .build();
        Subscriptions subscriptions = Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .representativeSubscription(representativeSubscription)
                .build();

        return SscsCaseData.builder()
                .caseReference(caseReference)
                .appeal(appeal)
                .events(Collections.singletonList(events))
                .subscriptions(subscriptions);
    }


    public static CcdNotificationWrapper buildBasicCcdNotificationWrapper(NotificationEventType notificationType) {
        return buildBasicCcdNotificationWrapper(notificationType, null);
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapper(NotificationEventType notificationType,
                                                                          String hearingType) {
        return new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .notificationEventType(notificationType)
                .newSscsCaseData(
                        SscsCaseData.builder()
                                .appeal(Appeal.builder()
                                        .hearingType(hearingType)
                                        .build())
                                .ccdCaseId(CASE_ID)
                                .events(Collections.emptyList())
                                .hearings(Collections.emptyList()).build())
                .build());
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapperWithEvent(
            NotificationEventType notificationType,
            EventType eventType,
            String eventDate
    ) {
        Event event = Event
                .builder()
                .value(EventDetails
                        .builder()
                        .date(eventDate)
                        .type(eventType.getCcdType())
                        .build()
                )
                .build();

        return new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .notificationEventType(notificationType)
                .newSscsCaseData(
                        SscsCaseData.builder()
                                .ccdCaseId(CASE_ID)
                                .events(Collections.singletonList(event))
                                .hearings(Collections.emptyList())
                                .build())
                .build());
    }

    public static CcdNotificationWrapper buildBasicCcdNotificationWrapperWithHearing(
            NotificationEventType notificationType,
            String hearingDate,
            String hearingTime
    ) {
        Hearing hearing = Hearing
                .builder()
                .value(HearingDetails
                        .builder()
                        .hearingDate(hearingDate)
                        .time(hearingTime)
                        .build()
                )
                .build();

        return new CcdNotificationWrapper(SscsCaseDataWrapper.builder()
                .notificationEventType(notificationType)
                .newSscsCaseData(SscsCaseData.builder()
                        .ccdCaseId(CASE_ID)
                        .events(Collections.emptyList())
                        .hearings(Collections.singletonList(hearing))
                        .build())
                .build());
    }

    public static void addEventTypeToCase(SscsCaseData response, EventType eventType) {
        Date now = new Date();
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

        Event events = Event.builder()
                .value(EventDetails.builder()
                        .type(eventType.getCcdType())
                        .description(eventType.getCcdType())
                        .date(dt1.format(now))
                        .build())
                .build();

        List<Event> addedEvents = new ArrayList<>(response.getEvents());
        addedEvents.add(events);
        response.setEvents(addedEvents);
    }

    public static void addEvidence(SscsCaseData response) {
        List<Document> documents = new ArrayList<>();

        Document doc = Document.builder().value(DocumentDetails.builder()
                .dateReceived("2016-01-01")
                .evidenceType("Medical")
                .evidenceProvidedBy("Caseworker").build()).build();

        documents.add(doc);

        Evidence evidence = Evidence.builder().documents(documents).build();

        response.setEvidence(evidence);
    }

    public static List<Hearing> addHearing(SscsCaseData response, Integer hearingDaysFromNow) {
        Hearing hearing = Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(hearingDaysFromNow).toString())
                .time("23:59")
                .venue(Venue.builder()
                        .name("The venue")
                        .address(Address.builder()
                                .line1("12 The Road Avenue")
                                .line2("Village")
                                .town("Aberdeen")
                                .county("Aberdeenshire")
                                .postcode("AB12 0HN").build())
                        .googleMapLink("http://www.googlemaps.com/aberdeenvenue")
                        .build()).build()).build();

        List<Hearing> hearingsList = new ArrayList<>();
        hearingsList.add(hearing);
        response.setHearings(hearingsList);

        return hearingsList;
    }

    public static void addAppointee(SscsCaseData response) {
        Appointee appointee = Appointee.builder()
                .name(Name.builder()
                        .firstName("Appointee")
                        .lastName("User")
                        .build())
                .build();
        Subscription appointeeSubscription = Subscription.builder()
                .email("sscstest+notify2@greencroftconsulting.com")
                .mobile("07398785051")
                .subscribeEmail("Yes")
                .subscribeSms("Yes")
                .build();
        Subscriptions subscriptions = response.getSubscriptions().toBuilder()
                .appointeeSubscription(appointeeSubscription).build();

        response.getAppeal().getAppellant().setAppointee(appointee);
        response.setSubscriptions(subscriptions);
    }
}

