/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
import { rs } from "@aerokit/sdk/http";
import { streams } from "@aerokit/sdk/io";
import { upload } from '@aerokit/sdk/http';

import * as documentsProcessor from "./processors/documentsProcessor";
import * as imageProcessor from "./processors/imageProcessor";

import * as zipUtils from "../utils/cmis/zip";

import { unescapePath, getNameFromPath } from "../utils/string";

rs.service()
	.resource("")
	.get(function (ctx, request, response) {
		let path = unescapePath(ctx.queryParameters.path || "/");

		let result = documentsProcessor.list(path);
		response.setContentType("application/json");
		response.println(JSON.stringify(result));
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
        throw error;
	})
	.post(function (ctx, request, response) {
		if (!upload.isMultipartContent()) {
			throw new Error("The request's content must be 'multipart'");
		}
		let path = unescapePath(ctx.queryParameters.path || "/");
		let overwrite = ctx.queryParameters.overwrite || false;
		let documents = upload.parseRequest();
		let result = documentsProcessor.create(path, documents, overwrite);
		response.setContentType("application/json");
		response.println(JSON.stringify(result));
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.put(function (ctx, request, response) {
		let { path, name } = request.getJSON();
		if (!(path && name)) {
			throw new Error("Request body must contain 'path' and 'name'");
		}

		documentsProcessor.rename(path, name);
		response.setContentType("application/json");
		response.setStatus(response.OK);
		response.print(JSON.stringify(name));
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.delete(function (ctx, request, response) {
		let forceDelete = ctx.queryParameters.force === "true" ? true : false;
		let objects = request.getJSON();

		documentsProcessor.remove(objects, forceDelete);

		response.setStatus(response.NO_CONTENT);
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.resource("folder")
	.post(function (ctx, request, response) {
		let { parentFolder, name } = request.getJSON();
		if (!(parentFolder && name)) {
			throw new Error("Request body must contain 'parentFolder' and 'name'");
		}

		let result = documentsProcessor.createFolder(parentFolder, name);
		response.setContentType("application/json");
		response.setStatus(response.CREATED);
		response.print(JSON.stringify(result));
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.resource("zip")
	.get(function (ctx, request, response) {
		;
		if (!ctx.queryParameters.path) {
			throw new Error("Query parameter 'path' must be provided.");
		}
		let path = unescapePath(ctx.queryParameters.path);
		let name = getNameFromPath(path);
		let outputStream = response.getOutputStream();
		response.setContentType("application/zip");
		response.addHeader("Content-Disposition", "attachment;filename=\"" + name + ".zip\"");
		zipUtils.makeZip(path, outputStream);
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.post(function (ctx, request, response) {
		if (!upload.isMultipartContent()) {
			throw new Error("The request's content must be 'multipart'");
		}
		let path = unescapePath(ctx.queryParameters.path || "/");
		let documents = upload.parseRequest();
		let result = [];
		for (let i = 0; i < documents.size(); i++) {
			result.push(zipUtils.unpackZip(path, documents.get(i)));
		}
		response.setContentType("application/json");
		response.println(JSON.stringify(result));
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.resource("image")
	.post(function (ctx, request, response) {
		if (!upload.isMultipartContent()) {
			throw new Error("The request's content must be 'multipart'");
		}
		let path = unescapePath(ctx.queryParameters.path || "/");
		let width = ctx.queryParameters.width;
		let height = ctx.queryParameters.height;
		let documents = upload.parseRequest();

		let result = imageProcessor.resize(path, documents, width, height);
		response.setContentType("application/json");
		response.println(JSON.stringify(result));
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.resource("preview")
	.get(function (ctx, request, response) {
		if (!ctx.queryParameters.path) {
			throw new Error("Query parameter 'path' must be provided.");
		}
		let path = unescapePath(ctx.queryParameters.path);

		let document = documentsProcessor.get(path);

		response.setContentType(document.contentType);
		response.write(document.content.getStream().readBytes());
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.resource("download")
	.get(function (ctx, request, response) {
		if (!ctx.queryParameters.path) {
			throw new Error("Query parameter 'path' must be provided.");
		}
		let path = unescapePath(ctx.queryParameters.path);

		let document = documentsProcessor.get(path);

		response.setContentType(document.contentType);
		response.addHeader("Content-Disposition", "attachment;filename=\"" + document.name + "\"");
		streams.copy(document.content.getStream(), response.getOutputStream());
	})
	.catch(function (ctx, error, request, response) {
		printError(response, response.BAD_REQUEST, 4, error);
	})
	.execute();

function printError(response, httpCode, errCode, error) {
    const errMessage = error.message;
	let body = {
		err: {
			code: errCode,
			message: errMessage
		}
	};
	console.error(JSON.stringify(body));
	response.setContentType("application/json");
	response.setStatus(httpCode);
	response.println(JSON.stringify(body));
	throw error;
}
