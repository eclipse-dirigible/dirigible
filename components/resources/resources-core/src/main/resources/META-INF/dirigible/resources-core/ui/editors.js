/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2024 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('ideEditors', ['ngResource', 'ideExtensions'])
    .constant('extensionPoint', {})
    .provider('Editors', function editorProvider() {
        this.$get = ['Extensions', 'extensionPoint', function editorsFactory(Extensions, extensionPoint) {
            let defaultEditor = {};
            let editorProviders = {};
            let editorsForContentType = {};

            Extensions.get('editor', extensionPoint.editors).then(function (response) {
                for (let i = 0; i < response.length; i++) {
                    editorProviders[response[i].id] = response[i].link;
                    if (response[i].defaultEditor) {
                        if (defaultEditor.id) console.error(`ide-editors: more then one editor is set as default - ${response[i].id}`);
                        else {
                            defaultEditor.id = response[i].id;
                            defaultEditor.label = response[i].label;
                        }
                    }
                    for (let j = 0; j < response[i].contentTypes.length; j++) {
                        let editorObj = {
                            'id': response[i].id,
                            'label': response[i].label
                        };
                        if (!editorsForContentType[response[i].contentTypes[j]]) {
                            editorsForContentType[response[i].contentTypes[j]] = [editorObj];
                        } else {
                            // This is needed because there might be duplicate editors from the back-end
                            if (!editorsForContentType[response[i].contentTypes[j]].some(e => e.id === editorObj.id)) {
                                editorsForContentType[response[i].contentTypes[j]].push(editorObj);
                            }
                        }
                    }
                }
            }, function (response) {
                console.error("ide-editors: could not get editors", response);
            });

            return {
                defaultEditor: defaultEditor,
                editorProviders: editorProviders,
                editorsForContentType: editorsForContentType
            };
        }];
    })