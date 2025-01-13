/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.processor;

import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmNavigationProperty;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.callback.*;
import org.apache.olingo.odata2.api.exception.ODataApplicationException;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.api.uri.ExpandSelectTreeNode;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.UriInfo;
import org.apache.olingo.odata2.api.uri.UriParser;
import org.eclipse.dirigible.engine.odata2.sql.utils.OData2Utils;

import java.net.URI;
import java.util.*;

/**
 * The Class ExpandCallBack.
 */
public class ExpandCallBack implements OnWriteFeedContent, OnWriteEntryContent, ODataCallback {

    /** The base uri. */
    private final URI baseUri;

    /** The expand list. */
    private final List<ArrayList<NavigationPropertySegment>> expandList;

    /**
     * Instantiates a new expand call back.
     *
     * @param baseUri the base uri
     * @param expandList the expand list
     */
    private ExpandCallBack(final URI baseUri, final List<ArrayList<NavigationPropertySegment>> expandList) {
        this.baseUri = baseUri;
        this.expandList = expandList;
    }

    /**
     * Gets the callbacks.
     *
     * @param baseUri the base uri
     * @param expandSelectTreeNode the expand select tree node
     * @param expandList the expand list
     * @return the callbacks
     */
    public static Map<String, ODataCallback> getCallbacks(final URI baseUri, final ExpandSelectTreeNode expandSelectTreeNode,
            final List<ArrayList<NavigationPropertySegment>> expandList) {
        Map<String, ODataCallback> callbacks = new HashMap<>();
        for (String navigationPropertyName : expandSelectTreeNode.getLinks()
                                                                 .keySet()) {
            callbacks.put(navigationPropertyName, new ExpandCallBack(baseUri, expandList));
        }
        return callbacks;
    }

    /**
     * Write entry with expand.
     *
     * @param context the context
     * @param uriInfo the uri info
     * @param entryAccumulator the entry accumulator
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    public static ODataResponse writeEntryWithExpand(ODataContext context, UriInfo uriInfo,
            ResultSetReader.ExpandAccumulator entryAccumulator, final String contentType) throws ODataException {
        if (entryAccumulator == null) {
            return OData2Utils.noContentResponse(contentType);
        }
        return writeEntryWithExpand(context, uriInfo, entryAccumulator.renderForExpand(), contentType);
    }

    /**
     * Write entry with expand.
     *
     * @param context the context
     * @param uriInfo the uri info
     * @param entry the entry
     * @param contentType the content type
     * @return the o data response
     * @throws ODataException the o data exception
     */
    public static ODataResponse writeEntryWithExpand(ODataContext context, UriInfo uriInfo, Map<String, Object> entry,
            final String contentType) throws ODataException {
        if (entry == null || entry.isEmpty()) {
            // Important NOTE:
            // If we use "Not found" and we return a payload, it has to be a standard
            // conform error response.
            // If we raised a ODataApplicationException, the exception would be logged and
            // potentially spam the
            // log. After discussing the matter, we decided to write the error response
            // directly and not log the
            // error as 404 is usually not related to coding bugs but just not existent
            // keys.
            return OData2Utils.noContentResponse(contentType);
        }
        final EdmEntitySet targetEntitySet = uriInfo.getTargetEntitySet();
        final ExpandSelectTreeNode expandSelectTree = UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand());

        EntityProviderWriteProperties writeProperties = EntityProviderWriteProperties.serviceRoot(context.getPathInfo()
                                                                                                         .getServiceRoot())
                                                                                     .expandSelectTree(expandSelectTree)//
                                                                                     .callbacks(ExpandCallBack.getCallbacks(context,
                                                                                             uriInfo, Collections.singletonList(entry)))
                                                                                     .build();

        return EntityProvider.writeEntry(contentType, targetEntitySet, entry, writeProperties);
    }


    /**
     * Write feed with expand.
     *
     * @param context the context
     * @param uriInfo the uri info
     * @param entitiesFeed the entities feed
     * @param contentType the content type
     * @param count the count
     * @param nextLink the next link
     * @return the o data response
     * @throws ODataException the o data exception
     */
    public static ODataResponse writeFeedWithExpand(ODataContext context, UriInfo uriInfo,
            List<ResultSetReader.ExpandAccumulator> entitiesFeed, final String contentType, Integer count, String nextLink)
            throws ODataException {

        List<Map<String, Object>> entities = new ArrayList<>();
        for (ResultSetReader.ExpandAccumulator acc : entitiesFeed) {
            entities.add(acc.renderForExpand());
        }

        EntityProviderWriteProperties feedProperties = EntityProviderWriteProperties.serviceRoot(context.getPathInfo()
                                                                                                        .getServiceRoot())
                                                                                    .inlineCountType(uriInfo.getInlineCount())
                                                                                    .inlineCount(count)
                                                                                    .expandSelectTree(UriParser.createExpandSelectTree(
                                                                                            uriInfo.getSelect(), uriInfo.getExpand()))
                                                                                    .callbacks(ExpandCallBack.getCallbacks(context, uriInfo,
                                                                                            entities))//
                                                                                    .nextLink(nextLink)
                                                                                    .build();

        List<Map<String, Object>> feedEntities = new ArrayList<>();
        feedEntities.addAll(entities);
        return EntityProvider.writeFeed(contentType, uriInfo.getTargetEntitySet(), feedEntities, feedProperties);
    }

    /**
     * Gets the callbacks.
     *
     * @param context the context
     * @param uriInfo the uri info
     * @param feedData the feed data
     * @return the callbacks
     * @throws ODataException the o data exception
     */
    private static Map<String, ODataCallback> getCallbacks(ODataContext context, UriInfo uriInfo, List<Map<String, Object>> feedData)
            throws ODataException {
        return ExpandCallBack.getCallbacks(context.getPathInfo()
                                                  .getServiceRoot(), //
                UriParser.createExpandSelectTree(uriInfo.getSelect(), uriInfo.getExpand()), //
                uriInfo.getExpand());
    }

    /**
     * Retrieve entry result.
     *
     * @param context the context
     * @return the write entry callback result
     * @throws ODataApplicationException the o data application exception
     */
    @Override
    public WriteEntryCallbackResult retrieveEntryResult(final WriteEntryCallbackContext context) throws ODataApplicationException {
        WriteEntryCallbackResult navigationEntryData = new WriteEntryCallbackResult();
        Map<String, Object> entry = context.getEntryData();
        EdmNavigationProperty currentNavigationProperty = context.getNavigationProperty();
        try {
            List<Map<String, Object>> expandEntries =
                    (List<Map<String, Object>>) entry.get(OData2Utils.fqn(currentNavigationProperty.getType()));
            if (null != expandEntries && !expandEntries.isEmpty()) {
                if (expandEntries.size() > 1) {
                    throw new IllegalStateException(
                            "More than 1 entries are not expected for association with multiplicity different than MANY");
                }
                navigationEntryData.setEntryData(expandEntries.get(0));
                navigationEntryData.setInlineProperties(getInlineEntityProviderProperties(context));
            }
        } catch (EdmException e) {
            throw new ODataApplicationException(e.getMessage(), Locale.getDefault(), e);
        }
        return navigationEntryData;
    }

    /**
     * Retrieve feed result.
     *
     * @param context the context
     * @return the write feed callback result
     * @throws ODataApplicationException the o data application exception
     */
    @Override
    public WriteFeedCallbackResult retrieveFeedResult(final WriteFeedCallbackContext context) throws ODataApplicationException {
        WriteFeedCallbackResult expandedPropertyFeed = new WriteFeedCallbackResult();
        HashMap<String, Object> entryData = (HashMap<String, Object>) context.getEntryData();
        try {
            // the data must be a map, for each navigation property the data should be in an entry with key the
            // FQN of the property
            List<Map<String, Object>> navigationEntryData =
                    (List<Map<String, Object>>) entryData.get(OData2Utils.fqn(context.getNavigationProperty()
                                                                                     .getType()));
            expandedPropertyFeed.setFeedData(navigationEntryData);
            expandedPropertyFeed.setInlineProperties(getInlineEntityProviderProperties(context));
        } catch (EdmException e) {
            throw new ODataApplicationException(e.getMessage(), Locale.getDefault(), e);
        }
        return expandedPropertyFeed;
    }

    /**
     * Gets the inline entity provider properties.
     *
     * @param context the context
     * @return the inline entity provider properties
     * @throws EdmException the edm exception
     */
    private EntityProviderWriteProperties getInlineEntityProviderProperties(final WriteCallbackContext context) throws EdmException {
        return EntityProviderWriteProperties.serviceRoot(baseUri) //
                                            .callbacks(getCallbacks(baseUri, context.getCurrentExpandSelectTreeNode(), expandList)) //
                                            .expandSelectTree(context.getCurrentExpandSelectTreeNode()) //
                                            .build();
    }
}
