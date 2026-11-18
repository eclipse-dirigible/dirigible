/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * An event type a webhook subscription may name, as an application declared it.
 *
 * @param type the type: the topic the entity event is published on
 * @param action {@code created}, {@code updated}, {@code deleted} or {@code transitioned}
 * @param project the project that declares it
 * @param entity the entity
 * @param idProperty the entity's identifier property, carried as {@code data.id}
 * @param uuidProperty the entity's {@code Uuid} property, carried as {@code data.uuid}, or
 *        {@code null}
 * @param statusProperty the entity's {@code function: EntityStatus} property, carried as
 *        {@code data.status}, or {@code null}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record WebhookEventType(String type, String action, String project, String entity, @JsonIgnore String idProperty,
        @JsonIgnore String uuidProperty, @JsonIgnore String statusProperty) {
}
