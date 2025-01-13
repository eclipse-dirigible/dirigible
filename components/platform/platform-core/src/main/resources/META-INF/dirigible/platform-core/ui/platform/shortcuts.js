/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
angular.module('platformShortcuts', []).factory('shortcuts', ['$document', function ($document) {
    // Factory is based on example code from zachsnow
    let shortcuts = [];
    let ignoreInputs = false;
    let separateCtrl = false;
    const charKeyCodes = { 'f1': 112, 'f2': 113, 'f3': 114, 'f4': 115, 'f5': 116, 'f6': 117, 'f7': 118, 'f8': 119, 'f9': 120, 'f10': 121, 'f11': 122, 'f12': 123, '0': 58, '1': 49, '2': 50, '3': 51, '4': 52, '5': 53, '6': 54, '7': 55, '8': 56, '9': 57, 'backspace': 8, 'delete': 46, 'tab': 9, 'enter': 13, 'return': 13, 'esc': 27, 'space': 32, 'left': 37, 'up': 38, 'right': 39, 'down': 40, ';': 186, '=': 187, ',': 188, '-': 189, '.': 190, '/': 191, '`': 192, '[': 219, '\\': 220, ']': 221, "'": 222, 'a': 65, 'b': 66, 'c': 67, 'd': 68, 'e': 69, 'f': 70, 'g': 71, 'h': 72, 'i': 73, 'j': 74, 'k': 75, 'l': 76, 'm': 77, 'n': 78, 'o': 79, 'p': 80, 'q': 81, 'r': 82, 's': 83, 't': 84, 'u': 85, 'v': 86, 'w': 87, 'x': 88, 'y': 89, 'z': 90 };
    const keyCodeChars = { 112: 'f1', 113: 'f2', 114: 'f3', 115: 'f4', 116: 'f5', 117: 'f6', 118: 'f7', 119: 'f8', 120: 'f9', 121: 'f10', 122: 'f11', 123: 'f12', 8: 'backspace', 46: 'delete', 9: 'tab', 13: 'return', 27: 'esc', 32: 'space', 37: 'left', 38: 'up', 39: 'right', 40: 'down', 49: '1', 50: '2', 51: '3', 52: '4', 53: '5', 54: '6', 55: '7', 56: '8', 57: '9', 58: '0', 65: 'a', 66: 'b', 67: 'c', 68: 'd', 69: 'e', 70: 'f', 71: 'g', 72: 'h', 73: 'i', 74: 'j', 75: 'k', 76: 'l', 77: 'm', 78: 'n', 79: 'o', 80: 'p', 81: 'q', 82: 'r', 83: 's', 84: 't', 85: 'u', 86: 'v', 87: 'w', 88: 'x', 89: 'y', 90: 'z', 186: ';', 187: '=', 188: ',', 189: '-', 190: '.', 191: '/', 192: '`', 219: '[', 220: '\\', 221: ']', 222: "'" };
    const modifierKeys = { 'shift': 'shift', 'ctrl': 'ctrl', 'meta': 'meta', 'alt': 'alt' };

    function parseKeySet(keySet) {
        let names;
        let keys = {};
        if (navigator.userAgent.includes('Mac') && !separateCtrl) {
            keySet = keySet.replaceAll('ctrl', 'meta').toLowerCase();
            names = keySet.split('+');
        } else names = keySet.split('+');
        for (const name in modifierKeys) {
            keys[modifierKeys[name]] = false;
        }
        for (const name in names) {
            const modifierKey = modifierKeys[names[name]];
            if (modifierKey) keys[modifierKey] = true;
            else {
                keys.keyCode = charKeyCodes[names[name]];
                if (!keys.keyCode) return;
            }
        }
        return keys;
    };

    function parseEvent(e) {
        return {
            keyCode: charKeyCodes[keyCodeChars[e.which]],
            meta: e.metaKey || false,
            alt: e.altKey || false,
            ctrl: e.ctrlKey || false,
            shift: e.shiftKey || false,
        };
    }

    function match(k1, k2) {
        return (
            k1.keyCode === k2.keyCode &&
            k1.ctrl === k2.ctrl &&
            k1.alt === k2.alt &&
            k1.meta === k2.meta &&
            k1.shift === k2.shift
        );
    };

    $document.bind('keydown', function (e) {
        if (ignoreInputs && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA')) return;
        let eventKeys = parseEvent(e);
        let shortcut;
        for (let i = 0; i < shortcuts.length; i++) {
            shortcut = shortcuts[i];
            if (match(eventKeys, shortcut.keys)) {
                if (shortcut.action) shortcut.action(shortcut.keySet, e);
                return;
            }
        }
    });

    return {
        ignoreInputs: function (ignore) {
            ignoreInputs = ignore;
        },
        separateCtrl: function (separate) {
            separateCtrl = separate;
        },
        shortcuts: shortcuts,
        register: function (shortcut) {
            shortcut.keys = parseKeySet(shortcut.keySet);
            if (!shortcut.keys) return;
            shortcuts.push(shortcut);
            return shortcut;
        },
        unregister: function (shortcut) {
            overwriteWithout(shortcuts, shortcut);
        }
    };
}]).directive('shortcut', ['shortcuts', function (shortcuts) {
    /**
     * Directive is based on example code from zachsnow 
     * How to use:
     * <div shortcut="'ctrl+s|ctrl+k'" shortcut-action="save" ignore-inputs separate-ctrl>
     * Options:
     * * shortcut - String containing the shortcut or shortcuts. There can be multiple shortcuts for a single action. You can separate the shortcuts using '|'.
     * * shortcut-action - The name of the function that will get called. It has two parameters:
     * * * keySet - The keyboard shortcut activated.
     * * * event - The JavaScript key event.
     * * shortcut-desc - Description of the shortcut(s).
     * * ignore-inputs - If this attribute is present, then events from 'input' and 'textarea' controls will be ignored.
     * * separate-ctrl - On macOS, by default, the ctrl key is replaced with the meta (cmd) key, so shortcuts like 'Ctrl+S' are automatically translated to 'Cmd+S'.
     * If you want the ctrl key to match the ctrl and be separate from the meta on a mac, then use this attribute.
     * If you want to register someting mac-specific to the Cmd key, then use 'meta' instead of 'ctrl'.
     */
    return {
        restrict: 'A',
        link: function (scope, _element, attrs) {
            let shortcutKeySets = scope.$eval(attrs.shortcut);
            const ignoreInputs = 'ignoreInputs' in attrs;
            const separateCtrl = 'separateCtrl' in attrs;
            shortcuts.ignoreInputs(ignoreInputs);
            shortcuts.separateCtrl(separateCtrl);
            if (shortcutKeySets !== undefined) {
                shortcutKeySets = shortcutKeySets.split('|');
                const action = scope.$eval(attrs.shortcutAction);
                let shortcutList = [];
                for (let i = 0; i < shortcutKeySets.length; i++) {
                    shortcutList.push(shortcuts.register({
                        keySet: shortcutKeySets[i],
                        action: action,
                        description: attrs.shortcutDesc || ''
                    }));
                }
                scope.$on('$destroy', function () {
                    for (let i = 0; i < shortcutList.length; i++) {
                        shortcuts.unregister(shortcutList[i]);
                    }
                });
            }
        }
    }
}]);