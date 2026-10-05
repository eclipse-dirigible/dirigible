/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.jms.JMSException;

import org.eclipse.dirigible.components.api.messaging.IntentSliceMessaging;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.store.config.CurrentTenantIdentifierResolverImpl;
import org.eclipse.dirigible.components.data.store.config.MultiTenantConnectionProviderImpl;
import org.eclipse.dirigible.components.data.store.java.manager.IntentSliceEntities;
import org.eclipse.dirigible.components.data.store.java.manager.JavaEntityManager;
import org.eclipse.dirigible.components.data.store.java.outbox.IntentSliceOutbox;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.components.engine.numbering.DocumentNumberService;
import org.eclipse.dirigible.components.engine.numbering.IntentSliceNumbering;
import org.eclipse.dirigible.components.listeners.service.MessageConsumer;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.db.Entity;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * One project's application, running in the test JVM: its classes from the test classpath, its
 * entities on a private H2 database, its components in the client bean container, and the platform
 * services they reach through {@code Beans}/{@code BeanProvider} - the entity store, document
 * numbering, the tenant - in a context holding nothing else. Built once per JVM and project, reset
 * before each test.
 */
final class SliceRuntime implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SliceRuntime.class);

    private static final String NUMBERS_EXTENSION = ".numbers";
    private static final String BUILD_FOLDER = "target";

    private final SliceDatabase database = new SliceDatabase();
    private final List<SentMessage> sent = Collections.synchronizedList(new ArrayList<>());
    private final MessageConsumer messageConsumer;
    private final MessageProducer messageProducer;
    private final JavaEntityManager entityManager;
    private final DocumentNumberService numbering;
    private final Map<String, NumberSeries> numberSeries;
    private final GenericApplicationContext context = new GenericApplicationContext();
    private final ComponentContainer container;
    private final Slice slice;

    SliceRuntime(Path projectRoot) {
        LOGGER.info("Starting the IntentSlice of [{}]", projectRoot);
        long started = System.nanoTime();
        DataSourcesManager dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDefaultDataSource()).thenReturn(database.dataSource());
        TenantContext tenantContext = new SingleTenantContext();
        entityManager =
                new JavaEntityManager(dataSourcesManager, new MultiTenantConnectionProviderImpl(dataSourcesManager, database.dataSource()),
                        new CurrentTenantIdentifierResolverImpl(tenantContext));
        JavaEntityStore store = new JavaEntityStore(entityManager, IntentSliceOutbox.create(dataSourcesManager, tenantContext));
        numbering = IntentSliceNumbering.create(dataSourcesManager);
        numberSeries = readNumberSeries(projectRoot);
        messageConsumer = mock(MessageConsumer.class, invocation -> {
            throw new UnsupportedOperationException("The IntentSlice has no broker to receive messages from");
        });
        messageProducer = recordingProducer();
        ClientBeansHolder beansHolder = new ClientBeansHolder();

        context.registerBean(JavaEntityStore.class, () -> store);
        context.registerBean(DocumentNumberService.class, () -> numbering);
        context.registerBean(ClientBeansHolder.class, () -> beansHolder);
        context.registerBean(TenantContext.class, () -> tenantContext);
        context.registerBean(Tenant.class, () -> SingleTenantContext.DEFAULT_TENANT);
        context.registerBean(DataSourcesManager.class, () -> dataSourcesManager);
        context.refresh();
        activate();

        String project = projectRoot.getFileName()
                                    .toString();
        List<LoadedClass> classes = ProjectClasses.load(projectRoot, Thread.currentThread()
                                                                           .getContextClassLoader())
                                                  .stream()
                                                  .map(type -> new LoadedClass(project, type.getName(), type, type.getClassLoader()))
                                                  .toList();
        Map<String, Class<?>> entities = new LinkedHashMap<>();
        classes.stream()
               .filter(loaded -> loaded.type()
                                       .isAnnotationPresent(Entity.class))
               .forEach(loaded -> entities.put(project + "::" + loaded.fqn(), loaded.type()));
        IntentSliceEntities.registerAll(entityManager, entities);

        container = new ComponentContainer(beansHolder);
        container.rebuild(classes);
        container.wiringWarnings()
                 .forEach((bean, warning) -> LOGGER.warn("IntentSlice wiring warning on [{}]: {}", bean, warning));
        if (!container.wiringErrors()
                      .isEmpty()) {
            close();
            throw new IllegalStateException("The application's components could not be wired: " + container.wiringErrors());
        }
        slice = new Slice(this);
        LOGGER.info("IntentSlice of [{}] started in [{}] ms: [{}] classes, [{}] entities", projectRoot,
                (System.nanoTime() - started) / 1_000_000, classes.size(), entities.size());
    }

    /** @return the test-facing handle of this runtime */
    Slice slice() {
        return slice;
    }

    /** @return the client bean container holding the application's components */
    ComponentContainer container() {
        return container;
    }

    /** @return every message sent since the last reset, in order */
    List<SentMessage> sent() {
        synchronized (sent) {
            return List.copyOf(sent);
        }
    }

    /**
     * Makes this runtime the one the application's static entry points reach - {@code Beans},
     * {@code BeanProvider}, the messaging facade. Several projects may be sliced in one JVM; the test
     * about to run decides which one is live.
     */
    void activate() {
        new BeanProvider().setApplicationContext(context);
        IntentSliceMessaging.install(messageConsumer, messageProducer);
    }

    /**
     * Puts the application back into the state of a fresh tenant: no rows, identities restarting at 1,
     * every declared number series at its first value, no messages sent.
     */
    void reset() {
        try {
            database.clear();
            for (NumberSeries series : numberSeries.values()) {
                numbering.provision(series.name(), series.prefix(), series.size());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to reset the IntentSlice database", ex);
        }
        sent.clear();
    }

    @Override
    public void close() {
        entityManager.destroy();
        context.close();
        try {
            database.close();
        } catch (SQLException ex) {
            LOGGER.warn("Failed to shut the IntentSlice database down", ex);
        }
    }

    private MessageProducer recordingProducer() {
        MessageProducer producer = mock(MessageProducer.class);
        Answer<Boolean> record = invocation -> sent.add(new SentMessage(invocation.getArgument(0), invocation.getArgument(1)));
        try {
            doAnswer(record).when(producer)
                            .sendMessageToTopic(anyString(), anyString());
            doAnswer(record).when(producer)
                            .sendMessageToQueue(anyString(), anyString());
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to stub the IntentSlice message producer", ex);
        }
        return producer;
    }

    /**
     * The number series the project declares in its {@code .numbers} files, which the platform's
     * synchronizer would provision on publish.
     */
    private static Map<String, NumberSeries> readNumberSeries(Path projectRoot) {
        Map<String, NumberSeries> series = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(projectRoot)) {
            for (Path file : files.filter(path -> !path.startsWith(projectRoot.resolve(BUILD_FOLDER)))
                                  .filter(path -> path.getFileName()
                                                      .toString()
                                                      .endsWith(NUMBERS_EXTENSION))
                                  .toList()) {
                JsonObject declarations = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                                                    .getAsJsonObject();
                for (JsonElement element : declarations.getAsJsonArray("series")) {
                    JsonObject declaration = element.getAsJsonObject();
                    String name = declaration.get("name")
                                             .getAsString();
                    String prefix = Optional.ofNullable(declaration.get("prefix"))
                                            .map(JsonElement::getAsString)
                                            .orElse("");
                    series.put(name, new NumberSeries(name, prefix, declaration.get("size")
                                                                               .getAsInt()));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read the number series of [" + projectRoot + "]", ex);
        }
        return series;
    }

    /** One declared document-number series. */
    private record NumberSeries(String name, String prefix, int size) {
    }
}
