package uk.gov.hmcts.sscs.service;

import feign.FeignException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.sscs.exception.AuthorisationException;

@Service
public class AuthorisationService {

    public static final String SERVICE_AUTHORISATION_HEADER = "ServiceAuthorization";

    private final ServiceAuthorisationApi serviceAuthorisationApi;

    public AuthorisationService(ServiceAuthorisationApi serviceAuthorisationApi) {
        this.serviceAuthorisationApi = serviceAuthorisationApi;
    }

    public Boolean authorise(String serviceAuthHeader) {
        try {
            serviceAuthorisationApi.getServiceName(serviceAuthHeader);
            return true;
        } catch (FeignException exc) {
            boolean isClientError = exc.status() >= 400 && exc.status() <= 499;

            throw isClientError ? new AuthorisationException(exc.getMessage(), exc) : exc;
        }
    }
}
