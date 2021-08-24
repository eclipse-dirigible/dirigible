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
package org.eclipse.dirigible.api.v3.platform;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.FileSystemUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.api.scripting.ScriptingException;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.repository.api.RepositoryPath;
import org.eclipse.dirigible.repository.local.LocalCollection;

public class RegistryFacade {

	public static byte[] getContent(String path) throws IOException, ScriptingException {
		// Check in the repository first
		IResource resource = RepositoryFacade.getResource(toRepositoryPath(path));
		if (resource.exists()) {
			return resource.getContent();
		}

		// Check in the pre-delivered content
		InputStream in = RegistryFacade.class.getResourceAsStream("/META-INF/dirigible" + toResourcePath(path));
		try {
			if (in != null) {
				return IOUtils.toByteArray(in);
			} else {
				return null;
			} 
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public static String getText(String path) throws IOException, ScriptingException {
		byte[] content = getContent(path);
		if (content == null) {
			return null;
		}
		return new String(content);
	}

	private static String toRepositoryPath(String path) {
		return new RepositoryPath().append(IRepositoryStructure.PATH_REGISTRY_PUBLIC).append(path).build();
	}

	private static String toResourcePath(String path) {
		if (!path.startsWith(IRepositoryStructure.SEPARATOR)) {
			return IRepositoryStructure.SEPARATOR + path;
		}
		return path;
	}
	
	/**
	 * Find all the files matching the pattern
	 * 
	 * @param path the root path
	 * @param pattern the glob pattern
	 * @return the list of file names
	 * @throws IOException in case of an error
	 * @throws ScriptingException in case of an error
	 */
	public static String find(String path, String pattern) throws IOException, ScriptingException {
		ICollection collection = RepositoryFacade.getCollection(toRepositoryPath(path));
		if (collection.exists() && collection instanceof LocalCollection) {
			List<String> list = FileSystemUtils.find(((LocalCollection) collection).getFolder().getPath(), pattern);
			int repositoryRootLength = ((LocalCollection) collection.getRepository().getRoot()).getFolder().getPath().length() +
					IRepositoryStructure.PATH_REGISTRY_PUBLIC.length();
			List<String> prepared = new ArrayList<String>();
			list.forEach(item -> {
				String truncated = item.substring(repositoryRootLength);
				if (!IRepository.SEPARATOR.equals(File.separator)) {
					truncated.replace(File.separator, IRepository.SEPARATOR);
				}
				prepared.add(truncated);
			});
			return GsonHelper.GSON.toJson(prepared);
		}
		return "[]";
	}
}
