package uk.gov.hmcts.sscs.domain.idam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdamTokens {
    String idamOauth2Token;
    String authenticationService;
}
