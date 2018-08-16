package uk.gov.hmcts.sscs.service.idam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.sscs.domain.idam.IdamTokens;

@Service
public class IdamTokensService {

    private IdamService idamService;

    @Autowired
    public IdamTokensService(IdamService idamService) {
        this.idamService = idamService;
    }

    public IdamTokens getIdamTokens() {
        return IdamTokens.builder()
                .idamOauth2Token(idamService.getIdamOauth2Token())
                .serviceAuthorization(idamService.generateServiceAuthorization())
                .userId(idamService.getUserId(idamService.getIdamOauth2Token()))
                .build();
    }
}
