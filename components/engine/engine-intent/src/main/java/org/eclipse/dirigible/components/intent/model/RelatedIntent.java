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
 * A read-only register of the records that REFERENCE the enclosing entity - the reverse of an
 * incoming to-one association, declared on the referenced side.
 *
 * <p>
 * A generated entity page shows its own fields, and a document shows its composition items; an
 * entity that is the TARGET of associations had no way to show the records pointing at it (a
 * project-month and its per-employee timesheet lines, a customer and its invoices, an account and
 * its journal entries). This declares one such register:
 *
 * <pre>
 * related:
 *   - entity: EmployeeTimesheet
 *     model: employee-timesheets   # omit for a source in this same model
 *     via: projectTimesheet        # omit when the source has exactly one relation pointing here
 *     label: Employee Timesheets
 *     show: [number, employee, totalHours, status]
 * </pre>
 *
 * <p>
 * The declaration lives on the REFERENCED entity and not on the referencing relation because
 * generation is per model and leaf-first: the owner of the register is generated before - and in
 * general knows nothing about - the models that reference it, so only the referenced side can say
 * "show these here".
 *
 * <p>
 * It is a window, never an owner: the referencing records have their own lifecycle, pages and
 * processes, so the register lists them read-only and each row opens the source's own record page.
 * Composition children stay the document items register, which is edited in place.
 */
public class RelatedIntent {

    /** The referencing entity whose records are listed. Required. */
    private String entity;

    /**
     * The {@code uses[].model} alias of the model that owns {@link #entity}; null/blank means the
     * source is declared in this same model.
     */
    private String model;

    /**
     * The source's to-one relation that points back at the enclosing entity. Optional when the source
     * declares exactly one such relation; required to disambiguate when it declares several (an invoice
     * referencing the same company as both issuer and recipient).
     */
    private String via;

    /**
     * The register's heading. Absent → the humanized, pluralized {@link #entity} name ("Employee
     * Timesheets").
     */
    private String label;

    /**
     * The source's field / relation names to show as columns, in this order. Absent → the source's own
     * list columns (its major properties, minus the foreign key back to this entity).
     */
    private List<String> show = new ArrayList<>();

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /** Whether the source entity is owned by another model (a {@code uses:} alias). */
    public boolean isCrossModel() {
        return model != null && !model.isBlank();
    }

    public String getVia() {
        return via;
    }

    public void setVia(String via) {
        this.via = via;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getShow() {
        return show;
    }

    public void setShow(List<String> show) {
        this.show = show == null ? new ArrayList<>() : show;
    }
}
