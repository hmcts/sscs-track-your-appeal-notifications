package uk.gov.hmcts.sscs;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.gov.hmcts.sscs.domain.CcdResponse;
import uk.gov.hmcts.sscs.domain.Hearing;
import uk.gov.hmcts.sscs.domain.Subscription;
import uk.gov.hmcts.sscs.domain.Subscriptions;
import uk.gov.hmcts.sscs.domain.notify.Event;
import uk.gov.hmcts.sscs.domain.notify.EventType;

public final class CcdResponseUtils {
    private CcdResponseUtils() {
    }

    public static CcdResponse buildCcdResponse(String caseReference) {

        Hearing hearing = Hearing.builder()
            .venueName("The venue")
            .venueAddressLine1("12 The Road Avenue")
            .venueAddressLine2("Village")
            .venueTown("Aberdeen")
            .venueCounty("Aberdeenshire")
            .venuePostcode("AB12 0HN")
            .venueGoogleMapUrl("http://www.googlemaps.com/aberdeenvenue")
            .hearingDateTime(LocalDateTime.now())
            .build();

        List<Hearing> hearingsList = new ArrayList<>();
        hearingsList.add(hearing);

        Event event = Event.builder()
            .eventType(EventType.APPEAL_CREATED)
            .dateTime(ZonedDateTime.now())
            .build();

        Subscription appellantSubscription = Subscription.builder()
            .appealNumber("")
            .email("")
            .mobileNumber("")
            .subscribeEmail(false)
            .subscribeSms(false)
            .build();
        Subscription supporterSubscription = Subscription.builder()
            .appealNumber("")
            .email("")
            .mobileNumber("")
            .subscribeEmail(false)
            .subscribeSms(false)
            .build();
        Subscriptions subscriptions = Subscriptions.builder()
            .appellantSubscription(appellantSubscription)
            .supporterSubscription(supporterSubscription)
            .build();

        return CcdResponse.builder()
            .caseReference(caseReference)
            .hearings(hearingsList)
            .events(Collections.singletonList(event))
            .subscriptions(subscriptions)
            .build();
    }
}
