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
var require = { paths: { vs: '/webjars/monaco-editor/min/vs' } };
angular.module('codeEditor', ['platformTheming']).directive('codeEditor', (Theme, $window) => {
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
            pre: (scope) => {
                scope.monacoTheme = 'vs-light';
                scope.autoListener = false;
                const theme = Theme.getTheme();
                scope.themeId = theme.id;
                if (theme.type === 'light') scope.monacoTheme = 'vs-light';
                else if (theme.type === 'dark') {
                    if (scope.themeId === 'classic-dark') scope.monacoTheme = 'classic-dark';
                    else scope.monacoTheme = 'blimpkit-dark';
                } else {
                    if ($window.matchMedia && $window.matchMedia('(prefers-color-scheme: dark)').matches) {
                        if (scope.themeId.startsWith('classic')) scope.monacoTheme = 'classic-dark';
                        else scope.monacoTheme = 'blimpkit-dark';
                    } else scope.monacoTheme = 'vs-light';
                    $window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
                        if (event.matches) {
                            if (scope.themeId.startsWith('classic')) scope.monacoTheme = 'classic-dark';
                            else scope.monacoTheme = 'blimpkit-dark';
                        } else scope.monacoTheme = 'vs-light';
                        monaco.editor.setTheme(scope.monacoTheme);
                        scope.autoListener = true;
                    });
                }
            },
            post: (scope, element, _, ngModel) => {
                const themingHub = new ThemingHub()
                let outsideChange = false;
                monaco.editor.defineTheme('blimpkit-dark', {
                    base: 'vs-dark',
                    inherit: true,
                    rules: [{ background: '1d1d1d' }],
                    colors: {
                        'editor.background': '#1d1d1d',
                        'breadcrumb.background': '#1d1d1d',
                        'minimap.background': '#1d1d1d',
                        'editorGutter.background': '#1d1d1d',
                        'editorMarkerNavigation.background': '#1d1d1d',
                        'input.background': '#242424',
                        'input.border': '#4e4e4e',
                        'editorWidget.background': '#1d1d1d',
                        'editorWidget.border': '#313131',
                        'editorSuggestWidget.background': '#262626',
                        'dropdown.background': '#262626',
                    }
                });

                monaco.editor.defineTheme('classic-dark', {
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

                ngModel.$render = () => {
                    outsideChange = true;
                    model.setValue(ngModel.$viewValue);
                };

                model.onDidChangeContent((event) => {
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

                themingHub.onThemeChange((theme) => {
                    scope.themeId = theme.id;
                    if (theme.type === 'light') {
                        scope.monacoTheme = 'vs-light';
                        monaco.editor.setTheme(scope.monacoTheme);
                    } else if (theme.type === 'dark') {
                        if (scope.themeId === 'classic-dark') monacoTheme = 'classic-dark';
                        else scope.monacoTheme = 'blimpkit-dark';
                        monaco.editor.setTheme(scope.monacoTheme);
                    } else if (!scope.autoListener) {
                        if ($window.matchMedia && $window.matchMedia('(prefers-color-scheme: dark)').matches) {
                            scope.monacoTheme = 'blimpkit-dark';
                        } else scope.monacoTheme = 'vs-light';
                        $window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', event => {
                            if (event.matches) {
                                if (scope.themeId.startsWith('classic')) scope.monacoTheme = 'classic-dark';
                                else scope.monacoTheme = 'blimpkit-dark';
                            } else scope.monacoTheme = 'vs-light';
                            monaco.editor.setTheme(scope.monacoTheme);
                            scope.autoListener = true;
                        });
                    }
                });
            }
        },
        template: `<div class="bk-fill-parent"><div class="bk-fill-parent"></div></div>`,
    }
});