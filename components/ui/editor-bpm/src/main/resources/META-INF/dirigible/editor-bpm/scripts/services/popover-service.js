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
 * Drop-in replacement for angular-strap 2.x's `$popover` factory using Bootstrap-3's
 * jQuery popover plugin. Mirrors the angular-strap surface area that the editor-bpm
 * controllers rely on:
 *
 *  - $popover(target, { template, placement, show, scope, container, prefixEvent })
 *    returns an instance with .show()/.hide()/.destroy() and an exposed .$scope
 *    (used by controllers as `state.popover.$scope.$on('tooltip.hide', destroy)`).
 *  - The popover scope (child of the caller scope) gains `$hide()` / `$show()`
 *    helpers so templates can do `ng-click="$hide()"`.
 *  - Events `<prefix>.show.before`, `<prefix>.show`, `<prefix>.hide.before`,
 *    `<prefix>.hide` are emitted UP the scope tree. Default prefix is 'tooltip'
 *    (matching angular-strap's $popover which is built on top of $tooltip).
 */
flowableModule.factory('$popover', ['$http', '$templateCache', '$compile', '$rootScope', '$timeout',
    function ($http, $templateCache, $compile, $rootScope, $timeout) {

        function mapPlacement(p) {
            // angular-strap supported 'top-right', 'bottom-left', etc. Bootstrap-3's jQuery popover
            // only takes 4 base sides — strip the suffix.
            if (!p) return 'right';
            if (p.indexOf('top') === 0) return 'top';
            if (p.indexOf('bottom') === 0) return 'bottom';
            if (p.indexOf('left') === 0) return 'left';
            return 'right';
        }

        return function (target, config) {
            var $target = jQuery(target);
            var parentScope = config.scope || $rootScope;
            var popoverScope = parentScope.$new();
            var prefix = config.prefixEvent || 'tooltip';
            var compiledContent = null;
            var initialized = false;
            var visible = false;

            function emit(name) { popoverScope.$emit(prefix + '.' + name); }

            function doShow() {
                if (!initialized || visible) return;
                emit('show.before');
                $target.popover('show');
                visible = true;
                emit('show');
            }

            function doHide() {
                if (!initialized || !visible) return;
                emit('hide.before');
                $target.popover('hide');
                visible = false;
                emit('hide');
            }

            function destroy() {
                if (initialized) {
                    try { $target.popover('destroy'); } catch (e) { /* ignore */ }
                    initialized = false;
                    visible = false;
                }
                if (popoverScope && !popoverScope.$$destroyed) popoverScope.$destroy();
            }

            var instance = {
                show: doShow,
                hide: doHide,
                destroy: destroy,
                $scope: popoverScope,
                $element: $target
            };

            // angular-strap convenience: templates can do `ng-click="$hide()"`.
            popoverScope.$hide = doHide;
            popoverScope.$show = doShow;

            function build(html) {
                // Compile the template against the popover scope so its bindings come alive.
                var holder = angular.element('<div>').append(html);
                compiledContent = $compile(holder.contents())(popoverScope);
                $target.popover({
                    html: true,
                    placement: mapPlacement(config.placement),
                    container: config.container || false,
                    trigger: 'manual',
                    content: function () {
                        // jQuery clones compiledContent into the popover's DOM each show().
                        var cloneHost = jQuery('<div>').append(compiledContent.clone(true));
                        return cloneHost.html();
                    }
                });
                initialized = true;
                if (config.show) doShow();
            }

            var cached = $templateCache.get(config.template);
            if (cached) {
                build(cached);
            } else {
                $http.get(config.template, { cache: $templateCache })
                    .then(function (response) { build(response.data); });
            }

            // If the parent scope is destroyed, take the popover down with it.
            parentScope.$on('$destroy', destroy);

            return instance;
        };
    }
]);
