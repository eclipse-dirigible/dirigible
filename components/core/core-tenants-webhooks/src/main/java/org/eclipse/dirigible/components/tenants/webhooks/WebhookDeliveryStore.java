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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * The webhook deliveries and their attempts, in the tenant's own schema next to its event outbox:
 * every operation runs against the default data source, which inside a tenant scope is routed to
 * that tenant, so no tenant column is needed and a delivery can be inserted on the connection of
 * the write it belongs to. The tables are created on first use.
 *
 * <ul>
 * <li>{@value #DELIVERIES} - one row per event and subscription (a resend adds one more with the
 * same event id): the signed body, its state ({@code PENDING}, {@code DELIVERED}, {@code GIVEN_UP})
 * and when it is due;</li>
 * <li>{@value #ATTEMPTS} - one row per attempt: when, the answer's status code, how long it took,
 * and the error.</li>
 * </ul>
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class WebhookDeliveryStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookDeliveryStore.class);

    static final String DELIVERIES = "DIRIGIBLE_WEBHOOK_DELIVERIES";
    static final String ATTEMPTS = "DIRIGIBLE_WEBHOOK_ATTEMPTS";

    private static final String DELIVERY_ID = "DELIVERY_ID";
    private static final String EVENT_ID = "DELIVERY_EVENT_ID";
    private static final String SUBSCRIPTION_ID = "DELIVERY_SUBSCRIPTION_ID";
    private static final String EVENT_TYPE = "DELIVERY_EVENT_TYPE";
    private static final String BODY = "DELIVERY_BODY";
    private static final String CREATED_AT = "DELIVERY_CREATED_AT";
    private static final String ATTEMPT_COUNT = "DELIVERY_ATTEMPTS";
    private static final String NEXT_ATTEMPT_AT = "DELIVERY_NEXT_ATTEMPT_AT";
    private static final String STATE = "DELIVERY_STATE";
    private static final String FINISHED_AT = "DELIVERY_FINISHED_AT";

    private static final String ATTEMPT_ID = "ATTEMPT_ID";
    private static final String ATTEMPT_DELIVERY_ID = "ATTEMPT_DELIVERY_ID";
    private static final String ATTEMPTED_AT = "ATTEMPT_AT";
    private static final String STATUS_CODE = "ATTEMPT_STATUS_CODE";
    private static final String DURATION = "ATTEMPT_DURATION_MS";
    private static final String ERROR = "ATTEMPT_ERROR";

    private static final int MAX_ERROR_LENGTH = 2000;

    /** The state of a delivery. */
    enum State {
        /** Due for an attempt at its next attempt time. */
        PENDING,
        /** Answered with a 2xx. */
        DELIVERED,
        /** Not delivered within the retry window, or its subscription went away. */
        GIVEN_UP
    }

    /**
     * One delivery.
     *
     * @param deliveryId the row's id
     * @param eventId the event's id - the {@code Webhook-Id}, the same for every attempt and resend
     * @param subscriptionId the subscription
     * @param eventType the event type
     * @param body the exact body posted
     * @param createdAt when it was recorded
     * @param attempts how many attempts were made; the claim's optimistic lock
     * @param state the state
     * @param finishedAt when it was delivered or given up
     */
    record Delivery(String deliveryId, String eventId, String subscriptionId, String eventType, String body, Instant createdAt,
            int attempts, State state, Instant finishedAt) {
    }

    /**
     * One attempt.
     *
     * @param attemptedAt when
     * @param statusCode the answer's status code, {@code null} when there was no answer
     * @param durationMillis how long it took
     * @param error why it failed, {@code null} when it succeeded
     */
    record Attempt(Instant attemptedAt, Integer statusCode, long durationMillis, String error) {
    }

    /** Tenants whose tables are known to exist. */
    private final Set<String> preparedTenants = ConcurrentHashMap.newKeySet();

    private final DataSourcesManager dataSourcesManager;

    private final TenantContext tenantContext;

    /**
     * Instantiates the store.
     *
     * @param dataSourcesManager the data sources
     * @param tenantContext the tenant context
     */
    WebhookDeliveryStore(DataSourcesManager dataSourcesManager, TenantContext tenantContext) {
        this.dataSourcesManager = dataSourcesManager;
        this.tenantContext = tenantContext;
    }

    /**
     * Makes sure the current tenant's tables exist, on a connection of its own.
     *
     * @throws SQLException if they cannot be created
     */
    void prepare() throws SQLException {
        String tenant = currentTenantKey();
        if (preparedTenants.contains(tenant)) {
            return;
        }
        try (Connection connection = connection()) {
            createDeliveriesIfAbsent(connection);
            createAttemptsIfAbsent(connection);
        }
        preparedTenants.add(tenant);
    }

    /**
     * Whether the current tenant has the tables - a tenant that never recorded a delivery has none.
     *
     * @return true when they exist
     * @throws SQLException if the lookup fails
     */
    boolean tablesExist() throws SQLException {
        String tenant = currentTenantKey();
        if (preparedTenants.contains(tenant)) {
            return true;
        }
        try (Connection connection = connection()) {
            boolean exist = SqlFactory.getNative(connection)
                                      .existsTable(connection, DELIVERIES)
                    && SqlFactory.getNative(connection)
                                 .existsTable(connection, ATTEMPTS);
            if (exist) {
                preparedTenants.add(tenant);
            }
            return exist;
        }
    }

    /**
     * Records a new pending delivery on the given connection - the write's own, or the resend's.
     *
     * @param connection the connection
     * @param eventId the event id
     * @param subscriptionId the subscription
     * @param eventType the event type
     * @param body the body
     * @param now the moment it is recorded and first due
     * @throws SQLException if the insert fails
     */
    void insert(Connection connection, String eventId, String subscriptionId, String eventType, String body, Instant now)
            throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .insert()
                               .into(DELIVERIES)
                               .column(DELIVERY_ID)
                               .column(EVENT_ID)
                               .column(SUBSCRIPTION_ID)
                               .column(EVENT_TYPE)
                               .column(BODY)
                               .column(CREATED_AT)
                               .column(ATTEMPT_COUNT)
                               .column(NEXT_ATTEMPT_AT)
                               .column(STATE)
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID()
                                       .toString());
            statement.setString(2, eventId);
            statement.setString(3, subscriptionId);
            statement.setString(4, eventType);
            statement.setString(5, body);
            statement.setTimestamp(6, Timestamp.from(now));
            statement.setInt(7, 0);
            statement.setTimestamp(8, Timestamp.from(now));
            statement.setString(9, State.PENDING.name());
            statement.executeUpdate();
        }
    }

    /**
     * Queues the same event once more for a subscription - a manual resend.
     *
     * @param subscriptionId the subscription
     * @param eventId the event id
     * @return whether the event was found
     * @throws SQLException if the read or the insert fails
     */
    boolean resend(String subscriptionId, String eventId) throws SQLException {
        try (Connection connection = connection()) {
            Optional<Delivery> original = find(connection, subscriptionId, eventId);
            if (original.isEmpty()) {
                return false;
            }
            insert(connection, eventId, subscriptionId, original.get()
                                                                .eventType(),
                    original.get()
                            .body(),
                    Instant.now());
            return true;
        }
    }

    /**
     * The pending deliveries due now, oldest first.
     *
     * @param now the moment
     * @param limit the most to read
     * @return the deliveries
     * @throws SQLException if the read fails
     */
    List<Delivery> findDue(Instant now, int limit) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(DELIVERIES)
                                   .where(STATE + " = ?")
                                   .where(NEXT_ATTEMPT_AT + " <= ?")
                                   .order(CREATED_AT)
                                   .limit(limit)
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, State.PENDING.name());
                statement.setTimestamp(2, Timestamp.from(now));
                return deliveries(statement);
            }
        }
    }

    /**
     * Claims a delivery for one attempt: counts the attempt and moves its next attempt past the lease,
     * so a second relay cannot take it meanwhile. The attempt count is the optimistic lock.
     *
     * @param delivery the delivery, with the attempt count it was read with
     * @param leaseUntil when it becomes due again should this attempt never report back
     * @return whether this caller won it
     * @throws SQLException if the update fails
     */
    boolean claim(Delivery delivery, Instant leaseUntil) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(DELIVERIES)
                                   .set(ATTEMPT_COUNT, "?")
                                   .set(NEXT_ATTEMPT_AT, "?")
                                   .where(DELIVERY_ID + " = ?")
                                   .where(ATTEMPT_COUNT + " = ?")
                                   .where(STATE + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, delivery.attempts() + 1);
                statement.setTimestamp(2, Timestamp.from(leaseUntil));
                statement.setString(3, delivery.deliveryId());
                statement.setInt(4, delivery.attempts());
                statement.setString(5, State.PENDING.name());
                return statement.executeUpdate() == 1;
            }
        }
    }

    /**
     * Sets when a pending delivery is attempted next.
     *
     * @param deliveryId the delivery
     * @param nextAttemptAt the moment
     * @throws SQLException if the update fails
     */
    void reschedule(String deliveryId, Instant nextAttemptAt) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(DELIVERIES)
                                   .set(NEXT_ATTEMPT_AT, "?")
                                   .where(DELIVERY_ID + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setTimestamp(1, Timestamp.from(nextAttemptAt));
                statement.setString(2, deliveryId);
                statement.executeUpdate();
            }
        }
    }

    /**
     * Ends a delivery.
     *
     * @param deliveryId the delivery
     * @param state {@code DELIVERED} or {@code GIVEN_UP}
     * @param now the moment
     * @throws SQLException if the update fails
     */
    void finish(String deliveryId, State state, Instant now) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(DELIVERIES)
                                   .set(STATE, "?")
                                   .set(FINISHED_AT, "?")
                                   .where(DELIVERY_ID + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, state.name());
                statement.setTimestamp(2, Timestamp.from(now));
                statement.setString(3, deliveryId);
                statement.executeUpdate();
            }
        }
    }

    /**
     * Logs one attempt of a delivery.
     *
     * @param deliveryId the delivery
     * @param attempt the attempt
     * @throws SQLException if the insert fails
     */
    void recordAttempt(String deliveryId, Attempt attempt) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .insert()
                                   .into(ATTEMPTS)
                                   .column(ATTEMPT_ID)
                                   .column(ATTEMPT_DELIVERY_ID)
                                   .column(ATTEMPTED_AT)
                                   .column(STATUS_CODE)
                                   .column(DURATION)
                                   .column(ERROR)
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, UUID.randomUUID()
                                           .toString());
                statement.setString(2, deliveryId);
                statement.setTimestamp(3, Timestamp.from(attempt.attemptedAt()));
                if (attempt.statusCode() == null) {
                    statement.setNull(4, Types.INTEGER);
                } else {
                    statement.setInt(4, attempt.statusCode());
                }
                statement.setLong(5, attempt.durationMillis());
                String error = attempt.error();
                statement.setString(6, error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH)));
                statement.executeUpdate();
            }
        }
    }

    /**
     * The newest deliveries of a subscription.
     *
     * @param subscriptionId the subscription
     * @param limit the most to read
     * @return the deliveries, newest first
     * @throws SQLException if the read fails
     */
    List<Delivery> log(String subscriptionId, int limit) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(DELIVERIES)
                                   .where(SUBSCRIPTION_ID + " = ?")
                                   .order(CREATED_AT, false)
                                   .limit(limit)
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, subscriptionId);
                return deliveries(statement);
            }
        }
    }

    /**
     * The attempts of a delivery.
     *
     * @param deliveryId the delivery
     * @return the attempts, oldest first
     * @throws SQLException if the read fails
     */
    List<Attempt> attempts(String deliveryId) throws SQLException {
        try (Connection connection = connection()) {
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(ATTEMPTS)
                                   .where(ATTEMPT_DELIVERY_ID + " = ?")
                                   .order(ATTEMPTED_AT)
                                   .build();
            List<Attempt> attempts = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, deliveryId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int code = resultSet.getInt(STATUS_CODE);
                        Integer statusCode = resultSet.wasNull() ? null : code;
                        attempts.add(new Attempt(resultSet.getTimestamp(ATTEMPTED_AT)
                                                          .toInstant(),
                                statusCode, resultSet.getLong(DURATION), resultSet.getString(ERROR)));
                    }
                }
            }
            return attempts;
        }
    }

    /**
     * Drops the finished deliveries, and their attempts, finished before a moment.
     *
     * @param before the moment
     * @return how many deliveries were dropped
     * @throws SQLException if the delete fails
     */
    int purge(Instant before) throws SQLException {
        try (Connection connection = connection()) {
            String attempts =
                    "DELETE FROM " + quoted(ATTEMPTS) + " WHERE " + quoted(ATTEMPT_DELIVERY_ID) + " IN (SELECT " + quoted(DELIVERY_ID)
                            + " FROM " + quoted(DELIVERIES) + " WHERE " + quoted(STATE) + " <> ? AND " + quoted(FINISHED_AT) + " < ?)";
            try (PreparedStatement statement = connection.prepareStatement(attempts)) {
                statement.setString(1, State.PENDING.name());
                statement.setTimestamp(2, Timestamp.from(before));
                statement.executeUpdate();
            }
            String deliveries =
                    "DELETE FROM " + quoted(DELIVERIES) + " WHERE " + quoted(STATE) + " <> ? AND " + quoted(FINISHED_AT) + " < ?";
            try (PreparedStatement statement = connection.prepareStatement(deliveries)) {
                statement.setString(1, State.PENDING.name());
                statement.setTimestamp(2, Timestamp.from(before));
                return statement.executeUpdate();
            }
        }
    }

    /**
     * Drops every delivery and attempt of the current tenant - deprovisioning. A tenant whose data
     * source was never registered has no schema and so nothing to drop.
     *
     * @throws SQLException if the delete fails
     */
    void deleteAll() throws SQLException {
        if (!dataSourcesManager.isDefaultDataSourceDefined() || !tablesExist()) {
            return;
        }
        try (Connection connection = connection()) {
            for (String table : List.of(ATTEMPTS, DELIVERIES)) {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + quoted(table))) {
                    statement.executeUpdate();
                }
            }
        }
        preparedTenants.remove(currentTenantKey());
    }

    private Optional<Delivery> find(Connection connection, String subscriptionId, String eventId) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column("*")
                               .from(DELIVERIES)
                               .where(SUBSCRIPTION_ID + " = ?")
                               .where(EVENT_ID + " = ?")
                               .order(CREATED_AT)
                               .limit(1)
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subscriptionId);
            statement.setString(2, eventId);
            return deliveries(statement).stream()
                                        .findFirst();
        }
    }

    private static List<Delivery> deliveries(PreparedStatement statement) throws SQLException {
        List<Delivery> deliveries = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Timestamp finishedAt = resultSet.getTimestamp(FINISHED_AT);
                deliveries.add(new Delivery(resultSet.getString(DELIVERY_ID), resultSet.getString(EVENT_ID),
                        resultSet.getString(SUBSCRIPTION_ID), resultSet.getString(EVENT_TYPE), resultSet.getString(BODY),
                        resultSet.getTimestamp(CREATED_AT)
                                 .toInstant(),
                        resultSet.getInt(ATTEMPT_COUNT), State.valueOf(resultSet.getString(STATE)),
                        finishedAt == null ? null : finishedAt.toInstant()));
            }
        }
        return deliveries;
    }

    private Connection connection() throws SQLException {
        return dataSourcesManager.getDefaultDataSource()
                                 .getConnection();
    }

    private String currentTenantKey() {
        return tenantContext.isNotInitialized() ? ""
                : tenantContext.getCurrentTenant()
                               .getId();
    }

    private void createDeliveriesIfAbsent(Connection connection) throws SQLException {
        createIfAbsent(connection, DELIVERIES, SqlFactory.getNative(connection)
                                                         .create()
                                                         .table(quoted(DELIVERIES))
                                                         .column(quoted(DELIVERY_ID), DataType.VARCHAR, true, false, false, "(36)")
                                                         .column(quoted(EVENT_ID), DataType.VARCHAR, false, false, false, "(36)")
                                                         .column(quoted(SUBSCRIPTION_ID), DataType.VARCHAR, false, false, false, "(255)")
                                                         .column(quoted(EVENT_TYPE), DataType.VARCHAR, false, false, false, "(255)")
                                                         .column(quoted(BODY), DataType.CLOB, false, false, false)
                                                         .column(quoted(CREATED_AT), DataType.TIMESTAMP, false, false, false)
                                                         .column(quoted(ATTEMPT_COUNT), DataType.INTEGER, false, false, false)
                                                         .column(quoted(NEXT_ATTEMPT_AT), DataType.TIMESTAMP, false, false, false)
                                                         .column(quoted(STATE), DataType.VARCHAR, false, false, false, "(16)")
                                                         .column(quoted(FINISHED_AT), DataType.TIMESTAMP, false, true, false)
                                                         .build());
    }

    private void createAttemptsIfAbsent(Connection connection) throws SQLException {
        createIfAbsent(connection, ATTEMPTS, SqlFactory.getNative(connection)
                                                       .create()
                                                       .table(quoted(ATTEMPTS))
                                                       .column(quoted(ATTEMPT_ID), DataType.VARCHAR, true, false, false, "(36)")
                                                       .column(quoted(ATTEMPT_DELIVERY_ID), DataType.VARCHAR, false, false, false, "(36)")
                                                       .column(quoted(ATTEMPTED_AT), DataType.TIMESTAMP, false, false, false)
                                                       .column(quoted(STATUS_CODE), DataType.INTEGER, false, true, false)
                                                       .column(quoted(DURATION), DataType.BIGINT, false, false, false)
                                                       .column(quoted(ERROR), DataType.VARCHAR, false, true, false, "(2000)")
                                                       .build());
    }

    private static void createIfAbsent(Connection connection, String table, String sql) throws SQLException {
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, table)) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.info("Created the webhook table using sql [{}]", sql);
        } catch (SQLException ex) {
            // Another write may have created it in the meantime; tolerate it.
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, table)) {
                LOGGER.debug("Webhook table [{}] already exists after a concurrent creation.", table, ex);
                return;
            }
            throw ex;
        }
    }

    private static String quoted(String identifier) {
        return "\"" + identifier + "\"";
    }
}
