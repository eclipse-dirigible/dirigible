/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

/**
 * Simple $modal factory compatible with angular-strap's $modal API.
 * Uses Bootstrap 3 jQuery modal under the hood — no angular-strap needed.
 */
flowableModule.factory('$modal', ['$http', '$templateCache', '$compile', '$rootScope', '$timeout',
    function ($http, $templateCache, $compile, $rootScope, $timeout) {

        return function (config) {
            var modalScope = config.scope ? config.scope.$new() : $rootScope.$new();
            var element = null;

            var instance = {
                hide: function () {
                    if (element) {
                        jQuery(element).modal('hide');
                        var el = element;
                        element = null;
                        $timeout(function () {
                            el.remove();
                            modalScope.$broadcast('modal.hide');
                        }, 300);
                    }
                }
            };

            function show(html) {
                element = angular.element(html);
                var compiled = $compile(element)(modalScope);
                angular.element(document.body).append(compiled);
                $timeout(function () {
                    jQuery(element).modal({ backdrop: 'static', keyboard: false });
                    jQuery(element).modal('show');
                    modalScope.$broadcast('modal.show');
                });
            }

            var cached = $templateCache.get(config.template);
            if (cached) {
                show(cached);
            } else {
                $http.get(config.template, { cache: $templateCache }).then(function (response) {
                    show(response.data);
                });
            }

            modalScope.modal = instance;

            return instance;
        };
    }
]);
