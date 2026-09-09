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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.components.listeners.service.DestinationNameManager;
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

import jakarta.jms.Connection;
import jakarta.jms.Destination;
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
 * consumer fetches it and opens a JMS connection per subscription, tearing them down on unload.
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
 */
@Component
@Order(500)
public class ListenerClassConsumer implements JavaClassConsumer, TenantPostProvisioningStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerClassConsumer.class);

    /** Width of the broker's client-id and durable-subscription-name columns. */
    private static final int MAX_SUBSCRIPTION_ID_LENGTH = 200;

    private final ComponentContainer componentContainer;
    private final ActiveMQConnectionArtifactsFactory connectionFactory;
    private final TenantContext tenantContext;
    private final TenantPropertyManager tenantPropertyManager;
    private final Tenant defaultTenant;
    private final TenantConfigurationService tenantConfigurationService;
    private final DestinationNameManager destinationNameManager;

    /** fqn → what the loaded class declared, and the connections open for it per tenant. */
    private final ConcurrentMap<String, Registration> registrations = new ConcurrentHashMap<>();

    /** Set while anything this consumer declared is not subscribed; drives {@link #reconcile()}. */
    private final AtomicBoolean subscriptionsIncomplete = new AtomicBoolean();

    /**
     * The {@code label|scope} pairs already reported as failing. A retry runs on a timer, so without
     * this a permanently refused subscription would log one WARN per tenant per tick forever - and the
     * refusal is not hypothetical: every node of a deployment derives the same durable subscription id,
     * so the second one to attach is turned away for good.
     */
    private final Set<String> reportedFailures = ConcurrentHashMap.newKeySet();

    @Autowired
    public ListenerClassConsumer(ComponentContainer componentContainer, ActiveMQConnectionArtifactsFactory connectionFactory,
            TenantContext tenantContext, TenantPropertyManager tenantPropertyManager, @DefaultTenant Tenant defaultTenant,
            TenantConfigurationService tenantConfigurationService, DestinationNameManager destinationNameManager) {
        this.componentContainer = componentContainer;
        this.connectionFactory = connectionFactory;
        this.tenantContext = tenantContext;
        this.tenantPropertyManager = tenantPropertyManager;
        this.defaultTenant = defaultTenant;
        this.tenantConfigurationService = tenantConfigurationService;
        this.destinationNameManager = destinationNameManager;
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
     * Re-attempt whatever is still not subscribed, on the reconciliation timer. Gated on the flag so an
     * instance whose subscriptions are all open pays nothing per tick - not even the tenant lookup the
     * fan-out below performs.
     *
     * <p>
     * The flag is cleared <b>before</b> the pass, never after: a failure the pass itself records has to
     * survive it, or the very subscription that just failed would never be retried again.
     */
    @Override
    public void reconcile() {
        if (!subscriptionsIncomplete.compareAndSet(true, false)) {
            return;
        }
        topUp();
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
     *
     * <p>
     * All or nothing, and idempotent, for the same reason the per-tenant branch is: a retry must not be
     * able to open a second consumer beside one that is already live.
     */
    private synchronized void subscribeGlobal(String fqn, Registration registration) {
        if (isSuperseded(fqn, registration) || registration.isGlobalSubscribed()) {
            return;
        }
        List<Connection> opened = new ArrayList<>();
        boolean complete = true;
        for (Subscription subscription : registration.globalSubscriptions()) {
            Connection connection = subscribe(subscription, "all tenants (global destination)");
            if (connection == null) {
                complete = false;
            } else {
                opened.add(connection);
            }
        }
        if (complete) {
            registration.globalSubscribed(opened);
        } else {
            // Leave the connections unrecorded before closing them, or a later teardown would close
            // them a second time and openConnections() would report handles that are already gone.
            closeAll(fqn, opened);
            markIncomplete();
        }
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
    private synchronized void subscribeTenant(String fqn, Registration registration, String tenantId) {
        if (isSuperseded(fqn, registration) || registration.isSubscribed(tenantId)) {
            return;
        }
        List<Connection> opened = new ArrayList<>();
        boolean complete = true;
        for (Subscription subscription : registration.tenantSubscriptions()) {
            Connection connection = subscribe(subscription, "tenant [" + tenantId + "]");
            if (connection == null) {
                complete = false;
            } else {
                opened.add(connection);
            }
        }
        if (complete) {
            registration.subscribed(tenantId, opened);
        } else {
            // All or nothing per tenant: leave the tenant unrecorded so the reconciliation timer
            // retries it cleanly, and close what did open so that retry cannot end up with two
            // consumers competing on the same destination.
            closeAll(fqn, opened);
            markIncomplete();
        }
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
     * Open one subscription. A tenant-scoped one runs inside the target tenant's context, so the
     * physical destination is the very name a producer in that tenant computes for the same logical
     * one; a global one resolves to the bare name regardless.
     *
     * @param subscription what to subscribe to
     * @param scope what this subscription covers, for the log
     * @return the connection it opened, or {@code null} if the broker refused the subscription
     */
    private Connection subscribe(Subscription subscription, String scope) {
        String label = subscription.label();
        // A queue holds its messages until something consumes them; a topic drops whatever it delivers
        // to nobody. So only a topic subscription needs to be durable - and a durable one needs the
        // connection to carry an id, which is how the broker recognises it across a reconnect.
        boolean topic = subscription.kind() == ListenerKind.TOPIC;
        // Resolving the physical name is inside the try with the attach it belongs to: anything thrown
        // between here and the consumer must stay this subscription's problem, or it escapes the
        // per-tenant fan-out and skips every tenant behind this one.
        try {
            String destinationName = destinationNameManager.toTenantName(subscription.destination());
            String subscriptionId = topic ? durableSubscriptionId(label, destinationName) : null;
            Connection connection = connectionFactory.createConnection(
                    ex -> LOGGER.error("[java-listener] JMS error for [{}]: {}", label, ex.getMessage(), ex), subscriptionId);
            Session session = connectionFactory.createSession(connection);
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
            consumer.setMessageListener(msg -> dispatch(msg, subscription.dispatcher(), label));
            reportedFailures.remove(failureKey(label, scope));
            LOGGER.info("Java @Listener [{}] connected to {} '{}' for {}.", label, subscription.kind(), destinationName, scope);
            return connection;
        } catch (JMSException | RuntimeException e) {
            // RuntimeException is not defensive padding: the connection factory reports a broker that is
            // not accepting yet by wrapping the JMSException in an IllegalStateException, which is
            // exactly the failure this whole retry exists for. Left uncaught it escaped the per-tenant
            // fan-out, skipping every remaining tenant and leaking the connections already opened.
            reportFailure(label, scope, e);
            return null;
        }
    }

    /**
     * Report a refused subscription once, then quietly. It is a WARN and not an ERROR because it is now
     * transient by construction - the reconciliation timer retries it - and the repeats fall to DEBUG
     * because that timer would otherwise emit one line per subscription per tenant, forever, for a
     * destination that can never be opened.
     */
    private void reportFailure(String label, String scope, Exception cause) {
        String consequence = "Failed to start listener [" + label + "] for " + scope
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

    private synchronized void stopExisting(String fqn) {
        Registration old = registrations.remove(fqn);
        reportedFailures.removeIf(key -> key.startsWith(fqn + "|") || key.startsWith(fqn + "#"));
        if (old != null) {
            closeAll(fqn, old.openConnections());
        }
    }

    private static void closeAll(String fqn, List<Connection> connections) {
        for (Connection connection : connections) {
            try {
                connection.close();
            } catch (JMSException e) {
                LOGGER.warn("Failed to close JMS connection for [{}]: {}", fqn, e.getMessage(), e);
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
        try {
            tenantContext.execute(tenantId, () -> {
                // Re-establishing the tenant's identity above is not enough: Configuration's tenant
                // override lookup depends on a thread-scoped map that only TenantConfigurationInitFilter
                // populates for HTTP requests. A listener dispatch is not a request, so without this the
                // handler would silently read the global default instead of this tenant's own override
                // (e.g. a generated notification's {appUrl} token) - load it here the same way, and clear
                // it so it never leaks onto the pooled broker thread.
                Configuration.setThreadConfiguration(tenantConfigurationService.resolveInjectableForCurrentTenant());
                try {
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

    /**
     * One subscription a loaded class asks for: the <em>logical</em> destination, its kind, and how a
     * message on it is dispatched. Tenant-independent — the same spec is opened once per tenant, or
     * once for the deployment when the destination is global.
     */
    private record Subscription(String destination, ListenerKind kind, Dispatcher dispatcher, String label) {
    }

    /** What one loaded class declared, plus the connections open for it in each tenant. */
    private static final class Registration {

        /** Subscriptions opened once per provisioned tenant. */
        private final List<Subscription> tenantSubscriptions;

        /** Subscriptions opened once for the whole deployment. */
        private final List<Subscription> globalSubscriptions;

        /** tenant id → the JMS connections open for this class in that tenant. */
        private final Map<String, List<Connection>> connectionsByTenant = new HashMap<>();

        /** The JMS connections open for this class's global destinations. */
        private List<Connection> globalConnections = List.of();

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

        void globalSubscribed(List<Connection> connections) {
            this.globalConnections = List.copyOf(connections);
            this.globalSubscribed = true;
        }

        boolean isSubscribed(String tenantId) {
            return connectionsByTenant.containsKey(tenantId);
        }

        void subscribed(String tenantId, List<Connection> connections) {
            connectionsByTenant.put(tenantId, connections);
        }

        List<Connection> openConnections() {
            List<Connection> all = new ArrayList<>(globalConnections);
            connectionsByTenant.values()
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
