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

/**
 * Where an {@link OutboundIntent} departs to: a {@link #queue} (point-to-point - one consumer
 * receives it) or a {@link #topic} (pub/sub - every subscriber does). Exactly one of the two is
 * declared, mirroring the arrival rule of {@link InboundSourceIntent}: two channels would be two
 * departures wearing one name, and neither is a promise with nowhere to land.
 */
public class OutboundTargetIntent {

    private String queue;
    private String topic;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
