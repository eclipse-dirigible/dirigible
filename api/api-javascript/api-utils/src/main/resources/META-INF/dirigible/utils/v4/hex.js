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
/**
 * API v4 Files
 * 
 * Note: This module is supported only with the Mozilla Rhino engine
 */

var streams = require('io/v4/streams');
var bytes = require("io/v4/bytes");

/**
 * Encode the input (text or byte array) as text
 */
exports.encode = function(input) {
	return bytes.byteArrayToText(exports.encodeAsNativeBytes(input));
};

/**
 * Encode the input (text or byte array) as byte array
 */
exports.encodeAsBytes = function(input) {
	return bytes.toJavaScriptBytes(exports.encodeAsNativeBytes(input));
};

/**
 * Encode the input (text or byte array) as java native byte array
 */
exports.encodeAsNativeBytes = function(input) {
	var data = input;
	var native;
	if (typeof data === 'string') {
		var baos = streams.createByteArrayOutputStream();
		baos.writeText(data);
		native = baos.getBytesNative();
	} else if (Array.isArray(data)) {
		native = bytes.toJavaBytes(data);
	}
	
	var output = org.eclipse.dirigible.api.v3.utils.HexFacade.encodeNative(native);
	return output;
};

/**
 * Decode the input (text or byte array) as text
 */
exports.decode = function(input) {
	var output = exports.decodeAsNativeBytes(input);
	if (output && output !== null) {
		var result = bytes.toJavaScriptBytes(output);
		return result;
	}
	return output;
};

/**
 * Decode the input (text or byte array) as java native byte array
 */
exports.decodeAsNativeBytes = function(input) {
	var data = input;
	var native;
	if (typeof data === 'string') {
		var baos = streams.createByteArrayOutputStream();
		baos.writeText(data);
		native = baos.getBytesNative();
	} else if (Array.isArray(data)) {
		native = bytes.toJavaBytes(data);
	}
	var output = org.eclipse.dirigible.api.v3.utils.HexFacade.decodeNative(native);
	return output;
};
