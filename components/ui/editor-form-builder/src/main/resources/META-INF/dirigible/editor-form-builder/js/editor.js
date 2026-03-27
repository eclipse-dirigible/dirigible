/*
 * Copyright (c) 2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
const editorView = angular.module('app', ['blimpKit', 'platformView', 'platformShortcuts', 'WorkspaceService', 'codeEditor', 'GenerateService', 'TemplatesService']);
editorView.controller('DesignerController', ($scope, $window, $document, $timeout, $compile, uuid, ViewParameters, WorkspaceService, GenerateService, TemplatesService, ButtonStates) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    const contextMenuHub = new ContextMenuHub();
    let genFile = '';
    let workspace = '';
    let formContainer;
    $scope.formSelected = false;
    $scope.canRegenerate = false
    $scope.selectedTab = 'designer';
    $scope.changed = false;
    $scope.state = {
        initialized: false,
        isBusy: true,
        error: false,
        busyText: 'Loading...',
        canSave: true,
        preview: false,
    };

    $scope.forms = {
        formProperties: {},
    };

    $scope.selectedCtrl = undefined;
    $scope.availableProps;
    $scope.selectedContainer = undefined;

    $scope.formModel = [];
    $scope.formData = {
        feeds: [],
        scripts: [],
        code: ''
    };

    angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

    $scope.getPaddingClass = (padding, side) => {
        if (!padding) return '';

        const sideMap = {
            'top': 'bk-padding-top',
            'bottom': 'bk-padding-bottom',
            'top-bottom': 'bk-padding-top-bottom',
            'begin-end': 'fd-padding-begin-end',
            '': 'bk-padding'
        };

        const baseClass = sideMap[side];

        return baseClass ? `${baseClass}--${padding}` : '';
    };

    const gapClasses = `{{ props.gap.value !== '' ? 'bk-box--gap-' + props.gap.value : 'bk-box--gap' }}`;

    const justifyClasses = `{{ props.justify.value === 'center' ? 'bk-box--center' : '' }} {{ props.justify.value === 'end' ? 'bk-box--end' : '' }} {{ props.justify.value === 'stretch' ? 'bk-box--items-stretch' : '' }} {{ props.justify.value === 'between' ? 'bk-box--space-between' : '' }}`;

    const paddingProps = {
        padding: {
            type: 'dropdown',
            label: 'Padding',
            value: '',
            items: [
                {
                    label: 'Default',
                    value: '',
                },
                {
                    label: 'Tiny',
                    value: 'tiny',
                },
                {
                    label: 'Small',
                    value: 'sm',
                },
                {
                    label: 'Medium',
                    value: 'md',
                },
                {
                    label: 'Large',
                    value: 'lg',
                },
            ]
        },
        side: {
            type: 'dropdown',
            label: 'Padding side',
            value: '',
            items: [
                {
                    label: 'All',
                    value: '',
                },
                {
                    label: 'Top',
                    value: 'top',
                },
                {
                    label: 'Bottom',
                    value: 'bottom',
                },
                {
                    label: 'Top & Bottom',
                    value: 'top-bottom',
                },
                {
                    label: 'Left & Right',
                    value: 'begin-end',
                },
            ]
        },
    };

    const gapProps = {
        gap: {
            type: 'dropdown',
            label: 'Item Gap',
            value: '',
            items: [
                {
                    label: 'Default',
                    value: '',
                },
                {
                    label: 'Medium',
                    value: 'm',
                },
                {
                    label: 'Large',
                    value: 'l',
                },
                {
                    label: 'Extra Large',
                    value: 'xl',
                },
                {
                    label: '2x Large',
                    value: 'xxl',
                },
            ]
        },
    };

    const justifyProps = {
        justify: {
            type: 'dropdown',
            label: 'Item Placement',
            value: 'start',
            items: [
                {
                    label: 'Start',
                    value: 'start',
                },
                {
                    label: 'Center',
                    value: 'center',
                },
                {
                    label: 'End',
                    value: 'end',
                },
                {
                    label: 'Between',
                    value: 'between',
                },
                {
                    label: 'Stretch',
                    value: 'stretch',
                },
            ]
        }
    };

    $scope.formProps = {
        name: {
            type: 'text',
            label: 'Title',
            value: '',
            required: false
        },
        url: {
            type: 'text',
            label: 'Action URL',
            value: '',
            required: false
        },
        method: {
            type: 'dropdown',
            label: 'Method',
            value: 'get',
            items: [
                {
                    label: 'GET',
                    value: 'get',
                },
                {
                    label: 'POST',
                    value: 'post',
                },
            ]
        },
        successMsg: {
            type: 'text',
            label: 'Success Message',
            value: '',
            required: false
        },
        ...JSON.parse(JSON.stringify(paddingProps))
    };

    const getControlWrapper = (content, classes) => {
        return `<div class="fb-control-wrapper ${classes ? classes : ''}" ng-click="showProps($event)" data-id="{{id}}">${content}</div>`;
    };

    $scope.builderComponents = [
        {
            id: 'fb-controls',
            label: 'Controls',
            items: [
                {
                    controlId: 'button',
                    label: 'Button',
                    icon: 'sap-icon--border',
                    description: 'Button',
                    template: getControlWrapper(`<bk-button type="{{props.isSubmit.value ? 'submit' : 'button'}}" compact="props.isCompact.value" label="{{props.label.value}}" state="{{props.type.value}}" ng-click="submit()"></bk-button>`),
                    props: {
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Button',
                            placeholder: 'Button label',
                            required: true
                        },
                        type: {
                            type: 'dropdown',
                            label: 'Button State',
                            value: '',
                            items: [
                                {
                                    label: 'Default',
                                    value: '',
                                },
                                {
                                    label: 'Emphasized',
                                    value: 'emphasized',
                                },
                                {
                                    label: 'Ghost',
                                    value: 'ghost',
                                },
                                {
                                    label: 'Positive',
                                    value: 'positive',
                                },
                                {
                                    label: 'Negative',
                                    value: 'negative',
                                },
                                {
                                    label: 'Attention',
                                    value: 'attention',
                                },
                                {
                                    label: 'Transparent',
                                    value: 'transparent',
                                }
                            ]
                        },
                        isSubmit: {
                            type: 'checkbox',
                            label: 'Submits Form',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        callback: {
                            type: 'text',
                            label: 'Callback Function',
                            value: '',
                            placeholder: 'callbackFn()',
                        },
                    }
                },
                {
                    controlId: 'input-textfield',
                    label: 'Text Field',
                    icon: 'sap-icon--edit',
                    description: 'Input field',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
                        <bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">
                            {{ props.label.value }}
                        </bk-form-label>
                        <bk-form-input-message state="{{ props.errorMessage.invalid ? 'error' : '' }}" message="props.errorMessage.value || 'Incorrect input'">
                            <bk-input id="{{props.id.value}}" type="text" placeholder="{{props.placeholder.value}}" compact="props.isCompact.value"
                                state="{'error' : props.errorMessage.invalid }" name="{{props.id.value}}" ng-required="props.required.value" ng-readonly="props.readonly.value">
                            </bk-input>
                        </bk-form-input-message>
                    </bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        placeholder: {
                            type: 'text',
                            label: 'Placeholder',
                            value: '',
                            placeholder: 'Input placeholder',
                        },
                        type: {
                            type: 'dropdown',
                            label: 'Input Type',
                            value: 'text',
                            required: true,
                            items: [
                                {
                                    label: 'Text',
                                    value: 'text',
                                },
                                {
                                    label: 'Email',
                                    value: 'email',
                                },
                                {
                                    label: 'Password',
                                    value: 'password',
                                },
                                {
                                    label: 'URL',
                                    value: 'URL',
                                }
                            ],
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'inputVar',
                            required: true,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                        minLength: {
                            type: 'number',
                            label: 'Minimum Length',
                            value: 0,
                            min: 0,
                            step: 1,
                            required: false,
                        },
                        maxLength: {
                            type: 'number',
                            label: 'Maximum Length',
                            value: -1,
                            min: -1,
                            step: 1,
                            required: false,
                        },
                        validationRegex: {
                            type: 'text',
                            label: 'Regex Validation',
                            value: '',
                            placeholder: '^[^/]*$',
                            required: false,
                        },
                        errorMessage: {
                            type: 'text',
                            label: 'Error State Message',
                            value: 'Incorrect input',
                            invalid: false,
                            placeholder: 'Incorrect input',
                            required: false,
                        },
                    },
                },
                {
                    controlId: 'input-textarea',
                    label: 'Text Area',
                    icon: 'sap-icon--edit',
                    description: 'Text Area',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
                        <bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">{{ props.label.value }}</bk-form-label>
                        <bk-form-input-message state="{{ props.errorMessage.invalid ? 'error' : '' }}" message="props.errorMessage.value || 'Incorrect input'">
                            <bk-textarea id="{{props.id.value}}" type="text" placeholder="{{props.placeholder.value}}" compact="props.isCompact.value"
                                state="{'error' : props.errorMessage.invalid }" name="{{props.id.value}}" ng-required="props.required.value" ng-readonly="props.readonly.value">
                            </bk-textarea>
                        </bk-form-input-message>
                    </bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        placeholder: {
                            type: 'text',
                            label: 'Placeholder',
                            value: '',
                            placeholder: 'Input placeholder',
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: '',
                            required: true,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                        minLength: {
                            type: 'number',
                            label: 'Minimum Length',
                            value: 0,
                            min: 0,
                            step: 1,
                            required: false,
                        },
                        maxLength: {
                            type: 'number',
                            label: 'Maximum Length',
                            value: -1,
                            min: -1,
                            step: 1,
                            required: false,
                        },
                        validationRegex: {
                            type: 'text',
                            label: 'Regex Validation',
                            value: '',
                            placeholder: 'Regular expression',
                            required: false,
                        },
                        errorMessage: {
                            type: 'text',
                            label: 'Error State Message',
                            value: 'Incorrect input',
                            invalid: false,
                            placeholder: 'Incorrect input',
                            required: false,
                        },
                    },
                },
                {
                    controlId: 'input-time',
                    label: 'Time Field',
                    icon: 'sap-icon--in-progress',
                    description: 'Time input',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
                    <bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">{{ props.label.value }}</bk-form-label>
                    <bk-input id="{{props.id.value}}" name="{{props.id.value}}" compact="props.isCompact.value" ng-required="props.required.value" ng-readonly="props.readonly.value" type="time" value="13:30">
	                </bk-input></bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'inputVar',
                            required: true,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                    },
                },
                {
                    controlId: 'input-date',
                    label: 'Date Field',
                    icon: 'sap-icon--calendar',
                    description: 'Date input',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
                    <bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">{{ props.label.value }}</bk-form-label>
                    <bk-input id="{{props.id.value}}" name="{{props.id.value}}" compact="props.isCompact.value" ng-required="props.required.value" ng-readonly="props.readonly.value" type="{{props.type.value}}">
	                </bk-input></bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        type: {
                            type: 'dropdown',
                            label: 'Input Type',
                            value: 'date',
                            required: true,
                            items: [
                                {
                                    label: 'Date',
                                    value: 'date',
                                },
                                {
                                    label: 'Datetime',
                                    value: 'datetime-local',
                                }
                            ],
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'inputVar',
                            required: true,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                    },
                },
                {
                    controlId: 'input-number',
                    label: 'Number Field',
                    icon: 'sap-icon--number-sign',
                    description: 'Stepped number input',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
						<bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">{{ props.label.value }}</bk-form-label>
						<bk-step-input input-id="{{props.id.value}}" name="{{props.id.value}}" compact="props.isCompact.value" ng-model="props.minNum.value" placeholder="{{props.placeholder.value}}" ng-required="props.required.value" is-readonly="props.readonly.value">
						</bk-step-input>
					</bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        placeholder: {
                            type: 'text',
                            label: 'Placeholder',
                            value: '',
                            placeholder: 'Input placeholder',
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'inputVar',
                            required: true,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                        minNum: {
                            type: 'number',
                            label: 'Minimum Number',
                            value: undefined,
                            step: 1,
                            required: false,
                        },
                        maxNum: {
                            type: 'number',
                            label: 'Maximum Number',
                            value: undefined,
                            step: 1,
                            required: false,
                        },
                        step: {
                            type: 'number',
                            label: 'Step',
                            value: 1,
                            step: 1,
                            required: false,
                        },
                    },
                },
                {
                    controlId: 'input-color',
                    label: 'Color',
                    icon: 'sap-icon--color-fill',
                    description: 'Color input',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
                    <bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">{{ props.label.value }}</bk-form-label>
                    <bk-input id="{{props.id.value}}" name="{{props.id.value}}" compact="props.isCompact.value" ng-required="props.required.value" ng-readonly="props.readonly.value" type="color" value="#ffbe6f">
	                </bk-input></bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'inputVar',
                            required: true,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                    },
                },
                {
                    controlId: 'input-combobox',
                    label: 'Combo Box',
                    svg: '/services/web/editor-form-builder/images/combobox.svg',
                    description: 'Combobox selection',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
                        <bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">{{ props.label.value }}</bk-form-label>
                        <bk-combobox-input compact="props.isCompact.value" filter="{{props.filter.value}}" dropdown-items="[{text: 'combo',value: 'combo'}]" ng-required="props.required.value" is-readonly="props.readonly.value" placeholder="{{props.placeholder.value}}" btn-aria-label="show/hide {{ props.label.value }} options" list-aria-label="{{ props.label.value }} options"></bk-combobox-input>
                    </bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        placeholder: {
                            type: 'text',
                            label: 'Placeholder',
                            value: '',
                            placeholder: 'Input placeholder',
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                        filter: {
                            type: 'dropdown',
                            label: 'Filter Type',
                            value: '',
                            items: [
                                {
                                    label: 'Starts With',
                                    value: '',
                                },
                                {
                                    label: 'Contains',
                                    value: 'Contains',
                                },
                                {
                                    label: 'Contains Each',
                                    value: 'ContainsEach',
                                },
                            ]
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: '',
                            required: true,
                        },
                        items: {
                            type: 'text',
                            label: 'Items',
                            value: '',
                            placeholder: '',
                        },
                        info: {
                            type: 'textinfo',
                            label: 'Example Items Data',
                            value: 'The "secondaryText" and "svg" keys are optinal.\n\n[\n  {\n    "value": "combo1",\n    "text": "Item 1",\n    "secondaryText": "1",\n    "svg": "./icon.svg"\n  },\n  {\n    "value": "combo2",\n    "text": "Item 2",\n    "secondaryText": "2",\n    "svg": "./icon.svg"\n  }\n]',
                        },
                    },
                },
                {
                    controlId: 'input-select',
                    label: 'Dropdown',
                    svg: '/services/web/editor-form-builder/images/select.svg',
                    description: 'Dropdown selection',
                    template: getControlWrapper(`<bk-form-item horizontal="props.horizontal.value">
                        <bk-form-label colon="true" ng-required="props.required.value" for="{{props.id.value}}">{{ props.label.value }}</bk-form-label>
                        <bk-select placeholder="{{props.placeholder.value}}" label-id="{{ props.id.value }}" compact="props.isCompact.value"
                            ng-required="props.required.value" is-readonly="props.readonly.value" ng-model="props.staticOptions.defaultValue" dropdown-fixed="true">
                            <bk-option text="{{ menuItem.label }}" value="menuItem.value" ng-repeat="menuItem in props.staticOptions.value track by $index"></bk-option>
                        </bk-select>
                    </bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Input',
                            placeholder: 'Input label',
                            required: false,
                        },
                        horizontal: {
                            type: 'checkbox',
                            label: 'Horizontal',
                            value: false,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        staticData: {
                            type: 'checkbox',
                            label: 'Static Data',
                            value: false,
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: '',
                            required: true,
                        },
                        options: {
                            enabledOn: { key: 'staticData', value: false },
                            type: 'text',
                            label: 'Options',
                            value: '',
                            placeholder: '',
                        },
                        optionLabel: {
                            enabledOn: { key: 'staticData', value: false },
                            type: 'text',
                            label: 'Option Label Key',
                            value: 'label',
                            placeholder: 'label',
                        },
                        optionValue: {
                            enabledOn: { key: 'staticData', value: false },
                            type: 'text',
                            label: 'Option Value Key',
                            value: 'value',
                            placeholder: 'value',
                        },
                        staticOptions: {
                            enabledOn: { key: 'staticData', value: true },
                            type: 'list',
                            label: 'Options',
                            labelText: 'Label',
                            valueText: 'Value',
                            defaultValue: '',
                            value: [
                                { label: 'Item 1', value: 'item1' },
                                { label: 'Item 2', value: 'item2' }
                            ]
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                    },
                },
                {
                    controlId: 'input-checkbox',
                    label: 'Checkbox',
                    icon: 'sap-icon--complete',
                    description: 'description',
                    template: getControlWrapper(`<bk-form-item>
                        <bk-checkbox id="{{ props.id.value }}" compact="props.isCompact.value" ng-readonly="props.readonly.value"></bk-checkbox>
                        <bk-checkbox-label for="{{ props.id.value }}">{{ props.label.value }}</bk-checkbox-label>
                    </bk-form-item>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true
                        },
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Checkbox',
                            placeholder: '',
                            required: true
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'modelVar',
                            required: true,
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                    },
                },
                {
                    controlId: 'input-radio',
                    label: 'Radio',
                    icon: 'sap-icon--record',
                    description: 'Radio select',
                    template: getControlWrapper(`<bk-form-group label="{{props.label.value}}">
                    <bk-form-item ng-repeat="option in props.staticOptions.value track by $index">
                        <bk-radio id="{{ props.id.value + $index }}" name="{{ props.id.value }}" compact="props.isCompact.value" ng-model="props.staticOptions.defaultValue" ng-value="option.value" ng-required="props.required.value" ng-readonly="props.readonly.value"></bk-radio>
                        <bk-radio-label for="{{ props.id.value + $index }}">{{option.label}}</bk-radio-label>
                    </bk-form-item>
                    </bk-form-group>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'The name of the radio button(s)',
                            value: '',
                            required: true
                        },
                        label: {
                            type: 'text',
                            label: 'Group Title',
                            value: 'Radio group',
                            placeholder: '',
                        },
                        staticData: {
                            type: 'checkbox',
                            label: 'Static Data',
                            value: true,
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'selectedOptionValue',
                            required: true,
                        },
                        optionLabel: {
                            enabledOn: { key: 'staticData', value: false },
                            type: 'text',
                            label: 'Option Label Key',
                            value: 'label',
                            placeholder: 'label',
                        },
                        optionValue: {
                            enabledOn: { key: 'staticData', value: false },
                            type: 'text',
                            label: 'Option Value Key',
                            value: 'value',
                            placeholder: 'value',
                        },
                        staticOptions: {
                            enabledOn: { key: 'staticData', value: true },
                            type: 'list',
                            label: 'Options',
                            labelText: 'Label',
                            valueText: 'Value',
                            defaultValue: '',
                            value: [
                                { label: 'Item 1', value: 'item1' },
                                { label: 'Item 2', value: 'item2' }
                            ]
                        },
                        isCompact: {
                            type: 'checkbox',
                            label: 'Compact',
                            value: false,
                        },
                        readonly: {
                            type: 'checkbox',
                            label: 'Prevent Editing',
                            value: false,
                        },
                        required: {
                            type: 'checkbox',
                            label: 'Required',
                            value: true,
                        },
                    },
                },
            ]
        },
        {
            id: 'fb-display',
            label: 'Display',
            items: [
                {
                    controlId: 'header',
                    label: 'Header',
                    icon: 'sap-icon--heading-1',
                    description: 'Text header',
                    template: getControlWrapper(`<h1 bk-title header-size="props.headerSize.value"><div ng-class="getPaddingClass(props.padding.value, props.side.value)">{{props.label.value || 'Header ' + props.headerSize.value}}</div></h1>`),
                    props: {
                        label: {
                            type: 'text',
                            label: 'Label',
                            value: 'Title',
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'headerVar',
                        },
                        headerSize: {
                            type: 'number',
                            label: 'Size',
                            value: 1,
                            max: 6,
                            min: 1,
                            step: 1,
                            required: true
                        },
                        level: {
                            type: 'number',
                            label: 'Level',
                            value: 1,
                            max: 6,
                            min: 1,
                            step: 1,
                            required: true
                        },
                        ...paddingProps
                    }
                },
                {
                    controlId: 'image',
                    label: 'Image',
                    icon: 'sap-icon--picture',
                    description: 'Image',
                    template: getControlWrapper(`<img class="bk-contain-image bk-border-box" ng-class="getPaddingClass(props.padding.value, props.side.value)" ng-attr-width="{{props.width.value || undefined}}" ng-attr-height="{{props.height.value || undefined}}" ng-src="{{props.imageLink.value}}" alt="{{props.desc.value}}"/>`),
                    props: {
                        imageLink: {
                            type: 'text',
                            label: 'Image Link',
                            value: '/services/web/resources/images/dirigible.svg',
                            placeholder: 'https://...',
                            required: true
                        },
                        desc: {
                            type: 'text',
                            label: 'Description',
                            value: 'Image description',
                            placeholder: '',
                            required: true
                        },
                        link: {
                            type: 'text',
                            label: 'Link To',
                            value: '',
                            placeholder: 'Link to open on click',
                        },
                        width: {
                            type: 'text',
                            label: 'Width',
                            value: '100%',
                            placeholder: '100% or 100px',
                        },
                        height: {
                            type: 'text',
                            label: 'Height',
                            value: '96px',
                            placeholder: '100% or 100px',
                        },
                        ...paddingProps
                    },
                },
                {
                    controlId: 'paragraph',
                    label: 'Paragraph',
                    icon: 'sap-icon--text-align-left',
                    description: 'Paragraph',
                    template: getControlWrapper(`<p class="fd-text" ng-class="[{'bk-pre-wrap': props.format.value, 'bk-text--subtitle': props.subtitle.value, 'bk-text--ellipsis': props.ellipsis.value, 'bk-font--italic': props.italic.value, 'bk-font--small': props.fontSize.value === 'sm', 'bk-font--large': props.fontSize.value === 'lg'}, getPaddingClass(props.padding.value, props.side.value)]">{{props.text.value}}</p>`),
                    props: {
                        format: {
                            type: 'checkbox',
                            label: 'Preserve Formatting',
                            value: true,
                        },
                        subtitle: {
                            type: 'checkbox',
                            label: 'Subtitle',
                            value: false,
                        },
                        ellipsis: {
                            type: 'checkbox',
                            label: 'Ellipsis',
                            value: false,
                        },
                        italic: {
                            type: 'checkbox',
                            label: 'Italic',
                            value: false,
                        },
                        fontSize: {
                            type: 'dropdown',
                            label: 'Font Size',
                            value: '',
                            items: [
                                {
                                    label: 'Default',
                                    value: '',
                                },
                                {
                                    label: 'Small',
                                    value: 'sm',
                                },
                                {
                                    label: 'Large',
                                    value: 'lg',
                                }
                            ]
                        },
                        text: {
                            type: 'textarea',
                            label: 'Text',
                            value: `Multiline\nParagraph\nText`,
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'textVar',
                        },
                        ...paddingProps
                    },
                },
                {
                    controlId: 'table',
                    label: 'Table',
                    icon: 'sap-icon--table-view',
                    description: 'Table container',
                    template: getControlWrapper(`<table bk-table class="{{getPaddingClass(props.padding.value, props.side.value)}}" fixed="props.isFixed.value" display-mode="{{props.displayMode.value}}" outer-borders="{{props.outerBorders.value}}" inner-borders="{{props.innerBorders.value}}">
                        <thead bk-table-header interactive="false">
                            <tr bk-table-row>
                                <th bk-table-header-cell ng-repeat="header in props.headers.value track by $index">{{header.label}}</th>
                            </tr>
                        </thead>
                        <tbody bk-table-body>
                            <tr bk-table-row hoverable="false" activable="false">
                                <td bk-table-cell ng-repeat="header in props.headers.value track by $index">row['{{header.value}}']</td>
                            </tr>
                        </tbody>
                    </table>`),
                    props: {
                        id: {
                            type: 'text',
                            label: 'Name',
                            placeholder: 'Form Item Name',
                            value: '',
                            required: true,
                        },
                        isFixed: {
                            type: 'checkbox',
                            label: 'Fixed',
                            value: false,
                        },
                        displayMode: {
                            type: 'dropdown',
                            label: 'Display Mode',
                            value: '',
                            items: [
                                {
                                    label: 'Default',
                                    value: '',
                                },
                                {
                                    label: 'Compact',
                                    value: 'compact',
                                },
                                {
                                    label: 'Condensed',
                                    value: 'condensed',
                                },
                            ]
                        },
                        ...paddingProps,
                        outerBorders: {
                            type: 'dropdown',
                            label: 'Outer Borders',
                            value: '',
                            items: [
                                {
                                    label: 'All',
                                    value: '',
                                },
                                {
                                    label: 'Horizontal',
                                    value: 'horizontal',
                                },
                                {
                                    label: 'Vertical',
                                    value: 'vertical',
                                },
                                {
                                    label: 'Top',
                                    value: 'top',
                                },
                                {
                                    label: 'Bottom',
                                    value: 'bottom',
                                },
                                {
                                    label: 'None',
                                    value: 'none',
                                },
                            ]
                        },
                        innerBorders: {
                            type: 'dropdown',
                            label: 'Inner Borders',
                            value: '',
                            items: [
                                {
                                    label: 'All',
                                    value: '',
                                },
                                {
                                    label: 'Horizontal',
                                    value: 'horizontal',
                                },
                                {
                                    label: 'Vertical',
                                    value: 'vertical',
                                },
                                {
                                    label: 'Top',
                                    value: 'top',
                                },
                                {
                                    label: 'None',
                                    value: 'none',
                                },
                            ]
                        },
                        headers: {
                            type: 'list',
                            label: 'Headers',
                            labelText: 'Label',
                            valueText: 'Key',
                            value: [
                                { label: 'Name', value: 'name' },
                                { label: 'Age', value: 'age' }
                            ]
                        },
                        model: {
                            type: 'text',
                            label: 'Model',
                            value: '',
                            placeholder: 'tableData',
                            required: true,
                        },
                        info: {
                            type: 'textinfo',
                            label: 'Example Model Data',
                            value: `[\n   {\n      "name":"John Doe",\n      "age":34\n   },\n   {\n      "name":"Jane Doe",\n      "age":35\n   }\n]`,
                        },
                    },
                },
                {
                    controlId: 'line',
                    label: 'Line',
                    icon: 'sap-icon--less',
                    description: 'Divider line',
                    template: getControlWrapper(`<div ng-class="{'bk-border--top': props.orientation.value === 'h', 'bk-border--left': props.orientation.value === 'v'}" ng-style="props.orientation.value === 'h' && {'height': '0.063rem'} || props.orientation.value === 'v' && {'width': '0.063rem'}"></div>`),
                    props: {
                        orientation: {
                            type: 'dropdown',
                            label: 'Orientation',
                            value: 'h',
                            items: [
                                {
                                    label: 'Horizontal',
                                    value: 'h',
                                },
                                {
                                    label: 'Vertical',
                                    value: 'v',
                                },
                            ]
                        }
                    }
                },
                {
                    controlId: 'link',
                    label: 'Link',
                    icon: 'sap-icon--chain-link',
                    iconRotate: true,
                    description: 'Hyperlink',
                    template: getControlWrapper(`<bk-link href="#">{{props.label.value ? props.label.value : 'Link'}}</bk-link>`),
                    props: {
                        label: {
                            type: 'text',
                            label: 'Label',
                            placeholder: 'Some Link',
                            value: '',
                            required: true,
                        },
                        href: {
                            type: 'text',
                            label: 'Link',
                            placeholder: 'https://...',
                            value: '',
                        },
                        variable: {
                            type: 'text',
                            label: 'Variable',
                            value: '',
                            placeholder: 'linkString',
                        },
                        newTab: {
                            type: 'checkbox',
                            label: 'Open in new tab',
                            value: true,
                        },
                    }
                },
                {
                    controlId: 'spacer',
                    label: 'Spacer',
                    icon: 'sap-icon--expand',
                    iconRotate: true,
                    description: 'Divider line',
                    template: getControlWrapper(`<div ng-class="{'bk-stretch': props.stretch.value}" ng-style="{ width: !props.stretch.value && props.width.value || undefined, height: !props.stretch.value && props.height.value || undefined }" data-type="spacer"></div>`),
                    props: {
                        stretch: {
                            type: 'checkbox',
                            label: 'Stretch',
                            value: false,
                        },
                        width: {
                            enabledOn: { key: 'stretch', value: false },
                            type: 'number',
                            label: 'Width',
                            value: 0,
                            max: 1000,
                            min: 0,
                            step: 1,
                        },
                        height: {
                            enabledOn: { key: 'stretch', value: false },
                            type: 'number',
                            label: 'Height',
                            value: 0,
                            max: 1000,
                            min: 0,
                            step: 1,
                        },
                    }
                },
            ]
        },
        {
            id: 'fb-containers',
            label: 'Containers',
            items: [
                {
                    controlId: 'container-vbox',
                    label: 'Vertical Box',
                    icon: 'sap-icon--screen-split-two',
                    iconRotate: true,
                    description: 'Vertical box container',
                    template: `<bk-form-item id="{{id}}" class="bk-vbox ${gapClasses} {{getPaddingClass(props.padding.value, props.side.value)}} ${justifyClasses}" data-type="container" ng-click="showContainerProps(id, $event)"></bk-form-item>`,
                    props: {
                        ...gapProps,
                        ...justifyProps,
                        ...paddingProps
                    },
                    children: []
                },
                {
                    controlId: 'container-hbox',
                    label: 'Horizontal Box',
                    icon: 'sap-icon--screen-split-two',
                    description: 'Horizontal box container',
                    template: `<bk-form-item id="{{id}}" class="bk-hbox ${gapClasses} {{getPaddingClass(props.padding.value, props.side.value)}} ${justifyClasses}" data-type="container" ng-click="showContainerProps(id, $event)"></bk-form-item>`,
                    props: {
                        ...gapProps,
                        ...justifyProps,
                        ...paddingProps
                    },
                    children: []
                },
            ]
        }
    ];

    function getGroupId(controlId) {
        for (let groupIndex = 0; groupIndex < $scope.builderComponents.length; groupIndex++) {
            for (let c = 0; c < $scope.builderComponents[groupIndex].items.length; c++) {
                if ($scope.builderComponents[groupIndex].items[c].controlId === controlId) return $scope.builderComponents[groupIndex].id;
            }
        }
    }

    let deleteControlData = {
        isContainer: false,
        controlId: '',
    };

    $scope.clearForm = () => {
        dialogHub.showDialog({
            title: `Clear form?`,
            message: 'Deletes everything inside the form. This action cannot be undone.',
            buttons: [{
                id: 'cl',
                state: ButtonStates.Negative,
                label: 'Clear',
            }, {
                id: 'c',
                state: ButtonStates.Transparent,
                label: 'Cancel',
            }]
        }).then((buttonId) => {
            if (buttonId === 'cl') {
                formContainer.textContent = '';
                $scope.$evalAsync(() => {
                    removeSelection();
                    $scope.formModel.length = 0;
                    $scope.fileChanged();
                });
            }
        });

    };

    $scope.showContextMenu = (event) => {
        const items = [];
        const type = event.target.getAttribute('data-type');
        if (type && type === 'container-main') {
            items.push({
                id: 'clearForm',
                label: 'Clear form',
            });
        } else {
            deleteControlData.isContainer = (type && type === 'container');
            if (deleteControlData.isContainer) deleteControlData.controlId = event.target.id;
            else deleteControlData.controlId = event.target.getAttribute('data-id')
            items.push({
                id: 'deleteControl',
                label: 'Delete',
            });
        }
        if (!items.length) return;
        event.preventDefault();
        contextMenuHub.showContextMenu({
            ariaLabel: 'form editor contextmenu',
            posX: event.clientX,
            posY: event.clientY,
            icons: false,
            items: items
        }).then((id) => {
            $scope.$evalAsync(() => {
                if (id === 'clearForm') {
                    $scope.clearForm();
                } else if (id === 'deleteControl') {
                    if (deleteControlData.isContainer) $scope.deleteControl(deleteControlData.controlId, true);
                    else $scope.deleteControl(deleteControlData.controlId);
                }
            });
        }, (error) => {
            console.error(error);
            statusBarHub.showError('Unable to process context menu data');
        })
    };

    const insertInModel = (model, control, containerId, index) => {
        for (let i = 0; i < model.length; i++) {
            if (model[i].controlId.startsWith('container')) {
                if (model[i].$scope.id === containerId) {
                    if (index) model[i].children.splice(index, 0, control);
                    else model[i].children.push(control);
                    return true;
                } else {
                    if (insertInModel(model[i].children, control, containerId, index)) break;
                }
            }
        }
    };

    const moveInModel = (model, control, containerId, insertIndex) => {
        if (containerId === 'formContainer') {
            model.splice(insertIndex, 0, control);
            return true;
        }
        for (let i = 0; i < model.length; i++) {
            if (model[i].controlId.startsWith('container')) {
                if (model[i].$scope.id === containerId) {
                    model[i].children.splice(insertIndex, 0, control);
                    return true;
                } else {
                    if (moveInModel(model[i].children, control, containerId, insertIndex)) break;
                }
            }
        }
    };

    const popFromModel = (model, controlId) => {
        if (controlId) {
            for (let i = 0; i < model.length; i++) {
                if (model[i].$scope.id === controlId) {
                    return model.splice(i, 1)[0];
                } else if (model[i].controlId.startsWith('container')) {
                    const control = popFromModel(model[i].children, controlId);
                    if (control) return control;
                }
            }
        }
        return;
    };

    function addFormItem(event) {
        const parentIndex = event.item.getAttribute('data-pindex');
        const controlIndex = event.item.getAttribute('data-cindex');
        if (event.from.getAttribute('data-type') === 'componentPanel') {
            const control = JSON.parse(JSON.stringify($scope.builderComponents[parentIndex].items[controlIndex]));
            control.groupId = $scope.builderComponents[parentIndex].id;
            control.$scope = $scope.$new(true);
            control.$scope.props = control.props;
            if (event.to.id !== 'formContainer') {
                insertInModel($scope.formModel, control, event.to.id, event.newIndex);
            } else {
                $scope.formModel.splice(event.newIndex, 0, control);
            }
            let isContainer = false;
            if (control.controlId.startsWith('container')) {
                isContainer = true;
                control.$scope.id = `c${uuid.generate()}`;
                control.$scope.showContainerProps = $scope.showContainerProps;
            } else {
                control.$scope.id = `w${uuid.generate()}`;
                if (control.$scope.props.id)
                    control.$scope.props.id.value = `i${uuid.generate()}`;
                control.$scope.showProps = $scope.showProps;
            }
            if (control.$scope.props.hasOwnProperty('padding')) {
                control.$scope.getPaddingClass = $scope.getPaddingClass;
            }
            const element = $compile(control.template)(control.$scope)[0];
            $(event.item).replaceWith(element);
            if (isContainer) createSublist(element, control.$scope.id);
            requestAnimationFrame(() => {
                $scope.$evalAsync(() => {
                    element.click();
                });
            });
        } else {
            const control = popFromModel($scope.formModel, event.item.getAttribute('data-id'));
            if (control) {
                moveInModel($scope.formModel, control, event.to.id, event.newIndex);
            } else {
                dialogHub.showAlert({
                    title: 'Move error',
                    message: 'There was an error while attempting to move the control in the data model. Control was not found.',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }
        }
        $scope.$evalAsync(() => $scope.fileChanged())
    }

    function createSublist(element, groupId) {
        Sortable.create(element, {
            group: {
                name: groupId,
                put: true
            },
            animation: 200,
            onAdd: addFormItem,
            onUpdate: addFormItem,
        });
    }

    function removeSelection() {
        if ($scope.selectedContainer) {
            $scope.selectedContainer.classList.remove('is-selected');
            $scope.selectedContainer = undefined;
        }
        if ($scope.selectedCtrl) {
            $scope.selectedCtrl.classList.remove('is-selected');
            $scope.selectedCtrl = undefined;
        }
        if ($scope.formSelected) {
            $scope.formSelected = false;
            formContainer.classList.remove('is-selected');
        }
        $scope.availableProps = undefined;
    }

    $scope.clearSelection = (event) => {
        if ($scope.state.canSave && event.target === event.currentTarget) {
            removeSelection();
        }
    };

    $scope.initControlGroup = (gid) => {
        Sortable.create($document[0].getElementById(gid), {
            sort: false,
            group: {
                name: gid,
                pull: 'clone',
                put: false,
            },
            animation: 200
        });
    };

    $scope.togglePreview = () => {
        removeSelection();
        $scope.state.preview = !$scope.state.preview;
    };

    $scope.showFormProps = (event) => {
        if (event.target === formContainer) {
            removeSelection();
            $scope.availableProps = $scope.formProps;
            $scope.formSelected = true;
            formContainer.classList.add('is-selected');
        }
    };

    $scope.showContainerProps = (controlId, event) => {
        function getProps(model) {
            for (let i = 0; i < model.length; i++) {
                if (model[i].controlId.startsWith('container')) {
                    if (model[i].$scope.id === $scope.selectedContainer.getAttribute('id')) {
                        $scope.availableProps = model[i].$scope.props;
                        return true;
                    } else {
                        if (getProps(model[i].children)) return true;
                    }
                }
            }
        }
        if ($scope.state.canSave) {
            if (event.target.id && event.target.id === controlId) {
                event.stopPropagation();
                removeSelection();
                $scope.selectedContainer = event.target;
                event.target.classList.add('is-selected');
                getProps($scope.formModel);
            } else $scope.selectedContainer = undefined;
        }
    };

    $scope.showProps = (event) => {
        function getProps(model) {
            if ($scope.selectedCtrl) {
                for (let i = 0; i < model.length; i++) {
                    if (model[i].controlId.startsWith('container')) {
                        if (getProps(model[i].children)) return true;
                    } else if (model[i].$scope.id === $scope.selectedCtrl.getAttribute('data-id')) {
                        $scope.availableProps = model[i].$scope.props;
                        return true;
                    }
                }
            } return false;
        }
        if ($scope.state.canSave) {
            removeSelection();
            event.target.classList.add('is-selected');
            $scope.selectedCtrl = event.target;
            getProps($scope.formModel);
        }
    };

    $scope.isPropEnabled = (enabledOn) => {
        if (enabledOn) return $scope.availableProps[enabledOn.key].value === enabledOn.value;
        return true;
    };

    $scope.setListDefault = (item, selectedIndex) => {
        for (let i = 0; i < item.value.length; i++) {
            item.value[i].isDefault = undefined;
        }
        item.value[selectedIndex].isDefault = true;
        item.defaultValue = item.value[selectedIndex].value;
        $scope.fileChanged();
    };

    $scope.addListItem = (item) => {
        dialogHub.showFormDialog({
            title: 'Add item',
            form: {
                'aliLabel': {
                    label: item.labelText,
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    focus: true,
                    required: true
                },
                'aliValue': {
                    label: item.valueText,
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    inputRules: {
                        patterns: ['^\\S*$'],
                    },
                    required: true
                },
            },
            submitLabel: 'Add',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    if (item.value) item.value.push({
                        label: form['aliLabel'],
                        value: form['aliValue']
                    });
                    else {
                        item.defaultValue = '';
                        item.value = [{
                            label: form['aliLabel'],
                            value: form['aliValue']
                        }];
                    }
                    $scope.fileChanged();
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New item error',
                message: 'There was an error while adding the new item.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.editListItem = (listItem, labelText, valueText) => {
        dialogHub.showFormDialog({
            title: 'Edit item',
            form: {
                'aliLabel': {
                    label: labelText,
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    value: listItem.label,
                    focus: true,
                    required: true
                },
                'aliValue': {
                    label: valueText,
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    inputRules: {
                        patterns: ['^\\S*$'],
                    },
                    value: listItem.value,
                    required: true
                },
            },
            submitLabel: 'Save',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    listItem.label = form['aliLabel'];
                    listItem.value = form['aliValue'];
                    $scope.fileChanged();
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New item error',
                message: 'There was an error while adding the new item.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.deleteListItem = (options, index) => {
        options.splice(index, 1);
        $scope.fileChanged();
    };

    $scope.addFeed = () => {
        dialogHub.showFormDialog({
            title: 'Add feed',
            form: {
                'afName': {
                    label: 'Name',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    focus: true,
                    required: true
                },
                'afUrl': {
                    label: 'URL',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    required: true
                },
                'afFunc': {
                    label: 'Function Name',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    required: false
                },
            },
            submitLabel: 'Add',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.formData.feeds.push({
                        name: form['afName'],
                        url: form['afUrl'],
                        func: form['afFunc']
                    });
                    $scope.fileChanged();
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New feed error',
                message: 'There was an error while adding the new feed.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.editFeed = (feed) => {
        dialogHub.showFormDialog({
            title: 'Edit feed',
            form: {
                'afName': {
                    label: 'Name',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    value: feed.name,
                    focus: true,
                    required: true
                },
                'afUrl': {
                    label: 'URL',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    value: feed.url,
                    required: true
                },
                'afFunc': {
                    label: 'Function Name',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    value: feed.func,
                    required: false
                },
            },
            submitLabel: 'Save',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    feed.name = form['afName'];
                    feed.url = form['afUrl'];
                    feed.func = form['afFunc'];
                    $scope.fileChanged();
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New feed error',
                message: 'There was an error while adding the new feed.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.deleteFeed = (index) => {
        $scope.formData.feeds.splice(index, 1);
        $scope.fileChanged();
    };

    $scope.addScript = () => {
        dialogHub.showFormDialog({
            title: 'Add script',
            form: {
                'asName': {
                    label: 'Name',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    focus: true,
                    required: true
                },
                'asUrl': {
                    label: 'URL',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    required: true
                },
            },
            submitLabel: 'Add',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    $scope.formData.scripts.push({
                        name: form['asName'],
                        url: form['asUrl']
                    });
                    $scope.fileChanged();
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New script error',
                message: 'There was an error while adding the new script.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.editScript = (script) => {
        dialogHub.showFormDialog({
            title: 'Edit script',
            form: {
                'asName': {
                    label: 'Name',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    value: script.name,
                    focus: true,
                    required: true
                },
                'asUrl': {
                    label: 'URL',
                    controlType: 'input',
                    placeholder: '',
                    type: 'text',
                    minlength: 1,
                    value: script.url,
                    required: true
                },
            },
            submitLabel: 'Save',
            cancelLabel: 'Cancel'
        }).then((form) => {
            if (form) {
                $scope.$evalAsync(() => {
                    script.name = form['asName'];
                    script.url = form['asUrl'];
                    $scope.fileChanged();
                });
            }
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'New script error',
                message: 'There was an error while adding the new script.',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.deleteScript = (index) => {
        $scope.formData.scripts.splice(index, 1);
        $scope.fileChanged();
    };

    $scope.isFormValid = () => {
        if ($scope.forms.formProperties) {
            if ($scope.forms.formProperties.$valid === true || $scope.forms.formProperties.$valid === undefined) {
                $scope.state.canSave = true;
                return true;
            } else {
                $scope.state.canSave = false;
                return false;
            }
        }
        $scope.state.canSave = true;
        return true;
    };

    $scope.switchTab = (tabId) => {
        removeSelection();
        $scope.selectedTab = tabId;
    };

    $scope.feedHelp = () => {
        dialogHub.showWindow({
            id: 'formBuilderHelp'
        });
    };

    const createDomFromJson = (model, containerId) => {
        for (let i = 0; i < model.length; i++) {
            let control;
            let groupIndex = 0;
            if (model[i].groupId === undefined) model[i].groupId = getGroupId(model[i].controlId);
            if (model[i].groupId === 'fb-display') groupIndex = 1;
            else if (model[i].groupId === 'fb-containers') groupIndex = 2;
            for (let c = 0; c < $scope.builderComponents[groupIndex].items.length; c++) {
                if ($scope.builderComponents[groupIndex].items[c].controlId === model[i].controlId) {
                    control = JSON.parse(JSON.stringify($scope.builderComponents[groupIndex].items[c]));
                    control.$scope = $scope.$new(true);
                    if (groupIndex === 2) {
                        control.$scope.id = `c${uuid.generate()}`;
                        control.$scope.showContainerProps = $scope.showContainerProps;
                        control.$scope.props = control.props;
                        for (const key in control.$scope.props) {
                            if (model[i][key] !== undefined) {
                                control.$scope.props[key].value = model[i][key];
                            }
                        }
                        break;
                    } else {
                        control.$scope.id = `w${uuid.generate()}`;
                        control.$scope.showProps = $scope.showProps;
                        control.$scope.props = control.props;
                        if (control.$scope.props.hasOwnProperty('padding')) {
                            control.$scope.getPaddingClass = $scope.getPaddingClass;
                        }
                        if (control.controlId === 'input-radio' && model[i].staticData === undefined) { // For backwards compatibility
                            delete Object.assign(model[i], { ['staticOptions']: model[i]['options'] })['options'];
                            $scope.fileChanged();
                        }
                        for (const key in control.$scope.props) {
                            if (model[i][key] !== undefined) {
                                control.$scope.props[key].value = model[i][key];
                                if (control.$scope.props[key].type === 'list') {
                                    control.$scope.props[key].defaultValue = '';
                                    for (let l = 0; l < model[i][key].length; l++) {
                                        if (model[i][key][l].isDefault) {
                                            control.$scope.props[key].defaultValue = model[i][key][l].value;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
            $timeout(() => {
                if (groupIndex === 2) {
                    insertInModel($scope.formModel, control, control.$scope.id);
                    const element = angular.element($document[0].querySelector(`#${control.$scope.id}`));
                    element.append($compile(control.template)(control.$scope)[0]);
                    element.ready(() => {
                        createSublist(document.querySelector(`#${control.$scope.id}`), control.$scope.id);
                    });
                }
                if (containerId !== undefined) {
                    insertInModel($scope.formModel, control, containerId);
                    const element = angular.element($document[0].querySelector(`#${containerId}`));
                    element.append($compile(control.template)(control.$scope)[0]);
                    element.ready(() => {
                        createSublist(document.querySelector(`#${containerId}`), containerId);
                    });
                } else {
                    $scope.formModel.push(control);
                    formContainer.append($compile(control.template)(control.$scope)[0]);
                }
                if (groupIndex === 2 && model[i].children.length > 0) {
                    createDomFromJson(model[i].children, control.$scope.id);
                }
            }, 0);
        }
    };

    const initFormProps = (metadata) => {
        for (const [key, value] of Object.entries(metadata)) {
            if ($scope.formProps.hasOwnProperty(key)) {
                $scope.formProps[key].value = value;
            }
        }
    };

    const getMetadataFromForm = () => {
        let metadata = {};
        for (const [key, value] of Object.entries($scope.formProps)) {
            if (value.value !== null && value.value !== undefined && value.value !== '') {
                metadata[key] = value.value;
            }
        }
        if (!metadata['url']) {
            delete metadata['method'];
        }
        return metadata;
    };

    // Same migration happens in generateUtils.js
    function migrateForm(formData) {
        for (let i = 0; i < formData.length; i++) {
            if (formData[i].hasOwnProperty('title')) {
                delete Object.assign(formData[i], { 'label': formData[i]['title'] })['title'];
                $scope.fileChanged();
            }
            if (formData[i].hasOwnProperty('name')) {
                delete Object.assign(formData[i], { 'label': formData[i]['name'] })['name'];
                $scope.fileChanged();
            }
            if (formData[i].hasOwnProperty('errorState')) {
                delete Object.assign(formData[i], { 'errorMessage': formData[i]['errorState'] })['errorState'];
                $scope.fileChanged();
            }
            if (formData[i].hasOwnProperty('size')) {
                delete Object.assign(formData[i], { 'headerSize': formData[i]['size'] })['size'];
                $scope.fileChanged();
            }
            if (formData[i].controlId === 'button' && formData[i].hasOwnProperty('sizeToText')) {
                delete formData[i]['sizeToText'];
                $scope.fileChanged();
            } else if ((formData[i].controlId === 'container-hbox' || formData[i].controlId === 'container-vbox') && !formData[i].hasOwnProperty('justify')) {
                formData[i]['justify'] = 'start';
                $scope.fileChanged();
            } else if (formData[i].controlId === 'header' && !formData[i].hasOwnProperty('level')) {
                formData[i]['level'] = 1;
            }
        }
        return formData;
    }

    const loadFileContents = () => {
        if (!$scope.state.error) {
            $scope.state.isBusy = true;
            WorkspaceService.loadContent(dataParameters.filePath).then((response) => {
                $scope.$evalAsync(() => {
                    if (response.data.hasOwnProperty('metadata')) {
                        initFormProps(response.data.metadata);
                    }
                    if (response.data.hasOwnProperty('feeds')) {
                        $scope.formData.feeds = response.data.feeds;
                    }
                    if (response.data.hasOwnProperty('code')) {
                        $scope.formData.code = response.data.code;
                    }
                    if (response.data.hasOwnProperty('scripts')) {
                        $scope.formData.scripts = response.data.scripts;
                    }
                    if (response.data.hasOwnProperty('form')) {
                        createDomFromJson(migrateForm(response.data.form));
                    }
                    $scope.state.isBusy = false;
                    $scope.state.initialized = true;
                    formContainer.click();
                });
            }, (response) => {
                console.error(response);
                $scope.$evalAsync(() => {
                    $scope.state.error = true;
                    $scope.errorMessage = 'Error while loading file. Please look at the console for more information.';
                    $scope.state.isBusy = false;
                });
            });
        }
    };

    const createFormJson = (model) => {
        const formJson = [];
        for (let i = 0; i < model.length; i++) {
            if (model[i].groupId === undefined) model[i].groupId = getGroupId(model[i].controlId);
            let controlObj;
            if (model[i].groupId === 'fb-containers') {
                controlObj = {
                    controlId: model[i].controlId,
                    groupId: model[i].groupId,
                    children: createFormJson(model[i].children)
                }
            } else {
                controlObj = {
                    controlId: model[i].controlId,
                    groupId: model[i].groupId
                };
            }
            for (const key in model[i].$scope.props) {
                if (model[i].$scope.props[key].type !== 'textinfo') {
                    if (model[i].$scope.props[key].enabledOn) {
                        if (model[i].$scope.props[model[i].$scope.props[key].enabledOn.key].value === model[i].$scope.props[key].enabledOn.value) {
                            if (model[i].$scope.props[key].value !== '' && model[i].$scope.props[key].value !== undefined && model[i].$scope.props[key].value !== null) {
                                //@ts-ignore
                                controlObj[key] = model[i].$scope.props[key].value;
                            }
                        }
                    } else {
                        if (model[i].$scope.props[key].value !== '' && model[i].$scope.props[key].value !== undefined && model[i].$scope.props[key].value !== null) {
                            //@ts-ignore
                            controlObj[key] = model[i].$scope.props[key].value;
                        }
                    }
                }
            }
            formJson.push(controlObj);
        }
        return formJson;
    };

    function saveContents() {
        const formFile = {
            metadata: getMetadataFromForm(),
            feeds: $scope.formData.feeds,
            scripts: $scope.formData.scripts,
            code: $scope.formData.code,
            form: createFormJson($scope.formModel)
        };
        WorkspaceService.saveContent(dataParameters.filePath, JSON.stringify(formFile, null, 4)).then(() => {
            layoutHub.setEditorDirty({
                path: dataParameters.filePath,
                dirty: false,
            });
            workspaceHub.announceFileSaved({
                path: dataParameters.filePath,
                contentType: dataParameters.contentType,
            });
            $scope.$evalAsync(() => {
                $scope.changed = false;
                $scope.state.isBusy = false;
            });
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = `Error saving '${dataParameters.filePath}'. Please look at the console for more information.`;
                $scope.state.isBusy = false;
            });
        });
    }

    $scope.shortcuts = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
        if (keySet === 'ctrl+s') {
            if ($scope.changed && $scope.state.canSave && !$scope.state.error) {
                $scope.state.busyText = 'Saving...';
                $scope.state.isBusy = true;
                saveContents();
            }
        } else if (keySet === 'delete') {
            if ($scope.selectedTab === 'designer' && event.target.tagName !== 'INPUT' && event.target.tagName !== 'TEXTAREA') {
                if ($scope.selectedCtrl) {
                    $scope.$evalAsync(() => $scope.deleteControl($scope.selectedCtrl.getAttribute('data-id')));
                } else if ($scope.selectedContainer) {
                    $scope.$evalAsync(() => $scope.deleteControl($scope.selectedContainer.id, true));
                }
            }
        }
    };

    $scope.fileChanged = () => {
        if (!$scope.changed) {
            $scope.changed = true;
            layoutHub.setEditorDirty({
                path: dataParameters.filePath,
                dirty: $scope.changed,
            });
        }
    };

    $scope.deleteControlFromModel = (id, model) => {
        for (let i = 0; i < model.length; i++) {
            if (model[i].controlId.startsWith('container') && model[i].$scope.id !== id) {
                if ($scope.deleteControlFromModel(id, model[i].children)) return true;
            } else if (model[i].$scope.id === id) {
                model.splice(i, 1);
                return true;
            }
        }
        return false;
    };

    $scope.deleteControl = (id, isContainer = false) => {
        if (!id) {
            dialogHub.showAlert({
                title: 'Delete error',
                message: 'Received an empty ID',
                type: AlertTypes.Error,
                preformatted: false,
            });
        } else if (!$scope.deleteControlFromModel(id, $scope.formModel)) {
            console.error(`Could not delete control with internal ID '${id}'`);
            dialogHub.showAlert({
                title: 'Delete error',
                message: `Could not delete control from model with internal ID '${id}'`,
                type: AlertTypes.Error,
                preformatted: false,
            });
        } else {
            $scope.fileChanged();
            removeSelection();
            let control;
            if (isContainer) control = $document[0].querySelector(`#${id}`);
            else control = $document[0].querySelector(`[data-id=${id}]`);
            if (control) {
                angular.element(control).remove();
            } else {
                dialogHub.showAlert({
                    title: 'Delete error',
                    message: `Could not delete control from UI with internal ID '${id}'`,
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            }
        }
    };

    const checkGenFile = () => {
        WorkspaceService.resourceExists(genFile).then(() => {
            $scope.$evalAsync(() => {
                $scope.canRegenerate = true;
            });
        }, () => {
            $scope.$evalAsync(() => {
                $scope.canRegenerate = false;
            });
        });
    };

    const chooseTemplate = (project, filePath, params) => {
        const templateItems = [];
        TemplatesService.listTemplates().then((response) => {
            for (let i = 0; i < response.data.length; i++) {
                if (response.data[i].hasOwnProperty('extension') && response.data[i].extension === 'form') {
                    templateItems.push({
                        label: response.data[i].name,
                        value: response.data[i].id,
                    });
                }
            }
            dialogHub.closeBusyDialog();
            dialogHub.showFormDialog({
                title: 'Choose template',
                form: {
                    'pgfd1': {
                        label: 'Choose template',
                        controlType: 'dropdown',
                        options: templateItems,
                        value: '',
                        required: true,
                    },
                },
                submitLabel: 'Add',
                cancelLabel: 'Cancel'
            }).then((form) => {
                if (form) {
                    dialogHub.showBusyDialog('Regenerating from model');
                    $scope.$evalAsync(() => {
                        generateFromModel(project, filePath, msg.data.formData[0].value, params);
                    });
                }
            }, (error) => {
                console.error(error);
                dialogHub.showAlert({
                    title: 'Template list error',
                    message: 'Please look at the console for more information',
                    type: AlertTypes.Error,
                    preformatted: false,
                });
            });
        }, (error) => {
            console.error(error);
            dialogHub.closeBusyDialog();
            dialogHub.showAlert({
                title: 'Unable to load template list',
                message: 'Please look at the console for more information',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    const generateFromModel = (project, filePath, templateId, params) => {
        GenerateService.generateFromModel(
            workspace,
            project,
            filePath,
            templateId,
            params
        ).then(() => {
            dialogHub.closeBusyDialog();
            statusBarHub.showMessage(`Generated from model '${filePath}'`);
            dialogHub.postMessage({ topic: 'projects.tree.refresh', data: { partial: true, project: project, workspace: workspace } });
        }, (error) => {
            console.error(error);
            dialogHub.showAlert({
                title: 'Failed to generate',
                message: 'Please look at the console for more information',
                type: AlertTypes.Error,
                preformatted: false,
            });
        });
    };

    $scope.regenerate = () => {
        $scope.shortcuts();
        dialogHub.showBusyDialog('Loading data');
        WorkspaceService.loadContent(genFile).then((response) => {
            let { models, perspectives, templateId, filePath, workspaceName, projectName, ...params } = response.data;
            if (!response.data.templateId) {
                chooseTemplate(response.data.projectName, response.data.filePath, params);
            } else {
                dialogHub.showBusyDialog('Regenerating');
                generateFromModel(response.data.projectName, response.data.filePath, response.data.templateId, params);
            }
        }, (error) => {
            console.error(error);
            dialogHub.closeBusyDialog();
            dialogHub.showAlert({
                title: 'Unable to load gen file',
                message: 'There was an error while loading the gen file.\nPlease look at the console for more information.',
                type: AlertTypes.Error,
                preformatted: true,
            });
        });
    };

    layoutHub.onFocusEditor((data) => {
        if (data.path && data.path === dataParameters.filePath) statusBarHub.showLabel('');
    });

    layoutHub.onReloadEditorParams((data) => {
        if (data.path === dataParameters.filePath) {
            $scope.$evalAsync(() => {
                dataParameters = ViewParameters.get();
                genFile = dataParameters.filePath.substring(0, dataParameters.filePath.lastIndexOf('.')) + '.gen';
                workspace = dataParameters.filePath.substring(dataParameters.filePath.indexOf('/', 1), 1);
                // loadFileContents(); // TODO: Make dynamic data reload possible
            });
        };
    });

    workspaceHub.onSaveAll(() => {
        if ($scope.changed && !$scope.state.error && $scope.state.canSave) {
            $scope.shortcuts();
        }
    });

    workspaceHub.onSaveFile((data) => {
        if (data.path && data.path === dataParameters.filePath) {
            if ($scope.changed && !$scope.state.error && $scope.state.canSave) {
                $scope.shortcuts();
            }
        }
    });

    let dataParameters = ViewParameters.get();
    if (!dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
    } else {
        genFile = dataParameters.filePath.substring(0, dataParameters.filePath.lastIndexOf('.')) + '.gen';
        workspace = dataParameters.filePath.substring(dataParameters.filePath.indexOf('/', 1), 1);
        angular.element($document[0]).ready(() => {
            formContainer = $document[0].getElementById('formContainer');
            $scope.$evalAsync(() => {
                loadFileContents();
                checkGenFile();
            });
            Sortable.create(formContainer, {
                group: {
                    name: 'formContainer',
                    put: true
                },
                animation: 200,
                onAdd: addFormItem,
                onUpdate: addFormItem,
            });
        });
    }
});