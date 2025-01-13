/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
(function (name, context, definition) {
	// AMD -> CommonJS -> Globals
	if (typeof module != 'undefined' && module.exports)
		module.exports = definition();
	else if (typeof define == 'function' && define.amd)
		define(name, definition);
	else
		context[name] = definition();
}('FramesMessageHub', this, function () {
	/**
	 * @class FramesMessageHub A Publish-Subscribe mechanism for secure cross-domain message exchange between browser windows/iframes entirely clientside, built on the HTML5 postMessage
	 framework.
	 *
	 * @param {Object} [settings] parameters overriding defaults
	 * @param {string} [settings.defaultTopic] an optional default topic sink for all messages posted without explicit topic. Note that the subscribers will still need to subscribe exactly to this topic name. No default value.
	 * @param {Object} [settings.hubWindow] optional window object used to propagate the 'message' event. Defaults to window.top
	 * @param {string[]} [settings.allowedOrigins] messages are inspected for their origin and then this setting is consulted to check if their origin is allowed or not and prevent message spoofing. Use for whitelisting origins. By default only the same origin as the current window's origin is allowed: [window.location.origin].
	 * @param {Object} [settings.targetOrigin] targetOrigin specifies the target of the message (the hubWindow) as a URI string. The event will make it ot its destination only if the destination window location matches this URL. Though it is possible to denote 'no preference' by using the *' alias, this is highly NOT recommended. Always specify the hubWindow targetOrigin URI to avoid spoofing messages. Defaults to settings.hubWindow.location.origin.
	 */
	function FramesMessageHub(settings) {
		this.settings = settings || {};
		let current = window;
		if (!window.GoldenLayout) {
			while (current !== top) {
				current = current.parent;
				if (current.GoldenLayout) {
					break;
				}
			}
		}

		this.settings.hubWindow = this.settings.hubWindow || current || top;
		this.settings.allowedOrigins = this.settings.allowedOrigins || [location.origin];
		this.settings.targetOrigin = this.settings.targetOrigin || this.settings.hubWindow.location.origin;
	}

	/**
	 * Posts a message to a topic. Subscribed message handler will be notified for this event and invoked to handle it (see the subscribe method for details).
	 *
	 * @param {Object|string} message - a JSON object message delivered in the topic. Alternatively, a simple string can be provided and it will be automatically wrapped in a JSON object according to the expected schema.
	 * @param {Object} message.data - the message payload.
	 * @param {string} [message.defaultTopic] - (optional) the message topic. Instead of provisioning a second argument to this method for a topic, the message object parameter topic can be supplied with the same purpose. In case both are present, the  method argument will take precedence.
	 */
	FramesMessageHub.prototype.post = function (msg = {}, topic) {
		// make it easy for simple string messages
		if (typeof msg === 'string') {
			msg = {
				data: msg
			};
		}
		topic = topic || this.settings.defaultTopic; //in case this client has been setup with a fixed topic and none overriding topic argument has been provided to the method.
		if (topic !== undefined) {
			//inject the topic, if any, into the message.
			msg.topic = topic;
		}
		this.settings.hubWindow.postMessage(msg, this.settings.targetOrigin);
	};

	let onMessageReceived = function (messageHandler, topic, e) {
		//check if this message origin is whitelisted
		if (this.settings.allowedOrigins === undefined || this.settings.allowedOrigins.length == 0)
			console.warn('[FramesMessageHub] settings.allowedOrigins is not used. This may impose security risks.');
		else {
			if (this.settings.allowedOrigins.indexOf(e.origin) < 0) {
				console.warn('[FramesMessageHub] message blocked from non-whitelisted origin: ' + e.origin);
				return;
			}
		}
		//ensure that the handler will be invoked only for messages posted in the topic it was subscribed to
		if (topic !== this.settings.defaultTopic && e.data && e.data.topic !== topic) {
			//console.warn('[FramesMessageHub] message is not for this subscription topic: ' + e.data.topic);
			return;
		}
		let message = e.data;
		messageHandler.apply(this, [message, e]);
	}

	/**
	 * Subscribes a handler function invoked upon posting messages in a topic.
	 *
	 * @param {FramesMessageHub~messageHandler} messageHandler - a function processing messages received in the subscribed topic.
	 * @returns - a reference to the function subscirbed as event listener. Note that to unsubscribe you need to use this reference instead of the messageHandler provided as argument to this method.
	 */
	FramesMessageHub.prototype.subscribe = function (messageHandler, topic) {
		//TODO: use stadnard jsonschema and json schema validation
		//settings.dataSchema = settings.dataSchema;
		//if we expect messages delegate them to message handler
		let handler;
		if (messageHandler && (typeof messageHandler === 'function')) {
			topic = topic || this.settings.defaultTopic;
			if (topic === undefined)
				throw Error('[FramesMessageHub] Invallid argument: cannot subscribe without topic. Either specifi topic as argument to the subscribe method or specify a default sink topic in settings parameter of the FramesMessageHub constructor');
			//ensure a unique function instance per handler to support single handler per multiple topics (controller-like pattern)
			handler = function (messageHandler, topic, evt) {
				onMessageReceived.call(this, messageHandler, topic, evt);
			}.bind(this, messageHandler, topic);
			this.settings.hubWindow.addEventListener("message", handler, false);
		}
		return handler;
	};

	/**
	 * A handler function processing messages posted in a topic, to which it has been subscribed to with subscribe method.
	 *
	 * @callback FramesMessageHub~messageHandler
	 * @param {Object|string} message - a JSON object message delivered in the topic. Alternatively, a simple string can be provided and it will be automatically wrapped in a JSON object according to the expected schema.
	 * @param {Object} message.data - the message payload.
	 * @param {string} message.topic - the message topic.
	 * @param {Object} event - the original 'message' event that invoked this handler.
	 */

	/**
	 * Unsubscribes a handler function that was subscribed previously with the subscribe method from listening for messages in a topic. 
	 *
	 * @callback {FramesMessageHub~messageHandler} - The handler to be unsubscribed. Must be the function returned by the subscribe mehtod.
	 */
	FramesMessageHub.prototype.unsubscribe = function (messageHandler) {
		this.settings.hubWindow.removeEventListener("message", messageHandler, false);
	}

	return FramesMessageHub;
}));
