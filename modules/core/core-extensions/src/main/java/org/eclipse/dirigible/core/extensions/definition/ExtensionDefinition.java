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
package org.eclipse.dirigible.core.extensions.definition;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;

/**
 * The ExtensionDefinition Entity.
 */
@Table(name = "DIRIGIBLE_EXTENSIONS")
public class ExtensionDefinition {

	@Id
	@Column(name = "EXTENSION_LOCATION", columnDefinition = "VARCHAR", nullable = false, length = 255)
	private String location;

	@Column(name = "EXTENSION_EXTENSIONPOINT_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
	private String extensionPoint;

	@Column(name = "EXTENSION_MODULE", columnDefinition = "VARCHAR", nullable = false, length = 255)
	private String module;

	@Column(name = "EXTENSION_DESCRIPTION", columnDefinition = "VARCHAR", nullable = true, length = 1024)
	private String description;

	@Column(name = "EXTENSION_CREATED_BY", columnDefinition = "VARCHAR", nullable = false, length = 32)
	private String createdBy;

	@Column(name = "EXTENSION_CREATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
	private Timestamp createdAt;

	/**
	 * Gets the location.
	 *
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Sets the location.
	 *
	 * @param location
	 *            the new location
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * Gets the extension point.
	 *
	 * @return the extension point
	 */
	public String getExtensionPoint() {
		return extensionPoint;
	}

	/**
	 * Sets the extension point.
	 *
	 * @param extensionPoint
	 *            the new extension point
	 */
	public void setExtensionPoint(String extensionPoint) {
		this.extensionPoint = extensionPoint;
	}

	/**
	 * Gets the module.
	 *
	 * @return the module
	 */
	public String getModule() {
		return module;
	}

	/**
	 * Sets the module.
	 *
	 * @param module
	 *            the new module
	 */
	public void setModule(String module) {
		this.module = module;
	}

	/**
	 * Gets the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param description
	 *            the new description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the created by.
	 *
	 * @return the created by
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * Sets the created by.
	 *
	 * @param createdBy
	 *            the new created by
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * Gets the created at.
	 *
	 * @return the created at
	 */
	public Timestamp getCreatedAt() {
		if (createdAt == null) {
			return null;
		}
		return new Timestamp(createdAt.getTime());
	}

	/**
	 * Sets the created at.
	 *
	 * @param createdAt
	 *            the new created at
	 */
	public void setCreatedAt(Timestamp createdAt) {
		if (createdAt == null) {
			this.createdAt = null;
			return;
		}
		this.createdAt = new Timestamp(createdAt.getTime());
	}

	/**
	 * Creates ExtensionDefinition from JSON.
	 *
	 * @param json
	 *            the JSON
	 * @return the extension definition
	 */
	public static ExtensionDefinition fromJson(String json) {
		return GsonHelper.GSON.fromJson(json, ExtensionDefinition.class);
	}

	/**
	 * Converts ExtensionDefinition to JSON.
	 *
	 * @return the JSON
	 */
	public String toJson() {
		return GsonHelper.GSON.toJson(this, ExtensionDefinition.class);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return toJson();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((description == null) ? 0 : description.hashCode());
		result = (prime * result) + ((extensionPoint == null) ? 0 : extensionPoint.hashCode());
		result = (prime * result) + ((location == null) ? 0 : location.hashCode());
		result = (prime * result) + ((module == null) ? 0 : module.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ExtensionDefinition other = (ExtensionDefinition) obj;
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (extensionPoint == null) {
			if (other.extensionPoint != null) {
				return false;
			}
		} else if (!extensionPoint.equals(other.extensionPoint)) {
			return false;
		}
		if (location == null) {
			if (other.location != null) {
				return false;
			}
		} else if (!location.equals(other.location)) {
			return false;
		}
		if (module == null) {
			if (other.module != null) {
				return false;
			}
		} else if (!module.equals(other.module)) {
			return false;
		}
		return true;
	}

}
