package uk.gov.hmcts.sscs.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Subscriptions {
    Subscription appellantSubscription;
    Subscription supporterSubscription;
}
