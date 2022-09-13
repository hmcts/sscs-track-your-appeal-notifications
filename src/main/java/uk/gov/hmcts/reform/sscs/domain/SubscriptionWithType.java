package uk.gov.hmcts.reform.sscs.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Party;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.config.SubscriptionType;

@Data
@AllArgsConstructor
@Builder
public class SubscriptionWithType {
    private Subscription subscription;
    private SubscriptionType subscriptionType;
    private Party party;
    private Entity entity;
    private int partyId;

    public SubscriptionWithType(Subscription subscription, SubscriptionType subscriptionType,
                                Party party, Entity entity) {
        this.subscription = subscription;
        this.subscriptionType = subscriptionType;
        this.party = party;
        this.entity = entity;
    }
}
