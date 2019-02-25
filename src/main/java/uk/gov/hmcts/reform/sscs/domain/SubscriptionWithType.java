package uk.gov.hmcts.reform.sscs.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;

@Data
@AllArgsConstructor
public class SubscriptionWithType {
    Subscription subscription;
    SubscriptionType subscriptionType;
}
