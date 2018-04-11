package uk.gov.hmcts.sscs.models.idam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdamTokens {
    String idamOauth2Token;
    String authenticationService;
}
