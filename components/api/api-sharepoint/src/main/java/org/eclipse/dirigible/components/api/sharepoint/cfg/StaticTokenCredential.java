package org.eclipse.dirigible.components.api.sharepoint.cfg;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * A testing class which will could be used for local testing. A token could be got from the Graph
 * Explorer https://developer.microsoft.com/en-us/graph/graph-explorer credentials
 */
class StaticTokenCredential implements TokenCredential {

    private final String token;

    public StaticTokenCredential(String token) {
        this.token = token;
    }

    @Override
    public Mono<AccessToken> getToken(TokenRequestContext request) {
        return Mono.just(new AccessToken(token, OffsetDateTime.now()
                                                              .plusHours(1)));
    }
}
