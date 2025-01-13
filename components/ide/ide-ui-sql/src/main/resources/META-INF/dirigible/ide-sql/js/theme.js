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
let monacoTheme = 'vs-light';
let headElement = document.getElementsByTagName('head')[0];

function setTheme(init = true) {
    let theme = JSON.parse(localStorage.getItem('DIRIGIBLE.theme') || '{}');
    if (theme.type === 'light') monacoTheme = 'vs-light';
    else monacoTheme = 'quartz-dark';
    if (theme.links) {
        if (!init) {
            let themeLinks = headElement.querySelectorAll("link[data-type='theme']");
            for (let i = 0; i < themeLinks.length; i++) {
                headElement.removeChild(themeLinks[i]);
            }
        }
        for (let i = 0; i < theme.links.length; i++) {
            const link = document.createElement('link');
            link.type = 'text/css';
            link.href = theme.links[i];
            link.rel = 'stylesheet';
            link.setAttribute("data-type", "theme");
            headElement.appendChild(link);
        }
    }
}