/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package custom;

import org.eclipse.dirigible.components.data.store.java.repository.Criteria;
import org.eclipse.dirigible.sdk.log.Logger;
import org.eclipse.dirigible.sdk.log.Logging;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import gen.assignments.data.purchaseorder.PurchaseOrderLineRepository;
import gen.assignments.data.purchaseorder.PurchaseOrderRepository;

/**
 * The CountLines process's only step: it counts the order's lines and writes the count onto the
 * order as LinesSeen.
 *
 * <p>
 * The process is started by the order's create event, and an arrival writes the order and its lines
 * in one transaction whose events are dispatched after the commit. So the count this step writes is
 * the number of lines the arrival carried - a step that found fewer would mean the process started
 * before the lines were written, which is exactly what the sample is here to rule out.
 */
public class CountLinesTask implements JavaDelegate {

    private static final Logger LOG = Logging.getLogger("custom.CountLinesTask");

    @Override
    public void execute(DelegateExecution execution) {
        Object key = execution.getVariable("Id");
        if (!(key instanceof Number id)) {
            LOG.warn("CountLines started without an order id - nothing counted");
            return;
        }
        int lines = new PurchaseOrderLineRepository().findAll(Criteria.create()
                                                                      .eq("PurchaseOrder", id.intValue()))
                                                     .size();
        LOG.info("Order {} has {} line(s) when its process starts", id, lines);
        new PurchaseOrderRepository().updateProperty(id.intValue(), "LinesSeen", lines);
    }
}
