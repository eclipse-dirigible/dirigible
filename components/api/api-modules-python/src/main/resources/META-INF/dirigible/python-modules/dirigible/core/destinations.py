# Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v2.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v20.html
# SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0

from org.eclipse.dirigible.components.api.core import DestinationsFacade
from org.eclipse.dirigible.components.api.cf import CloudFoundryModule
from org.eclipse.dirigible.components.api.kyma import KymaModule
from org.eclipse.dirigible.http.client import httpClient
from java.lang import Class
import json

def get(name):
    if is_cloud_environment():
        return json.loads(get_cloud_destination(name))
    return json.loads(DestinationsFacade.get(name))

def set(name, destination):
    if is_cloud_environment():
        create_or_update_cloud_destination(name, destination)
    DestinationsFacade.set(name, json.dumps(destination))

def remove(name):
    if not is_cloud_environment():
        raise Exception("The delete destination operation is not supported for non-cloud environments")
    delete_cloud_destination(name)

def is_cloud_environment():
    try:
        Class.forName("org.eclipse.dirigible.cf.CloudFoundryModule")
        return True
    except:
        pass

    try:
        Class.forName("org.eclipse.dirigible.kyma.KymaModule")
        return True
    except:
        pass

    return False

def get_cloud_destination(name):
    token = get_oauth_token()
    destination_url = f"{get_destinations_base_path()}/{name}"
    headers = [
        {"name": "Authorization", "value": f"{token.token_type} {token.access_token}"}
    ]
    response = httpClient.get(destination_url, {"headers": headers})

    if response.statusCode == 404:
        raise Exception(f"Destination with name '{name}' not found")

    return response.text

def get_destinations_base_path():
    return f"{configurations.get('DIRIGIBLE_DESTINATION_URI')}/destination-configuration/v1/destinations"

def get_instance_destinations_base_path():
    return f"{configurations.get('DIRIGIBLE_DESTINATION_URI')}/destination-configuration/v1/instanceDestinations"

def create_or_update_cloud_destination(name, destination):
    is_existing_destination = True
    try:
        get_cloud_destination(name)
    except:
        is_existing_destination = False

    instance_destination_url = get_instance_destinations_base_path()
    destination["Name"] = name
    token = get_oauth_token()
    headers = [
        {"name": "Authorization", "value": f"{token.token_type} {token.access_token}"}
    ]
    options = {
        "headers": headers,
        "text": json.dumps(destination)
    }

    if is_existing_destination:
        response = httpClient.put(instance_destination_url, options)
        if response.statusCode != 200:
            raise Exception(f"Error occurred while updating destination '{name}': {response.text}")
    else:
        response = httpClient.post(instance_destination_url, options)
        if response.statusCode != 201:
            raise Exception(f"Error occurred while creating destination '{name}': {response.text}")

def delete_cloud_destination(name):
    token = get_oauth_token()
    headers = [
        {"name": "Authorization", "value": f"{token.token_type} {token.access_token}"}
    ]
    url = f"{get_instance_destinations_base_path()}/{name}"
    response = httpClient.delete(url, {"headers": headers})
    if response.statusCode != 200:
        raise Exception(f"Error occurred while deleting destination '{name}': {response.text}")

def get_oauth_token():
    oauth_client_id = configurations.get("DIRIGIBLE_DESTINATION_CLIENT_ID")
    oauth_client_secret = configurations.get("DIRIGIBLE_DESTINATION_CLIENT_SECRET")
    oauth_url = configurations.get("DIRIGIBLE_DESTINATION_URL")
    uri = configurations.get("DIRIGIBLE_DESTINATION_URI")

    if not oauth_client_id or not oauth_client_secret or not oauth_url or not uri:
        raise Exception("Invalid destination configuration")

    oauth_config = {
        "url": oauth_url,
        "clientId": oauth_client_id,
        "clientSecret": oauth_client_secret
    }

    client = oauth.getClient(oauth_config)
    return client.getToken()
