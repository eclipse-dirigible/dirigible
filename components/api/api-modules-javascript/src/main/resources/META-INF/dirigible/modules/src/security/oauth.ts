/**
 * @module security/oauth
 * @package @aerokit/sdk/security
 * @name OAuthClient
 * @overview
 * 
 * The OAuthClient class provides a simple interface for obtaining OAuth access tokens using the client credentials grant type. It abstracts the process of making HTTP requests to an OAuth token endpoint, allowing developers to easily integrate OAuth authentication into their applications. By providing the necessary configuration parameters such as the token endpoint URL, client ID, and client secret, developers can use this class to retrieve access tokens that can be used for authenticating API requests or accessing protected resources.
 * 
 * ### Key Features:
 * - **Token Retrieval**: The `getToken` method sends a POST request to the specified OAuth token endpoint with the client credentials and retrieves the access token.
 * - **Configurable Grant Type**: While the default grant type is set to 'client_credentials', developers can specify other grant types if needed by providing it in the configuration.
 * 
 * ### Use Cases:
 * - **API Authentication**: The OAuthClient can be used to authenticate API requests by obtaining access tokens that are required for accessing protected endpoints.
 * - **Integration with OAuth Providers**: Developers can use this class to integrate their applications with various OAuth providers that support the client credentials grant type, enabling secure access to resources.
 * 
 * ### Example Usage:
 * ```ts
 * import { OAuthClient } from "@aerokit/sdk/security";
 * 
 * const oauthClient = new OAuthClient({
 *   url: "https://example.com/oauth/token",
 *   clientId: "your-client-id",
 *   clientSecret: "your-client-secret"
 * });
 * ```
 */

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
