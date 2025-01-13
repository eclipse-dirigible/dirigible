/*
 * Copyright (c) 2010-2024 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *   SAP - initial API and implementation
 */
// /webjars/monaco-editor/min/vs/base/browser/ui/codicons/codicon/codicon.ttf
var require = { paths: { vs: '/webjars/monaco-editor/min/vs' } };
angular.module('codeEditor', ['ideMessageHub']).directive('codeEditor', ['messageHub', function (messageHub) {
    /**
     * readOnly: Boolean - Sets the editor mode. Default is 'false'.
     * codeLang: String - The language of the code. Default is 'javascript'.
     * actions: Array<IActionDescriptor> - An array of Monaco actions.
     * onModelChange: Function - Callback function triggered when the model gets changed. Does not trigger when the model is changed from the outside.
     */
    return {
        restrict: 'E',
        transclude: false,
        replace: true,
        require: '?ngModel',
        scope: {
            readOnly: '<?',
            codeLang: '@?',
            actions: '<?',
            onModelChange: '&?',
        },
        link: {
            pre: function (scope) {
                scope.monacoTheme = 'vs-light';
                const theme = JSON.parse(localStorage.getItem('DIRIGIBLE.theme') || '{}');
                if (theme.type === 'light') scope.monacoTheme = 'vs-light';
                else scope.monacoTheme = 'quartz-dark';
            },
            post: function (scope, element, _, ngModel) {
                let outsideChange = false;
                monaco.editor.defineTheme('quartz-dark', {
                    base: 'vs-dark',
                    inherit: true,
                    rules: [{ background: '1c2228' }],
                    colors: {
                        'editor.background': '#1c2228',
                        'breadcrumb.background': '#1c2228',
                        'minimap.background': '#1c2228',
                        'editorGutter.background': '#1c2228',
                        'editorMarkerNavigation.background': '#1c2228',
                        'input.background': '#29313a',
                        'input.border': '#8696a9',
                        'editorWidget.background': '#1c2228',
                        'editorWidget.border': '#495767',
                        'editorSuggestWidget.background': '#29313a',
                        'dropdown.background': '#29313a',
                    }
                });

                const codeEditor = monaco.editor.create(element[0].firstChild, {
                    value: ngModel.$viewValue || '',
                    automaticLayout: true,
                    language: scope.codeLang || 'javascript',
                    readOnly: scope.readOnly ? true : false,
                });

                const model = codeEditor.getModel();

                ngModel.$render = function () {
                    outsideChange = true;
                    model.setValue(ngModel.$viewValue);
                }

                model.onDidChangeContent(function (event) {
                    if (!outsideChange) {
                        ngModel.$setViewValue(model.getValue());
                        ngModel.$validate();
                    }
                    if (scope.onModelChange && !outsideChange) scope.onModelChange()(event);
                    outsideChange = false;
                });


                monaco.editor.setTheme(scope.monacoTheme);

                if (scope.actions) {
                    for (let i = 0; i < scope.actions.length; i++) {
                        codeEditor.addAction(scope.actions[i]);
                    }
                }

                messageHub.onThemeChanged(function () {
                    monaco.editor.setTheme(scope.monacoTheme);
                });
            }
        },
        template: `<div class="dg-fill-parent"><div class="dg-fill-parent"></div></div>`,
    }
}])