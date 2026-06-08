/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.messaging.monitoring.endpoint;

import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.ide.messaging.monitoring.dto.BrokerSummary;
import org.eclipse.dirigible.components.ide.messaging.monitoring.dto.MessageDetail;
import org.eclipse.dirigible.components.ide.messaging.monitoring.service.MessagingMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes the embedded ActiveMQ broker for read-only inspection plus a few targeted devops actions
 * (purge, delete) needed when debugging queue/topic behaviour from the browser IDE.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "messaging-monitoring")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER", "OPERATOR"})
public class MessagingMonitoringEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(MessagingMonitoringEndpoint.class);

    private final MessagingMonitoringService monitoringService;

    public MessagingMonitoringEndpoint(MessagingMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping(value = "/summary", produces = "application/json")
    public ResponseEntity<BrokerSummary> summary() {
        return ResponseEntity.ok(monitoringService.summary());
    }

    @GetMapping(value = "/queues/{name}/messages", produces = "application/json")
    public ResponseEntity<List<MessageDetail>> browseQueue(@PathVariable("name") String queueName,
            @RequestParam(value = "limit", required = false, defaultValue = "200") int limit) {
        return ResponseEntity.ok(monitoringService.browseQueue(queueName, limit));
    }

    @DeleteMapping(value = "/queues/{name}/messages", produces = "application/json")
    public ResponseEntity<Map<String, Object>> purgeQueue(@PathVariable("name") String queueName) {
        try {
            long purged = monitoringService.purgeQueue(queueName);
            if (purged < 0) {
                throw notFound("Queue [" + queueName + "] not found");
            }
            return ResponseEntity.ok(Map.of("queue", queueName, "purged", purged));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failed to purge queue [{}]", queueName, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to purge queue: " + ex.getMessage(), ex);
        }
    }

    @DeleteMapping(value = "/queues/{name}/messages/{messageId}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> removeMessage(@PathVariable("name") String queueName,
            @PathVariable("messageId") String messageId) {
        try {
            boolean removed = monitoringService.removeMessage(queueName, messageId);
            if (!removed) {
                throw notFound("Message [" + messageId + "] not found on queue [" + queueName + "]");
            }
            return ResponseEntity.ok(Map.of("queue", queueName, "messageId", messageId, "removed", true));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failed to remove message [{}] from queue [{}]", messageId, queueName, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove message: " + ex.getMessage(), ex);
        }
    }

    @DeleteMapping(value = "/queues/{name}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> deleteQueue(@PathVariable("name") String queueName) {
        try {
            boolean removed = monitoringService.removeQueue(queueName);
            if (!removed) {
                throw notFound("Queue [" + queueName + "] not found");
            }
            return ResponseEntity.ok(Map.of("queue", queueName, "removed", true));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failed to remove queue [{}]", queueName, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove queue: " + ex.getMessage(), ex);
        }
    }

    @DeleteMapping(value = "/topics/{name}", produces = "application/json")
    public ResponseEntity<Map<String, Object>> deleteTopic(@PathVariable("name") String topicName) {
        try {
            boolean removed = monitoringService.removeTopic(topicName);
            if (!removed) {
                throw notFound("Topic [" + topicName + "] not found");
            }
            return ResponseEntity.ok(Map.of("topic", topicName, "removed", true));
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.warn("Failed to remove topic [{}]", topicName, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove topic: " + ex.getMessage(), ex);
        }
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
