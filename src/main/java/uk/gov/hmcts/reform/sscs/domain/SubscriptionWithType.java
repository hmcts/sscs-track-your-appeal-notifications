package uk.gov.hmcts.reform.sscs.domain;

import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;

@Value
public class SubscriptionWithType {
    Subscription subscription;
    SubscriptionType subscriptionType;
}
