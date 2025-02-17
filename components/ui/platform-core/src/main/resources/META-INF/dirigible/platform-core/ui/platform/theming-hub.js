/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
class ThemingHub extends MessageHubApi {

    getSavedTheme() {
        return JSON.parse(localStorage.getItem(`${brandingInfo.keyPrefix}.platform.theme`) || '{}');
    }

    setSavedTheme(theme) {
        localStorage.setItem(`${brandingInfo.keyPrefix}.platform.theme`, JSON.stringify(theme));
    }

    /**
     * Triggered when the theme list is loaded.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onThemesLoaded(handler) {
        return this.addMessageListener({ topic: 'platform.shell.themes.loaded', handler: handler });
    }

    /**
     * Trigger the theme list loaded event.
     */
    themesLoaded() {
        this.triggerEvent('platform.shell.themes.loaded');
    }

    /**
     * Triggered when the theme is changed.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onThemeChange(handler) {
        return this.addMessageListener({ topic: 'platform.shell.themes.change', handler: handler });
    }

    /**
     * Trigger the theme change event.
     * @param {string} id - ID of the theme.
     * @param {('light'|'dark'|'auto')} type - Type of the theme.
     * @param {Array.<string>} links - Links to the theme css files.
     */ // @ts-ignore
    themeChanged({ id, type, links } = {}) {
        this.postMessage({
            topic: 'platform.shell.themes.change', data: {
                id: id,
                type: type,
                links: links,
            }
        });
    }
}