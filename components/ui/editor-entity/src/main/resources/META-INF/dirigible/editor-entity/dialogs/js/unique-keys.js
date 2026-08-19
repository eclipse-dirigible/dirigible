/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('edmUniqueKeys', ['blimpKit', 'platformView'])
    .constant('Dialogs', new DialogHub())
    .controller('UniqueKeysController', ($scope, $document, Dialogs, ViewParameters) => {
        $scope.state = {
            isBusy: false,
            error: false,
            busyText: 'Loading...',
        };
        $scope.showInnerDialog = false;
        $scope.editElement = {
            editType: 'Add', // Update
            index: 0,
            entity: '',
            message: '',
            selected: {}, // property name -> boolean
        };

        /** The properties of the entity currently chosen in the inner form. */
        $scope.propertiesOf = (entityName) => {
            const entity = $scope.dataParameters.entities.find((candidate) => candidate.name === entityName);
            return entity ? entity.properties : [];
        };

        /** The chosen properties, in the entity's own order - which is how the key reads. */
        $scope.chosenProperties = () => $scope.propertiesOf($scope.editElement.entity).filter((name) => $scope.editElement.selected[name]);

        /**
         * The constraint name, derived exactly as the intent derives it. It is not decorative: the
         * generated controller matches this name against what the database reports on a violation, so a
         * key declared here and the same key declared in an intent have to arrive at one name.
         */
        $scope.derivedName = () => {
            const chosen = $scope.chosenProperties();
            return chosen.length ? [$scope.editElement.entity].concat(chosen).join('_') : '';
        };

        $scope.derivedMessage = () => {
            const chosen = $scope.chosenProperties();
            return chosen.length ? `A ${$scope.editElement.entity} with the same ${chosen.join(', ')} already exists` : '';
        };

        /** A key needs two or more properties (one is the field-level unique) and must not repeat one. */
        $scope.isValid = () => {
            const chosen = $scope.chosenProperties();
            if (!$scope.editElement.entity || chosen.length < 2) return false;
            const candidate = `${$scope.editElement.entity}:${chosen.join(',')}`;
            for (let i = 0; i < $scope.dataParameters.uniqueKeys.length; i++) {
                if ($scope.editElement.editType === 'Update' && i === $scope.editElement.index) continue;
                const key = $scope.dataParameters.uniqueKeys[i];
                if (`${key.entity}:${(key.properties || []).join(',')}` === candidate) return false;
            }
            return true;
        };

        $scope.validationMessage = () => {
            if (!$scope.editElement.entity) return 'Select the entity the key belongs to.';
            if ($scope.chosenProperties().length < 2) {
                return 'Select two or more properties - a key over a single property is the field\'s own "Unique" flag.';
            }
            if (!$scope.isValid()) return 'This entity already has a key over exactly these properties.';
            return '';
        };

        $scope.add = () => {
            $scope.editElement.editType = 'Add';
            $scope.editElement.index = -1;
            $scope.editElement.entity = '';
            $scope.editElement.message = '';
            $scope.editElement.selected = {};
            $scope.showInnerDialog = true;
        };

        $scope.edit = (index) => {
            const key = $scope.dataParameters.uniqueKeys[index];
            $scope.editElement.editType = 'Update';
            $scope.editElement.index = index;
            $scope.editElement.entity = key.entity;
            $scope.editElement.message = key.message;
            $scope.editElement.selected = {};
            (key.properties || []).forEach((name) => { $scope.editElement.selected[name] = true; });
            $scope.showInnerDialog = true;
        };

        /** Changing the entity drops the selection - the properties belonged to the other one. */
        $scope.entityChanged = () => {
            $scope.editElement.selected = {};
        };

        $scope.delete = (index) => {
            $scope.dataParameters.uniqueKeys.splice(index, 1);
        };

        $scope.innerAction = () => {
            if (!$scope.isValid()) return;
            const key = {
                entity: $scope.editElement.entity,
                name: $scope.derivedName(),
                properties: $scope.chosenProperties(),
                message: $scope.editElement.message ? $scope.editElement.message : $scope.derivedMessage(),
            };
            if ($scope.editElement.editType === 'Add') $scope.dataParameters.uniqueKeys.push(key);
            else $scope.dataParameters.uniqueKeys[$scope.editElement.index] = key;
            $scope.showInnerDialog = false;
        };

        $scope.cancel = () => {
            if (!$scope.state.error && $scope.showInnerDialog) $scope.showInnerDialog = false;
            else Dialogs.closeWindow();
        };

        $scope.save = () => {
            if (!$scope.state.error) {
                $scope.state.busyText = 'Saving...';
                $scope.state.isBusy = true;
                Dialogs.postMessage({
                    topic: 'edmEditor.unique.keys',
                    data: { uniqueKeys: $scope.dataParameters.uniqueKeys },
                });
            }
        };

        $scope.dataParameters = ViewParameters.get();
        angular.element($document[0]).ready(() => {
            if (!$scope.dataParameters.uniqueKeys) $scope.dataParameters.uniqueKeys = [];
            if (!$scope.dataParameters.entities) $scope.dataParameters.entities = [];
        });
    });
