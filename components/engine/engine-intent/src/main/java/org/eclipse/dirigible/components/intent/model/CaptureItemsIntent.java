/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The line items of a {@link CaptureIntent}: every line the model reads off the document becomes
 * one row of {@link #into}, a composition child of the captured entity, saved with the record in
 * one unit of work.
 */
public class CaptureItemsIntent {

    private String into;
    private List<CaptureFieldIntent> fields = new ArrayList<>();

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }

    public List<CaptureFieldIntent> getFields() {
        return fields;
    }

    public void setFields(List<CaptureFieldIntent> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
    }
}
