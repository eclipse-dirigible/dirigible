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
 * A hand-held sender, so the queue arrival can be tried by hand.
 *
 * <p>
 * The platform's broker listens on {@code vm://localhost} only - there is no TCP transport - so
 * nothing outside the running instance can publish to a queue, and the message has to originate from
 * inside it. This posts whatever envelope it is given onto the queue the intent's
 * {@code assignmentRequests} arrival consumes, which is the whole of what it is for.
 *
 * <p>
 * It exists in {@code custom/} because it belongs to nobody's model: it is a test affordance, not part
 * of the application. Generate never touches this folder, so it survives regeneration.
 *
 * <pre>
 * curl -u admin:admin -H 'Content-Type: application/json' \
 *   -d '{"messageId":"m-1","type":"user.assignment.requested","version":1,
 *        "tenantId":"acme","email":"new.user@example.com","role":"User","seatCount":3}' \
 *   http://localhost:8080/services/java/sample-intent-inbound-mapping/custom/AssignmentRequestSender/send
 * </pre>
 */
@Controller
public class AssignmentRequestSender {

    /** The destination the intent's queue arrival binds to. */
    private static final String QUEUE = "codbex.user-assignment-requests";

    private static final Logger LOG = Logging.getLogger("custom.AssignmentRequestSender");

    @Post("/send")
    public String send(@Body Map<String, Object> envelope) {
        Response.setContentType("application/json");
        if (envelope == null || envelope.isEmpty()) {
            Response.setStatus(400);
            return "{\"error\": \"post the envelope to publish\"}";
        }
        String message = Json.stringify(envelope);
        Producer.sendToQueue(QUEUE, message);
        // Logged because the consumer's own outcome is a log line too, so both halves read together.
        LOG.info("Published to [{}]: {}", QUEUE, message);
        return "{\"published\": \"" + QUEUE + "\"}";
    }
}
