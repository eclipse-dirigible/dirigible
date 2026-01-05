/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { repository as repositoryManager } from "@aerokit/sdk/platform";
import * as documentsUtils from "../../utils/cmis/document";
import * as folderUtils from "../../utils/cmis/folder";
import * as objectUtils from "../../utils/cmis/object";
import { bytes, streams } from "@aerokit/sdk/io";

const INTERNAL_FOLDER = "__internal";
const INTERNAL_FOLDER_LOCATION = INTERNAL_FOLDER;
const ACCESS_CONSTRAINTS_FILE = "roles-access.json";
const ACCESS_CONSTRAINTS_FILE_LOCATION = INTERNAL_FOLDER + "/" + ACCESS_CONSTRAINTS_FILE;

function updateAccessDefinitionsInCMS(data) {

    if (!folderUtils.existFolder(INTERNAL_FOLDER_LOCATION)) {
        let rootFolder = folderUtils.getFolderOrRoot("/");
        folderUtils.createFolder(rootFolder, INTERNAL_FOLDER);
    }
    var folder = folderUtils.getFolder(INTERNAL_FOLDER_LOCATION);
    let document = {
        getName: function () {
            return ACCESS_CONSTRAINTS_FILE;
        },
        getContentType: function () {
            return "application/json";
        },
        getSize: function () {
            return data.length;
        },
        getInputStream: function () {
            let contentBytes = bytes.textToByteArray(data);
            return streams.createByteArrayInputStream(contentBytes);
        }
    }


    if (objectUtils.existObject(ACCESS_CONSTRAINTS_FILE_LOCATION)) {
        let object = objectUtils.getObject(ACCESS_CONSTRAINTS_FILE_LOCATION);
        objectUtils.deleteObject(object);
    }

    documentsUtils.uploadDocument(folder, document);
}

export const getAccessDefinitions = () => {
    let content = {
        constraints: []
    };
    if (documentsUtils.existDocument(ACCESS_CONSTRAINTS_FILE_LOCATION)) {
        let document = documentsUtils.getDocument(ACCESS_CONSTRAINTS_FILE_LOCATION);

        try {
            let inputStream = documentsUtils.getDocumentStream(document);
            let data = inputStream.getStream().readBytes();
            content = JSON.parse(bytes.byteArrayToText(data));
        } catch (e) {
            // Do nothing
        }
    }
    return content;
};

export const updateAccessDefinitions = (accessDefinitions) => {
    let path = "/registry/public/documents/security/roles.access";
    let content = JSON.stringify(accessDefinitions);
    let resource = repositoryManager.getResource(path);
    if (resource.exists()) {
        if (resource.getText() !== content) {
            repositoryManager.deleteResource(path);
            repositoryManager.createResource(path, content);
            updateAccessDefinitionsInCMS(content);
        }
    } else {
        repositoryManager.createResource(path, content);
        updateAccessDefinitionsInCMS(content);
    }
};
