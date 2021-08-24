/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
var restTemplateManager = require("template-application-rest-mongodb/template/template");
var apiTemplate = require("template-application-ui-angular-mongodb/template/api/template");
var uiTemplate = require("template-application-ui-angular-mongodb/template/ui/template");


exports.getTemplate = function (parameters) {
    let restTemplate = restTemplateManager.getTemplate(parameters);

    let templateSources = [];
    templateSources = templateSources.concat(restTemplate.sources);
    templateSources = templateSources.concat(apiTemplate.getSources(parameters));
    templateSources = templateSources.concat(uiTemplate.getSources(parameters));

    let templateParameters = getTemplateParameters();
    templateParameters = templateParameters.concat(restTemplate.parameters);

    return {
        name: "Application - UI (AngularJS) - Persistence (MongoDB)",
        description: "Application with UI, REST APIs and MongoDB based DAOs",
        extension: "model",
        sources: templateSources,
        parameters: templateParameters
    };
};

function getTemplateParameters() {
    return [
        {
            name: "extensionName",
            label: "Extension",
            placeholder: "Extension name"
        },
        {
            name: "includeLaunchpad",
            label: "Embedded",
            type: "checkbox"
        },
        {
            name: "launchpadName",
            label: "Launchpad",
            placeholder: "Launchpad project name",
            ui: {
                hide: {
                    property: "includeLaunchpad",
                    value: true
                }
            }
        },
        {
            name: "title",
            label: "Title",
            placeholder: "Launchpad title",
            ui: {
                hide: {
                    property: "includeLaunchpad",
                    value: false
                }
            }
        },
        {
            name: "subTitle",
            label: "Sub-title",
            placeholder: "Launchpad sub-title",
            ui: {
                hide: {
                    property: "includeLaunchpad",
                    value: false
                }
            }
        },
        {
            name: "description",
            label: "Description",
            placeholder: "Launchpad description",
            ui: {
                hide: {
                    property: "includeLaunchpad",
                    value: false
                }
            }
        },
        {
            name: "brand",
            label: "Brand",
            placeholder: "Brand"
        }
    ];
}