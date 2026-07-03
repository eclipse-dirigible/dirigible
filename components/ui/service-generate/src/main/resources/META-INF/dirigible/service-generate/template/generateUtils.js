/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { Registry } from "@aerokit/sdk/platform";
import { TemplateEngines as templateEngines } from "@aerokit/sdk/template";
import { sanitizeJavaIdentifier } from "service-generate/template/parameterUtils";

function getTranslationId(str) {
    return `${str.replaceAll(' ', '').replaceAll('_', '').replaceAll('.', '').replaceAll(':', '')}`;
}

// Humanize a PascalCase/camelCase identifier for display ("SalesInvoice" -> "Sales Invoice").
// Mirrors org.eclipse.dirigible.components.intent.generator.IntentNaming.humanize, including its
// acronym overrides, so a hand-authored .edm (no entityLabel/menuLabel baked by the intent
// generator) still yields the same labels the intent path would.
const HUMANIZE_OVERRIDES = { 'uom': 'Unit of Measure' };

function humanizeName(name) {
    if (!name) return '';
    const override = HUMANIZE_OVERRIDES[name.toLowerCase()];
    if (override) return override;
    let out = '';
    for (let i = 0; i < name.length; i++) {
        const c = name.charAt(i);
        if (i > 0 && c >= 'A' && c <= 'Z' && !(name.charAt(i - 1) >= 'A' && name.charAt(i - 1) <= 'Z')) {
            out += ' ';
        }
        out += i === 0 ? c.toUpperCase() : c;
    }
    return out;
}

// Pluralize a humanized label's last word ("Sales Invoice" -> "Sales Invoices", "Country" ->
// "Countries"). Mirrors IntentNaming.pluralize, including its irregular overrides.
const PLURALIZE_OVERRIDES = { 'unit of measure': 'Units of Measure', 'uom': 'Units of Measure' };

function pluralizeLabel(label) {
    if (!label) return '';
    const override = PLURALIZE_OVERRIDES[label.toLowerCase()];
    if (override) return override;
    const sp = label.lastIndexOf(' ');
    const head = sp >= 0 ? label.substring(0, sp + 1) : '';
    const last = sp >= 0 ? label.substring(sp + 1) : label;
    if (!last) return label;
    const lower = last.toLowerCase();
    let plural;
    if (lower.length > 1 && lower.endsWith('y') && 'aeiou'.indexOf(lower.charAt(lower.length - 2)) < 0) {
        plural = last.substring(0, last.length - 1) + 'ies';
    } else if (lower.endsWith('s') || lower.endsWith('x') || lower.endsWith('z') || lower.endsWith('ch') || lower.endsWith('sh')) {
        plural = last + 'es';
    } else {
        plural = last + 's';
    }
    return head + plural;
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
function migrateForm(model, fileName) {
    if (!model.hasOwnProperty('metadata')) {
        model['metadata'] = {
            name: getFormName(model.form) || fileName
        }
    } else if (!model.metadata.hasOwnProperty('name')) {
        model.metadata['name'] = getFormName(model.form) || fileName;
    }
    for (let i = 0; i < model.form.length; i++) {
        if (model.form[i].hasOwnProperty('title')) {
            delete Object.assign(model.form[i], { 'label': model.form[i]['title'] })['title'];
        }
        if (model.form[i].hasOwnProperty('name')) {
            delete Object.assign(model.form[i], { 'label': model.form[i]['name'] })['name'];
        }
        if (model.form[i].hasOwnProperty('errorState')) {
            delete Object.assign(model.form[i], { 'errorMessage': model.form[i]['errorState'] })['errorState'];
        }
        if (model.form[i].hasOwnProperty('size')) {
            delete Object.assign(model.form[i], { 'headerSize': model.form[i]['size'] })['size'];
        }
        if (model.form[i].controlId === 'header' && !model.form[i].hasOwnProperty('level')) {
            model.form[i]['level'] = 1;
        }
    }
}

function getFormName(formData) {
    for (let i = 0; i < formData.length; i++) {
        if (formData[i].controlId === 'header' && formData[i].headerSize === 1) {
            return `${formData[i].label} Form`;
        }
    }
    return '';
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
    let isForm = false;
    const generatedFiles = []
    const templateParameters = {};
    if (parameters.filePath.endsWith('.form')) {
        isForm = true;
        migrateForm(model, `${parameters['fileName']}`);
    } else if (parameters.filePath.endsWith('.report')) {
        isReport = true;
        migrateReport(model);
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
            else {
                if (isForm && model.metadata['successMsg']) {
                    translations.dialogs.successMsg = model.metadata['successMsg'];
                }
                translations.t = { ...getTranslations(model) };
            }
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
    annotateDocumentModels(model.entities);
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

    // UI Document (header-items): a master owning a composition child whose name ends in "Item"
    // (set by EdmIntentGenerator as layoutType MANAGE_DOCUMENT) — header form + inline items table +
    // totals footer. The items child stays a DEPENDENT detail (its inline columns + controller come
    // from its registration); other composition children render as ordinary detail panels.
    const uiDocumentModels = model.entities.filter(e => e.layoutType === "MANAGE_DOCUMENT" && e.type === "PRIMARY");

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
                case "uiDocumentModels":
                    generatedFiles.push(...generateCollection(location, content, template, uiDocumentModels, parameters));
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
                case "triggers":
                    // Process triggers (intent layer): one glue-code artefact per process started on an
                    // entity's create. Not entity-shaped, so it gets its own loop rather than
                    // generateCollection (which dereferences entity perspectives).
                    if (model.triggers) {
                        for (let t = 0; t < model.triggers.length; t++) {
                            const triggerParameters = {
                                ...parameters,
                                process: model.triggers[t].process,
                                entity: model.triggers[t].entity,
                                perspective: model.triggers[t].perspective,
                                // The Java package segment is the lowercased perspective, the same
                                // sanitization the DAO/entity templates apply (javaPerspectiveName).
                                // The event-topic name keeps the raw perspective so it matches the
                                // topic the DAO publishes to (${projectName}-${perspectiveName}-${name}).
                                javaPerspective: sanitizeJavaIdentifier(model.triggers[t].perspective),
                                keyProperty: model.triggers[t].keyProperty,
                                businessKeyProperty: model.triggers[t].businessKeyProperty,
                                generateBusinessKey: model.triggers[t].generateBusinessKey,
                                topicSuffix: model.triggers[t].topicSuffix,
                                guardExpression: model.triggers[t].guardExpression,
                                // Per to-one relation: assemble the target controller URL here (the
                                // template engine knows the path layout; the intent glue carried only
                                // logical names). A cross-model link uses the target project + sanitized
                                // model alias as the gen folder; a same-model one uses the owner's.
                                relationLinks: (model.triggers[t].relationLinks || []).map(function (rl) {
                                    const targetGenFolder = rl.crossModel ? sanitizeJavaIdentifier(rl.targetModel) : parameters.javaGenFolderName;
                                    const targetProject = rl.crossModel ? rl.targetProject : parameters.projectName;
                                    return {
                                        fkProperty: rl.fkProperty,
                                        labelField: rl.labelField,
                                        url: '/services/java/' + targetProject + '/gen/' + targetGenFolder + '/api/'
                                            + sanitizeJavaIdentifier(rl.targetPerspective) + '/' + rl.targetEntity + 'Controller'
                                    };
                                })
                            };
                            const cleanTriggerParameters = cleanData(triggerParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanTriggerParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanTriggerParameters)
                            });
                        }
                    }
                    break;
                case "resolvers":
                    // Decision resolvers (intent layer): one JavaDelegate per relation.field a decision
                    // references. Like triggers, not entity-shaped, so it gets its own loop. The Java
                    // package segment of the target entity is its lowercased perspective.
                    if (model.resolvers) {
                        for (let r = 0; r < model.resolvers.length; r++) {
                            const resolverParameters = {
                                ...parameters,
                                process: model.resolvers[r].process,
                                handler: model.resolvers[r].handler,
                                fkProperty: model.resolvers[r].fkProperty,
                                targetEntity: model.resolvers[r].targetEntity,
                                targetPerspective: model.resolvers[r].targetPerspective,
                                javaTargetPerspective: sanitizeJavaIdentifier(model.resolvers[r].targetPerspective),
                                targetField: model.resolvers[r].targetField,
                                targetIdAccessor: model.resolvers[r].targetIdAccessor,
                                variable: model.resolvers[r].variable,
                                ownerEntity: model.resolvers[r].ownerEntity,
                                ownerPerspective: model.resolvers[r].ownerPerspective,
                                javaOwnerPerspective: sanitizeJavaIdentifier(model.resolvers[r].ownerPerspective),
                                ownerKeyProperty: model.resolvers[r].ownerKeyProperty,
                                ownerKeyAccessor: model.resolvers[r].ownerKeyAccessor
                            };
                            const cleanResolverParameters = cleanData(resolverParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanResolverParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanResolverParameters)
                            });
                        }
                    }
                    break;
                case "fieldLoaders":
                    // Own-field decision loaders (intent layer): one JavaDelegate per decision that
                    // branches on the trigger entity's own field. Loads the owner by id and publishes
                    // the referenced fields before the gateway (clear-D id-only context). Like resolvers,
                    // not entity-shaped; the Java package segment is the owner's lowercased perspective.
                    if (model.fieldLoaders) {
                        for (let f = 0; f < model.fieldLoaders.length; f++) {
                            const fieldLoaderParameters = {
                                ...parameters,
                                process: model.fieldLoaders[f].process,
                                handler: model.fieldLoaders[f].handler,
                                ownerEntity: model.fieldLoaders[f].ownerEntity,
                                ownerPerspective: model.fieldLoaders[f].ownerPerspective,
                                javaOwnerPerspective: sanitizeJavaIdentifier(model.fieldLoaders[f].ownerPerspective),
                                ownerKeyProperty: model.fieldLoaders[f].ownerKeyProperty,
                                ownerKeyAccessor: model.fieldLoaders[f].ownerKeyAccessor,
                                fields: model.fieldLoaders[f].fields
                            };
                            const cleanFieldLoaderParameters = cleanData(fieldLoaderParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanFieldLoaderParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanFieldLoaderParameters)
                            });
                        }
                    }
                    break;
                case "setters":
                    // Field setters (intent layer): one JavaDelegate per step that declares a setField
                    // (set a string/text field to a literal) or a setRelationField (set a to-one relation's
                    // FK to a seed id; relation=="true"). Like resolvers, not entity-shaped, so it gets its
                    // own loop; the Java package segment of the entity is its lowercased perspective.
                    if (model.setters) {
                        for (let s = 0; s < model.setters.length; s++) {
                            const setterParameters = {
                                ...parameters,
                                process: model.setters[s].process,
                                className: model.setters[s].className,
                                entity: model.setters[s].entity,
                                perspective: model.setters[s].perspective,
                                javaPerspective: sanitizeJavaIdentifier(model.setters[s].perspective),
                                keyProperty: model.setters[s].keyProperty,
                                keyAccessor: model.setters[s].keyAccessor,
                                field: model.setters[s].field,
                                value: model.setters[s].value,
                                relation: model.setters[s].relation
                            };
                            const cleanSetterParameters = cleanData(setterParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanSetterParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanSetterParameters)
                            });
                        }
                    }
                    break;
                case "writers":
                    // Writers (intent layer): one JavaDelegate per user task with editable fields.
                    // Inserted after the user task, it writes the reviewer's edits from the process
                    // variables back onto the trigger entity (updateWithoutEvent). Not entity-shaped, so
                    // it gets its own loop; the Java package segment is the lowercased perspective.
                    if (model.writers) {
                        for (let w = 0; w < model.writers.length; w++) {
                            const writerParameters = {
                                ...parameters,
                                process: model.writers[w].process,
                                userTask: model.writers[w].userTask,
                                className: model.writers[w].className,
                                entity: model.writers[w].entity,
                                perspective: model.writers[w].perspective,
                                javaPerspective: sanitizeJavaIdentifier(model.writers[w].perspective),
                                keyProperty: model.writers[w].keyProperty,
                                keyAccessor: model.writers[w].keyAccessor,
                                fields: model.writers[w].fields
                            };
                            const cleanWriterParameters = cleanData(writerParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanWriterParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanWriterParameters)
                            });
                        }
                    }
                    break;
                case "notifications":
                    // Notifications (intent layer): one @Listener per declarative notification, binding
                    // to the entity's event topic and sending via the SDK. Not entity-shaped, so it gets
                    // its own loop. The Java package segment is the lowercased perspective (matching the
                    // entity/DAO templates); the event-topic name keeps the raw perspective + the
                    // operation suffix so it matches what the DAO publishes to.
                    if (model.notifications) {
                        for (let n = 0; n < model.notifications.length; n++) {
                            const notificationParameters = {
                                ...parameters,
                                name: model.notifications[n].name,
                                className: model.notifications[n].className,
                                entity: model.notifications[n].entity,
                                perspective: model.notifications[n].perspective,
                                javaPerspective: sanitizeJavaIdentifier(model.notifications[n].perspective),
                                topicSuffix: model.notifications[n].topicSuffix,
                                // Each one-hop relation load needs the lowercased Java package of its target.
                                relationLoads: (model.notifications[n].relationLoads || []).map(load => ({
                                    ...load,
                                    javaTargetPerspective: sanitizeJavaIdentifier(load.targetPerspective)
                                })),
                                guardExpression: model.notifications[n].guardExpression,
                                toExpression: model.notifications[n].toExpression,
                                subjectExpression: model.notifications[n].subjectExpression,
                                bodyExpression: model.notifications[n].bodyExpression
                            };
                            const cleanNotificationParameters = cleanData(notificationParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanNotificationParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanNotificationParameters)
                            });
                        }
                    }
                    break;
                case "schedules":
                    // Schedules (intent layer): one @Scheduled JobHandler per schedule - query the entity
                    // via a typed Criteria and notify per matching row. Not entity-shaped, own loop.
                    if (model.schedules) {
                        for (let s = 0; s < model.schedules.length; s++) {
                            const scheduleParameters = {
                                ...parameters,
                                name: model.schedules[s].name,
                                className: model.schedules[s].className,
                                cron: model.schedules[s].cron,
                                entity: model.schedules[s].entity,
                                perspective: model.schedules[s].perspective,
                                javaPerspective: sanitizeJavaIdentifier(model.schedules[s].perspective),
                                criteriaExpression: model.schedules[s].criteriaExpression,
                                relationLoads: (model.schedules[s].relationLoads || []).map(load => ({
                                    ...load,
                                    javaTargetPerspective: sanitizeJavaIdentifier(load.targetPerspective)
                                })),
                                toExpression: model.schedules[s].toExpression,
                                subjectExpression: model.schedules[s].subjectExpression,
                                bodyExpression: model.schedules[s].bodyExpression
                            };
                            const cleanScheduleParameters = cleanData(scheduleParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanScheduleParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanScheduleParameters)
                            });
                        }
                    }
                    break;
                case "integrations":
                    // Integrations (intent layer): one @Listener per outbound integration - forward the
                    // entity event to an external HTTP endpoint. Not entity-shaped, own loop.
                    if (model.integrations) {
                        for (let i = 0; i < model.integrations.length; i++) {
                            const integrationParameters = {
                                ...parameters,
                                name: model.integrations[i].name,
                                className: model.integrations[i].className,
                                entity: model.integrations[i].entity,
                                perspective: model.integrations[i].perspective,
                                topicSuffix: model.integrations[i].topicSuffix,
                                clientMethod: model.integrations[i].clientMethod,
                                hasBody: model.integrations[i].hasBody,
                                urlExpression: model.integrations[i].urlExpression
                            };
                            const cleanIntegrationParameters = cleanData(integrationParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanIntegrationParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanIntegrationParameters)
                            });
                        }
                    }
                    break;
                case "inbound":
                    // Inbound webhooks (intent layer): one @Controller per webhook - ingest a posted JSON
                    // payload as an entity. Not entity-shaped, own loop.
                    if (model.inbound) {
                        for (let w = 0; w < model.inbound.length; w++) {
                            const inboundParameters = {
                                ...parameters,
                                name: model.inbound[w].name,
                                className: model.inbound[w].className,
                                entity: model.inbound[w].entity,
                                perspective: model.inbound[w].perspective,
                                javaPerspective: sanitizeJavaIdentifier(model.inbound[w].perspective),
                                path: model.inbound[w].path
                            };
                            const cleanInboundParameters = cleanData(inboundParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanInboundParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanInboundParameters)
                            });
                        }
                    }
                    break;
                case "rollups":
                    // Rollups (intent layer): per rollup, two @Listeners (child create/delete) that
                    // recompute a parent counter. Not entity-shaped, own loop.
                    if (model.rollups) {
                        for (let r = 0; r < model.rollups.length; r++) {
                            const rollupParameters = {
                                ...parameters,
                                className: model.rollups[r].className,
                                childEntity: model.rollups[r].childEntity,
                                childPerspective: model.rollups[r].childPerspective,
                                javaChildPerspective: sanitizeJavaIdentifier(model.rollups[r].childPerspective),
                                parentEntity: model.rollups[r].parentEntity,
                                javaParentPerspective: sanitizeJavaIdentifier(model.rollups[r].parentPerspective),
                                fkProperty: model.rollups[r].fkProperty,
                                countField: model.rollups[r].countField,
                                op: model.rollups[r].op,
                                sumField: model.rollups[r].sumField,
                                capacityField: model.rollups[r].capacityField,
                                balanceField: model.rollups[r].balanceField,
                                statusField: model.rollups[r].statusField,
                                statusWhenFull: model.rollups[r].statusWhenFull,
                                statusWhenPartial: model.rollups[r].statusWhenPartial,
                                topicSuffix: model.rollups[r].topicSuffix,
                                criteriaExpression: model.rollups[r].criteriaExpression
                            };
                            const cleanRollupParameters = cleanData(rollupParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanRollupParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanRollupParameters)
                            });
                        }
                    }
                    break;
                case "settlements":
                    // Auto-settlement (intent layer): per settlement, an onPayment listener + an
                    // onInvoice delegate. The junction + invoice live in this project; the payment may be
                    // cross-model (its gen folder = sanitized model alias, its topic keeps the raw
                    // perspective). Java package segments are the lowercased (sanitized) perspectives.
                    if (model.settlements) {
                        for (let i = 0; i < model.settlements.length; i++) {
                            const st = model.settlements[i];
                            const settlementParameters = {
                                ...parameters,
                                name: st.name,
                                match: st.match,
                                order: st.order,
                                invoiceEntity: st.invoiceEntity,
                                invoiceJavaPerspective: sanitizeJavaIdentifier(st.invoicePerspective),
                                invoicePk: st.invoicePk,
                                invoiceTotal: st.invoiceTotal,
                                invoicePaid: st.invoicePaid,
                                invoiceStatus: st.invoiceStatus,
                                payableCondition: st.payableCondition,
                                junctionEntity: st.junctionEntity,
                                junctionJavaPerspective: sanitizeJavaIdentifier(st.junctionPerspective),
                                junctionFkInvoice: st.junctionFkInvoice,
                                junctionFkPayment: st.junctionFkPayment,
                                junctionAmount: st.junctionAmount,
                                paymentEntity: st.paymentEntity,
                                paymentGenFolder: st.crossModel ? sanitizeJavaIdentifier(st.paymentModel) : parameters.javaGenFolderName,
                                paymentJavaPerspective: sanitizeJavaIdentifier(st.paymentPerspective),
                                paymentPk: st.paymentPk,
                                paymentPot: st.paymentPot,
                                paymentTopic: st.paymentTopic
                            };
                            const cleanSettlementParameters = cleanData(settlementParameters);
                            generatedFiles.push({
                                location: location,
                                content: getGenerationEngine(template).generate(location, content, cleanSettlementParameters),
                                path: templateEngines.getMustacheEngine().generate(location, template.rename, cleanSettlementParameters)
                            });
                        }
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
                        // The entity's humanized singular ("Sales Invoice") and pluralized
                        // ("Sales Invoices") display names, per language: t.<dataName> feeds the
                        // singular UI texts (form captions, "New X", "X #id"), t.<dataName>_plural
                        // the plural ones (sidebar entries, list/master titles, detail panels).
                        // The model's own entityLabel/menuLabel (EdmIntentGenerator) win; models
                        // without them (hand-authored .edm) get the same derivation applied here.
                        const singular = model.entities[i].entityLabel || humanizeName(model.entities[i].name);
                        translations.t[model.entities[i].dataName] = singular;
                        translations.t[`${model.entities[i].dataName}_plural`] = model.entities[i].menuLabel || pluralizeLabel(singular);
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

/**
 * Annotate a document master + its line-items child with the metadata their DAOs need to keep the
 * document totals consistent SYNCHRONOUSLY (in the same request), instead of via async roll-up
 * listeners. A master (layoutType MANAGE_DOCUMENT) gets `documentMaster` = the child entity, its Java
 * package, the FK column, the master PK, and the aggregate fields the child also carries by name; the
 * child gets `documentItem` = its parent entity, package and FK. The DAO template reads these.
 */
function annotateDocumentModels(entities) {
    const byName = {};
    for (const e of entities) {
        byName[e.name] = e;
    }
    for (const master of entities) {
        if (master.layoutType !== "MANAGE_DOCUMENT" || !master.documentItemsEntity) {
            continue;
        }
        const child = byName[master.documentItemsEntity];
        if (!child) {
            continue;
        }
        const fkProperty = child.masterEntityId; // FK property on the child pointing at the master
        const masterPk = (master.primaryKeys && master.primaryKeys[0]) || "Id";
        const childFieldNames = new Set((child.properties || []).map(p => p.name));
        const fields = (master.properties || [])
            .filter(p => p.aggregate === "true" && childFieldNames.has(p.name))
            .map(p => ({ field: p.name }));
        if (fields.length === 0 || !fkProperty) {
            continue;
        }
        master.documentMaster = {
            childEntity: child.name,
            javaChildPerspective: sanitizeJavaIdentifier(child.perspectiveName),
            fkProperty: fkProperty,
            masterPk: masterPk,
            fields: fields
        };
        child.documentItem = {
            parentEntity: master.name,
            javaParentPerspective: sanitizeJavaIdentifier(master.perspectiveName),
            fkProperty: fkProperty
        };
    }
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
