/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.api.kafka;

import static java.text.MessageFormat.format;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.api.scripting.ScriptingException;
import org.eclipse.dirigible.engine.api.script.ScriptEngineExecutorsManager;
import org.eclipse.dirigible.engine.js.api.IJavascriptEngineExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConsumerRunner implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerRunner.class);
	
	private static final String DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_MESSAGE = "messaging/wrappers/onMessage";
	private static final String DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_ERROR = "messaging/wrappers/onError";

	private final AtomicBoolean stopped = new AtomicBoolean(false);
	private final Consumer consumer;

	private String name;
	private String handler;
	private int timeout = 1000;

	public KafkaConsumerRunner(Consumer consumer, String name, String handler, int timeout) {
		this.consumer = consumer;
		this.name = name;
		this.handler = handler;
		this.timeout = timeout;
	}

	/**
	 * Start the consumer
	 */
	@Override
	public void run() {
		try {
			logger.info("Starting a Kafka listener for {} ...", this.name);
			consumer.subscribe(Arrays.asList(this.name));
			while (!stopped.get()) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(this.timeout));
				for (ConsumerRecord<String, String> record : records) {
					logger.trace(format("Start processing a received record in [{0}] by [{1}] ...", this.name, this.handler));
					if (this.handler != null) {
						Map<Object, Object> context = createMessagingContext();
						context.put("message", escapeCodeString(GsonHelper.GSON.toJson(record)));
						try {
							ScriptEngineExecutorsManager.executeServiceModule(IJavascriptEngineExecutor.JAVASCRIPT_TYPE_DEFAULT, DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_MESSAGE, context);
						} catch (ScriptingException e) {
							logger.error(e.getMessage(), e);
							try {
								context.put("error", escapeCodeString(e.getMessage()));
								ScriptEngineExecutorsManager.executeServiceModule(IJavascriptEngineExecutor.JAVASCRIPT_TYPE_DEFAULT, DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_ERROR, context);
							} catch (ScriptingException es) {
								logger.error(es.getMessage(), es);
							}
						}
					} else {
						logger.info(String.format("[Kafka Consumer] %s -  offset = %d, key = %s, value = %s%n", this.name, record.offset(), record.key(), record.value()));
					}
					logger.trace(format("Done processing the received record in [{0}] by [{1}]", this.name, this.handler));
				}
				
			}
		} catch (WakeupException e) {
			// Ignore exception if closing
			if (!stopped.get())
				throw e;
		} finally {
			consumer.close();
		}
	}

	/**
	 * Stop the consumer
	 */
	public void stop() {
		stopped.set(true);
		consumer.wakeup();
	}
	
	/**
	 * Create a context map and set the handler
	 * 
	 * @return the context map
	 */
	private Map<Object, Object> createMessagingContext() {
		Map<Object, Object> context = new HashMap<Object, Object>();
		context.put("handler", this.handler);
		return context;
	}
	
	/**
	 * Escape code string.
	 *
	 * @param raw
	 *            the raw
	 * @return the string
	 */
	private String escapeCodeString(String raw) {
		return raw.replace("'", "&amp;");
	}

}
