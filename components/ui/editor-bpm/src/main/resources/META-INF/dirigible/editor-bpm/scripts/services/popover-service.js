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
 * Drop-in replacement for angular-strap's `$popover` factory using Bootstrap-3's
 * jQuery popover plugin. Same call signature as the angular-strap original:
 *   $popover(targetElement, { template, placement, show, scope, container })
 *     -> { show(), hide(), destroy() }
 */
flowableModule.factory('$popover', ['$http', '$templateCache', '$compile', '$rootScope', '$timeout',
    function ($http, $templateCache, $compile, $rootScope, $timeout) {

        function mapPlacement(p) {
            // angular-strap supported 'top-right' etc.; Bootstrap-3 jQuery popover only takes 4 base sides.
            if (!p) return 'right';
            if (p.indexOf('top') === 0) return 'top';
            if (p.indexOf('bottom') === 0) return 'bottom';
            if (p.indexOf('left') === 0) return 'left';
            return 'right';
        }

        return function (target, config) {
            var $target = jQuery(target);
            var popoverScope = config.scope ? config.scope.$new() : $rootScope.$new();
            var compiledContent = null;
            var initialized = false;

            function build(html) {
                var element = angular.element('<div>').append(html);
                compiledContent = $compile(element.contents())(popoverScope);
                $target.popover({
                    html: true,
                    placement: mapPlacement(config.placement),
                    container: config.container || false,
                    trigger: 'manual',
                    content: function () {
                        // The DOM nodes belong to compiledContent; pull them out as raw HTML each call.
                        var holder = jQuery('<div>').append(compiledContent.clone(true));
                        return holder.html();
                    }
                });
                initialized = true;
                if (config.show) $target.popover('show');
            }

            var cached = $templateCache.get(config.template);
            if (cached) {
                build(cached);
            } else {
                $http.get(config.template, { cache: $templateCache })
                    .then(function (response) { build(response.data); });
            }

            return {
                show: function () { if (initialized) $target.popover('show'); },
                hide: function () { if (initialized) $target.popover('hide'); },
                destroy: function () {
                    if (initialized) { $target.popover('destroy'); initialized = false; }
                    popoverScope.$destroy();
                }
            };
        };
    }
]);
