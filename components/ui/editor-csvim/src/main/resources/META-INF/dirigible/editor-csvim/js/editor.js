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
const editorView = angular.module('csvim-editor', ['blimpKit', 'platformView', 'platformShortcuts', 'platformSplit', 'WorkspaceService']);
editorView.directive('uniqueField', ($parse) => ({
    require: 'ngModel',
    scope: false,
    link: (scope, _elem, attrs, controller) => {
        let parseFn = $parse(attrs.uniqueField);
        scope.uniqueField = parseFn(scope);
        controller.$validators.forbiddenName = value => {
            let unique = true;
            let correct = RegExp(scope.uniqueField.regex, 'g').test(value);
            if (correct) {
                if ('index' in attrs) {
                    unique = scope.uniqueField.checkUniqueColumn(attrs.index, value);
                } else if ('kindex' in attrs && 'vindex' in attrs) {
                    unique = scope.uniqueField.checkUniqueValue(attrs.kindex, attrs.vindex, value);
                }
            }
            return unique;
        };
    }
}));
editorView.controller('CsvimController', ($scope, $window, WorkspaceService, ViewParameters, ButtonStates) => {
    const statusBarHub = new StatusBarHub();
    const workspaceHub = new WorkspaceHub();
    const layoutHub = new LayoutHub();
    const dialogHub = new DialogHub();
    $scope.changed = false;
    let workspace = WorkspaceService.getCurrentWorkspace();
    $scope.errorMessage = 'An unknown error was encountered. Please see console for more information.';
    $scope.forms = {
        editor: {},
    };
    $scope.state = {
        isBusy: true,
        error: false,
        busyText: 'Loading...',
    };
    $scope.searchVisible = false;
    $scope.searchField = { text: '' };
    $scope.locales = [{ value: 'af-NA', text: 'Afrikaans (Namibia)', secondaryText: 'af-NA' }, { value: 'af-ZA', text: 'Afrikaans (South Africa)', secondaryText: 'af-ZA' }, { value: 'agq-CM', text: 'Aghem (Cameroon)', secondaryText: 'agq-CM' }, { value: 'ak-GH', text: 'Akan (Ghana)', secondaryText: 'ak-GH' }, { value: 'sq-AL', text: 'Albanian (Albania)', secondaryText: 'sq-AL' }, { value: 'sq-XK', text: 'Albanian (Kosovo)', secondaryText: 'sq-XK' }, { value: 'sq-MK', text: 'Albanian (North Macedonia)', secondaryText: 'sq-MK' }, { value: 'am-ET', text: 'Amharic (Ethiopia)', secondaryText: 'am-ET' }, { value: 'ar-DZ', text: 'Arabic (Algeria)', secondaryText: 'ar-DZ' }, { value: 'ar-BH', text: 'Arabic (Bahrain)', secondaryText: 'ar-BH' }, { value: 'ar-TD', text: 'Arabic (Chad)', secondaryText: 'ar-TD' }, { value: 'ar-KM', text: 'Arabic (Comoros)', secondaryText: 'ar-KM' }, { value: 'ar-DJ', text: 'Arabic (Djibouti)', secondaryText: 'ar-DJ' }, { value: 'ar-EG', text: 'Arabic (Egypt)', secondaryText: 'ar-EG' }, { value: 'ar-ER', text: 'Arabic (Eritrea)', secondaryText: 'ar-ER' }, { value: 'ar-IQ', text: 'Arabic (Iraq)', secondaryText: 'ar-IQ' }, { value: 'ar-IL', text: 'Arabic (Israel)', secondaryText: 'ar-IL' }, { value: 'ar-JO', text: 'Arabic (Jordan)', secondaryText: 'ar-JO' }, { value: 'ar-KW', text: 'Arabic (Kuwait)', secondaryText: 'ar-KW' }, { value: 'ar-LB', text: 'Arabic (Lebanon)', secondaryText: 'ar-LB' }, { value: 'ar-LY', text: 'Arabic (Libya)', secondaryText: 'ar-LY' }, { value: 'ar-MR', text: 'Arabic (Mauritania)', secondaryText: 'ar-MR' }, { value: 'ar-MA', text: 'Arabic (Morocco)', secondaryText: 'ar-MA' }, { value: 'ar-OM', text: 'Arabic (Oman)', secondaryText: 'ar-OM' }, { value: 'ar-PS', text: 'Arabic (Palestinian Territories)', secondaryText: 'ar-PS' }, { value: 'ar-QA', text: 'Arabic (Qatar)', secondaryText: 'ar-QA' }, { value: 'ar-SA', text: 'Arabic (Saudi Arabia)', secondaryText: 'ar-SA' }, { value: 'ar-SO', text: 'Arabic (Somalia)', secondaryText: 'ar-SO' }, { value: 'ar-SS', text: 'Arabic (South Sudan)', secondaryText: 'ar-SS' }, { value: 'ar-SD', text: 'Arabic (Sudan)', secondaryText: 'ar-SD' }, { value: 'ar-SY', text: 'Arabic (Syria)', secondaryText: 'ar-SY' }, { value: 'ar-TN', text: 'Arabic (Tunisia)', secondaryText: 'ar-TN' }, { value: 'ar-AE', text: 'Arabic (United Arab Emirates)', secondaryText: 'ar-AE' }, { value: 'ar-EH', text: 'Arabic (Western Sahara)', secondaryText: 'ar-EH' }, { value: 'ar-001', text: 'Arabic (world)', secondaryText: 'ar-001' }, { value: 'ar-YE', text: 'Arabic (Yemen)', secondaryText: 'ar-YE' }, { value: 'hy-AM', text: 'Armenian (Armenia)', secondaryText: 'hy-AM' }, { value: 'as-IN', text: 'Assamese (India)', secondaryText: 'as-IN' }, { value: 'ast-ES', text: 'Asturian (Spain)', secondaryText: 'ast-ES' }, { value: 'asa-TZ', text: 'Asu (Tanzania)', secondaryText: 'asa-TZ' }, { value: 'az-AZ', text: 'Azerbaijani (Azerbaijan)', secondaryText: 'az-AZ' }, { value: 'az-Cyrl', text: 'Azerbaijani (Cyrillic)', secondaryText: 'az-Cyrl' }, { value: 'az-Latn', text: 'Azerbaijani (Latin)', secondaryText: 'az-Latn' }, { value: 'ksf-CM', text: 'Bafia (Cameroon)', secondaryText: 'ksf-CM' }, { value: 'bm-ML', text: 'Bambara (Mali)', secondaryText: 'bm-ML' }, { value: 'bn-BD', text: 'Bangla (Bangladesh)', secondaryText: 'bn-BD' }, { value: 'bn-IN', text: 'Bangla (India)', secondaryText: 'bn-IN' }, { value: 'bas-CM', text: 'Basaa (Cameroon)', secondaryText: 'bas-CM' }, { value: 'eu-ES', text: 'Basque (Spain)', secondaryText: 'eu-ES' }, { value: 'be-BY', text: 'Belarusian (Belarus)', secondaryText: 'be-BY' }, { value: 'be-TARASK', text: 'Belarusian (Taraskievica orthography)', secondaryText: 'be-TARASK' }, { value: 'bem-ZM', text: 'Bemba (Zambia)', secondaryText: 'bem-ZM' }, { value: 'bez-TZ', text: 'Bena (Tanzania)', secondaryText: 'bez-TZ' }, { value: 'bho-IN', text: 'Bhojpuri (India)', secondaryText: 'bho-IN' }, { value: 'brx-IN', text: 'Bodo (India)', secondaryText: 'brx-IN' }, { value: 'bs-BA', text: 'Bosnian (Bosnia & Herzegovina)', secondaryText: 'bs-BA' }, { value: 'bs-Cyrl', text: 'Bosnian (Cyrillic)', secondaryText: 'bs-Cyrl' }, { value: 'bs-Latn', text: 'Bosnian (Latin)', secondaryText: 'bs-Latn' }, { value: 'br-FR', text: 'Breton (France)', secondaryText: 'br-FR' }, { value: 'bg-BG', text: 'Bulgarian (Bulgaria)', secondaryText: 'bg-BG' }, { value: 'my-MM', text: 'Burmese (Myanmar (Burma))', secondaryText: 'my-MM' }, { value: 'yue-CN', text: 'Cantonese (China)', secondaryText: 'yue-CN' }, { value: 'yue-HK', text: 'Cantonese (Hong Kong SAR China)', secondaryText: 'yue-HK' }, { value: 'yue-Hans', text: 'Cantonese (Simplified)', secondaryText: 'yue-Hans' }, { value: 'yue-Hant', text: 'Cantonese (Traditional)', secondaryText: 'yue-Hant' }, { value: 'ca-AD', text: 'Catalan (Andorra)', secondaryText: 'ca-AD' }, { value: 'ca-FR', text: 'Catalan (France)', secondaryText: 'ca-FR' }, { value: 'ca-IT', text: 'Catalan (Italy)', secondaryText: 'ca-IT' }, { value: 'ca-ES', text: 'Catalan (Spain)', secondaryText: 'ca-ES' }, { value: 'ceb-PH', text: 'Cebuano (Philippines)', secondaryText: 'ceb-PH' }, { value: 'tzm-MA', text: 'Central Atlas Tamazight (Morocco)', secondaryText: 'tzm-MA' }, { value: 'ckb-IR', text: 'Central Kurdish (Iran)', secondaryText: 'ckb-IR' }, { value: 'ckb-IQ', text: 'Central Kurdish (Iraq)', secondaryText: 'ckb-IQ' }, { value: 'ccp-BD', text: 'Chakma (Bangladesh)', secondaryText: 'ccp-BD' }, { value: 'ccp-IN', text: 'Chakma (India)', secondaryText: 'ccp-IN' }, { value: 'ce-RU', text: 'Chechen (Russia)', secondaryText: 'ce-RU' }, { value: 'chr-US', text: 'Cherokee (United States)', secondaryText: 'chr-US' }, { value: 'cgg-UG', text: 'Chiga (Uganda)', secondaryText: 'cgg-UG' }, { value: 'zh-CN', text: 'Chinese (China)', secondaryText: 'zh-CN' }, { value: 'zh-HK', text: 'Chinese (Hong Kong SAR China)', secondaryText: 'zh-HK' }, { value: 'zh-MO', text: 'Chinese (Macao SAR China)', secondaryText: 'zh-MO' }, { value: 'zh-Hans', text: 'Chinese (Simplified)', secondaryText: 'zh-Hans' }, { value: 'zh-SG', text: 'Chinese (Singapore)', secondaryText: 'zh-SG' }, { value: 'zh-TW', text: 'Chinese (Taiwan)', secondaryText: 'zh-TW' }, { value: 'zh-Hant', text: 'Chinese (Traditional)', secondaryText: 'zh-Hant' }, { value: 'cv-RU', text: 'Chuvash (Russia)', secondaryText: 'cv-RU' }, { value: 'ksh-DE', text: 'Colognian (Germany)', secondaryText: 'ksh-DE' }, { value: 'kw-GB', text: 'Cornish (United Kingdom)', secondaryText: 'kw-GB' }, { value: 'hr-BA', text: 'Croatian (Bosnia & Herzegovina)', secondaryText: 'hr-BA' }, { value: 'hr-HR', text: 'Croatian (Croatia)', secondaryText: 'hr-HR' }, { value: 'cs-CZ', text: 'Czech (Czechia)', secondaryText: 'cs-CZ' }, { value: 'da-DK', text: 'Danish (Denmark)', secondaryText: 'da-DK' }, { value: 'da-GL', text: 'Danish (Greenland)', secondaryText: 'da-GL' }, { value: 'doi-IN', text: 'Dogri (India)', secondaryText: 'doi-IN' }, { value: 'dua-CM', text: 'Duala (Cameroon)', secondaryText: 'dua-CM' }, { value: 'nl-AW', text: 'Dutch (Aruba)', secondaryText: 'nl-AW' }, { value: 'nl-BE', text: 'Dutch (Belgium)', secondaryText: 'nl-BE' }, { value: 'nl-BQ', text: 'Dutch (Caribbean Netherlands)', secondaryText: 'nl-BQ' }, { value: 'nl-CW', text: 'Dutch (Curaçao)', secondaryText: 'nl-CW' }, { value: 'nl-NL', text: 'Dutch (Netherlands)', secondaryText: 'nl-NL' }, { value: 'nl-SX', text: 'Dutch (Sint Maarten)', secondaryText: 'nl-SX' }, { value: 'nl-SR', text: 'Dutch (Suriname)', secondaryText: 'nl-SR' }, { value: 'dz-BT', text: 'Dzongkha (Bhutan)', secondaryText: 'dz-BT' }, { value: 'ebu-KE', text: 'Embu (Kenya)', secondaryText: 'ebu-KE' }, { value: 'en-AS', text: 'English (American Samoa)', secondaryText: 'en-AS' }, { value: 'en-AI', text: 'English (Anguilla)', secondaryText: 'en-AI' }, { value: 'en-AG', text: 'English (Antigua & Barbuda)', secondaryText: 'en-AG' }, { value: 'en-AU', text: 'English (Australia)', secondaryText: 'en-AU' }, { value: 'en-AT', text: 'English (Austria)', secondaryText: 'en-AT' }, { value: 'en-BS', text: 'English (Bahamas)', secondaryText: 'en-BS' }, { value: 'en-BB', text: 'English (Barbados)', secondaryText: 'en-BB' }, { value: 'en-BE', text: 'English (Belgium)', secondaryText: 'en-BE' }, { value: 'en-BZ', text: 'English (Belize)', secondaryText: 'en-BZ' }, { value: 'en-BM', text: 'English (Bermuda)', secondaryText: 'en-BM' }, { value: 'en-BW', text: 'English (Botswana)', secondaryText: 'en-BW' }, { value: 'en-IO', text: 'English (British Indian Ocean Territory)', secondaryText: 'en-IO' }, { value: 'en-VG', text: 'English (British Virgin Islands)', secondaryText: 'en-VG' }, { value: 'en-BI', text: 'English (Burundi)', secondaryText: 'en-BI' }, { value: 'en-CM', text: 'English (Cameroon)', secondaryText: 'en-CM' }, { value: 'en-CA', text: 'English (Canada)', secondaryText: 'en-CA' }, { value: 'en-KY', text: 'English (Cayman Islands)', secondaryText: 'en-KY' }, { value: 'en-CX', text: 'English (Christmas Island)', secondaryText: 'en-CX' }, { value: 'en-CC', text: 'English (Cocos (Keeling) Islands)', secondaryText: 'en-CC' }, { value: 'en-CK', text: 'English (Cook Islands)', secondaryText: 'en-CK' }, { value: 'en-CY', text: 'English (Cyprus)', secondaryText: 'en-CY' }, { value: 'en-DK', text: 'English (Denmark)', secondaryText: 'en-DK' }, { value: 'en-DG', text: 'English (Diego Garcia)', secondaryText: 'en-DG' }, { value: 'en-DM', text: 'English (Dominica)', secondaryText: 'en-DM' }, { value: 'en-ER', text: 'English (Eritrea)', secondaryText: 'en-ER' }, { value: 'en-SZ', text: 'English (Eswatini)', secondaryText: 'en-SZ' }, { value: 'en-150', text: 'English (Europe)', secondaryText: 'en-150' }, { value: 'en-FK', text: 'English (Falkland Islands)', secondaryText: 'en-FK' }, { value: 'en-FJ', text: 'English (Fiji)', secondaryText: 'en-FJ' }, { value: 'en-FI', text: 'English (Finland)', secondaryText: 'en-FI' }, { value: 'en-GM', text: 'English (Gambia)', secondaryText: 'en-GM' }, { value: 'en-DE', text: 'English (Germany)', secondaryText: 'en-DE' }, { value: 'en-GH', text: 'English (Ghana)', secondaryText: 'en-GH' }, { value: 'en-GI', text: 'English (Gibraltar)', secondaryText: 'en-GI' }, { value: 'en-GD', text: 'English (Grenada)', secondaryText: 'en-GD' }, { value: 'en-GU', text: 'English (Guam)', secondaryText: 'en-GU' }, { value: 'en-GG', text: 'English (Guernsey)', secondaryText: 'en-GG' }, { value: 'en-GY', text: 'English (Guyana)', secondaryText: 'en-GY' }, { value: 'en-HK', text: 'English (Hong Kong SAR China)', secondaryText: 'en-HK' }, { value: 'en-IN', text: 'English (India)', secondaryText: 'en-IN' }, { value: 'en-IE', text: 'English (Ireland)', secondaryText: 'en-IE' }, { value: 'en-IM', text: 'English (Isle of Man)', secondaryText: 'en-IM' }, { value: 'en-IL', text: 'English (Israel)', secondaryText: 'en-IL' }, { value: 'en-JM', text: 'English (Jamaica)', secondaryText: 'en-JM' }, { value: 'en-JE', text: 'English (Jersey)', secondaryText: 'en-JE' }, { value: 'en-KE', text: 'English (Kenya)', secondaryText: 'en-KE' }, { value: 'en-KI', text: 'English (Kiribati)', secondaryText: 'en-KI' }, { value: 'en-LS', text: 'English (Lesotho)', secondaryText: 'en-LS' }, { value: 'en-LR', text: 'English (Liberia)', secondaryText: 'en-LR' }, { value: 'en-MO', text: 'English (Macao SAR China)', secondaryText: 'en-MO' }, { value: 'en-MG', text: 'English (Madagascar)', secondaryText: 'en-MG' }, { value: 'en-MW', text: 'English (Malawi)', secondaryText: 'en-MW' }, { value: 'en-MY', text: 'English (Malaysia)', secondaryText: 'en-MY' }, { value: 'en-MV', text: 'English (Maldives)', secondaryText: 'en-MV' }, { value: 'en-MT', text: 'English (Malta)', secondaryText: 'en-MT' }, { value: 'en-MH', text: 'English (Marshall Islands)', secondaryText: 'en-MH' }, { value: 'en-MU', text: 'English (Mauritius)', secondaryText: 'en-MU' }, { value: 'en-FM', text: 'English (Micronesia)', secondaryText: 'en-FM' }, { value: 'en-MS', text: 'English (Montserrat)', secondaryText: 'en-MS' }, { value: 'en-NA', text: 'English (Namibia)', secondaryText: 'en-NA' }, { value: 'en-NR', text: 'English (Nauru)', secondaryText: 'en-NR' }, { value: 'en-NL', text: 'English (Netherlands)', secondaryText: 'en-NL' }, { value: 'en-NZ', text: 'English (New Zealand)', secondaryText: 'en-NZ' }, { value: 'en-NG', text: 'English (Nigeria)', secondaryText: 'en-NG' }, { value: 'en-NU', text: 'English (Niue)', secondaryText: 'en-NU' }, { value: 'en-NF', text: 'English (Norfolk Island)', secondaryText: 'en-NF' }, { value: 'en-MP', text: 'English (Northern Mariana Islands)', secondaryText: 'en-MP' }, { value: 'en-PK', text: 'English (Pakistan)', secondaryText: 'en-PK' }, { value: 'en-PW', text: 'English (Palau)', secondaryText: 'en-PW' }, { value: 'en-PG', text: 'English (Papua New Guinea)', secondaryText: 'en-PG' }, { value: 'en-PH', text: 'English (Philippines)', secondaryText: 'en-PH' }, { value: 'en-PN', text: 'English (Pitcairn Islands)', secondaryText: 'en-PN' }, { value: 'en-PR', text: 'English (Puerto Rico)', secondaryText: 'en-PR' }, { value: 'en-RW', text: 'English (Rwanda)', secondaryText: 'en-RW' }, { value: 'en-WS', text: 'English (Samoa)', secondaryText: 'en-WS' }, { value: 'en-SC', text: 'English (Seychelles)', secondaryText: 'en-SC' }, { value: 'en-SL', text: 'English (Sierra Leone)', secondaryText: 'en-SL' }, { value: 'en-SG', text: 'English (Singapore)', secondaryText: 'en-SG' }, { value: 'en-SX', text: 'English (Sint Maarten)', secondaryText: 'en-SX' }, { value: 'en-SI', text: 'English (Slovenia)', secondaryText: 'en-SI' }, { value: 'en-SB', text: 'English (Solomon Islands)', secondaryText: 'en-SB' }, { value: 'en-ZA', text: 'English (South Africa)', secondaryText: 'en-ZA' }, { value: 'en-SS', text: 'English (South Sudan)', secondaryText: 'en-SS' }, { value: 'en-SH', text: 'English (St. Helena)', secondaryText: 'en-SH' }, { value: 'en-KN', text: 'English (St. Kitts & Nevis)', secondaryText: 'en-KN' }, { value: 'en-LC', text: 'English (St. Lucia)', secondaryText: 'en-LC' }, { value: 'en-VC', text: 'English (St. Vincent & Grenadines)', secondaryText: 'en-VC' }, { value: 'en-SD', text: 'English (Sudan)', secondaryText: 'en-SD' }, { value: 'en-SE', text: 'English (Sweden)', secondaryText: 'en-SE' }, { value: 'en-CH', text: 'English (Switzerland)', secondaryText: 'en-CH' }, { value: 'en-TZ', text: 'English (Tanzania)', secondaryText: 'en-TZ' }, { value: 'en-TK', text: 'English (Tokelau)', secondaryText: 'en-TK' }, { value: 'en-TO', text: 'English (Tonga)', secondaryText: 'en-TO' }, { value: 'en-TT', text: 'English (Trinidad & Tobago)', secondaryText: 'en-TT' }, { value: 'en-TC', text: 'English (Turks & Caicos Islands)', secondaryText: 'en-TC' }, { value: 'en-TV', text: 'English (Tuvalu)', secondaryText: 'en-TV' }, { value: 'en-UM', text: 'English (U.S. Outlying Islands)', secondaryText: 'en-UM' }, { value: 'en-VI', text: 'English (U.S. Virgin Islands)', secondaryText: 'en-VI' }, { value: 'en-UG', text: 'English (Uganda)', secondaryText: 'en-UG' }, { value: 'en-AE', text: 'English (United Arab Emirates)', secondaryText: 'en-AE' }, { value: 'en-GB', text: 'English (United Kingdom)', secondaryText: 'en-GB' }, { value: 'en-US', text: 'English (United States)', secondaryText: 'en-US' }, { value: 'en-VU', text: 'English (Vanuatu)', secondaryText: 'en-VU' }, { value: 'en-001', text: 'English (world)', secondaryText: 'en-001' }, { value: 'en-ZM', text: 'English (Zambia)', secondaryText: 'en-ZM' }, { value: 'en-ZW', text: 'English (Zimbabwe)', secondaryText: 'en-ZW' }, { value: 'eo-001', text: 'Esperanto (world)', secondaryText: 'eo-001' }, { value: 'et-EE', text: 'Estonian (Estonia)', secondaryText: 'et-EE' }, { value: 'ee-GH', text: 'Ewe (Ghana)', secondaryText: 'ee-GH' }, { value: 'ee-TG', text: 'Ewe (Togo)', secondaryText: 'ee-TG' }, { value: 'ewo-CM', text: 'Ewondo (Cameroon)', secondaryText: 'ewo-CM' }, { value: 'fo-DK', text: 'Faroese (Denmark)', secondaryText: 'fo-DK' }, { value: 'fo-FO', text: 'Faroese (Faroe Islands)', secondaryText: 'fo-FO' }, { value: 'fil-PH', text: 'Filipino (Philippines)', secondaryText: 'fil-PH' }, { value: 'fi-FI', text: 'Finnish (Finland)', secondaryText: 'fi-FI' }, { value: 'fr-DZ', text: 'French (Algeria)', secondaryText: 'fr-DZ' }, { value: 'fr-BE', text: 'French (Belgium)', secondaryText: 'fr-BE' }, { value: 'fr-BJ', text: 'French (Benin)', secondaryText: 'fr-BJ' }, { value: 'fr-BF', text: 'French (Burkina Faso)', secondaryText: 'fr-BF' }, { value: 'fr-BI', text: 'French (Burundi)', secondaryText: 'fr-BI' }, { value: 'fr-CM', text: 'French (Cameroon)', secondaryText: 'fr-CM' }, { value: 'fr-CA', text: 'French (Canada)', secondaryText: 'fr-CA' }, { value: 'fr-CF', text: 'French (Central African Republic)', secondaryText: 'fr-CF' }, { value: 'fr-TD', text: 'French (Chad)', secondaryText: 'fr-TD' }, { value: 'fr-KM', text: 'French (Comoros)', secondaryText: 'fr-KM' }, { value: 'fr-CG', text: 'French (Congo - Brazzaville)', secondaryText: 'fr-CG' }, { value: 'fr-CD', text: 'French (Congo - Kinshasa)', secondaryText: 'fr-CD' }, { value: 'fr-CI', text: 'French (Côte d’Ivoire)', secondaryText: 'fr-CI' }, { value: 'fr-DJ', text: 'French (Djibouti)', secondaryText: 'fr-DJ' }, { value: 'fr-GQ', text: 'French (Equatorial Guinea)', secondaryText: 'fr-GQ' }, { value: 'fr-FR', text: 'French (France)', secondaryText: 'fr-FR' }, { value: 'fr-GF', text: 'French (French Guiana)', secondaryText: 'fr-GF' }, { value: 'fr-PF', text: 'French (French Polynesia)', secondaryText: 'fr-PF' }, { value: 'fr-GA', text: 'French (Gabon)', secondaryText: 'fr-GA' }, { value: 'fr-GP', text: 'French (Guadeloupe)', secondaryText: 'fr-GP' }, { value: 'fr-GN', text: 'French (Guinea)', secondaryText: 'fr-GN' }, { value: 'fr-HT', text: 'French (Haiti)', secondaryText: 'fr-HT' }, { value: 'fr-LU', text: 'French (Luxembourg)', secondaryText: 'fr-LU' }, { value: 'fr-MG', text: 'French (Madagascar)', secondaryText: 'fr-MG' }, { value: 'fr-ML', text: 'French (Mali)', secondaryText: 'fr-ML' }, { value: 'fr-MQ', text: 'French (Martinique)', secondaryText: 'fr-MQ' }, { value: 'fr-MR', text: 'French (Mauritania)', secondaryText: 'fr-MR' }, { value: 'fr-MU', text: 'French (Mauritius)', secondaryText: 'fr-MU' }, { value: 'fr-YT', text: 'French (Mayotte)', secondaryText: 'fr-YT' }, { value: 'fr-MC', text: 'French (Monaco)', secondaryText: 'fr-MC' }, { value: 'fr-MA', text: 'French (Morocco)', secondaryText: 'fr-MA' }, { value: 'fr-NC', text: 'French (New Caledonia)', secondaryText: 'fr-NC' }, { value: 'fr-NE', text: 'French (Niger)', secondaryText: 'fr-NE' }, { value: 'fr-RW', text: 'French (Rwanda)', secondaryText: 'fr-RW' }, { value: 'fr-RE', text: 'French (Réunion)', secondaryText: 'fr-RE' }, { value: 'fr-SN', text: 'French (Senegal)', secondaryText: 'fr-SN' }, { value: 'fr-SC', text: 'French (Seychelles)', secondaryText: 'fr-SC' }, { value: 'fr-BL', text: 'French (St. Barthélemy)', secondaryText: 'fr-BL' }, { value: 'fr-MF', text: 'French (St. Martin)', secondaryText: 'fr-MF' }, { value: 'fr-PM', text: 'French (St. Pierre & Miquelon)', secondaryText: 'fr-PM' }, { value: 'fr-CH', text: 'French (Switzerland)', secondaryText: 'fr-CH' }, { value: 'fr-SY', text: 'French (Syria)', secondaryText: 'fr-SY' }, { value: 'fr-TG', text: 'French (Togo)', secondaryText: 'fr-TG' }, { value: 'fr-TN', text: 'French (Tunisia)', secondaryText: 'fr-TN' }, { value: 'fr-VU', text: 'French (Vanuatu)', secondaryText: 'fr-VU' }, { value: 'fr-WF', text: 'French (Wallis & Futuna)', secondaryText: 'fr-WF' }, { value: 'fur-IT', text: 'Friulian (Italy)', secondaryText: 'fur-IT' }, { value: 'ff-Adlm', text: 'Fula (Adlam)', secondaryText: 'ff-Adlm' }, { value: 'ff-GN', text: 'Fula (Guinea)', secondaryText: 'ff-GN' }, { value: 'ff-Latn', text: 'Fula (Latin)', secondaryText: 'ff-Latn' }, { value: 'ff-SN', text: 'Fula (Senegal)', secondaryText: 'ff-SN' }, { value: 'gl-ES', text: 'Galician (Spain)', secondaryText: 'gl-ES' }, { value: 'lg-UG', text: 'Ganda (Uganda)', secondaryText: 'lg-UG' }, { value: 'ka-GE', text: 'Georgian (Georgia)', secondaryText: 'ka-GE' }, { value: 'de-AT', text: 'German (Austria)', secondaryText: 'de-AT' }, { value: 'de-BE', text: 'German (Belgium)', secondaryText: 'de-BE' }, { value: 'de-DE', text: 'German (Germany)', secondaryText: 'de-DE' }, { value: 'de-IT', text: 'German (Italy)', secondaryText: 'de-IT' }, { value: 'de-LI', text: 'German (Liechtenstein)', secondaryText: 'de-LI' }, { value: 'de-LU', text: 'German (Luxembourg)', secondaryText: 'de-LU' }, { value: 'de-CH', text: 'German (Switzerland)', secondaryText: 'de-CH' }, { value: 'el-CY', text: 'Greek (Cyprus)', secondaryText: 'el-CY' }, { value: 'el-GR', text: 'Greek (Greece)', secondaryText: 'el-GR' }, { value: 'el-POLYTON', text: 'Greek (Polytonic)', secondaryText: 'el-POLYTON' }, { value: 'gu-IN', text: 'Gujarati (India)', secondaryText: 'gu-IN' }, { value: 'guz-KE', text: 'Gusii (Kenya)', secondaryText: 'guz-KE' }, { value: 'bgc-IN', text: 'Haryanvi (India)', secondaryText: 'bgc-IN' }, { value: 'ha-GH', text: 'Hausa (Ghana)', secondaryText: 'ha-GH' }, { value: 'ha-NE', text: 'Hausa (Niger)', secondaryText: 'ha-NE' }, { value: 'ha-NG', text: 'Hausa (Nigeria)', secondaryText: 'ha-NG' }, { value: 'haw-US', text: 'Hawaiian (United States)', secondaryText: 'haw-US' }, { value: 'he-IL', text: 'Hebrew (Israel)', secondaryText: 'he-IL' }, { value: 'hi-IN', text: 'Hindi (India)', secondaryText: 'hi-IN' }, { value: 'hi-Latn', text: 'Hindi (Latin)', secondaryText: 'hi-Latn' }, { value: 'hu-HU', text: 'Hungarian (Hungary)', secondaryText: 'hu-HU' }, { value: 'is-IS', text: 'Icelandic (Iceland)', secondaryText: 'is-IS' }, { value: 'ig-NG', text: 'Igbo (Nigeria)', secondaryText: 'ig-NG' }, { value: 'smn-FI', text: 'Inari Sami (Finland)', secondaryText: 'smn-FI' }, { value: 'id-ID', text: 'Indonesian (Indonesia)', secondaryText: 'id-ID' }, { value: 'ia-001', text: 'Interlingua (world)', secondaryText: 'ia-001' }, { value: 'ga-IE', text: 'Irish (Ireland)', secondaryText: 'ga-IE' }, { value: 'ga-GB', text: 'Irish (United Kingdom)', secondaryText: 'ga-GB' }, { value: 'it-IT', text: 'Italian (Italy)', secondaryText: 'it-IT' }, { value: 'it-SM', text: 'Italian (San Marino)', secondaryText: 'it-SM' }, { value: 'it-CH', text: 'Italian (Switzerland)', secondaryText: 'it-CH' }, { value: 'it-VA', text: 'Italian (Vatican City)', secondaryText: 'it-VA' }, { value: 'ja-JP', text: 'Japanese (Japan)', secondaryText: 'ja-JP' }, { value: 'jv-ID', text: 'Javanese (Indonesia)', secondaryText: 'jv-ID' }, { value: 'dyo-SN', text: 'Jola-Fonyi (Senegal)', secondaryText: 'dyo-SN' }, { value: 'kea-CV', text: 'Kabuverdianu (Cape Verde)', secondaryText: 'kea-CV' }, { value: 'kab-DZ', text: 'Kabyle (Algeria)', secondaryText: 'kab-DZ' }, { value: 'kgp-BR', text: 'Kaingang (Brazil)', secondaryText: 'kgp-BR' }, { value: 'kkj-CM', text: 'Kako (Cameroon)', secondaryText: 'kkj-CM' }, { value: 'kl-GL', text: 'Kalaallisut (Greenland)', secondaryText: 'kl-GL' }, { value: 'kln-KE', text: 'Kalenjin (Kenya)', secondaryText: 'kln-KE' }, { value: 'kam-KE', text: 'Kamba (Kenya)', secondaryText: 'kam-KE' }, { value: 'kn-IN', text: 'Kannada (India)', secondaryText: 'kn-IN' }, { value: 'ks-Arab', text: 'Kashmiri (Arabic)', secondaryText: 'ks-Arab' }, { value: 'ks-Deva', text: 'Kashmiri (Devanagari)', secondaryText: 'ks-Deva' }, { value: 'ks-IN', text: 'Kashmiri (India)', secondaryText: 'ks-IN' }, { value: 'kk-KZ', text: 'Kazakh (Kazakhstan)', secondaryText: 'kk-KZ' }, { value: 'km-KH', text: 'Khmer (Cambodia)', secondaryText: 'km-KH' }, { value: 'ki-KE', text: 'Kikuyu (Kenya)', secondaryText: 'ki-KE' }, { value: 'rw-RW', text: 'Kinyarwanda (Rwanda)', secondaryText: 'rw-RW' }, { value: 'kok-IN', text: 'Konkani (India)', secondaryText: 'kok-IN' }, { value: 'ko-KP', text: 'Korean (North Korea)', secondaryText: 'ko-KP' }, { value: 'ko-KR', text: 'Korean (South Korea)', secondaryText: 'ko-KR' }, { value: 'khq-ML', text: 'Koyra Chiini (Mali)', secondaryText: 'khq-ML' }, { value: 'ses-ML', text: 'Koyraboro Senni (Mali)', secondaryText: 'ses-ML' }, { value: 'ku-TR', text: 'Kurdish (Türkiye)', secondaryText: 'ku-TR' }, { value: 'nmg-CM', text: 'Kwasio (Cameroon)', secondaryText: 'nmg-CM' }, { value: 'ky-KG', text: 'Kyrgyz (Kyrgyzstan)', secondaryText: 'ky-KG' }, { value: 'lkt-US', text: 'Lakota (United States)', secondaryText: 'lkt-US' }, { value: 'lag-TZ', text: 'Langi (Tanzania)', secondaryText: 'lag-TZ' }, { value: 'lo-LA', text: 'Lao (Laos)', secondaryText: 'lo-LA' }, { value: 'lv-LV', text: 'Latvian (Latvia)', secondaryText: 'lv-LV' }, { value: 'ln-AO', text: 'Lingala (Angola)', secondaryText: 'ln-AO' }, { value: 'ln-CF', text: 'Lingala (Central African Republic)', secondaryText: 'ln-CF' }, { value: 'ln-CG', text: 'Lingala (Congo - Brazzaville)', secondaryText: 'ln-CG' }, { value: 'ln-CD', text: 'Lingala (Congo - Kinshasa)', secondaryText: 'ln-CD' }, { value: 'lt-LT', text: 'Lithuanian (Lithuania)', secondaryText: 'lt-LT' }, { value: 'nds-DE', text: 'Low German (Germany)', secondaryText: 'nds-DE' }, { value: 'nds-NL', text: 'Low German (Netherlands)', secondaryText: 'nds-NL' }, { value: 'dsb-DE', text: 'Lower Sorbian (Germany)', secondaryText: 'dsb-DE' }, { value: 'lu-CD', text: 'Luba-Katanga (Congo - Kinshasa)', secondaryText: 'lu-CD' }, { value: 'luo-KE', text: 'Luo (Kenya)', secondaryText: 'luo-KE' }, { value: 'lb-LU', text: 'Luxembourgish (Luxembourg)', secondaryText: 'lb-LU' }, { value: 'luy-KE', text: 'Luyia (Kenya)', secondaryText: 'luy-KE' }, { value: 'mk-MK', text: 'Macedonian (North Macedonia)', secondaryText: 'mk-MK' }, { value: 'jmc-TZ', text: 'Machame (Tanzania)', secondaryText: 'jmc-TZ' }, { value: 'mai-IN', text: 'Maithili (India)', secondaryText: 'mai-IN' }, { value: 'mgh-MZ', text: 'Makhuwa-Meetto (Mozambique)', secondaryText: 'mgh-MZ' }, { value: 'kde-TZ', text: 'Makonde (Tanzania)', secondaryText: 'kde-TZ' }, { value: 'mg-MG', text: 'Malagasy (Madagascar)', secondaryText: 'mg-MG' }, { value: 'ms-BN', text: 'Malay (Brunei)', secondaryText: 'ms-BN' }, { value: 'ms-ID', text: 'Malay (Indonesia)', secondaryText: 'ms-ID' }, { value: 'ms-MY', text: 'Malay (Malaysia)', secondaryText: 'ms-MY' }, { value: 'ms-SG', text: 'Malay (Singapore)', secondaryText: 'ms-SG' }, { value: 'ml-IN', text: 'Malayalam (India)', secondaryText: 'ml-IN' }, { value: 'mt-MT', text: 'Maltese (Malta)', secondaryText: 'mt-MT' }, { value: 'mni-Beng', text: 'Manipuri (Bangla)', secondaryText: 'mni-Beng' }, { value: 'mni-IN', text: 'Manipuri (India)', secondaryText: 'mni-IN' }, { value: 'gv-IM', text: 'Manx (Isle of Man)', secondaryText: 'gv-IM' }, { value: 'mr-IN', text: 'Marathi (India)', secondaryText: 'mr-IN' }, { value: 'mas-KE', text: 'Masai (Kenya)', secondaryText: 'mas-KE' }, { value: 'mas-TZ', text: 'Masai (Tanzania)', secondaryText: 'mas-TZ' }, { value: 'mzn-IR', text: 'Mazanderani (Iran)', secondaryText: 'mzn-IR' }, { value: 'mer-KE', text: 'Meru (Kenya)', secondaryText: 'mer-KE' }, { value: 'mgo-CM', text: 'Metaʼ (Cameroon)', secondaryText: 'mgo-CM' }, { value: 'mdf-RU', text: 'Moksha (Russia)', secondaryText: 'mdf-RU' }, { value: 'mn-MN', text: 'Mongolian (Mongolia)', secondaryText: 'mn-MN' }, { value: 'mfe-MU', text: 'Morisyen (Mauritius)', secondaryText: 'mfe-MU' }, { value: 'mua-CM', text: 'Mundang (Cameroon)', secondaryText: 'mua-CM' }, { value: 'mi-NZ', text: 'Māori (New Zealand)', secondaryText: 'mi-NZ' }, { value: 'naq-NA', text: 'Nama (Namibia)', secondaryText: 'naq-NA' }, { value: 'ne-IN', text: 'Nepali (India)', secondaryText: 'ne-IN' }, { value: 'ne-NP', text: 'Nepali (Nepal)', secondaryText: 'ne-NP' }, { value: 'nnh-CM', text: 'Ngiemboon (Cameroon)', secondaryText: 'nnh-CM' }, { value: 'jgo-CM', text: 'Ngomba (Cameroon)', secondaryText: 'jgo-CM' }, { value: 'yrl-BR', text: 'Nheengatu (Brazil)', secondaryText: 'yrl-BR' }, { value: 'yrl-CO', text: 'Nheengatu (Colombia)', secondaryText: 'yrl-CO' }, { value: 'yrl-VE', text: 'Nheengatu (Venezuela)', secondaryText: 'yrl-VE' }, { value: 'pcm-NG', text: 'Nigerian Pidgin (Nigeria)', secondaryText: 'pcm-NG' }, { value: 'nd-ZW', text: 'North Ndebele (Zimbabwe)', secondaryText: 'nd-ZW' }, { value: 'frr-DE', text: 'Northern Frisian (Germany)', secondaryText: 'frr-DE' }, { value: 'lrc-IR', text: 'Northern Luri (Iran)', secondaryText: 'lrc-IR' }, { value: 'lrc-IQ', text: 'Northern Luri (Iraq)', secondaryText: 'lrc-IQ' }, { value: 'se-FI', text: 'Northern Sami (Finland)', secondaryText: 'se-FI' }, { value: 'se-NO', text: 'Northern Sami (Norway)', secondaryText: 'se-NO' }, { value: 'se-SE', text: 'Northern Sami (Sweden)', secondaryText: 'se-SE' }, { value: 'no-NO', text: 'Norwegian (Norway)', secondaryText: 'no-NO' }, { value: 'nn-NO', text: 'Norwegian (Norway, Nynorsk)', secondaryText: 'nn-NO' }, { value: 'nb-NO', text: 'Norwegian Bokmål (Norway)', secondaryText: 'nb-NO' }, { value: 'nb-SJ', text: 'Norwegian Bokmål (Svalbard & Jan Mayen)', secondaryText: 'nb-SJ' }, { value: 'nus-SS', text: 'Nuer (South Sudan)', secondaryText: 'nus-SS' }, { value: 'nyn-UG', text: 'Nyankole (Uganda)', secondaryText: 'nyn-UG' }, { value: 'ann-NG', text: 'Obolo (Nigeria)', secondaryText: 'ann-NG' }, { value: 'oc-FR', text: 'Occitan (France)', secondaryText: 'oc-FR' }, { value: 'oc-ES', text: 'Occitan (Spain)', secondaryText: 'oc-ES' }, { value: 'or-IN', text: 'Odia (India)', secondaryText: 'or-IN' }, { value: 'om-ET', text: 'Oromo (Ethiopia)', secondaryText: 'om-ET' }, { value: 'om-KE', text: 'Oromo (Kenya)', secondaryText: 'om-KE' }, { value: 'os-GE', text: 'Ossetic (Georgia)', secondaryText: 'os-GE' }, { value: 'os-RU', text: 'Ossetic (Russia)', secondaryText: 'os-RU' }, { value: 'ps-AF', text: 'Pashto (Afghanistan)', secondaryText: 'ps-AF' }, { value: 'ps-PK', text: 'Pashto (Pakistan)', secondaryText: 'ps-PK' }, { value: 'fa-AF', text: 'Persian (Afghanistan)', secondaryText: 'fa-AF' }, { value: 'fa-IR', text: 'Persian (Iran)', secondaryText: 'fa-IR' }, { value: 'pis-SB', text: 'Pijin (Solomon Islands)', secondaryText: 'pis-SB' }, { value: 'pl-PL', text: 'Polish (Poland)', secondaryText: 'pl-PL' }, { value: 'pt-AO', text: 'Portuguese (Angola)', secondaryText: 'pt-AO' }, { value: 'pt-BR', text: 'Portuguese (Brazil)', secondaryText: 'pt-BR' }, { value: 'pt-CV', text: 'Portuguese (Cape Verde)', secondaryText: 'pt-CV' }, { value: 'pt-GQ', text: 'Portuguese (Equatorial Guinea)', secondaryText: 'pt-GQ' }, { value: 'pt-GW', text: 'Portuguese (Guinea-Bissau)', secondaryText: 'pt-GW' }, { value: 'pt-LU', text: 'Portuguese (Luxembourg)', secondaryText: 'pt-LU' }, { value: 'pt-MO', text: 'Portuguese (Macao SAR China)', secondaryText: 'pt-MO' }, { value: 'pt-MZ', text: 'Portuguese (Mozambique)', secondaryText: 'pt-MZ' }, { value: 'pt-PT', text: 'Portuguese (Portugal)', secondaryText: 'pt-PT' }, { value: 'pt-CH', text: 'Portuguese (Switzerland)', secondaryText: 'pt-CH' }, { value: 'pt-ST', text: 'Portuguese (São Tomé & Príncipe)', secondaryText: 'pt-ST' }, { value: 'pt-TL', text: 'Portuguese (Timor-Leste)', secondaryText: 'pt-TL' }, { value: 'pa-Arab', text: 'Punjabi (Arabic)', secondaryText: 'pa-Arab' }, { value: 'pa-Guru', text: 'Punjabi (Gurmukhi)', secondaryText: 'pa-Guru' }, { value: 'pa-IN', text: 'Punjabi (India)', secondaryText: 'pa-IN' }, { value: 'pa-PK', text: 'Punjabi (Pakistan)', secondaryText: 'pa-PK' }, { value: 'qu-BO', text: 'Quechua (Bolivia)', secondaryText: 'qu-BO' }, { value: 'qu-EC', text: 'Quechua (Ecuador)', secondaryText: 'qu-EC' }, { value: 'qu-PE', text: 'Quechua (Peru)', secondaryText: 'qu-PE' }, { value: 'raj-IN', text: 'Rajasthani (India)', secondaryText: 'raj-IN' }, { value: 'ro-MD', text: 'Romanian (Moldova)', secondaryText: 'ro-MD' }, { value: 'ro-RO', text: 'Romanian (Romania)', secondaryText: 'ro-RO' }, { value: 'rm-CH', text: 'Romansh (Switzerland)', secondaryText: 'rm-CH' }, { value: 'rof-TZ', text: 'Rombo (Tanzania)', secondaryText: 'rof-TZ' }, { value: 'rn-BI', text: 'Rundi (Burundi)', secondaryText: 'rn-BI' }, { value: 'ru-BY', text: 'Russian (Belarus)', secondaryText: 'ru-BY' }, { value: 'ru-KZ', text: 'Russian (Kazakhstan)', secondaryText: 'ru-KZ' }, { value: 'ru-KG', text: 'Russian (Kyrgyzstan)', secondaryText: 'ru-KG' }, { value: 'ru-MD', text: 'Russian (Moldova)', secondaryText: 'ru-MD' }, { value: 'ru-RU', text: 'Russian (Russia)', secondaryText: 'ru-RU' }, { value: 'ru-UA', text: 'Russian (Ukraine)', secondaryText: 'ru-UA' }, { value: 'rwk-TZ', text: 'Rwa (Tanzania)', secondaryText: 'rwk-TZ' }, { value: 'saq-KE', text: 'Samburu (Kenya)', secondaryText: 'saq-KE' }, { value: 'sg-CF', text: 'Sango (Central African Republic)', secondaryText: 'sg-CF' }, { value: 'sbp-TZ', text: 'Sangu (Tanzania)', secondaryText: 'sbp-TZ' }, { value: 'sa-IN', text: 'Sanskrit (India)', secondaryText: 'sa-IN' }, { value: 'sat-IN', text: 'Santali (India)', secondaryText: 'sat-IN' }, { value: 'sat-Olck', text: 'Santali (Ol Chiki)', secondaryText: 'sat-Olck' }, { value: 'sc-IT', text: 'Sardinian (Italy)', secondaryText: 'sc-IT' }, { value: 'gd-GB', text: 'Scottish Gaelic (United Kingdom)', secondaryText: 'gd-GB' }, { value: 'seh-MZ', text: 'Sena (Mozambique)', secondaryText: 'seh-MZ' }, { value: 'sr-BA', text: 'Serbian (Bosnia & Herzegovina)', secondaryText: 'sr-BA' }, { value: 'sr-Cyrl', text: 'Serbian (Cyrillic)', secondaryText: 'sr-Cyrl' }, { value: 'sr-Latn', text: 'Serbian (Latin)', secondaryText: 'sr-Latn' }, { value: 'sr-ME', text: 'Serbian (Montenegro)', secondaryText: 'sr-ME' }, { value: 'sr-CS', text: 'Serbian (Serbia and Montenegro)', secondaryText: 'sr-CS' }, { value: 'sr-RS', text: 'Serbian (Serbia)', secondaryText: 'sr-RS' }, { value: 'ksb-TZ', text: 'Shambala (Tanzania)', secondaryText: 'ksb-TZ' }, { value: 'sn-ZW', text: 'Shona (Zimbabwe)', secondaryText: 'sn-ZW' }, { value: 'ii-CN', text: 'Sichuan Yi (China)', secondaryText: 'ii-CN' }, { value: 'sd-Arab', text: 'Sindhi (Arabic)', secondaryText: 'sd-Arab' }, { value: 'sd-Deva', text: 'Sindhi (Devanagari)', secondaryText: 'sd-Deva' }, { value: 'sd-IN', text: 'Sindhi (India)', secondaryText: 'sd-IN' }, { value: 'sd-PK', text: 'Sindhi (Pakistan)', secondaryText: 'sd-PK' }, { value: 'si-LK', text: 'Sinhala (Sri Lanka)', secondaryText: 'si-LK' }, { value: 'sms-FI', text: 'Skolt Sami (Finland)', secondaryText: 'sms-FI' }, { value: 'sk-SK', text: 'Slovak (Slovakia)', secondaryText: 'sk-SK' }, { value: 'sl-SI', text: 'Slovenian (Slovenia)', secondaryText: 'sl-SI' }, { value: 'xog-UG', text: 'Soga (Uganda)', secondaryText: 'xog-UG' }, { value: 'so-DJ', text: 'Somali (Djibouti)', secondaryText: 'so-DJ' }, { value: 'so-ET', text: 'Somali (Ethiopia)', secondaryText: 'so-ET' }, { value: 'so-KE', text: 'Somali (Kenya)', secondaryText: 'so-KE' }, { value: 'so-SO', text: 'Somali (Somalia)', secondaryText: 'so-SO' }, { value: 'es-AR', text: 'Spanish (Argentina)', secondaryText: 'es-AR' }, { value: 'es-BZ', text: 'Spanish (Belize)', secondaryText: 'es-BZ' }, { value: 'es-BO', text: 'Spanish (Bolivia)', secondaryText: 'es-BO' }, { value: 'es-BR', text: 'Spanish (Brazil)', secondaryText: 'es-BR' }, { value: 'es-IC', text: 'Spanish (Canary Islands)', secondaryText: 'es-IC' }, { value: 'es-EA', text: 'Spanish (Ceuta & Melilla)', secondaryText: 'es-EA' }, { value: 'es-CL', text: 'Spanish (Chile)', secondaryText: 'es-CL' }, { value: 'es-CO', text: 'Spanish (Colombia)', secondaryText: 'es-CO' }, { value: 'es-CR', text: 'Spanish (Costa Rica)', secondaryText: 'es-CR' }, { value: 'es-CU', text: 'Spanish (Cuba)', secondaryText: 'es-CU' }, { value: 'es-DO', text: 'Spanish (Dominican Republic)', secondaryText: 'es-DO' }, { value: 'es-EC', text: 'Spanish (Ecuador)', secondaryText: 'es-EC' }, { value: 'es-SV', text: 'Spanish (El Salvador)', secondaryText: 'es-SV' }, { value: 'es-GQ', text: 'Spanish (Equatorial Guinea)', secondaryText: 'es-GQ' }, { value: 'es-GT', text: 'Spanish (Guatemala)', secondaryText: 'es-GT' }, { value: 'es-HN', text: 'Spanish (Honduras)', secondaryText: 'es-HN' }, { value: 'es-419', text: 'Spanish (Latin America)', secondaryText: 'es-419' }, { value: 'es-MX', text: 'Spanish (Mexico)', secondaryText: 'es-MX' }, { value: 'es-NI', text: 'Spanish (Nicaragua)', secondaryText: 'es-NI' }, { value: 'es-PA', text: 'Spanish (Panama)', secondaryText: 'es-PA' }, { value: 'es-PY', text: 'Spanish (Paraguay)', secondaryText: 'es-PY' }, { value: 'es-PE', text: 'Spanish (Peru)', secondaryText: 'es-PE' }, { value: 'es-PH', text: 'Spanish (Philippines)', secondaryText: 'es-PH' }, { value: 'es-PR', text: 'Spanish (Puerto Rico)', secondaryText: 'es-PR' }, { value: 'es-ES', text: 'Spanish (Spain)', secondaryText: 'es-ES' }, { value: 'es-US', text: 'Spanish (United States)', secondaryText: 'es-US' }, { value: 'es-UY', text: 'Spanish (Uruguay)', secondaryText: 'es-UY' }, { value: 'es-VE', text: 'Spanish (Venezuela)', secondaryText: 'es-VE' }, { value: 'zgh-MA', text: 'Standard Moroccan Tamazight (Morocco)', secondaryText: 'zgh-MA' }, { value: 'su-ID', text: 'Sundanese (Indonesia)', secondaryText: 'su-ID' }, { value: 'su-Latn', text: 'Sundanese (Latin)', secondaryText: 'su-Latn' }, { value: 'sw-CD', text: 'Swahili (Congo - Kinshasa)', secondaryText: 'sw-CD' }, { value: 'sw-KE', text: 'Swahili (Kenya)', secondaryText: 'sw-KE' }, { value: 'sw-TZ', text: 'Swahili (Tanzania)', secondaryText: 'sw-TZ' }, { value: 'sw-UG', text: 'Swahili (Uganda)', secondaryText: 'sw-UG' }, { value: 'sv-FI', text: 'Swedish (Finland)', secondaryText: 'sv-FI' }, { value: 'sv-SE', text: 'Swedish (Sweden)', secondaryText: 'sv-SE' }, { value: 'sv-AX', text: 'Swedish (Åland Islands)', secondaryText: 'sv-AX' }, { value: 'gsw-FR', text: 'Swiss German (France)', secondaryText: 'gsw-FR' }, { value: 'gsw-LI', text: 'Swiss German (Liechtenstein)', secondaryText: 'gsw-LI' }, { value: 'gsw-CH', text: 'Swiss German (Switzerland)', secondaryText: 'gsw-CH' }, { value: 'shi-Latn', text: 'Tachelhit (Latin)', secondaryText: 'shi-Latn' }, { value: 'shi-MA', text: 'Tachelhit (Morocco)', secondaryText: 'shi-MA' }, { value: 'shi-Tfng', text: 'Tachelhit (Tifinagh)', secondaryText: 'shi-Tfng' }, { value: 'dav-KE', text: 'Taita (Kenya)', secondaryText: 'dav-KE' }, { value: 'tg-TJ', text: 'Tajik (Tajikistan)', secondaryText: 'tg-TJ' }, { value: 'ta-IN', text: 'Tamil (India)', secondaryText: 'ta-IN' }, { value: 'ta-MY', text: 'Tamil (Malaysia)', secondaryText: 'ta-MY' }, { value: 'ta-SG', text: 'Tamil (Singapore)', secondaryText: 'ta-SG' }, { value: 'ta-LK', text: 'Tamil (Sri Lanka)', secondaryText: 'ta-LK' }, { value: 'twq-NE', text: 'Tasawaq (Niger)', secondaryText: 'twq-NE' }, { value: 'tt-RU', text: 'Tatar (Russia)', secondaryText: 'tt-RU' }, { value: 'te-IN', text: 'Telugu (India)', secondaryText: 'te-IN' }, { value: 'teo-KE', text: 'Teso (Kenya)', secondaryText: 'teo-KE' }, { value: 'teo-UG', text: 'Teso (Uganda)', secondaryText: 'teo-UG' }, { value: 'th-TH', text: 'Thai (Thailand)', secondaryText: 'th-TH' }, { value: 'bo-CN', text: 'Tibetan (China)', secondaryText: 'bo-CN' }, { value: 'bo-IN', text: 'Tibetan (India)', secondaryText: 'bo-IN' }, { value: 'ti-ER', text: 'Tigrinya (Eritrea)', secondaryText: 'ti-ER' }, { value: 'ti-ET', text: 'Tigrinya (Ethiopia)', secondaryText: 'ti-ET' }, { value: 'tok-001', text: 'Toki Pona (world)', secondaryText: 'tok-001' }, { value: 'to-TO', text: 'Tongan (Tonga)', secondaryText: 'to-TO' }, { value: 'tr-CY', text: 'Turkish (Cyprus)', secondaryText: 'tr-CY' }, { value: 'tr-TR', text: 'Turkish (Türkiye)', secondaryText: 'tr-TR' }, { value: 'tk-TM', text: 'Turkmen (Turkmenistan)', secondaryText: 'tk-TM' }, { value: 'uk-UA', text: 'Ukrainian (Ukraine)', secondaryText: 'uk-UA' }, { value: 'hsb-DE', text: 'Upper Sorbian (Germany)', secondaryText: 'hsb-DE' }, { value: 'ur-IN', text: 'Urdu (India)', secondaryText: 'ur-IN' }, { value: 'ur-PK', text: 'Urdu (Pakistan)', secondaryText: 'ur-PK' }, { value: 'ug-CN', text: 'Uyghur (China)', secondaryText: 'ug-CN' }, { value: 'uz-AF', text: 'Uzbek (Afghanistan)', secondaryText: 'uz-AF' }, { value: 'uz-Arab', text: 'Uzbek (Arabic)', secondaryText: 'uz-Arab' }, { value: 'uz-Cyrl', text: 'Uzbek (Cyrillic)', secondaryText: 'uz-Cyrl' }, { value: 'uz-Latn', text: 'Uzbek (Latin)', secondaryText: 'uz-Latn' }, { value: 'uz-UZ', text: 'Uzbek (Uzbekistan)', secondaryText: 'uz-UZ' }, { value: 'vai-Latn', text: 'Vai (Latin)', secondaryText: 'vai-Latn' }, { value: 'vai-LR', text: 'Vai (Liberia)', secondaryText: 'vai-LR' }, { value: 'vai-Vaii', text: 'Vai (Vai)', secondaryText: 'vai-Vaii' }, { value: 'vi-VN', text: 'Vietnamese (Vietnam)', secondaryText: 'vi-VN' }, { value: 'vun-TZ', text: 'Vunjo (Tanzania)', secondaryText: 'vun-TZ' }, { value: 'wae-CH', text: 'Walser (Switzerland)', secondaryText: 'wae-CH' }, { value: 'cy-GB', text: 'Welsh (United Kingdom)', secondaryText: 'cy-GB' }, { value: 'fy-NL', text: 'Western Frisian (Netherlands)', secondaryText: 'fy-NL' }, { value: 'wo-SN', text: 'Wolof (Senegal)', secondaryText: 'wo-SN' }, { value: 'xh-ZA', text: 'Xhosa (South Africa)', secondaryText: 'xh-ZA' }, { value: 'sah-RU', text: 'Yakut (Russia)', secondaryText: 'sah-RU' }, { value: 'yav-CM', text: 'Yangben (Cameroon)', secondaryText: 'yav-CM' }, { value: 'yi-001', text: 'Yiddish (world)', secondaryText: 'yi-001' }, { value: 'yo-BJ', text: 'Yoruba (Benin)', secondaryText: 'yo-BJ' }, { value: 'yo-NG', text: 'Yoruba (Nigeria)', secondaryText: 'yo-NG' }, { value: 'dje-NE', text: 'Zarma (Niger)', secondaryText: 'dje-NE' }, { value: 'zu-ZA', text: 'Zulu (South Africa)', secondaryText: 'zu-ZA' }];
    $scope.schemaError = "Schema can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.sequenceError = "Sequence can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.tableError = "Table can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), dollar signs ('$') and two consecutive colons ('::')";
    $scope.filepathError = ["Path can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), forward slashes ('/'), dots ('.'), underscores ('_'), and dollar signs ('$')', 'File does not exist."];
    $scope.columnError = "Column keys must be unique and can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.versionError = "Version can only contain letters (a-z, A-Z), numbers (0-9), hyphens ('-'), dots ('.'), underscores ('_'), and dollar signs ('$')";
    $scope.fileExists = true;
    $scope.editEnabled = false;
    $scope.dataEmpty = true;
    $scope.csvimData = { files: [] };
    $scope.activeItemId = 0;
    $scope.delimiterList = [
        {
            label: ',',
            value: ','
        }, {
            label: 'Tab',
            value: '\t'
        }, {
            label: '|',
            value: '|'
        }, {
            label: ';',
            value: ';'
        }, {
            label: '#',
            value: '#'
        }
    ];
    $scope.quoteCharList = ['\'', '"', '#'];

    angular.element($window).bind('focus', () => { statusBarHub.showLabel('') });

    $scope.toggleSearch = () => {
        $scope.searchField.text = '';
        $scope.searchVisible = !$scope.searchVisible;
    };

    $scope.checkUniqueColumn = (index, value) => {
        for (let i = 0; i < $scope.csvimData.files[$scope.activeItemId].keys.length; i++) {
            if (i != index) {
                if (value === $scope.csvimData.files[$scope.activeItemId].keys[i].column) {
                    return false;
                }
            }
        }
        return true;
    };

    $scope.checkUniqueValue = (kindex, vindex, value) => {
        for (let i = 0; i < $scope.csvimData.files[$scope.activeItemId].keys[kindex].values.length; i++) {
            if (i != vindex) {
                if (value === $scope.csvimData.files[$scope.activeItemId].keys[kindex].values[i]) {
                    return false;
                }
            }
        }
        return true;
    };

    $scope.openFile = () => {
        WorkspaceService.resourceExists(`${workspace}${$scope.csvimData.files[$scope.activeItemId].file}`).then(() => {
            $scope.$evalAsync(() => {
                $scope.fileExists = true;
            });
            layoutHub.openEditor({
                path: `/${workspace}${$scope.csvimData.files[$scope.activeItemId].file}`,
                contentType: 'text/csv',
                params: {
                    'header': $scope.csvimData.files[$scope.activeItemId].header,
                    'delimiter': $scope.csvimData.files[$scope.activeItemId].delimField,
                    'quotechar': $scope.csvimData.files[$scope.activeItemId].delimEnclosing
                },
            });
        }, () => {
            $scope.$evalAsync(() => {
                $scope.fileExists = false;
            });
        });
    };

    $scope.setEditEnabled = (enabled) => {
        if (enabled != undefined) {
            $scope.editEnabled = enabled;
        } else {
            $scope.editEnabled = !$scope.editEnabled;
        }
    };

    $scope.addNew = () => {
        $scope.searchField.text = '';
        $scope.filterFiles();
        $scope.csvimData.files.push({
            'name': 'Untitled',
            'visible': true,
            'table': '',
            'schema': '',
            'sequence': '',
            'file': '',
            'header': false,
            'useHeaderNames': false,
            'delimField': ';',
            'delimEnclosing': '\'',
            'distinguishEmptyFromNull': true,
            'version': '',
            'locale': ''
        });
        $scope.activeItemId = $scope.csvimData.files.length - 1;
        $scope.dataEmpty = false;
        $scope.setEditEnabled(true);
        $scope.fileChanged();
    };

    $scope.getFileName = (str, canBeEmpty = true) => {
        if (canBeEmpty) {
            return str.split('\\').pop().split('/').pop();
        }
        let title = str.split('\\').pop().split('/').pop();
        if (title) return title;
        else return 'Untitled';
    };

    $scope.fileSelected = (id) => {
        if ($scope.forms.editor.$valid) {
            $scope.setEditEnabled(false);
            $scope.fileExists = true;
            $scope.activeItemId = id;
        }
    };

    $scope.isDelimiterSupported = (delimiter) => $scope.delimiterList.some((element) => element.value === delimiter);

    $scope.isQuoteCharSupported = (quoteChar) => $scope.quoteCharList.includes(quoteChar);

    $scope.save = (keySet = 'ctrl+s', event) => {
        event?.preventDefault();
        if (keySet === 'ctrl+s') {
            if ($scope.changed && $scope.forms.editor.$valid && !$scope.state.error) {
                $scope.state.busyText = 'Saving...';
                $scope.state.isBusy = true;
                $scope.csvimData.files[$scope.activeItemId].name = $scope.getFileName($scope.csvimData.files[$scope.activeItemId].file, false);
                saveContents(JSON.stringify($scope.csvimData, cleanForOutput, 2));
            }
        }
    };

    $scope.deleteFile = (index) => {
        dialogHub.showDialog({
            title: 'Delete file?',
            message: `Are you sure you want to delete '${$scope.csvimData.files[index].name}'?\nThis action cannot be undone.`,
            preformatted: true,
            buttons: [{
                id: 'bd',
                state: ButtonStates.Negative,
                label: 'Delete',
            },
            {
                id: 'bc',
                state: ButtonStates.Transparent,
                label: 'Cancel',
            }]
        }).then((buttonId) => {
            if (buttonId === 'bd') {
                $scope.$evalAsync(() => {
                    $scope.csvimData.files.splice(index, 1);
                    $scope.fileExists = true;
                    if ($scope.csvimData.files.length > 0) {
                        $scope.dataEmpty = false;
                        if ($scope.activeItemId === index) {
                            $scope.activeItemId = $scope.csvimData.files.length - 1;
                            $scope.setEditEnabled(false);
                        }
                    } else {
                        $scope.setEditEnabled(false);
                        $scope.dataEmpty = true;
                        $scope.activeItemId = 0;
                    }
                    $scope.fileChanged();
                });
            }
        });
    };

    $scope.filterFiles = (event) => {
        if (event && event.originalEvent.key === 'Escape') {
            $scope.searchField.text = '';
            $scope.toggleSearch();
        } else if ($scope.searchField.text) {
            for (let i = 0; i < $scope.csvimData.files.length; i++) {
                if ($scope.csvimData.files[i].name.toLowerCase().includes($scope.searchField.text.toLowerCase())) {
                    $scope.csvimData.files[i].visible = true;
                } else {
                    $scope.csvimData.files[i].visible = false;
                }
            }
            return;
        }
        for (let i = 0; i < $scope.csvimData.files.length; i++) {
            $scope.csvimData.files[i].visible = true;
        }
    };

    $scope.fileChanged = () => {
        $scope.changed = true;
        layoutHub.setEditorDirty({
            path: $scope.dataParameters.filePath,
            dirty: true,
        });
    };

    /**
     * Used for removing some keys from the object before turning it into a string.
     */
    function cleanForOutput(key, value) {
        if (key === 'name' || key === 'visible') {
            return undefined;
        }
        if (key === 'schema' && value === '') {
            return undefined;
        }
        if (key === 'sequence' && value === '') {
            return undefined;
        }
        if (key === 'locale' && value === '') {
            return undefined;
        }
        return value;
    }

    function isObject(value) {
        return (
            typeof value === 'object' &&
            value !== null &&
            !Array.isArray(value)
        );
    }

    const loadFileContents = () => {
        if (!$scope.state.error) {
            $scope.state.isBusy = true;
            WorkspaceService.loadContent($scope.dataParameters.filePath).then((response) => {
                $scope.$evalAsync(() => {
                    let contents = response.data;
                    if (!contents || !isObject(contents)) {
                        contents = { files: [] };
                    }
                    $scope.csvimData = contents;
                    $scope.activeItemId = 0;
                    if ($scope.csvimData.files && $scope.csvimData.files.length > 0) {
                        $scope.dataEmpty = false;
                        for (let i = 0; i < $scope.csvimData.files.length; i++) {
                            $scope.csvimData.files[i]['name'] = $scope.getFileName($scope.csvimData.files[i].file, false);
                            $scope.csvimData.files[i]['visible'] = true;
                        }
                    } else {
                        $scope.dataEmpty = true;
                    }
                    $scope.state.isBusy = false;
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

    function saveContents(text) {
        WorkspaceService.saveContent($scope.dataParameters.filePath, text).then(() => {
            contents = text;
            layoutHub.setEditorDirty({
                path: $scope.dataParameters.filePath,
                dirty: false,
            });
            workspaceHub.announceFileSaved({
                path: $scope.dataParameters.filePath,
                contentType: $scope.dataParameters.contentType,
            });
            $scope.$evalAsync(() => {
                $scope.changed = false;
                $scope.state.isBusy = false;
            });
        }, (response) => {
            console.error(response);
            $scope.$evalAsync(() => {
                $scope.state.error = true;
                $scope.errorMessage = `Error saving "${$scope.dataParameters.filePath}". Please look at the console for more information.`;
                $scope.state.isBusy = false;
            });
        });
    }

    layoutHub.onFocusEditor((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) statusBarHub.showLabel('');
    });

    layoutHub.onReloadEditorParams((data) => {
        if (data.path === $scope.dataParameters.filePath) {
            $scope.$evalAsync(() => {
                $scope.dataParameters = ViewParameters.get();
            });
        };
    });

    workspaceHub.onSaveAll(() => {
        if ($scope.changed && !$scope.state.error && $scope.forms.editor.$valid) {
            $scope.save();
        }
    });

    workspaceHub.onSaveFile((data) => {
        if (data.path && data.path === $scope.dataParameters.filePath) {
            if ($scope.changed && !$scope.state.error && $scope.forms.editor.$valid) {
                $scope.save();
            }
        }
    });

    $scope.dataParameters = ViewParameters.get();
    if (!$scope.dataParameters.hasOwnProperty('filePath')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'filePath\' data parameter is missing.';
    } else if (!$scope.dataParameters.hasOwnProperty('contentType')) {
        $scope.state.error = true;
        $scope.errorMessage = 'The \'contentType\' data parameter is missing.';
    } else loadFileContents();
});