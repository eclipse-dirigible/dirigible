/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
// @ts-nocheck
angular.module('platformEditors', ['platformExtensions'])
    .provider('Editors', function editorProvider() {
        this.$get = ['Extensions', function editorsFactory(Extensions) {
            let defaultEditor = {};
            let editorProviders = {};
            let editorsForContentType = {};

            Extensions.getEditors().then(function (response) {
                for (let i = 0; i < response.data.length; i++) {
                    editorProviders[response.data[i].id] = response.data[i].path;
                    if (response.data[i].defaultEditor) {
                        if (defaultEditor.id) console.error(`platform-editors: more then one editor is set as default - ${response.data[i].id}`);
                        else {
                            defaultEditor.id = response.data[i].id;
                            defaultEditor.label = response.data[i].label;
                        }
                    }
                    for (let j = 0; j < response.data[i].contentTypes.length; j++) {
                        const editorObj = {
                            'id': response.data[i].id,
                            'label': response.data[i].label
                        };
                        if (!editorsForContentType[response.data[i].contentTypes[j]]) {
                            editorsForContentType[response.data[i].contentTypes[j]] = [editorObj];
                        } else {
                            // This is needed because there might be duplicate editors from the back-end
                            if (!editorsForContentType[response.data[i].contentTypes[j]].some(e => e.id === editorObj.id)) {
                                editorsForContentType[response.data[i].contentTypes[j]].push(editorObj);
                            }
                        }
                    }
                }
            }, function (response) {
                console.error("platform-editors: could not get editors", response);
            });

            return {
                defaultEditor: defaultEditor,
                editorProviders: editorProviders,
                editorsForContentType: editorsForContentType
            };
        }];
    });