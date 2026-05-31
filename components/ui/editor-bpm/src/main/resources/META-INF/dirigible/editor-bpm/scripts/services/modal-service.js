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
 * Drop-in replacement for angular-strap 2.x's `$modal` factory. The factory
 * signature and the `$hide`/`$show`/`modal`/`prefixEvent` contract are
 * preserved so the ~30 existing call-sites in editor-bpm don't have to change.
 *
 * Two rendering paths are supported during the BlimpKit visual migration:
 *
 *  - BlimpKit `<bk-dialog>` templates (preferred, target shape).
 *  - Legacy Bootstrap-3 `<div class="modal">` templates (kept alive until every
 *    popup template has been migrated, then removable).
 *
 * Detection is purely structural — if the template root is a `<bk-dialog>` (or
 * the markup contains one), the new path is taken. Once every popup is on
 * `<bk-dialog>`, the Bootstrap-3 branch can be deleted along with the jQuery
 * modal plugin dependency.
 */
flowableModule.factory('$modal', ['$http', '$templateCache', '$compile', '$rootScope', '$timeout',
    function ($http, $templateCache, $compile, $rootScope, $timeout) {

        var BK_DIALOG_PATTERN = /<bk-dialog\b/i;

        return function (config) {
            var parentScope = config.scope || $rootScope;
            var modalScope = parentScope.$new();
            var prefix = config.prefixEvent || 'modal';
            var element = null;
            var pending = null;
            var bkDialog = false;

            function emit(name) { modalScope.$emit(prefix + '.' + name); }

            function applyAsync(fn) {
                if (modalScope.$$phase || $rootScope.$$phase) {
                    fn();
                } else {
                    modalScope.$apply(fn);
                }
            }

            function doHide() {
                if (!element) return;
                emit('hide.before');
                var el = element;
                element = null;

                if (bkDialog) {
                    // The `visible` binding drives the `fd-dialog--active` class via
                    // ng-class; flipping it animates the dialog out. Re-evaluating
                    // synchronously inside $apply keeps the close button responsive
                    // even when the click came from outside an Angular handler.
                    applyAsync(function () { instance.visible = false; });
                } else {
                    try { jQuery(el).modal('hide'); } catch (e) { /* ignore */ }
                }
                instance.$isShown = false;

                $timeout(function () {
                    el.remove();
                    emit('hide');
                    if (modalScope && !modalScope.$$destroyed) modalScope.$destroy();
                }, 300);
            }

            var instance = {
                $isShown: false,
                visible: true,
                show: function () {
                    if (!element) return;
                    if (bkDialog) {
                        applyAsync(function () { instance.visible = true; });
                    } else {
                        jQuery(element).modal('show');
                    }
                    instance.$isShown = true;
                },
                hide: doHide,
                toggle: function () { instance.$isShown ? doHide() : instance.show(); },
                destroy: doHide
            };

            // angular-strap helpers expected on the modal scope by many controllers.
            modalScope.$hide = doHide;
            modalScope.$show = instance.show;
            // The instance is also surfaced as scope.modal so callers can do scope.modal.hide()
            // and migrated templates can bind `visible="modal.visible"`.
            modalScope.modal = instance;

            function render(html) {
                emit('show.before');
                bkDialog = BK_DIALOG_PATTERN.test(html);
                element = angular.element(html);
                $compile(element)(modalScope);
                angular.element(document.body).append(element);

                if (bkDialog) {
                    // bk-dialog reads `visible` from `modal.visible` via ng-class binding.
                    // It's already true on the instance, so the dialog renders active on
                    // its first digest. Emit `show` after that digest so listeners run
                    // against fully-rendered DOM (parity with the legacy path).
                    instance.$isShown = true;
                    pending = $timeout(function () { emit('show'); });
                } else {
                    pending = $timeout(function () {
                        // Bootstrap-3 modal CSS sets `.modal { display: none; }` and the
                        // plugin's .show() flips it via jQuery.fn.show(). In practice
                        // (jQuery 2.0.3 + Bootstrap 3.1.1 inside the editor iframe) the
                        // inline style sometimes ends up empty and the modal stays
                        // invisible while the backdrop covers the editor — leaving the
                        // canvas locked. angular-strap historically forced display:block
                        // itself; replicate that.
                        element[0].style.display = 'block';
                        jQuery(element).modal({ show: false, backdrop: 'static', keyboard: false });
                        jQuery(element).modal('show');
                        instance.$isShown = true;
                        emit('show');
                    });
                }
            }

            var cached = $templateCache.get(config.template);
            if (cached) {
                render(cached);
            } else {
                $http.get(config.template, { cache: $templateCache })
                    .then(function (response) { render(response.data); });
            }

            // If the parent scope is destroyed while the modal is open, take it down too.
            parentScope.$on('$destroy', function () {
                if (pending) $timeout.cancel(pending);
                if (element) {
                    if (!bkDialog) {
                        try { jQuery(element).modal('hide'); } catch (e) { /* ignore */ }
                    }
                    element.remove();
                    element = null;
                }
            });

            return instance;
        };
    }
]);
