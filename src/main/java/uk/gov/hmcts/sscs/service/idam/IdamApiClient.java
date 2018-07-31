package uk.gov.hmcts.sscs.service.idam;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.sscs.domain.idam.Authorize;

@FeignClient(
    name = "idam-api",
    url = "${idam.url}"
)
public interface IdamApiClient {

    @PostMapping(
        consumes = APPLICATION_JSON_VALUE,
        value = "/oauth2/authorize"
    )
    Authorize authorizeCodeType(
        @RequestHeader(HttpHeaders.AUTHORIZATION) final String authorisation,
        @RequestParam("response_type") final String responseType,
        @RequestParam("client_id") final String clientId,
        @RequestParam("redirect_uri") final String redirectUri
    );

    default Authorize authorizeToken(
        final String code,
        final String grantType,
        final String redirectUri,
        final String clientId,
        final String clientSecret
    ) {
        return authorizeToken(
            ImmutableMap.of(
                "code", code,
                "grant_type", grantType,
                "redirect_uri", redirectUri,
                "client_id", clientId,
                "client_secret", clientSecret
            )
        );
    }

    @PostMapping(
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        value = "/oauth2/token"
    )
    Authorize authorizeToken(
        Map<String, ?> formParams
    );

}
