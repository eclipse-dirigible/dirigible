import { client as httpClient} from "@aerokit/sdk/http"
import { url } from "@aerokit/sdk/utils"

/**
 * Configuration structure for the OAuth client.
 */
export interface OAuthClientConfig {
    /** The URL endpoint for the OAuth token service (e.g., '/oauth/token'). */
    readonly url: string;
    /** The client ID for authentication. */
    readonly clientId: string;
    /** The client secret for authentication. */
    readonly clientSecret: string;
    /** The grant type to be used. Defaults to 'client_credentials'. */
    readonly grantType?: string;
}

/**
 * A client class for fetching OAuth access tokens.
 *
 * It uses the HTTP client to send a POST request with client credentials
 * to the specified token endpoint.
 */
export class OAuthClient {
    private config: OAuthClientConfig;

    /**
     * Initializes the OAuthClient with the required configuration.
     * Sets 'client_credentials' as the default grant type if none is provided.
     *
     * @param config The configuration object containing URL, client ID, and secret.
     */
    constructor(config: OAuthClientConfig) {
        this.config = config;
        if (!config.grantType) {
            // @ts-ignore
            config.grantType = "client_credentials";
        }
    }

    /**
     * Executes the OAuth token request and returns the parsed response.
     *
     * The request uses the client credentials grant type (default) and
     * sends credentials as URL-encoded parameters in the body.
     *
     * @returns A parsed JSON object containing the OAuth token (e.g., { access_token: string, expires_in: number, ... }).
     * @throws {Error} If the HTTP status code is not 200.
     */
    public getToken() {
        // Prepare the request body parameters
        const params = [{
            name: "grant_type",
            value: this.config.grantType
        }, {
            name: "client_id",
            // The client ID is URL-encoded
            value: url.encode(this.config.clientId)
        }, {
            name: "client_secret",
            // The client secret is URL-encoded
            value: url.encode(this.config.clientSecret)
        }];

        const oauthResponse = httpClient.post(this.config.url, {
            params: params,
            headers: [{
                name: "Content-Type",
                value: "application/x-www-form-urlencoded"
            }]
        });

        if (oauthResponse.statusCode !== 200) {
            const errorMessage = `Error occurred while retrieving OAuth token. Status code: [${oauthResponse.statusCode}], text: [${oauthResponse.text}]`;
            console.error(errorMessage);
            throw new Error(errorMessage);
        }

        // Parse and return the token response
        return JSON.parse(oauthResponse.text);
    }
}
