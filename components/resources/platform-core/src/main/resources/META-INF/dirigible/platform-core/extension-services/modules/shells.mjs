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
import { extensions } from '@aerokit/sdk/extensions';

function sortShells(a, b) {
    if (a.order !== undefined && b.order !== undefined) {
        return (parseInt(a.order) - parseInt(b.order));
    } else if (a.order === undefined && b.order === undefined) {
        return a.label.toLowerCase().localeCompare(b.label.toLowerCase());
    } else if (a.order === undefined) {
        return 1;
    } else if (b.order === undefined) {
        return -1;
    }
    return 0;
}

export async function getShells(extensionPoints = []) {
    const shells = [];
    const shellExtensions = [];
    for (let i = 0; i < extensionPoints.length; i++) {
        const extensionList = await Promise.resolve(extensions.loadExtensionModules(extensionPoints[i]));
        shellExtensions.push(...extensionList);
    }

    shellLoop: for (let i = 0; i < shellExtensions?.length; i++) {
        const shell = shellExtensions[i].getShell();
        if (!shell.id) {
            console.error(`Shell ['${shell.label || shell.path}'] does not have an id.`);
        } else if (!shell.label) {
            console.error(`Shell ['${shell.id}'] does not have a label.`);
        } else if (!shell.path) {
            console.error(`Shell ['${shell.id}'] does not have a path.`);
        } else {
            for (let v = 0; v < shells.length; v++) {
                if (shells[v].id === shell.id) {
                    console.error(`Duplication at shell with id: ['${shells[v].id}'] pointing to paths: ['${shells[v].path}'] and ['${shell.path}']`);
                    continue shellLoop;
                }
            }
            shells.push(shell);
        }
    }

    return shells.sort(sortShells);
}
