/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.listener;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.context.OriginatingUser;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.base.tenant.TenantSuspensionParticipant;
import org.eclipse.dirigible.components.base.tenant.TenantTeardownParticipant;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.components.listeners.config.ListenerConnectionMode;
import org.eclipse.dirigible.components.listeners.service.DestinationNameManager;
import org.eclipse.dirigible.components.listeners.service.OriginatingUserProperty;
import org.eclipse.dirigible.components.listeners.service.TenantPropertyManager;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.messaging.Listener;
import org.eclipse.dirigible.sdk.messaging.ListenerKind;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;

/**
 * {@link JavaClassConsumer} that connects client listeners to ActiveMQ queues or topics. Two
 * styles, never mixed on one class:
 * <ul>
 * <li><b>self-describing interface</b> — a {@code @Component} bean implementing
 * {@link MessageHandler}, which supplies its own {@code destination()} / {@code kind()} and
 * {@code onMessage(String)};</li>
 * <li><b>method level</b> — public {@code void m(String)} methods annotated
 * {@link Listener @Listener} on a client bean, Spring's {@code @JmsListener}-on-a-method style; a
 * bean may host several.</li>
 * </ul>
 * The bean is built (with constructor + field injection) by the {@link ComponentContainer}; this
 * consumer fetches it and opens its subscriptions on the broker, tearing them down on unload.
 *
 * <p>
 * <b>One subscription per provisioned tenant.</b> A destination name a client class declares is
 * logical; its physical name on the broker is tenant-scoped, exactly as the producer computes it at
 * send time (see {@link DestinationNameManager} for the shared contract). Class loading, though,
 * happens once per generation on a thread with no tenant of its own — so this consumer fans out
 * explicitly over {@link TenantContext#executeForEachTenant}, the same way the sibling
 * {@code ScheduledClassConsumer} registers a client-Java job per tenant. Without that fan-out the
 * subscription sat on the unprefixed name while every non-default tenant published to a prefixed
 * one, and nothing a generated application's glue layer subscribes to ever fired outside the
 * default tenant.
 *
 * <p>
 * <b>A global destination is subscribed once.</b> A name marked {@code global:} is a contract with
 * something outside this deployment and is never tenant-scoped (again
 * {@link DestinationNameManager}), so fanning it out would open one consumer per tenant on the very
 * same physical destination - competing for one queue's messages, or handling one topic's message
 * once per tenant. Those subscriptions are therefore opened once for the whole deployment, and the
 * tenant a message is handled in comes from its {@code tenant_id} stamp, which for a global
 * destination is the default tenant.
 *
 * <p>
 * Tenants also outlive a generation: this consumer is a {@link TenantPostProvisioningStep}, so a
 * tenant provisioned after the last client-Java rebuild is topped up without disturbing the
 * subscriptions already open. A tenant that goes away leaves its subscriptions behind until the
 * next rebuild — inert, because nothing publishes to a removed tenant's destinations.
 *
 * <p>
 * <b>A subscription the broker refuses is retried, indefinitely.</b> The attempt races the embedded
 * broker on every boot — the {@code vm://localhost} transport may not be accepting yet, and against
 * a shared message store the broker may not hold the lease for much longer than that — and a topic
 * discards whatever it delivers to nobody, so an unsubscribed handler loses every event silently.
 * Neither of the two triggers this consumer used to rely on ever arrives on a steady-state
 * instance: a generation is rebuilt only on publish, and {@link #execute()} runs only when a tenant
 * was actually provisioned. So whatever could not be opened is remembered and re-attempted by
 * {@link #reconcile()}, which the JVM-local {@code JavaConsumersReconciler} calls on a timer (issue
 * #7217). Retrying is safe because a partial attempt is undone: a tenant is recorded only once all
 * of its subscriptions are open, and what did open in a failed attempt is closed again, so a retry
 * can never end up with two consumers competing on the same destination.
 *
 * <p>
 * <b>A topic subscription that loses its broker connection is re-established by the same pass</b>
 * (#7851). Its connection carries a client id (it is durable), and the transport does not reconnect
 * such a connection by itself - over a {@code failover:} URL it would replay the client id while
 * the broker still holds the previous socket, be refused, and loop. So the connection's exception
 * listener hands its scope (the tenant, or the global destinations) back: the scope's connections
 * leave the registration, the next pass closes them and subscribes the scope again, and the durable
 * subscription keeps what was published to it in between.
 *
 * <p>
 * <b>The subscriptions of a scope share one connection</b> (#7875,
 * {@link ListenerConnectionMode#SHARED}, the default). A connection is what the broker meters - a
 * generated application suite declares well over a hundred handlers, and one connection per
 * subscription per tenant exceeded a small broker's quota with a single tenant. So each scope - a
 * tenant, or the global destinations - opens one connection carrying a client id of its own
 * ({@link #sharedClientId}), and every subscription is a session on it: the durable subscriptions
 * of a tenant's handlers are told apart by their subscription names, which still derive from the
 * handler and the destination. The loss of that connection is the loss of its whole scope, which is
 * exactly the unit #7851 already re-subscribes. In the {@link ListenerConnectionMode#PER_LISTENER}
 * mode every subscription keeps a connection of its own, the shape before #7875.
 *
 * <p>
 * <b>A mode switch reclaims what the previous mode registered</b> (#7890). The broker remembers a
 * durable subscription by client id and subscription name, and the two modes derive the client id
 * differently - the subscription's own name per listener, the scope's in the shared mode - so the
 * subscriptions a switch leaves behind would go on retaining every message published to their
 * topics, for good on a broker without an offline timeout. Once a scope is subscribed, the other
 * mode's identity of each of its durable subscriptions is queued, and {@link #reconcile()}
 * unsubscribes it on its own connection, one client id at a time; a client id still held elsewhere
 * (the previous instance of a rolling deploy) is refused and stays queued for the next pass.
 */
@Component
@Order(500)
public class ListenerClassConsumer
        implements JavaClassConsumer, TenantPostProvisioningStep, TenantTeardownParticipant, TenantSuspensionParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerClassConsumer.class);

    /** Width of the broker's client-id and durable-subscription-name columns. */
    private static final int MAX_SUBSCRIPTION_ID_LENGTH = 200;

    /** The failure-report label of a scope's shared connection; no class name starts with it. */
    private static final String SHARED_CONNECTION_LABEL = "<shared connection>";

    private static final String GLOBAL_SCOPE = "all tenants (global destination)";

    private final ComponentContainer componentContainer;
    private final ActiveMQConnectionArtifactsFactory connectionFactory;
    private final TenantContext tenantContext;
    private final TenantPropertyManager tenantPropertyManager;
    private final Tenant defaultTenant;
    private final TenantConfigurationService tenantConfigurationService;
    private final DestinationNameManager destinationNameManager;
    private final ListenerConnectionMode connectionMode;

    /** fqn → what the loaded class declared, and the subscriptions open for it per tenant. */
    private final ConcurrentMap<String, Registration> registrations = new ConcurrentHashMap<>();

    /**
     * client id → the connection every subscription of that scope rides, in the shared mode. Guarded by
     * this consumer's monitor, as the registrations' open handles are.
     */
    private final Map<String, Connection> sharedConnections = new HashMap<>();

    /** Set while anything this consumer declared is not subscribed; drives {@link #reconcile()}. */
    private final AtomicBoolean subscriptionsIncomplete = new AtomicBoolean();

    /**
     * The {@code label|scope} pairs already reported as failing. A retry runs on a timer, so without
     * this a permanently refused subscription would log one WARN per tenant per tick forever - and the
     * refusal is not hypothetical: every node of a deployment derives the same durable subscription id,
     * so the second one to attach is turned away for good.
     */
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();

    /**
     * Connections whose broker connection is gone, waiting to be closed by the next pass. Closed there
     * and not on the JMS exception thread that reported the loss: that thread belongs to the very
     * connection being closed.
     */
    private final ConcurrentLinkedQueue<Connection> lostConnections = new ConcurrentLinkedQueue<>();

    /**
     * The other connection mode's identities of the durable subscriptions this instance opened, waiting
     * for {@link #reclaimPreviousMode()} to unsubscribe them (#7890).
     */
    private final Set<DurableIdentity> pendingReclaims = ConcurrentHashMap.newKeySet();

    /**
     * The identities already unsubscribed, or found absent: a scope subscribed again - after a lost
     * connection, a republish, a new tenant - must not open a connection per handler to find out again.
     */
    private final Set<DurableIdentity> reclaimed = ConcurrentHashMap.newKeySet();

    /** The client ids whose refusal to reclaim was already reported, so a retry logs at DEBUG. */
    private final Set<String> reportedReclaimFailures = ConcurrentHashMap.newKeySet();

    @Autowired
    public ListenerClassConsumer(ComponentContainer componentContainer, ActiveMQConnectionArtifactsFactory connectionFactory,
            TenantContext tenantContext, TenantPropertyManager tenantPropertyManager, @DefaultTenant Tenant defaultTenant,
            TenantConfigurationService tenantConfigurationService, DestinationNameManager destinationNameManager,
            ListenerConnectionMode connectionMode) {
        this.componentContainer = componentContainer;
        this.connectionFactory = connectionFactory;
        this.tenantContext = tenantContext;
        this.tenantPropertyManager = tenantPropertyManager;
        this.defaultTenant = defaultTenant;
        this.tenantConfigurationService = tenantConfigurationService;
        this.destinationNameManager = destinationNameManager;
        this.connectionMode = connectionMode;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return MessageHandler.class.isAssignableFrom(clazz) || hasListenerMethod(clazz);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Class<?> type = info.type();
        Object instance = componentContainer.instanceOf(type)
                                            .orElse(null);
        if (instance == null) {
            LOGGER.error("Listener [{}] was not instantiated as a bean — a MessageHandler and a @Listener method both require "
                    + "the class to be a @Component; skipped.", info.fqn());
            return;
        }

        boolean messageHandler = instance instanceof MessageHandler;
        boolean methodLevel = hasListenerMethod(type);
        if (messageHandler && methodLevel) {
            LOGGER.error("[{}] mixes listener styles — it implements MessageHandler and also declares @Listener methods. "
                    + "Use one style or the other; skipped.", info.fqn());
            return;
        }

        List<Subscription> subscriptions = new ArrayList<>();

        if (messageHandler) {
            MessageHandler handler = (MessageHandler) instance;
            subscriptions.add(new Subscription(handler.destination(), handler.kind(), new TypedDispatcher(handler), info.fqn()));
        } else {
            for (Method method : type.getDeclaredMethods()) {
                Listener annotation = method.getAnnotation(Listener.class);
                if (annotation == null) {
                    continue;
                }
                if (!isEligibleMethod(method)) {
                    LOGGER.error("@Listener method [{}#{}] must be public and take a single String parameter; skipped.", info.fqn(),
                            method.getName());
                    continue;
                }
                method.setAccessible(true);
                String label = info.fqn() + "#" + method.getName();
                subscriptions.add(new Subscription(annotation.name(), annotation.kind(), new MethodDispatcher(instance, method), label));
            }
        }

        if (subscriptions.isEmpty()) {
            stopExisting(info.fqn());
            LOGGER.warn("Listener [{}] produced no subscription.", info.fqn());
            return;
        }
        register(info.fqn(), subscriptions);
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        stopExisting(info.fqn());
        LOGGER.info("Java @Listener [{}] disconnected.", info.fqn());
    }

    /**
     * Top up whatever the classes already loaded have not subscribed yet. Called once a tenant
     * provisioning round completes: the client-Java generation is JVM-wide and is only rebuilt on
     * publish, so a tenant created afterwards would otherwise stay unsubscribed until the next rebuild.
     */
    @Override
    public void execute() {
        topUp();
    }

    /**
     * Re-attempt whatever is still not subscribed, on the reconciliation timer, then reclaim what the
     * previous connection mode left on the broker. Both are gated so an instance whose subscriptions
     * are all open and whose reclaims are done pays nothing per tick - not even the tenant lookup the
     * fan-out below performs.
     *
     * <p>
     * The flag is cleared <b>before</b> the pass, never after: a failure the pass itself records has to
     * survive it, or the very subscription that just failed would never be retried again.
     */
    @Override
    public void reconcile() {
        if (subscriptionsIncomplete.compareAndSet(true, false)) {
            topUp();
        }
        reclaimPreviousMode();
    }

    /** Replace whatever this class had subscribed with the subscriptions it declares now. */
    private synchronized void register(String fqn, List<Subscription> subscriptions) {
        stopExisting(fqn);
        Registration registration = new Registration(subscriptions);
        registrations.put(fqn, registration);
        subscribeGlobal(fqn, registration);
        subscribeMissingTenants(fqn, registration);
    }

    /**
     * One reconciliation pass over everything loaded: the global destinations first, then the tenant
     * ones. The tenant fan-out happens <b>once for the pass</b> rather than once per class, because
     * {@link TenantContext#executeForEachTenant} reads the provisioned tenants from the database and a
     * per-class fan-out would turn one tick into one query per registered listener.
     */
    private void topUp() {
        closeLost();
        registrations.forEach(this::subscribeGlobal);
        try {
            tenantContext.executeForEachTenant(() -> {
                String tenantId = tenantContext.getCurrentTenant()
                                               .getId();
                registrations.forEach((fqn, registration) -> subscribeTenant(fqn, registration, tenantId));
                return null;
            });
        } catch (Exception e) {
            markIncomplete();
            LOGGER.error("Failed to top up the client-Java listener subscriptions of the provisioned tenants: {}", e.getMessage(), e);
        }
        reportIncomplete();
    }

    /**
     * Open this class's global subscriptions - once for the deployment, not once per tenant. Runs on
     * the loading thread, which has no tenant of its own, and needs none: a global destination resolves
     * to the same physical name everywhere.
     */
    private void subscribeGlobal(String fqn, Registration registration) {
        subscribeScope(fqn, registration, null);
    }

    /**
     * Open one class's subscriptions in every provisioned tenant that does not have them yet. Additive
     * on purpose — an already-subscribed tenant is left alone, so topping up never drops a message in
     * flight.
     */
    private void subscribeMissingTenants(String fqn, Registration registration) {
        try {
            tenantContext.executeForEachTenant(() -> {
                subscribeTenant(fqn, registration, tenantContext.getCurrentTenant()
                                                                .getId());
                return null;
            });
        } catch (Exception e) {
            markIncomplete();
            LOGGER.error("Failed to subscribe listener [{}] for the provisioned tenants: {}", fqn, e.getMessage(), e);
        }
    }

    /** Open one class's tenant-scoped subscriptions in the tenant whose context is currently active. */
    private void subscribeTenant(String fqn, Registration registration, String tenantId) {
        subscribeScope(fqn, registration, tenantId);
    }

    /**
     * Open one class's subscriptions of one scope: a tenant's, or - for {@code null} - the global ones.
     *
     * <p>
     * All or nothing, and idempotent: a retry must not be able to open a second consumer beside one
     * that is already live, so the scope is recorded only once every subscription of it is open, and
     * what did open in a failed attempt is closed again. In the shared mode the scope's connection is
     * taken first; while the broker refuses it there is nothing to attempt per subscription, and the
     * refusal is reported once for the scope rather than once per handler.
     */
    private synchronized void subscribeScope(String fqn, Registration registration, String tenantId) {
        boolean global = tenantId == null;
        if (isSuperseded(fqn, registration) || (global ? registration.isGlobalSubscribed() : registration.isSubscribed(tenantId))) {
            return;
        }
        List<Subscription> subscriptions = global ? registration.globalSubscriptions() : registration.tenantSubscriptions();
        if (subscriptions.isEmpty()) {
            // Nothing to open in this scope - and so no reason to hold a connection for it.
            registration.subscribed(tenantId, List.of());
            return;
        }
        String scope = global ? GLOBAL_SCOPE : "tenant [" + tenantId + "]";
        Connection shared = null;
        if (connectionMode == ListenerConnectionMode.SHARED) {
            shared = sharedConnection(tenantId, scope);
            if (shared == null) {
                markIncomplete();
                return;
            }
        }
        List<AutoCloseable> opened = new ArrayList<>();
        boolean complete = true;
        for (Subscription subscription : subscriptions) {
            AutoCloseable handle = shared == null ? subscribe(subscription, scope, () -> connectionLost(fqn, registration, tenantId))
                    : subscribeOn(shared, subscription, scope);
            if (handle == null) {
                complete = false;
            } else {
                opened.add(handle);
            }
        }
        if (!complete) {
            // Leave the handles unrecorded before closing them, or a later teardown would close them a
            // second time and openHandles() would report what is already gone.
            closeAll(fqn, opened);
            markIncomplete();
        } else {
            if (global) {
                registration.globalSubscribed(opened);
            } else {
                registration.subscribed(tenantId, opened);
            }
            queuePreviousModeIdentities(subscriptions, tenantId);
        }
    }

    /**
     * Queue the identity the other connection mode gives each durable subscription of a scope that has
     * just been subscribed, unless it was reclaimed already. Runs in the scope's tenant context, so the
     * subscription name resolves exactly as the subscribe did.
     *
     * @param subscriptions the scope's subscriptions, all of them open
     * @param tenantId the tenant, or {@code null} for the global destinations
     */
    private void queuePreviousModeIdentities(List<Subscription> subscriptions, String tenantId) {
        for (Subscription subscription : subscriptions) {
            if (subscription.kind() != ListenerKind.TOPIC) {
                continue;
            }
            String name = durableSubscriptionId(subscription.label(), destinationNameManager.toTenantName(subscription.destination()));
            String previousClientId = connectionMode == ListenerConnectionMode.SHARED ? name : sharedClientId(tenantId);
            DurableIdentity previous = new DurableIdentity(previousClientId, name);
            if (!reclaimed.contains(previous)) {
                pendingReclaims.add(previous);
            }
        }
    }

    /**
     * Unsubscribe the queued identities, one client id - and so one connection - at a time, so the
     * burst a mode switch causes stays small. Runs on the reconciliation thread and outside this
     * consumer's monitor: it touches none of the live subscriptions.
     */
    private void reclaimPreviousMode() {
        if (pendingReclaims.isEmpty()) {
            return;
        }
        Map<String, List<String>> namesByClientId = new LinkedHashMap<>();
        for (DurableIdentity identity : pendingReclaims) {
            namesByClientId.computeIfAbsent(identity.clientId(), clientId -> new ArrayList<>())
                           .add(identity.subscriptionName());
        }
        namesByClientId.forEach(this::reclaim);
    }

    /**
     * Unsubscribe the given durable subscriptions of one client id. The broker refuses the connection
     * while another connection holds the client id - the previous instance of a rolling deploy - and
     * the identities then stay queued for the next pass, never skipped for good.
     *
     * @param clientId the client id the previous mode registered them under
     * @param subscriptionNames their subscription names
     */
    private void reclaim(String clientId, List<String> subscriptionNames) {
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection(
                    ex -> LOGGER.debug("[java-listener] JMS error on the reclaim connection [{}]: {}", clientId, ex.getMessage(), ex),
                    clientId);
            Session session = connectionFactory.createSession(connection);
            for (String subscriptionName : subscriptionNames) {
                unsubscribe(session, new DurableIdentity(clientId, subscriptionName));
            }
            reportedReclaimFailures.remove(clientId);
        } catch (JMSException | RuntimeException e) {
            // RuntimeException: the connection factory reports a refused client id as an
            // IllegalStateException - the very case this retry exists for.
            String message = "The durable subscription(s) " + subscriptionNames + " the previous listener connection mode registered under"
                    + " client id [" + clientId + "] are not reclaimed yet - the client id or a subscription is still in use; retrying on"
                    + " the next pass.";
            if (reportedReclaimFailures.add(clientId)) {
                LOGGER.warn("{} {}", message, e.getMessage(), e);
            } else {
                LOGGER.debug("{} {}", message, e.getMessage(), e);
            }
        } finally {
            closeQuietly("reclaim " + clientId, connection);
        }
    }

    private void unsubscribe(Session session, DurableIdentity identity) throws JMSException {
        try {
            session.unsubscribe(identity.subscriptionName());
            LOGGER.info("Reclaimed the durable subscription [{}] the previous listener connection mode registered under client id [{}].",
                    identity.subscriptionName(), identity.clientId());
        } catch (InvalidDestinationException absent) {
            // The usual answer: the mode never changed, so there is nothing registered under it.
            LOGGER.debug("No durable subscription [{}] under client id [{}] to reclaim: {}", identity.subscriptionName(),
                    identity.clientId(), absent.getMessage(), absent);
        }
        reclaimed.add(identity);
        pendingReclaims.remove(identity);
    }

    /**
     * The connection a scope's subscriptions share, opened on first use (shared mode). Its client id is
     * the scope's, so the loss of the connection reaches one exception listener, which hands the whole
     * scope back for the next pass to subscribe again - the connection is not reconnected by its
     * transport, as no client-id connection is (#7851).
     *
     * @param tenantId the tenant, or {@code null} for the global destinations
     * @param scope what the connection covers, for the log
     * @return the connection, or {@code null} if the broker refused it
     */
    private Connection sharedConnection(String tenantId, String scope) {
        String clientId = sharedClientId(tenantId);
        Connection connection = sharedConnections.get(clientId);
        if (connection != null) {
            return connection;
        }
        try {
            connection = connectionFactory.createConnection(ex -> {
                LOGGER.error("[java-listener] JMS error on the shared connection [{}] for {}: {}", clientId, scope, ex.getMessage(), ex);
                sharedConnectionLost(clientId, tenantId, scope);
            }, clientId);
        } catch (RuntimeException e) {
            reportFailure(SHARED_CONNECTION_LABEL, scope, e);
            return null;
        }
        sharedConnections.put(clientId, connection);
        reportedFailures.remove(failureKey(SHARED_CONNECTION_LABEL, scope));
        LOGGER.info("Java listeners share the broker connection [{}] for {}.", clientId, scope);
        return connection;
    }

    /**
     * The client id of a scope's shared connection. Stable, so the durable subscriptions registered
     * under it are resumed rather than replaced on every boot, and unique per scope - two tenants' or
     * two instances' connections may not claim one id. Two deployments sharing one broker share the
     * default tenant's id as well, but they already share that tenant's physical destinations, which is
     * the older of the two collisions.
     *
     * @param tenantId the tenant, or {@code null} for the global destinations
     * @return the client id
     */
    static String sharedClientId(String tenantId) {
        String id = tenantId == null ? "dirigible-java-global" : "dirigible-java-tenant-" + tenantId;
        return id.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Whether this registration has been replaced or unloaded since the caller picked it up. A pass
     * iterates a weakly consistent map, so a republish can install a new registration for the same
     * class in between - and subscribing the old one would open connections nothing holds a reference
     * to, live on the destination its replacement is also consuming, and impossible to ever close.
     */
    private boolean isSuperseded(String fqn, Registration registration) {
        return registrations.get(fqn) != registration;
    }

    /**
     * A subscription's broker connection is gone for good (#7851, per-listener mode): take its whole
     * scope out of the registration - all or nothing, as {@link #subscribeScope} opens it - so the next
     * pass subscribes it again, and queue the scope's connections for that pass to close. Called on the
     * JMS exception thread; every connection of a scope may report the same loss, and only the first
     * one finds anything left to take.
     *
     * @param fqn the class the subscription belongs to
     * @param registration the registration that opened it
     * @param tenantId the tenant whose subscriptions it is, or {@code null} for the global ones
     */
    private void connectionLost(String fqn, Registration registration, String tenantId) {
        List<AutoCloseable> lost;
        synchronized (this) {
            if (isSuperseded(fqn, registration)) {
                // A republish or an unload already closed it with the rest of the old registration.
                return;
            }
            lost = tenantId == null ? registration.globalLost() : registration.lost(tenantId);
        }
        if (lost.isEmpty()) {
            return;
        }
        for (AutoCloseable handle : lost) {
            // Every handle of this mode is the subscription's own connection.
            lostConnections.add((Connection) handle);
        }
        markIncomplete();
        LOGGER.warn("Java @Listener [{}] lost its broker connection for {} - subscribing it again on the next reconciliation pass.", fqn,
                tenantId == null ? GLOBAL_SCOPE : "tenant [" + tenantId + "]");
    }

    /**
     * A scope's shared connection is gone for good (shared mode): every loaded class loses that scope,
     * so the next pass opens a new connection and subscribes them all again. The sessions die with the
     * connection and are only forgotten; the connection itself is queued for the pass to close. Called
     * on the JMS exception thread; a loss reported twice finds nothing left the second time.
     *
     * @param clientId the lost connection's client id
     * @param tenantId the scope's tenant, or {@code null} for the global destinations
     * @param scope what the connection covered, for the log
     */
    private void sharedConnectionLost(String clientId, String tenantId, String scope) {
        int subscriptions = 0;
        synchronized (this) {
            Connection connection = sharedConnections.remove(clientId);
            if (connection == null) {
                return;
            }
            for (Registration registration : registrations.values()) {
                subscriptions += (tenantId == null ? registration.globalLost() : registration.lost(tenantId)).size();
            }
            lostConnections.add(connection);
        }
        markIncomplete();
        LOGGER.warn("The shared broker connection [{}] for {} is gone - subscribing its [{}] Java listener subscription(s) again on the"
                + " next reconciliation pass.", clientId, scope, subscriptions);
    }

    /**
     * Close what {@link #connectionLost} and {@link #sharedConnectionLost} took out of the
     * registrations.
     */
    private void closeLost() {
        Connection connection;
        while ((connection = lostConnections.poll()) != null) {
            closeQuietly("lost connection", connection);
        }
    }

    private void markIncomplete() {
        subscriptionsIncomplete.set(true);
    }

    /**
     * One line per pass while anything is still down. The per-attempt log falls to DEBUG after the
     * first failure, so this summary is what keeps a lasting outage visible instead of scrolling past
     * once at boot - which is how an unsubscribed handler went unnoticed in the first place.
     */
    private void reportIncomplete() {
        if (!subscriptionsIncomplete.get() || reportedFailures.isEmpty()) {
            // An empty set means the fan-out itself failed, which the outer catch has already reported.
            return;
        }
        LOGGER.warn(
                "[{}] client-Java listener subscription(s) are still not open, so messages published to their destinations are"
                        + " not handled: {}. Retrying on the next reconciliation pass.",
                reportedFailures.size(), new LinkedHashSet<>(reportedFailures));
    }

    /**
     * Open one subscription on a connection of its own (per-listener mode). A tenant-scoped one runs
     * inside the target tenant's context, so the physical destination is the very name a producer in
     * that tenant computes for the same logical one; a global one resolves to the bare name regardless.
     *
     * @param subscription what to subscribe to
     * @param scope what this subscription covers, for the log
     * @param onLost what to do when a topic subscription's connection is gone for good - its transport
     *        does not reconnect it (see the class comment)
     * @return the connection it opened, or {@code null} if the broker refused the subscription
     */
    private Connection subscribe(Subscription subscription, String scope, Runnable onLost) {
        String label = subscription.label();
        // A queue holds its messages until something consumes them; a topic drops whatever it delivers
        // to nobody. So only a topic subscription needs to be durable - and a durable one needs the
        // connection to carry an id, which is how the broker recognises it across a reconnect.
        boolean topic = subscription.kind() == ListenerKind.TOPIC;
        // Resolving the physical name is inside the try with the attach it belongs to: anything thrown
        // between here and the consumer must stay this subscription's problem, or it escapes the
        // per-tenant fan-out and skips every tenant behind this one.
        // The connection is held outside the try because the factory START()s it before returning: every
        // step after that - the session, the destination, the redelivery policy, the consumer - can be
        // refused by the broker with a live transport socket and its thread already in existence. A
        // refusal that is permanent (a destination the broker's authorization refuses) is retried on
        // every reconciliation pass, so an attempt that walked away from its connection would orphan one
        // per subscription per tenant per tick until the process ran out of file descriptors.
        Connection connection = null;
        try {
            String destinationName = destinationNameManager.toTenantName(subscription.destination());
            String subscriptionId = topic ? durableSubscriptionId(label, destinationName) : null;
            connection = connectionFactory.createConnection(ex -> {
                LOGGER.error("[java-listener] JMS error for [{}]: {}", label, ex.getMessage(), ex);
                if (topic) {
                    // A client-id connection the transport no longer reconnects: re-subscribe its scope.
                    onLost.run();
                }
            }, subscriptionId);
            Session session = connectionFactory.createSession(connection);
            attach(connection, session, subscription, destinationName, subscriptionId);
            reportedFailures.remove(failureKey(label, scope));
            LOGGER.info("Java @Listener [{}] connected to {} '{}' for {}.", label, subscription.kind(), destinationName, scope);
            return connection;
        } catch (JMSException | RuntimeException e) {
            // RuntimeException is not defensive padding: the connection factory reports a broker that is
            // not accepting yet by wrapping the JMSException in an IllegalStateException, which is
            // exactly the failure this whole retry exists for. Left uncaught it escaped the per-tenant
            // fan-out, skipping every remaining tenant and leaking the connections already opened.
            reportFailure(label, scope, e);
            closeQuietly(label, connection);
            return null;
        }
    }

    /**
     * Open one subscription as a session on its scope's shared connection (shared mode). What the
     * subscription owns, and what a refusal closes, is the session alone: the connection stays up for
     * the scope's other subscriptions.
     *
     * @param connection the scope's shared connection
     * @param subscription what to subscribe to
     * @param scope what this subscription covers, for the log
     * @return the session it opened, or {@code null} if the broker refused the subscription
     */
    private Session subscribeOn(Connection connection, Subscription subscription, String scope) {
        String label = subscription.label();
        boolean topic = subscription.kind() == ListenerKind.TOPIC;
        Session session = null;
        try {
            String destinationName = destinationNameManager.toTenantName(subscription.destination());
            session = connectionFactory.createSession(connection);
            attach(connection, session, subscription, destinationName, topic ? durableSubscriptionId(label, destinationName) : null);
            reportedFailures.remove(failureKey(label, scope));
            LOGGER.info("Java @Listener [{}] connected to {} '{}' for {} on the shared connection.", label, subscription.kind(),
                    destinationName, scope);
            return session;
        } catch (JMSException | RuntimeException e) {
            reportFailure(label, scope, e);
            closeQuietly(label, session);
            return null;
        }
    }

    /**
     * The part of opening a subscription that both modes share: the destination, the redelivery budget,
     * the consumer and its dispatch.
     *
     * @param connection the connection the session belongs to - the redelivery policy is kept per
     *        connection and destination
     * @param session the session to open the consumer on
     * @param subscription what to subscribe to
     * @param destinationName the physical, tenant-resolved destination
     * @param subscriptionId the durable subscription's name, or {@code null} for a queue
     * @throws JMSException if the broker refuses any step
     */
    private void attach(Connection connection, Session session, Subscription subscription, String destinationName, String subscriptionId)
            throws JMSException {
        boolean topic = subscriptionId != null;
        Destination destination = topic ? session.createTopic(destinationName) : session.createQueue(destinationName);
        // Bound the retries, exactly as the JavaScript listener path does. Without this the broker
        // still retries a failed delivery, but on its own defaults rather than a budget this
        // project chose - and the two listener paths would disagree about how forgiving they are.
        connectionFactory.configureRedeliveryPolicy(connection, destination);
        // A topic subscription is also DURABLE, because it goes down on every republish: the handler
        // set is torn down and re-registered, and a plain subscriber would silently lose every event
        // published in that window - a record created mid-republish would never start its process,
        // resolve its register or post its document, and would look exactly like one the automation
        // had nothing to do for. The cast is what `topic` already decided one line above.
        MessageConsumer consumer =
                topic ? session.createDurableSubscriber((Topic) destination, subscriptionId) : session.createConsumer(destination);
        consumer.setMessageListener(msg -> dispatch(msg, subscription.dispatcher(), subscription.label()));
    }

    /**
     * Close a connection or session nothing will ever hold a reference to. The failure that brought us
     * here is already reported, and a broker that just refused the subscription is likely to refuse the
     * close too, so this one is DEBUG - it must not add a second line per tick to a lasting outage.
     */
    private static void closeQuietly(String label, AutoCloseable handle) {
        if (handle == null) {
            return;
        }
        try {
            handle.close();
        } catch (Exception e) {
            LOGGER.debug("Failed to close the JMS handle of the refused subscription [{}]: {}", LogSanitizer.sanitize(label),
                    e.getMessage(), e);
        }
    }

    /**
     * Report a refused subscription once, then quietly. It is a WARN and not an ERROR because it is now
     * transient by construction - the reconciliation timer retries it - and the repeats fall to DEBUG
     * because that timer would otherwise emit one line per subscription per tenant, forever, for a
     * destination that can never be opened.
     */
    private void reportFailure(String label, String scope, Exception cause) {
        String consequence = SHARED_CONNECTION_LABEL.equals(label)
                ? "Failed to open the shared broker connection for " + scope
                        + " - none of its Java listeners is subscribed until it connects; retrying on the next pass."
                : "Failed to start listener [" + label + "] for " + scope
                        + " - messages published to its destination are not handled until it connects; retrying on the next pass.";
        if (reportedFailures.add(failureKey(label, scope))) {
            LOGGER.warn("{} {}", consequence, cause.getMessage(), cause);
        } else {
            LOGGER.debug("{} {}", consequence, cause.getMessage(), cause);
        }
    }

    private static String failureKey(String label, String scope) {
        return label + "|" + scope;
    }

    /**
     * The identity the broker remembers a durable subscription by. It has to be <b>stable</b> - the
     * same handler on the same physical destination must derive the same id on every reload, because an
     * id that varied would start a fresh subscription each time, retaining nothing and stranding the
     * previous one - and unique per live subscription, which the handler's label and the
     * tenant-resolved destination name together already are.
     *
     * @param label the handler this subscription dispatches to
     * @param destinationName the physical, tenant-resolved destination
     * @return the durable subscription id
     */
    private static String durableSubscriptionId(String label, String destinationName) {
        String id = ("dirigible-java-" + label + "-" + destinationName).replaceAll("[^A-Za-z0-9._-]", "_");
        if (id.length() <= MAX_SUBSCRIPTION_ID_LENGTH) {
            return id;
        }
        // Keep it within the broker's column width without letting two long ids collapse into one.
        String digest = Integer.toHexString(id.hashCode());
        return id.substring(0, MAX_SUBSCRIPTION_ID_LENGTH - digest.length() - 1) + "-" + digest;
    }

    /**
     * Deprovisioning and suspension (participant 100 of both): closes every subscription open for the
     * tenant, and the tenant's shared connection in the shared mode. Its status already keeps the
     * top-up and the reconciliation pass from opening them again. The closing happens outside the
     * monitor, as in {@link #shutdown()}. The broker's durable subscriptions and destinations stay, so
     * a suspended tenant's messages wait there for its resume.
     */
    @Override
    public int order() {
        return 100;
    }

    @Override
    public void teardown(String tenantId) {
        closeTenant(tenantId, "deprovisioned");
    }

    @Override
    public void suspend(String tenantId) {
        closeTenant(tenantId, "suspended");
    }

    /**
     * Marks the subscriptions incomplete, so the next {@link #reconcile()} - the one {@link #resumed}
     * runs once the tenant is provisioned again - opens the tenant's subscriptions. Nothing is opened
     * here: the tenant is still suspended, and the top-up reaches provisioned tenants only.
     */
    @Override
    public void resume(String tenantId) {
        markIncomplete();
    }

    @Override
    public void resumed() {
        reconcile();
    }

    private void closeTenant(String tenantId, String why) {
        List<AutoCloseable> open = new ArrayList<>();
        synchronized (this) {
            registrations.values()
                         .forEach(registration -> open.addAll(registration.lost(tenantId)));
            Connection shared = sharedConnections.remove(sharedClientId(tenantId));
            if (shared != null) {
                open.add(shared);
            }
        }
        for (AutoCloseable handle : open) {
            closeQuietly(why + " tenant [" + tenantId + "]", handle);
        }
        if (!open.isEmpty()) {
            LOGGER.info("Closed [{}] Java listener subscription handle(s) of {} tenant [{}].", open.size(), why,
                    LogSanitizer.sanitize(tenantId));
        }
    }

    private synchronized void stopExisting(String fqn) {
        Registration old = registrations.remove(fqn);
        reportedFailures.removeIf(key -> key.startsWith(fqn + "|") || key.startsWith(fqn + "#"));
        if (old != null) {
            closeAll(fqn, old.openHandles());
        }
    }

    /**
     * Close every subscription and every shared connection when the context goes down, the lost ones
     * included: no pass comes after it to close them, and a connection left open keeps its executor
     * thread, which holds this consumer and through it the whole application context (#7868). A clean
     * close also releases the client ids on the broker at once, so the instance replacing this one in a
     * rolling deploy is not refused them until the broker gives up on a dead socket. The closing
     * happens outside the monitor: a connection's exception listener takes it, and a close that waited
     * for that listener while holding it would never return.
     */
    @PreDestroy
    void shutdown() {
        List<AutoCloseable> open = new ArrayList<>();
        synchronized (this) {
            registrations.values()
                         .forEach(registration -> open.addAll(registration.openHandles()));
            registrations.clear();
            open.addAll(sharedConnections.values());
            sharedConnections.clear();
        }
        for (AutoCloseable handle : open) {
            closeQuietly("shutdown", handle);
        }
        closeLost();
    }

    private static void closeAll(String fqn, List<AutoCloseable> handles) {
        for (AutoCloseable handle : handles) {
            try {
                handle.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close the JMS subscription of [{}]: {}", fqn, e.getMessage(), e);
            }
        }
    }

    private static boolean hasListenerMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Listener.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEligibleMethod(Method method) {
        return Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class
                && !method.isSynthetic();
    }

    private void dispatch(Message msg, Dispatcher dispatcher, String label) {
        if (!(msg instanceof TextMessage textMsg)) {
            LOGGER.warn("@Listener [{}] received a non-text message; ignored.", label);
            return;
        }
        String text;
        try {
            text = textMsg.getText();
        } catch (JMSException e) {
            LOGGER.error("@Listener [{}] failed to read text message: {}", label, e.getMessage(), e);
            dispatcher.onError(e.getMessage(), label);
            return;
        }
        // The message arrives on a broker thread with no tenant context. Recover the originating
        // tenant the producer stamped on it and re-establish it for the handler, the same way the
        // built-in asynchronous listener does - otherwise handler code that touches tenant-scoped
        // services (DB, BPM via Process.start, ...) fails with "current tenant is not initialized".
        // Messages from outside the platform producer carry no tenant; fall back to the default
        // tenant so the handler still runs within a valid context.
        String tenantId;
        try {
            tenantId = tenantPropertyManager.getCurrentTenantId(msg);
        } catch (JMSException | RuntimeException e) {
            LOGGER.debug("@Listener [{}] message carries no tenant; using the default tenant. {}", label, e.getMessage(), e);
            tenantId = defaultTenant.getId();
        }
        // The user whose write raised the event, stamped by the platform's producer from the outbox
        // row: bound for the handler, so the rows it writes are attributed to that user rather than to
        // the guest fallback of a thread without a request (#8228). Absent, the handler runs as before.
        String user = originatingUserOf(msg, label);
        try {
            tenantContext.execute(tenantId, () -> {
                // Re-establishing the tenant's identity above is not enough: Configuration's tenant
                // override lookup depends on a thread-scoped map that only TenantConfigurationInitFilter
                // populates for HTTP requests. A listener dispatch is not a request, so without this the
                // handler would silently read the global default instead of this tenant's own override
                // (e.g. a generated notification's {appUrl} token) - load it here the same way, and clear
                // it so it never leaks onto the pooled broker thread.
                Configuration.setThreadConfiguration(tenantConfigurationService.resolveInjectableForCurrentTenant());
                try (OriginatingUser.Binding binding = OriginatingUser.bind(user)) {
                    dispatcher.onMessage(text);
                } finally {
                    Configuration.removeThreadConfiguration();
                }
                return null;
            });
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            // This log IS the failure report: the handler's own onError defaults to a no-op, so without
            // this line a throwing handler leaves no explanation anywhere. Redelivery (below) re-runs
            // the work; it does not say why the work failed. Pass the throwable, not just its message,
            // or the stack trace dies here.
            LOGGER.error("@Listener [{}] failed handling a message: {}", label, cause.getMessage(), cause);
            dispatcher.onError(cause.getMessage(), label);
            // Let the failure reach the broker. Swallowing it here acknowledged the message and lost
            // the event permanently: no retry, no dead letter, and handlers whose correctness depends
            // on a second delivery - the generated posting glue repairs a half-written post on
            // redelivery - could never run their repair. Throwing hands the message back so the
            // redelivery policy configured above retries it, and dead-letters it once exhausted.
            throw new IllegalStateException("@Listener [" + label + "] failed handling a message", cause);
        }
    }

    private static String originatingUserOf(Message msg, String label) {
        try {
            return OriginatingUserProperty.read(msg);
        } catch (JMSException e) {
            LOGGER.debug("@Listener [{}] message has an unreadable originating user; handling it without one.", label, e);
            return null;
        }
    }

    /** How the broker remembers a durable subscription: the connection's client id and its name. */
    private record DurableIdentity(String clientId, String subscriptionName) {
    }

    /**
     * One subscription a loaded class asks for: the <em>logical</em> destination, its kind, and how a
     * message on it is dispatched. Tenant-independent — the same spec is opened once per tenant, or
     * once for the deployment when the destination is global.
     */
    private record Subscription(String destination, ListenerKind kind, Dispatcher dispatcher, String label) {
    }

    /**
     * What one loaded class declared, plus what is open for it in each tenant: the subscription's own
     * connection in the per-listener mode, its session on the shared one in the shared mode.
     */
    private static final class Registration {

        /** Subscriptions opened once per provisioned tenant. */
        private final List<Subscription> tenantSubscriptions;

        /** Subscriptions opened once for the whole deployment. */
        private final List<Subscription> globalSubscriptions;

        /** tenant id → the handles open for this class in that tenant. */
        private final Map<String, List<AutoCloseable>> handlesByTenant = new HashMap<>();

        /** The handles open for this class's global destinations. */
        private List<AutoCloseable> globalHandles = List.of();

        /** Whether every global destination this class declares is subscribed. */
        private boolean globalSubscribed;

        Registration(List<Subscription> subscriptions) {
            this.tenantSubscriptions = subscriptions.stream()
                                                    .filter(subscription -> !DestinationNameManager.isGlobal(subscription.destination()))
                                                    .toList();
            this.globalSubscriptions = subscriptions.stream()
                                                    .filter(subscription -> DestinationNameManager.isGlobal(subscription.destination()))
                                                    .toList();
            // A class declaring no global destination has nothing outstanding on that side, ever.
            this.globalSubscribed = this.globalSubscriptions.isEmpty();
        }

        List<Subscription> tenantSubscriptions() {
            return tenantSubscriptions;
        }

        List<Subscription> globalSubscriptions() {
            return globalSubscriptions;
        }

        boolean isGlobalSubscribed() {
            return globalSubscribed;
        }

        void globalSubscribed(List<AutoCloseable> handles) {
            this.globalHandles = List.copyOf(handles);
            this.globalSubscribed = true;
        }

        boolean isSubscribed(String tenantId) {
            return handlesByTenant.containsKey(tenantId);
        }

        void subscribed(String tenantId, List<AutoCloseable> handles) {
            handlesByTenant.put(tenantId, handles);
        }

        /** Forget a tenant's subscriptions, so they are opened again; returns their handles. */
        List<AutoCloseable> lost(String tenantId) {
            List<AutoCloseable> handles = handlesByTenant.remove(tenantId);
            return handles == null ? List.of() : handles;
        }

        /** Forget the global subscriptions, so they are opened again; returns their handles. */
        List<AutoCloseable> globalLost() {
            if (globalSubscriptions.isEmpty() || !globalSubscribed) {
                return List.of();
            }
            List<AutoCloseable> handles = globalHandles;
            globalHandles = List.of();
            globalSubscribed = false;
            return handles;
        }

        List<AutoCloseable> openHandles() {
            List<AutoCloseable> all = new ArrayList<>(globalHandles);
            handlesByTenant.values()
                           .forEach(all::addAll);
            return all;
        }
    }

    /** Abstraction over the typed (MessageHandler) and method-level callback paths. */
    private interface Dispatcher {
        void onMessage(String text) throws Exception;

        void onError(String error, String label);
    }

    private record TypedDispatcher(MessageHandler handler) implements Dispatcher {

        @Override
        public void onMessage(String text) {
            handler.onMessage(text);
        }

        @Override
        public void onError(String error, String label) {
            try {
                handler.onError(error);
            } catch (RuntimeException ex) {
                LOGGER.error("@Listener [{}] onError() threw: {}", label, ex.getMessage(), ex);
            }
        }
    }

    private record MethodDispatcher(Object instance, Method method) implements Dispatcher {

        @Override
        public void onMessage(String text) throws ReflectiveOperationException {
            method.invoke(instance, text);
        }

        @Override
        public void onError(String error, String label) {
            LOGGER.error("@Listener [{}] handler threw: {}", label, error);
        }
    }
}
