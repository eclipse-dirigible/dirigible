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
import { Registry } from "sdk/platform";
import { TemplateEngines as templateEngines } from "sdk/template";

function getTranslationId(str) {
    return `${str.replaceAll(' ', '').replaceAll('_', '').replaceAll('.', '').replaceAll(':', '')}`;
}

function getTranslations(model) {
    let translations = {};
    for (const [key, value] of Object.entries(model)) {
        if ((key === 'label' || key === 'errorMessage') && value !== undefined && value !== null && value !== '') {
            const translationId = getTranslationId(value);
            translations[translationId] = value;
            model[key === 'errorMessage' ? 'errorTranslation' : 'translation'] = translationId;
        } else if (typeof value === 'object') {
            translations = { ...translations, ...getTranslations(value) };
        }
    }
    return translations;
}

function getReportTranslations(report) {
    let translations = {};
    translations[report.tId] = report.label;
    for (let i = 0; i < report.columns.length; i++) {
        translations[report.columns[i]['tId']] = report.columns[i]['label'];
    }
    return translations;
}

function generateTranslationPrefix(prefix) {
    return `${prefix.substring(prefix.lastIndexOf('/') + 1).replaceAll(' ', '').replaceAll('_', '').replaceAll('.', '-').replaceAll(':', '')}`;
}

// Migrates the old form data to the new one. Should be deleted in the future
function migrateForm(formData) {
    for (let i = 0; i < formData.length; i++) {
        if (formData[i].hasOwnProperty('title')) {
            delete Object.assign(formData[i], { 'label': formData[i]['title'] })['title'];
        }
        if (formData[i].hasOwnProperty('name')) {
            delete Object.assign(formData[i], { 'label': formData[i]['name'] })['name'];
        }
        if (formData[i].hasOwnProperty('errorState')) {
            delete Object.assign(formData[i], { 'errorMessage': formData[i]['errorState'] })['errorState'];
        }
        if (formData[i].hasOwnProperty('size')) {
            delete Object.assign(formData[i], { 'headerSize': formData[i]['size'] })['size'];
        }
    }
}

function migrateReport(report) {
    if (!report.hasOwnProperty('tId')) {
        report['tId'] = getTranslationId(report.alias);
        report['label'] = report.alias;
    }
    for (let i = 0; i < report.columns.length; i++) {
        if (!report.columns[i].hasOwnProperty('tId')) {
            report.columns[i]['tId'] = getTranslationId(report.columns[i]['alias']);
            report.columns[i]['label'] = report.columns[i]['alias'];
        }
    }
}

export function generateGeneric(model, parameters, templateSources) {
    let isReport = false;
    const generatedFiles = []
    const templateParameters = {};
    if (parameters.filePath.endsWith('.form')) migrateForm(model.form);
    else if (parameters.filePath.endsWith('.report')) {
        migrateReport(model);
        isReport = true;
    }
    Object.assign(templateParameters, model, parameters);
    templateParameters['tprefix'] = generateTranslationPrefix(parameters.filePath);

    const cleanTemplateParameters = cleanData(templateParameters);

    for (let i = 0; i < templateSources.length; i++) {
        const template = templateSources[i];
        const location = template.location;
        const content = Registry.getText(template.location);
        if (content == null) {
            throw new Error(`Template file at location '${templateSources[i].location}' does not exists.`)
        }

        if (template.action === "copy") {
            generatedFiles.push({
                location: location,
                content: content,
                path: templateEngines.getMustacheEngine().generate(location, template.rename, parameters)
            });
        } else if (template.action === "translate") {
            let translations = JSON.parse(content);
            if (isReport) translations.t = { ...getReportTranslations(model) };
            else translations.t = { ...getTranslations(model) };
            generatedFiles.push({
                content: JSON.stringify({ [cleanTemplateParameters['tprefix']]: translations }, null, 2),
                path: `translations/en-US/${parameters.filePath.substring(parameters.filePath.lastIndexOf('/') + 1)}.json`
            });
        } else {
            generatedFiles.push({
                location: location,
                content: getGenerationEngine(template).generate(location, content, cleanTemplateParameters),
                path: templateEngines.getMustacheEngine().generate(location, template.rename, parameters)
            });
        }
    }
    return generatedFiles;
}

export function generateFiles(model, parameters, templateSources) {
    const generatedFiles = [];

    const models = model.entities.filter(e => e.type !== "REPORT" && e.type !== "FILTER");
    const apiModels = model.entities.filter(e => e.type !== "PROJECTION");
    const daoModels = model.entities.filter(e => e.type !== "PROJECTION");
    const feedModels = model.entities.filter(e => e.feedUrl);

    const generateReportModels = model.entities.filter(e => e.generateReport === "true");
    const reportModels = model.entities.filter(e => e.type === "REPORT");
    const reportFilterModels = model.entities.filter(e => e.type === "FILTER");
    for (const filter of reportFilterModels) {
        const reportModelName = filter.properties.filter(e => e.relationshipType === "ASSOCIATION" && e.relationshipCardinality === "1_1").map(e => e.relationshipEntityName)[0];
        if (reportModelName) {
            for (const model of reportModels) {
                if (model.name === reportModelName) {
                    model.filter = filter;
                    break;
                }
            }
        }
    }

    // UI Basic
    const uiManageModels = model.entities.filter(e => e.layoutType === "MANAGE" && e.type === "PRIMARY");
    const uiListModels = model.entities.filter(e => e.layoutType === "LIST" && e.type === "PRIMARY");
    const uiSettingModels = model.entities.filter(e => e.type === "SETTING");

    // UI Master-Details
    const uiManageMasterModels = model.entities.filter(e => e.layoutType === "MANAGE_MASTER" && e.type === "PRIMARY");
    const uiListMasterModels = model.entities.filter(e => e.layoutType === "LIST_MASTER" && e.type === "PRIMARY");
    const uiManageDetailsModels = model.entities.filter(e => e.layoutType === "MANAGE_DETAILS" && e.type === "DEPENDENT");
    const uiListDetailsModels = model.entities.filter(e => e.layoutType === "LIST_DETAILS" && e.type === "DEPENDENT");

    // UI Reports
    const uiReportChartModels = reportModels.filter(e => e.layoutType !== "REPORT_TABLE");
    const uiReportTableModels = reportModels.filter(e => e.layoutType === "REPORT_TABLE");

    for (let i = 0; i < templateSources.length; i++) {
        const template = templateSources[i];
        const location = template.location;
        const content = Registry.getText(template.location);
        if (content == null) {
            throw new Error(`Template file at location '${templateSources[i].location}' does not exists.`)
        }

        parameters['tprefix'] = generateTranslationPrefix(parameters.filePath);

        if (template.action === "copy") {
            generatedFiles.push({
                location: location,
                content: content,
                path: templateEngines.getMustacheEngine().generate(location, template.rename, parameters)
            });
        } else if (template.action === "generate") {
            switch (template.collection) {
                case "models":
                    generatedFiles.push(...generateCollection(location, content, template, models, parameters));
                    break;
                case "apiModels":
                    generatedFiles.push(...generateCollection(location, content, template, apiModels, parameters));
                    break;
                case "daoModels":
                    generatedFiles.push(...generateCollection(location, content, template, daoModels, parameters));
                    break;
                case "generateReportModels":
                    generatedFiles.push(...generateCollection(location, content, template, generateReportModels, parameters));
                    break;
                case "reportModels":
                    generatedFiles.push(...generateCollection(location, content, template, reportModels, parameters));
                    break;
                case "feedModels":
                    generatedFiles.push(...generateCollection(location, content, template, feedModels, parameters));
                    break;
                case "uiManageModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiManageModels, parameters));
                    break;
                case "uiSettingModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiSettingModels, parameters));
                    break;
                case "uiListModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiListModels, parameters));
                    break;
                case "uiManageMasterModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiManageMasterModels, parameters));
                    break;
                case "uiListMasterModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiListMasterModels, parameters));
                    break;
                case "uiManageDetailsModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiManageDetailsModels, parameters));
                    break;
                case "uiListDetailsModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiListDetailsModels, parameters));
                    break;
                case "uiReportChartModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiReportChartModels, parameters));
                    break;
                case "uiReportTableModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiReportTableModels, parameters));
                    break;
                case "uiNavigations":
                    for (let i = 0; i < model.navigations.length; i++) {
                        const templateParameters = {
                            ...parameters,
                            navId: model.navigations[i].id,
                            navLabel: model.navigations[i].label,
                            navHeader: model.navigations[i].header,
                            navExpanded: model.navigations[i].expanded,
                            navOrder: model.navigations[i].order,
                            navIcon: model.navigations[i].icon,
                            navRole: model.navigations[i].role
                        };
                        const cleanParams = cleanData(templateParameters);
                        generatedFiles.push({
                            location: location,
                            content: getGenerationEngine(template).generate(location, content, cleanParams),
                            path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanParams)
                        });
                    }
                    break;
                default:
                    // No collection
                    parameters.models = model.entities;

                    const cleanParameters = cleanData(parameters);

                    generatedFiles.push({
                        location: location,
                        content: getGenerationEngine(template).generate(location, content, cleanParameters),
                        path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanParameters)
                    });
                    break;
            }
        } else if (template.action === "translate") {
            const translations = JSON.parse(content) ?? { t: {} };

            function properties(props) {
                for (let p = 0; p < props.length; p++) {
                    if (props[p].dataName) {
                        if (props[p].perspectiveHeader) {
                            translations.t[`${props[p].perspectiveName}pheader`] = props[p].perspectiveHeader;
                        }
                        if (props[p].perspectiveLabel) {
                            translations.t[props[p].dataName] = props[p].perspectiveLabel;
                        } else if (props[p].widgetLabel) {
                            translations.t[props[p].dataName] = props[p].widgetLabel;
                        } else if (props[p].name) {
                            translations.t[props[p].dataName] = props[p].name;
                        }
                    }
                    if (props[p].masterProperties) {
                        masterProperties(props[p].masterProperties);
                    }
                }
            }

            function masterProperties(mp) {
                if (mp.title && mp.title.dataName) {
                    if (mp.title.widgetLabel) {
                        translations.t[mp.title.dataName] = mp.title.widgetLabel;
                    } else if (mp.title.name) {
                        translations.t[mp.title.dataName] = mp.title.name;
                    }
                }
                if (mp.properties) {
                    properties(mp.properties);
                }
            }

            if (model.entities) {
                for (let i = 0; i < model.entities.length; i++) {
                    if (model.entities[i].dataName && model.entities[i].name) {
                        translations.t[model.entities[i].dataName] = model.entities[i].name;
                    }
                    if (model.entities[i].properties) {
                        properties(model.entities[i].properties);
                    }
                    if (model.entities[i].masterProperties) {
                        masterProperties(model.entities[i].masterProperties);
                    }
                }
            }
            if (model.perspectives) {
                for (let i = 0; i < model.perspectives.length; i++) {
                    if (model.perspectives[i].header) {
                        translations.t[`${model.perspectives[i].name}pheader`] = model.perspectives[i].label;
                    }
                    translations.t[model.perspectives[i].name] = model.perspectives[i].label;
                }
            }
            if (model.navigations) {
                for (let i = 0; i < model.navigations.length; i++) {
                    if (model.navigations[i].header) {
                        translations.t[`${model.navigations[i].id}nheader`] = model.navigations[i].header;
                    }
                    translations.t[model.navigations[i].id] = model.navigations[i].label;
                }
            }
            generatedFiles.push({
                content: JSON.stringify({ [parameters['tprefix']]: translations }, null, 2),
                path: `translations/en-US/${parameters.filePath.substring(parameters.filePath.lastIndexOf('/') + 1)}.json`
            });
        }
    }
    return generatedFiles;
}

function generateCollection(location, content, template, collection, parameters) {
    try {
        const generationEngine = getGenerationEngine(template);
        const generatedFiles = [];
        for (let i = 0; i < collection.length; i++) {
            const templateParameters = {};
            if (collection[i].type === 'SETTING') {
                collection[i].layoutType = undefined;
                collection[i].perspectiveName = "Settings"; // ID of the Settings perspective
                collection[i].perspectiveLabel = undefined;
                collection[i].navigationPath = undefined;
            } else if (collection[i].type === 'REPORT') {
                collection[i].layoutType = undefined;
                collection[i].perspectiveName = "Reports"; // ID of the Reports perspective
                collection[i].perspectiveLabel = undefined;
                collection[i].navigationPath = undefined;
            }
            Object.assign(templateParameters, collection[i], parameters);
            if (collection[i].type !== 'SETTING') {
                // TODO Move this to the more generic "generate()" function, with layoutType === "MANAGE_MASTER" check
                templateParameters.perspectiveViews = templateParameters.perspectives[collection[i].perspectiveName].views;
                if (template.collection === "uiManageMasterModels" || template.collection === "uiListMasterModels") {
                    collection.filter(e => e.perspectiveName === collection[i].perspectiveName).forEach(e => templateParameters.perspectiveViews.push(e.name + "-details"));
                }
            }

            const cleanTemplateParameters = cleanData(templateParameters);

            generatedFiles.push({
                location: location,
                content: generationEngine.generate(location, content, cleanTemplateParameters),
                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanTemplateParameters)
            });
        }
        return generatedFiles;
    } catch (e) {
        const message = `Error occurred while generating template:\n\nError: ${e.message}\n\nTemplate:\n${JSON.stringify(template, null, 2)}\n`;
        console.error(message);
        throw e;
    }
}

function getGenerationEngine(template) {
    let generationEngine = null;
    if (template.engine === "velocity") {
        generationEngine = templateEngines.getVelocityEngine();
    } else if (template.engine === "javascript") {
        generationEngine = templateEngines.getJavascriptEngine();
    } else if (template.engine === "mustache") {
        generationEngine = templateEngines.getMustacheEngine();
    } else if (template.engine === undefined) {
        console.debug("Template engine is not explicitly defined, so will be used the default Mustache engine.");
        generationEngine = templateEngines.getMustacheEngine();
    } else {
        console.error("Template engine: " + template.engine + " does not exist, so the default Mustache engine will be used.");
        generationEngine = templateEngines.getMustacheEngine();
    }

    if (template.sm) {
        generationEngine.setSm(template.sm);
    }
    if (template.em) {
        generationEngine.setEm(template.em);
    }
    return generationEngine;
}

function cleanData(data) {
    if (typeof data === 'object' && data !== null) {
        if (Array.isArray(data)) {
            for (let i = 0; i < data.length; i++) {
                cleanData(data[i]);
            }
        } else {
            for (let key in data) {
                if (data[key] !== undefined) {
                    if ((typeof data[key] === 'number' && isNaN(data[key])) || data[key] === 'NaN') {
                        delete data[key];
                    } else {
                        cleanData(data[key]);
                    }
                }
            }
        }
    }
    return data;
}