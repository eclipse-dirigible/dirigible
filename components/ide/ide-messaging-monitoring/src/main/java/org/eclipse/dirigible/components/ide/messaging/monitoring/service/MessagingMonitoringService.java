/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.messaging.monitoring.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.jms.JMSException;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.DestinationStatistics;
import org.apache.activemq.broker.region.Queue;
import org.apache.activemq.broker.region.Region;
import org.apache.activemq.broker.region.RegionBroker;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.command.Message;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.util.ByteSequence;
import org.eclipse.dirigible.components.ide.messaging.monitoring.dto.BrokerSummary;
import org.eclipse.dirigible.components.ide.messaging.monitoring.dto.DestinationSummary;
import org.eclipse.dirigible.components.ide.messaging.monitoring.dto.MessageDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads broker-side state straight from the embedded ActiveMQ {@link BrokerService} — no JMX, since
 * the broker is started with JMX disabled. Queues expose a {@code browse()} accessor that returns
 * the in-memory pending set; topics do not retain non-persistent messages, so the topic browser
 * only reports current subscriber/producer counts.
 */
@Service
public class MessagingMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MessagingMonitoringService.class);

    /** Bytes after which the rendered message body is truncated in the browser preview. */
    private static final int DEFAULT_BODY_PREVIEW_BYTES = 64 * 1024;

    /** Default max number of messages returned by a single browse call. */
    private static final int DEFAULT_BROWSE_LIMIT = 200;

    private final BrokerService brokerService;

    public MessagingMonitoringService(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    /**
     * Snapshots every queue and topic visible to the broker, with their live counters. The collection
     * is sorted alphabetically by destination name so the UI renders deterministically across
     * refreshes.
     */
    public BrokerSummary summary() {
        RegionBroker region = regionBroker();
        if (region == null) {
            return new BrokerSummary(System.currentTimeMillis(), brokerService.getBrokerName(), brokerService.isStarted(),
                    Collections.emptyList(), Collections.emptyList());
        }
        List<DestinationSummary> queues = summarize(region.getQueueRegion(), "queue");
        List<DestinationSummary> topics = summarize(region.getTopicRegion(), "topic");
        return new BrokerSummary(System.currentTimeMillis(), brokerService.getBrokerName(), brokerService.isStarted(), queues, topics);
    }

    /**
     * Browses up to {@code limit} messages currently pending on {@code queueName} without consuming
     * them. A non-positive {@code limit} maps to {@link #DEFAULT_BROWSE_LIMIT}.
     */
    public List<MessageDetail> browseQueue(String queueName, int limit) {
        Destination destination = findDestination(new ActiveMQQueue(queueName));
        if (destination == null) {
            return Collections.emptyList();
        }
        Message[] messages = destination.browse();
        int max = limit > 0 ? Math.min(limit, messages.length) : Math.min(DEFAULT_BROWSE_LIMIT, messages.length);
        List<MessageDetail> result = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            result.add(toDetail(messages[i]));
        }
        return result;
    }

    /**
     * Discards every pending message on {@code queueName}. Returns the number of messages that were
     * dropped, or {@code -1} when the queue does not exist.
     */
    public long purgeQueue(String queueName) throws Exception {
        Destination destination = findDestination(new ActiveMQQueue(queueName));
        if (!(destination instanceof Queue queue)) {
            return -1L;
        }
        long size = queue.getDestinationStatistics()
                         .getMessages()
                         .getCount();
        queue.purge();
        return size;
    }

    /**
     * Removes a single pending message from {@code queueName} by its JMS message id. Returns
     * {@code true} if the message was found and removed.
     */
    public boolean removeMessage(String queueName, String messageId) throws Exception {
        Destination destination = findDestination(new ActiveMQQueue(queueName));
        if (!(destination instanceof Queue queue)) {
            return false;
        }
        return queue.removeMessage(messageId);
    }

    /**
     * Removes the queue from the broker. The call is rejected (returns {@code false}) if a consumer is
     * still subscribed, to avoid surprising live listeners.
     */
    public boolean removeQueue(String queueName) throws Exception {
        return removeDestination(new ActiveMQQueue(queueName));
    }

    /**
     * Removes the topic from the broker. Rejected when subscribers are still attached.
     */
    public boolean removeTopic(String topicName) throws Exception {
        return removeDestination(new ActiveMQTopic(topicName));
    }

    private boolean removeDestination(ActiveMQDestination destination) throws Exception {
        Destination existing = findDestination(destination);
        if (existing == null) {
            return false;
        }
        long consumers = existing.getDestinationStatistics()
                                 .getConsumers()
                                 .getCount();
        if (consumers > 0) {
            throw new IllegalStateException(
                    "Destination [" + destination.getPhysicalName() + "] still has " + consumers + " consumer(s) — refusing to remove");
        }
        brokerService.removeDestination(destination);
        return true;
    }

    private List<DestinationSummary> summarize(Region region, String type) {
        if (region == null) {
            return Collections.emptyList();
        }
        Map<ActiveMQDestination, Destination> destinations = region.getDestinationMap();
        List<DestinationSummary> summaries = new ArrayList<>(destinations.size());
        for (Destination destination : destinations.values()) {
            summaries.add(toSummary(destination, type));
        }
        summaries.sort(Comparator.comparing(DestinationSummary::name, String.CASE_INSENSITIVE_ORDER));
        return summaries;
    }

    private static DestinationSummary toSummary(Destination destination, String type) {
        DestinationStatistics stats = destination.getDestinationStatistics();
        MemoryUsage memory = destination.getMemoryUsage();
        long memUsed = memory != null ? memory.getUsage() : -1L;
        long memLimit = memory != null ? memory.getLimit() : -1L;
        return new DestinationSummary(destination.getName(), type, stats.getMessages()
                                                                        .getCount(),
                stats.getEnqueues()
                     .getCount(),
                stats.getDequeues()
                     .getCount(),
                stats.getDispatched()
                     .getCount(),
                stats.getInflight()
                     .getCount(),
                stats.getExpired()
                     .getCount(),
                stats.getConsumers()
                     .getCount(),
                stats.getProducers()
                     .getCount(),
                memUsed, memLimit);
    }

    private static MessageDetail toDetail(Message message) {
        String id = message.getMessageId() != null ? message.getMessageId()
                                                            .toString()
                : null;
        String bodyKind = message.getClass()
                                 .getSimpleName();
        String body = "";
        boolean truncated = false;
        long sizeBytes = 0L;
        if (message instanceof ActiveMQTextMessage textMessage) {
            try {
                String text = textMessage.getText();
                if (text != null) {
                    sizeBytes = text.length();
                    if (text.length() > DEFAULT_BODY_PREVIEW_BYTES) {
                        body = text.substring(0, DEFAULT_BODY_PREVIEW_BYTES);
                        truncated = true;
                    } else {
                        body = text;
                    }
                }
            } catch (JMSException ex) {
                logger.debug("Failed to read text body of message [{}]", id, ex);
                body = "<unable to read text body: " + ex.getMessage() + ">";
            }
        } else {
            ByteSequence content = message.getContent();
            if (content != null) {
                sizeBytes = content.getLength();
                body = previewBytes(content);
                truncated = content.getLength() > DEFAULT_BODY_PREVIEW_BYTES;
            }
        }
        Map<String, Object> properties = readProperties(message, id);
        return new MessageDetail(id, message.getCorrelationId(), message.getType(), bodyKind, body, truncated, message.getTimestamp(),
                message.getExpiration(), message.getPriority(), message.getRedeliveryCounter(), message.isPersistent(), sizeBytes,
                properties);
    }

    private static Map<String, Object> readProperties(Message message, String messageId) {
        try {
            Map<String, Object> raw = message.getProperties();
            if (raw == null || raw.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, Object> result = new LinkedHashMap<>(raw.size());
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                Object value = entry.getValue();
                result.put(entry.getKey(), value == null ? null : value.toString());
            }
            return result;
        } catch (IOException ex) {
            logger.debug("Failed to read properties of message [{}]", messageId, ex);
            return Collections.emptyMap();
        }
    }

    /**
     * Renders a binary payload as best-effort UTF-8 when the bytes look textual, otherwise base64. The
     * first {@value #DEFAULT_BODY_PREVIEW_BYTES} bytes are inspected.
     */
    private static String previewBytes(ByteSequence content) {
        int length = Math.min(content.getLength(), DEFAULT_BODY_PREVIEW_BYTES);
        byte[] sliced = new byte[length];
        System.arraycopy(content.getData(), content.getOffset(), sliced, 0, length);
        if (isProbablyText(sliced)) {
            return new String(sliced, StandardCharsets.UTF_8);
        }
        return "base64:" + Base64.getEncoder()
                                 .encodeToString(sliced);
    }

    private static boolean isProbablyText(byte[] bytes) {
        if (bytes.length == 0) {
            return true;
        }
        int printable = 0;
        for (byte b : bytes) {
            if (b == 0) {
                return false;
            }
            if (b == 0x09 || b == 0x0A || b == 0x0D || (b >= 0x20 && b < 0x7F) || (b & 0xFF) >= 0x80) {
                printable++;
            }
        }
        return printable * 10 >= bytes.length * 9;
    }

    private Destination findDestination(ActiveMQDestination destination) {
        RegionBroker region = regionBroker();
        if (region == null) {
            return null;
        }
        Region target = destination.isQueue() ? region.getQueueRegion() : region.getTopicRegion();
        if (target == null) {
            return null;
        }
        Map<ActiveMQDestination, Destination> destinations = target.getDestinationMap();
        Destination match = destinations.get(destination);
        if (match != null) {
            return match;
        }
        for (Destination candidate : destinations.values()) {
            if (candidate.getName()
                         .equals(destination.getPhysicalName())) {
                return candidate;
            }
        }
        return null;
    }

    private RegionBroker regionBroker() {
        if (!brokerService.isStarted()) {
            logger.debug("Broker is not started yet — no destinations to report");
            return null;
        }
        Broker broker = brokerService.getRegionBroker();
        if (broker instanceof RegionBroker region) {
            return region;
        }
        Broker delegate = broker;
        while (delegate != null) {
            if (delegate instanceof RegionBroker region) {
                return region;
            }
            Broker next = delegate.getAdaptor(RegionBroker.class);
            if (next == delegate) {
                return null;
            }
            delegate = next;
        }
        return null;
    }

}
