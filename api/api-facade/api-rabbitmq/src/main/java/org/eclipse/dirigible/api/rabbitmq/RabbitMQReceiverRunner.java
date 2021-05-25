/*
 * Copyright (c) 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.api.rabbitmq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.dirigible.commons.api.scripting.ScriptingException;
import org.eclipse.dirigible.engine.api.script.ScriptEngineExecutorsManager;
import org.eclipse.dirigible.engine.js.api.IJavascriptEngineExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RabbitMQReceiverRunner implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(RabbitMQReceiverRunner.class);

	private static final String DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_MESSAGE = "messaging/wrappers/onMessage";
	private static final String DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_ERROR = "messaging/wrappers/onError";

	private final Connection connection;
	private final Channel channel;
	private String queue;
	private String handler;

	private final AtomicBoolean stopped = new AtomicBoolean(false);

	public RabbitMQReceiverRunner(Connection connection, Channel channel, String queue, String handler) {
		this.connection = connection;
		this.channel = channel;
		this.queue = queue;
		this.handler = handler;
	}

	/**
	 * Start the consumer
	 */
	@Override
	public void run() {
		try {
			logger.info("Starting a RabbitMQ receiver for: " + this.queue);
			channel.queueDeclare(queue, false, false, false, null);
			while (!stopped.get()) {
				Map<Object, Object> context = createMessagingContext();
				Consumer consumer = new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
							byte[] body) throws IOException {
						String message = new String(body, "UTF-8");
						context.put("message", message);
						try {
							ScriptEngineExecutorsManager.executeServiceModule(
									IJavascriptEngineExecutor.JAVASCRIPT_TYPE_DEFAULT,
									DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_MESSAGE, context);
						} catch (ScriptingException e) {
							logger.error(e.getMessage(), e);
							try {
								context.put("error", escapeCodeString(e.getMessage()));
								ScriptEngineExecutorsManager.executeServiceModule(
										IJavascriptEngineExecutor.JAVASCRIPT_TYPE_DEFAULT,
										DIRIGIBLE_MESSAGING_WRAPPER_MODULE_ON_ERROR, context);
							} catch (ScriptingException es) {
								logger.error(es.getMessage(), es);
							}
						}
					}
				};
				channel.basicConsume(queue, true, consumer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				channel.queueDelete(queue);
				channel.close();
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Stop the consumer
	 */
	public void stop() {
		stopped.set(true);
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
	 * @param raw the raw
	 * @return the string
	 */
	private String escapeCodeString(String raw) {
		return raw.replace("'", "&amp;");
	}

}
