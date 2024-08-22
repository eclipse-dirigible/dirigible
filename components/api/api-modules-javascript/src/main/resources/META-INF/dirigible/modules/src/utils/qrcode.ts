/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2023 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */

import * as bytes from "sdk/io/bytes";
const QRCodeFacade = Java.type("org.eclipse.dirigible.components.api.utils.QRCodeFacade");

export class QRCode {

    public static generateQRCode(text: string): any[] {
        return bytes.toJavaScriptBytes(QRCodeFacade.generateQRCode(text));
    }
}

// @ts-ignore
if (typeof module !== 'undefined') {
    // @ts-ignore
    module.exports = QRCode;
}
