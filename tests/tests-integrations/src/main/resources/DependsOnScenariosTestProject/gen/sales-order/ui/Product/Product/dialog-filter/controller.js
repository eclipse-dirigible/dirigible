/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('page', ['blimpKit', 'platformView']).controller('PageController', ($scope, ViewParameters) => {
	const Dialogs = new DialogHub();
	$scope.entity = {};
	$scope.forms = {
		details: {},
	};

	let params = ViewParameters.get();
	if (Object.keys(params).length) {
		$scope.entity = params.entity ?? {};
		$scope.selectedMainEntityKey = params.selectedMainEntityKey;
		$scope.selectedMainEntityId = params.selectedMainEntityId;
		$scope.optionsUoM = params.optionsUoM;
	}

	$scope.filter = () => {
		let entity = $scope.entity;
		const filter = {
			$filter: {
				equals: {
				},
				notEquals: {
				},
				contains: {
				},
				greaterThan: {
				},
				greaterThanOrEqual: {
				},
				lessThan: {
				},
				lessThanOrEqual: {
				}
			},
		};
		if (entity.Id !== undefined) {
			filter.$filter.equals.Id = entity.Id;
		}
		if (entity.Name) {
			filter.$filter.contains.Name = entity.Name;
		}
		if (entity.UoM !== undefined) {
			filter.$filter.equals.UoM = entity.UoM;
		}
		if (entity.Price !== undefined) {
			filter.$filter.equals.Price = entity.Price;
		}
		Dialogs.postMessage({ topic: 'DependsOnScenariosTestProject.Product.Product.entitySearch', data: {
			entity: entity,
			filter: filter
		}});
		Dialogs.triggerEvent('DependsOnScenariosTestProject.Product.Product.clearDetails');
		$scope.cancel();
	};

	$scope.resetFilter = () => {
		$scope.entity = {};
		$scope.filter();
	};

	$scope.cancel = () => {
		Dialogs.closeWindow({ id: 'Product-filter' });
	};

	$scope.clearErrorMessage = () => {
		$scope.errorMessage = null;
	};
});