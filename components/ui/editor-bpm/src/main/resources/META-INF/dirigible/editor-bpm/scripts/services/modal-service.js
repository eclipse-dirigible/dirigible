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
 * Drop-in replacement for angular-strap 2.x's `$modal` factory using Bootstrap-3's
 * jQuery modal plugin. Compatible with the legacy call-sites in the editor-bpm
 * code-base, which rely on the following angular-strap behaviours:
 *
 *  - $modal({ template, scope, prefixEvent }) returns an instance with .show()/.hide();
 *    the same instance is also exposed as `scope.modal` on the new modal scope.
 *  - The modal scope (a child of the caller scope) gains `$hide()` and `$show()`
 *    helpers — many Flowable controllers call `$scope.$hide()` from their close()
 *    handler instead of going through the returned instance.
 *  - Events `<prefix>.show.before`, `<prefix>.show`, `<prefix>.hide.before`,
 *    `<prefix>.hide` are emitted UP the scope tree (so the caller scope can listen
 *    via $scope.$on or $scope.$parent.$on). Default prefix is 'modal'.
 */
flowableModule.factory('$modal', ['$http', '$templateCache', '$compile', '$rootScope', '$timeout',
    function ($http, $templateCache, $compile, $rootScope, $timeout) {

        return function (config) {
            var parentScope = config.scope || $rootScope;
            var modalScope = parentScope.$new();
            var prefix = config.prefixEvent || 'modal';
            var element = null;
            var pending = null;

            function emit(name) { modalScope.$emit(prefix + '.' + name); }

            function doHide() {
                if (!element) return;
                emit('hide.before');
                var el = element;
                element = null;
                jQuery(el).modal('hide');
                $timeout(function () {
                    el.remove();
                    emit('hide');
                    if (modalScope && !modalScope.$$destroyed) modalScope.$destroy();
                }, 300);
            }

            var instance = {
                $isShown: false,
                show: function () { if (element) { jQuery(element).modal('show'); instance.$isShown = true; } },
                hide: doHide,
                toggle: function () { instance.$isShown ? doHide() : instance.show(); },
                destroy: doHide
            };

            // angular-strap helpers expected on the modal scope by many controllers.
            modalScope.$hide = doHide;
            modalScope.$show = instance.show;
            // The instance is also surfaced as scope.modal so callers can do scope.modal.hide().
            modalScope.modal = instance;

            function show(html) {
                emit('show.before');
                element = angular.element(html);
                $compile(element)(modalScope);
                angular.element(document.body).append(element);
                pending = $timeout(function () {
                    // Bootstrap-3 modal CSS sets `.modal { display: none; }` and the plugin's
                    // .show() is supposed to flip it via jQuery.fn.show(). In practice (jQuery 2.0.3
                    // + Bootstrap 3.1.1 inside the editor iframe) the inline style sometimes ends
                    // up empty and the modal stays invisible while the backdrop covers the editor
                    // — leaving the canvas locked. angular-strap historically forced display:block
                    // itself; replicate that to avoid the lock-up.
                    element[0].style.display = 'block';
                    jQuery(element).modal({ show: false, backdrop: 'static', keyboard: false });
                    jQuery(element).modal('show');
                    instance.$isShown = true;
                    emit('show');
                });
            }

            var cached = $templateCache.get(config.template);
            if (cached) {
                show(cached);
            } else {
                $http.get(config.template, { cache: $templateCache })
                    .then(function (response) { show(response.data); });
            }

            // If the parent scope is destroyed while the modal is open, take the modal down with it.
            parentScope.$on('$destroy', function () {
                if (pending) $timeout.cancel(pending);
                if (element) {
                    try { jQuery(element).modal('hide'); } catch (e) { /* ignore */ }
                    element.remove();
                    element = null;
                }
            });

            return instance;
        };
    }
]);
