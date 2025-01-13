/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/**
 * API Destinations
 *
 * Note: This module is supported only with the Mozilla Rhino engine
 */

import { Configurations } from "sdk/core/configurations";
import { HttpClient } from "sdk/http/client";
import { OAuthClient } from "sdk/security/oauth";

const JClass = Java.type("java.lang.Class");
const DestinationsFacade = Java.type("org.eclipse.dirigible.components.api.core.DestinationsFacade");

export class Destinations {

    public static get(name: string): any {
        if (this.isCloudEnvironment()) {
            return JSON.parse(this.getCloudDestination(name));
        }
        return JSON.parse(DestinationsFacade.get(name));
    };

    public static set(name: string, destination: any) {
        if (this.isCloudEnvironment()) {
            this.createOrUpdateCloudDestination(name, destination);
        }
        DestinationsFacade.set(name, JSON.stringify(destination));
    };

    public static remove(name: string) {
        if (!this.isCloudEnvironment()) {
            throw new Error("The delete destination operation is not supported for non-cloud environments");
        }
        this.deleteCloudDestination(name);
    };

    public static isCloudEnvironment() {
        try {
            JClass.forName("org.eclipse.dirigible.cf.CloudFoundryModule");
            return true;
        } catch (e) {
            // Do nothing
        }
        try {
            JClass.forName("org.eclipse.dirigible.kyma.KymaModule");
            return true;
        } catch (e) {
            // Do nothing
        }
        return false;
    }

    private static getCloudDestination(name: string) {
        let token = this.getOAuthToken();

        let destinationUrl = `${this.getDestinationsBasePath()}/${name}`;

        let requestOptions = {
            headers: [{
                name: "Authorization",
                value: `${token.token_type} ${token.access_token}`
            }]
        };

        let response = HttpClient.get(destinationUrl, requestOptions);

        if (response.statusCode === 404) {
            throw new Error(`Destination with name '${name}' not found`);
        };

        return response.text;

    }

    private static getDestinationsBasePath() {
        return `${Configurations.get("DIRIGIBLE_DESTINATION_URI")}/destination-configuration/v1/destinations`;
    }

    private static getInstanceDestinationsBasePath() {
        return `${Configurations.get("DIRIGIBLE_DESTINATION_URI")}/destination-configuration/v1/instanceDestinations`;
    }

    private static getSubaccountDestinationsBasePath() {
        return `${Configurations.get("DIRIGIBLE_DESTINATION_URI")}/destination-configuration/v1/subaccountDestinations`;
    }

    private static createOrUpdateCloudDestination(name: string, destination) {
        let isExistingDestination = true;
        try {
            this.getCloudDestination(name);
        } catch (e) {
            isExistingDestination = false;
        }

        let instanceDestinationUrl = this.getInstanceDestinationsBasePath();
        destination.Name = name;

        let token = this.getOAuthToken();
        let requestOptions = {
            headers: [{
                name: "Authorization",
                value: `${token.token_type} ${token.access_token}`
            }],
            text: JSON.stringify(destination)
        };

        if (isExistingDestination) {
            let response = HttpClient.put(instanceDestinationUrl, requestOptions);
            if (response.statusCode != 200) {
                throw new Error(`Error occurred while updating destinaton '${name}': ${response.text}`);
            }
        } else {
            let response = HttpClient.post(instanceDestinationUrl, requestOptions);
            if (response.statusCode != 201) {
                throw new Error(`Error occurred while creating destinaton '${name}': ${response.text}`);
            }
        }
    }

    private static deleteCloudDestination(name: string) {
        let token = this.getOAuthToken();
        let requestOptions = {
            headers: [{
                name: "Authorization",
                value: `${token.token_type} ${token.access_token}`
            }]
        };
        let url = `${this.getInstanceDestinationsBasePath()}/${name}`;
        let response = HttpClient.del(url, requestOptions);
        if (response.statusCode != 200) {
            throw new Error(`Error occurred while deleting destinaton '${name}': ${response.text}`);
        }
    }

    private static getOAuthToken() {
        let oauthClientId = Configurations.get("DIRIGIBLE_DESTINATION_CLIENT_ID");
        let oauthClientSecret = Configurations.get("DIRIGIBLE_DESTINATION_CLIENT_SECRET");
        let oauthUrl = Configurations.get("DIRIGIBLE_DESTINATION_URL");
        let uri = Configurations.get("DIRIGIBLE_DESTINATION_URI");

        if (!oauthClientId || !oauthClientSecret || !oauthUrl || !uri) {
            throw new Error("Invalid destination configuration");
        }

        const client = new OAuthClient({
            url: oauthUrl,
            clientId: oauthClientId,
            clientSecret: oauthClientSecret
        });
        return client.getToken();
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
	// @ts-ignore
	module.exports = Destinations;
}
