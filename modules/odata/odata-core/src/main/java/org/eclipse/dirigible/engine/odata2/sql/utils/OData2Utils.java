/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.utils;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.*;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.exception.ODataHttpException;
import org.apache.olingo.odata2.api.exception.ODataRuntimeApplicationException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataErrorContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.KeyPredicate;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.expression.CommonExpression;
import org.apache.olingo.odata2.api.uri.expression.MemberExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderExpression;
import org.eclipse.dirigible.engine.odata2.sql.api.OData2Exception;
import org.eclipse.dirigible.engine.odata2.sql.builder.SQLSelectBuilder;

import java.util.*;

/**
 * The Class OData2Utils.
 */
public class OData2Utils {

    /**
     * Instantiates a new o data 2 utils.
     */
    private OData2Utils() {}

    /**
     * Fqn.
     *
     * @param type the type
     * @return the string
     */
    public static String fqn(EdmType type) {
        try {
            return fqn(type.getNamespace(), type.getName());
        } catch (EdmException e) {
            throw new RuntimeException("Unable to get the fully qualified name from type " + type, e);
        }
    }

    /**
     * Fqn.
     *
     * @param ns the ns
     * @param name the name
     * @return the string
     */
    public static String fqn(String ns, String name) {
        return ns + "." + name;
    }

    /**
     * Generates the next link for server-side paging. The next-link is based on the URI of the current
     * request, except that {@code $skip} or {@code $skiptoken} will be removed.
     *
     * @param context the context
     * @param top the top param
     * @param pagingSize the page size
     * @return the link
     * @throws ODataException in case of an error
     */
    public static String generateNextLink(ODataContext context, int top, int pagingSize) throws ODataException {
        String nextLink;

        final int skipToken;
        if (top > 0) {
            // We already have limited the maximum number of results for the current request
            // (which could be either a server-side paging
            // or a client-side $top).
            skipToken = top;
        } else {
            skipToken = pagingSize;
        }

        final StringBuilder nextLinkBuilder = new StringBuilder();
        String requestUri = percentEncodeNextLink(context.getPathInfo()
                                                         .getServiceRoot()
                                                         .relativize(context.getPathInfo()
                                                                            .getRequestUri())
                                                         .toString());
        nextLinkBuilder.append(requestUri);
        nextLinkBuilder.append(requestUri.contains("?") ? "&" : "?");
        nextLinkBuilder.append("$skiptoken=");
        nextLinkBuilder.append(skipToken);
        nextLink = nextLinkBuilder.toString();
        return nextLink;
    }

    /**
     * Gets the inline entry key value.
     *
     * @param values the values
     * @param inlineEntry the inline entry
     * @param inlinEntityKey the inlin entity key
     * @return the inline entry key value
     * @throws EdmException the edm exception
     */

    public static String getInlineEntryKeyValue(Map<String, Object> values, EdmTyped inlineEntry, EdmProperty inlinEntityKey)
            throws EdmException {
        if (inlineEntry.getType() instanceof EdmEntityType) {
            Object inlineEntryData = values.get(inlineEntry.getName());
            if (inlineEntryData instanceof ODataEntry) {
                Map<String, Object> inlineEntryDataProperties = ((ODataEntry) inlineEntryData).getProperties();

                for (int idx = 0; idx < inlineEntryDataProperties.size(); idx++) {
                    Object inlineKeyValue = inlineEntryDataProperties.get(inlinEntityKey.getName());
                    if (inlineKeyValue instanceof String) {
                        return (String) inlineKeyValue;
                    }
                    throw new OData2Exception("Invalid inline entity: the key " + inlinEntityKey.getName()//
                            + " of entity " + inlineEntry.getName() + " must be of type String!", HttpStatusCodes.BAD_REQUEST);
                }
            }
            throw new OData2Exception("Invalid inline entity: missing key " + inlinEntityKey.getName() + //
                    " of entity " + inlineEntry.getName(), HttpStatusCodes.BAD_REQUEST);
        }
        throw new OData2Exception("Invalid inline entity: required is EntityType for " + inlineEntry.getName(),
                HttpStatusCodes.BAD_REQUEST);
    }

    /**
     * Percent encode next link.
     *
     * @param link the link
     * @return the string
     */
    static String percentEncodeNextLink(final String link) {
        if (link == null) {
            return null;
        }
        return link.replaceAll("\\$skiptoken=.+?(?:&|$)", "")
                   .replaceAll("\\$skip=.+?(?:&|$)", "")
                   .replaceFirst("(?:\\?|&)$", ""); // Remove
                                                    // potentially
                                                    // trailing
                                                    // "?"
                                                    // or
                                                    // "&"
                                                    // left
                                                    // over
                                                    // from
                                                    // remove
                                                    // actions
    }

    /**
     * Checks for expand.
     *
     * @param info the info
     * @return true, if successful
     */
    public static boolean hasExpand(UriInfo info) {
        return hasExpand(info.getExpand());
    }

    /**
     * Checks if is empty.
     *
     * @param entityType the entity type
     * @param entityData the entity data
     * @return true, if is empty
     * @throws ODataException the o data exception
     */
    public static boolean isEmpty(EdmEntityType entityType, Map<String, Object> entityData) throws ODataException {
        final List<String> keyPropertyNames = entityType.getKeyPropertyNames();
        boolean allKeysNull = true;
        for (String keyPropertyName : keyPropertyNames) {
            EdmTyped keyProperty = entityType.getProperty(keyPropertyName);
            if (keyProperty instanceof EdmProperty) {
                Object pv = entityData.get(keyProperty.getName());
                if (pv != null) {
                    allKeysNull = false;
                    break;
                }
            }
        }
        return allKeysNull;
    }

    /**
     * Checks for expand.
     *
     * @param expand the expand
     * @return true, if successful
     */
    public static boolean hasExpand(List<ArrayList<NavigationPropertySegment>> expand) {
        return expand != null && !expand.isEmpty();
    }


    /**
     * Checks if is order by entity in expand.
     *
     * @param orderExpression the order expression
     * @param uriInfo the uri info
     * @return true, if is order by entity in expand
     * @throws EdmException the edm exception
     */
    public static boolean isOrderByEntityInExpand(OrderExpression orderExpression, UriInfo uriInfo) throws EdmException {
        MemberExpression memberExpression = (MemberExpression) orderExpression.getExpression();
        CommonExpression pathExpression = memberExpression.getPath();
        EdmType entityType = pathExpression.getEdmType();
        if (!hasExpand(uriInfo.getExpand())) {
            return false;
        }
        for (List<NavigationPropertySegment> expand : uriInfo.getExpand()) {
            for (NavigationPropertySegment segment : expand) {
                EdmEntitySet epxandEntity = segment.getTargetEntitySet();
                if (epxandEntity.getEntityType()
                                .getName()
                                .equals(entityType.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the tenant name from context.
     *
     * @param context the context
     * @return the tenant name from context
     */
    public static String getTenantNameFromContext(ODataContext context) {
        ODataContext parentContext = context.getBatchParentContext();
        if (parentContext != null) {
            while (parentContext.getBatchParentContext() != null) {
                parentContext = parentContext.getBatchParentContext();
            }
            return (String) parentContext.getParameter(OData2Constants.ODATA_CTX_PARAMETER_TENANT_NAME);
        } else {
            return (String) context.getParameter(OData2Constants.ODATA_CTX_PARAMETER_TENANT_NAME);
        }
    }

    /**
     * Equals.
     *
     * @param propertyValue1 the property value 1
     * @param propertyValue2 the property value 2
     * @return true, if successful
     */
    private static boolean equals(Object propertyValue1, Object propertyValue2) {
        if (propertyValue1 == propertyValue2)
            return true;
        if (propertyValue1 == null)
            return false;
        if (propertyValue2 == null)
            return false;
        return propertyValue1.equals(propertyValue2);
    }

    /**
     * Gets the status code for exception.
     *
     * @param e the e
     * @return the status code for exception
     */
    public static HttpStatusCodes getStatusCodeForException(Exception e) {
        HttpStatusCodes codeOnFailedRequest = HttpStatusCodes.INTERNAL_SERVER_ERROR;
        if (e instanceof OData2Exception) {
            codeOnFailedRequest = ((OData2Exception) e).getHttpStatus();
        } else if (e instanceof ODataApplicationException) {
            ODataApplicationException odae = (ODataApplicationException) e;
            codeOnFailedRequest = odae.getHttpStatus();
            if (codeOnFailedRequest == null) {
                codeOnFailedRequest = determineStatusCode(odae);
            }
        } else if (e instanceof ODataException) {
            codeOnFailedRequest = determineStatusCode((ODataException) e);
        } else if (e instanceof ODataRuntimeApplicationException) {
            codeOnFailedRequest = ((ODataRuntimeApplicationException) e).getHttpStatus();
            if (codeOnFailedRequest == null) {
                codeOnFailedRequest = HttpStatusCodes.INTERNAL_SERVER_ERROR;
            }
        }
        return codeOnFailedRequest;
    }

    /**
     * Determine status code.
     *
     * @param ode the ode
     * @return the http status codes
     */
    private static HttpStatusCodes determineStatusCode(ODataException ode) {
        HttpStatusCodes codeOnFailedRequest = HttpStatusCodes.INTERNAL_SERVER_ERROR;
        if (ode.isCausedByHttpException()) {
            codeOnFailedRequest = ((ODataHttpException) ode).getHttpStatus();
        } else if (ode.isCausedByMessageException()) {
            codeOnFailedRequest = HttpStatusCodes.BAD_REQUEST;
        } else if (ode.isCausedByApplicationException()) {
            codeOnFailedRequest = HttpStatusCodes.INTERNAL_SERVER_ERROR;
        }
        return codeOnFailedRequest;
    }

    /**
     * No content response.
     *
     * @param contentType the content type
     * @return the o data response
     */
    public static ODataResponse noContentResponse(String contentType) {
        ODataErrorContext errorContext = new ODataErrorContext();
        errorContext.setContentType(contentType);
        errorContext.setHttpStatus(HttpStatusCodes.NOT_FOUND);
        errorContext.setErrorCode(HttpStatusCodes.NOT_FOUND.toString());
        errorContext.setLocale(Locale.ENGLISH);
        errorContext.setMessage("No content");
        return EntityProvider.writeErrorDocument(errorContext);
    }

    /**
     * Gets the key predicate value by property name.
     *
     * @param propertyName the property name
     * @param keyPredicates the key predicates
     * @return the key predicate value by property name
     * @throws EdmException the edm exception
     */
    public static String getKeyPredicateValueByPropertyName(String propertyName, List<KeyPredicate> keyPredicates) throws EdmException {
        String keyPredicateValue = "";
        for (KeyPredicate keyPredicate : keyPredicates) {
            if (keyPredicate.getProperty()
                            .getName()
                            .equals(propertyName)) {
                keyPredicateValue = keyPredicate.getLiteral();
            }
        }
        return keyPredicateValue;
    }

    /**
     * Checks if is property parameter.
     *
     * @param property the property
     * @param query the query
     * @param entityType the entity type
     * @return true, if is property parameter
     * @throws EdmException the edm exception
     */
    public static boolean isPropertyParameter(EdmProperty property, SQLSelectBuilder query, EdmStructuralType entityType)
            throws EdmException {
        boolean isParameter = false;
        List<String> sqlTableParameters = query.getSQLTableParameters(entityType);

        if (sqlTableParameters.contains(property.getName())) {
            isParameter = true;
        }

        return isParameter;
    }
}
