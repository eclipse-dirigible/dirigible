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
function openReferEntity(title, message, $scope, graph, uuid) { // Not used
	$scope.$parent.dialogTitle = title;
	$scope.$parent.okReferEntity = function () {
		if (!$scope.$parent.referencedModel || !$scope.$parent.referencedEntity) {
			$('#Delete').click();
			return;
		}

		let model = graph.getModel();
		model.beginUpdate();
		try {
			$scope.$cell.value.name = $scope.$parent.referencedModel + ":" + $scope.$parent.referencedEntity;
			$scope.$cell.value.entityType = "PROJECTION";
			$scope.$cell.value.projectionReferencedModel = $scope.$parent.referencedModel;
			$scope.$cell.value.projectionReferencedEntity = $scope.$parent.referencedEntity;

			$scope.$parent.availableEntities.forEach(entity => {
				if (entity.name === $scope.$parent.referencedEntity) {
					entity.properties.forEach(projectionProperty => {
						let propertyObject = new Property('propertyName');
						let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
						property.setId(uuid.generate());
						property.setVertex(true);
						property.setConnectable(false);
						for (let attributeName in projectionProperty) {
							property.value[attributeName] = projectionProperty[attributeName];
						}
						property.style = 'projectionproperty';
						$scope.$cell.insert(property);
					});
				}
			});

			model.setCollapsed($scope.$cell, true);

		} finally {
			model.endUpdate();
		}
		graph.refresh();
	};
	$scope.$parent.cancelReferEntity = function () {
		$('#Delete').click();
	}
	$scope.$apply();
	$('#referEntityOpen').click();
}


function openCopiedEntity(title, message, $scope, graph, uuid) { // Not used
	$scope.$parent.dialogTitle = title;
	$scope.$parent.okReferEntity = function () {
		if (!$scope.$parent.referencedModel || !$scope.$parent.referencedEntity) {
			$('#Delete').click();
			return;
		}

		let model = graph.getModel();
		model.beginUpdate();
		try {
			$scope.$cell.value.name = $scope.$parent.referencedEntity;
			$scope.$cell.value.entityType = "COPIED";
			$scope.$cell.value.projectionReferencedModel = $scope.$parent.referencedModel;
			$scope.$cell.value.projectionReferencedEntity = $scope.$parent.referencedEntity;

			$scope.$parent.availableEntities.forEach(entity => {
				if (entity.name === $scope.$parent.referencedEntity) {
					entity.properties.forEach(projectionProperty => {
						let propertyObject = new Property('propertyName');
						let property = new mxCell(propertyObject, new mxGeometry(0, 0, 0, 26));
						property.setId(uuid.generate());
						property.setVertex(true);
						property.setConnectable(false);
						for (let attributeName in projectionProperty) {
							property.value[attributeName] = projectionProperty[attributeName];
						}
						$scope.$cell.insert(property);
					});
				}
			});

			model.setCollapsed($scope.$cell, true);

		} finally {
			model.endUpdate();
		}
		graph.refresh();
	};
	$scope.$parent.cancelReferEntity = function () {
		$('#Delete').click();
	}
	$scope.$apply();
	$('#referEntityOpen').click();
}