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
addExtPoints({ locale: 'application-locales' });
const translationView = angular.module('translation', ['blimpKit', 'platformView', 'platformLocale']);
translationView.controller('TranslationController', ($scope, Extensions) => {
	$scope.langs = [];

	const flatten = (obj, prefix = '') => Object.keys(obj).reduce((acc, k) => {
		const pre = prefix.length ? `${prefix}.` : '';
		if (
			typeof obj[k] === "object" &&
			obj[k] !== null &&
			Object.keys(obj[k]).length > 0
		) Object.assign(acc, flatten(obj[k], pre + k));
		else acc[pre + k] = obj[k];
		return acc;
	}, {});

	Extensions.getTranslations().then((response) => {
		$scope.langs.push(...response.data.locales);
		$scope.translations = {};
		for (let i = 0; i < $scope.langs.length; i++) {
			Object.keys(response.data.translations[$scope.langs[i].id]).forEach((namespace) => {
				const flatTranslations = flatten(response.data.translations[$scope.langs[i].id][namespace]);
				if (!$scope.translations[namespace]) $scope.translations[namespace] = [];
				for (const [tkey, value] of Object.entries(flatTranslations)) {
					const index = $scope.translations[namespace].findIndex((t) => t.tkey === tkey);
					if (index === -1) {
						$scope.translations[namespace].push({
							tkey: tkey,
							[$scope.langs[i].id]: value
						})
					} else {
						$scope.translations[namespace][index][$scope.langs[i].id] = value;
					}
				}
				$scope.translations[namespace].sort((a, b) => a.tkey.localeCompare(b.tkey));
			});
		}
	}, (error) => {
		console.error(error);
	});
});