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
class ContextMenuHub extends MessageHubApi {
    /**
     * Definition of a menu item object.
     * @typedef {Object} MenuItem
     * @property {string} id - Id of the item.
     * @property {string} label - Label for the item.
     * @property {string} [leftIconClass] - CSS icon class. Icon will be shown before the label.
     * @property {string} [rightIconClass] - CSS icon class. Icon will be shown after the label.
     * @property {string} [leftIconPath] - Icon url path. Icon will be shown before the label.
     * @property {string} [rightIconPath] - Icon url path. Icon will be shown after the label.
     * @property {string} [shortcut] - Secondary text. Most often used as a shotcut hint.
     * @property {boolean} [separator] - Set a menu item separator after this item.
     * @property {boolean} [disabled] - Disable the menu item.
     */

    /**
     * Definition of a menu sublist object.
     * @typedef {Object} MenuSublist
     * @property {string} id - Id of the sublist.
     * @property {string} label - Label for the sublist.
     * @property {boolean} [separator] - Set a menu item separator after this item.
     * @property {string} [iconClass] - CSS icon class. Icon will be shown before the label.
     * @property {string} [iconPath] - Icon url path. Icon will be shown before the label.
     * @property {Array.<MenuItem|MenuSublist>} items - List of menu items and/or sublists.
     * @property {boolean} [disabled] - Disable the sublist.
     */

    /**
     * Shows a contextmenu.
     * @param {string} ariaLabel - Accessibility text.
     * @param {number} posX - The position of the cursor at the X axis.
     * @param {number} posY - The position of the cursor at the Y axis.
     * @param {boolean} [icons=false] - If the contextmenu items have icons.
     * @param {Array.<MenuItem|MenuSublist>} items - List of menu items and/or sublists.
     * @return {Promise} - Returns a promise and gives the menu item id as a parameter. If the user has closed the menu without selecting an item, you will receive undefined.
     */ // @ts-ignore
    showContextMenu({ ariaLabel, posX, posY, icons, items } = {}) {
        let absoluteX = posX;
        let absoluteY = posY;
        if (window.frameElement) {
            let current = window;
            while (current !== top) {
                const frame = current.frameElement.getBoundingClientRect();
                absoluteX += frame.x;
                absoluteY += frame.y;
                current = current.parent;
            }
        }
        const callbackTopic = `platform.contextmenu.${new Date().valueOf()}`;
        this.postMessage({
            topic: 'platform.contextmenu',
            data: {
                ariaLabel: ariaLabel,
                posX: absoluteX,
                posY: absoluteY,
                icons: icons ?? false,
                items: items,
                topic: callbackTopic
            }
        });
        return new Promise((resolve) => {
            const callbackListener = this.addMessageListener({
                topic: callbackTopic,
                handler: (data) => {
                    this.removeMessageListener(callbackListener);
                    resolve(data);
                }
            });
        });
    }

    /**
     * Triggered when a context menu should be shown.
     * @param handler - Callback function.
     * @returns - A reference to the listener. In order to remove/disable the listener, you need to use this reference and pass it to the 'removeMessageListener' function.
     */
    onContextMenu(handler) {
        return this.addMessageListener({ topic: 'platform.contextmenu', handler: handler });
    }
}