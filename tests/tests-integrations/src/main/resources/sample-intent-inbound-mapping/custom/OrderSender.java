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

import java.util.Map;

import org.eclipse.dirigible.sdk.http.Body;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Post;
import org.eclipse.dirigible.sdk.http.Response;
import org.eclipse.dirigible.sdk.log.Logger;
import org.eclipse.dirigible.sdk.log.Logging;
import org.eclipse.dirigible.sdk.messaging.Producer;
import org.eclipse.dirigible.sdk.utils.Json;

/**
 * A hand-held sender for the {@code orders} arrival, for the same reason as
 * {@link AssignmentRequestSender}: the platform's broker listens on {@code vm://localhost} only, so a
 * message has to originate from inside the running instance.
 *
 * <pre>
 * curl -u admin:admin -H 'Content-Type: application/json' \
 *   -d '{"orderNumber":"PO-1","lines":[{"sku":"A","qty":2},{"sku":"B","qty":1}],"tags":["rush","gift"]}' \
 *   http://localhost:8080/services/java/sample-intent-inbound-mapping/custom/OrderSender/send
 * </pre>
 */
@Controller
public class OrderSender {

    /** The destination the intent's orders arrival binds to. */
    private static final String QUEUE = "sample.orders";

    private static final Logger LOG = Logging.getLogger("custom.OrderSender");

    @Post("/send")
    public String send(@Body Map<String, Object> order) {
        Response.setContentType("application/json");
        if (order == null || order.isEmpty()) {
            Response.setStatus(400);
            return "{\"error\": \"post the order to publish\"}";
        }
        String message = Json.stringify(order);
        Producer.sendToQueue(QUEUE, message);
        LOG.info("Published to [{}]: {}", QUEUE, message);
        return "{\"published\": \"" + QUEUE + "\"}";
    }
}
