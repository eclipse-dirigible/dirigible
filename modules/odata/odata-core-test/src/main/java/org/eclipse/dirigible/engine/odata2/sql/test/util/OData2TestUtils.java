/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql.test.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.apache.olingo.odata2.annotation.processor.core.util.AnnotationHelper;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.FullQualifiedName;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataErrorContext;

/**
 * OData2TestUtils.
 */
public class OData2TestUtils {

    /**
     * Instantiates a new o data 2 test utils.
     */
    private OData2TestUtils() {}

    /**
     * Helper method to retrieve the {@link ODataEntry} from the response object returned by
     * OData2RequestBuilder#executeRequest().
     *
     * @param response the response returned by OData2RequestBuilder#executeRequest()
     * @param entitySet the EdmEntitySet used to parse the response
     * @return the ODataEntry
     * @throws IOException in case of error
     * @throws ODataException in case of error
     */
    public static ODataEntry retrieveODataEntryFromResponse(final Response response, final EdmEntitySet entitySet)
            throws IOException, ODataException {
        try (InputStream content = response.readEntity(InputStream.class)) {

            ODataEntry entry = EntityProvider.readEntry(response.getMediaType()
                                                                .toString(),
                    entitySet, content, EntityProviderReadProperties.init()
                                                                    .build());
            return entry;
        }
    }

    /**
     * Helper method to retrieve the ODataFeed from the response object returned by
     * OData2RequestBuilder#executeRequest().
     *
     * @param response the response returned by OData2RequestBuilder#executeRequest()
     * @param entitySet the EdmEntitySet used to parse the response
     * @return the ODataFeed
     * @throws IOException in case of error
     * @throws ODataException in case of error
     */
    public static ODataFeed retrieveODataFeedFromResponse(final Response response, final EdmEntitySet entitySet)
            throws IOException, ODataException {
        try (InputStream content = response.readEntity(InputStream.class)) {

            ODataFeed feed = EntityProvider.readFeed(response.getMediaType()
                                                             .toString(),
                    entitySet, content, EntityProviderReadProperties.init()
                                                                    .build());
            return feed;
        }
    }

    /**
     * Helper method to retrieve the ODataErrorContext representing the error response to a failed call
     * to an OData API from the response object returned by OData2RequestBuilder#executeRequest().
     *
     * @param response the object containing the error response
     * @return the ODataErrorContext representing the content of the returned error document.
     *         <b>NOTE:</b> The used parser does not parse the message's locale so it will always be
     *         <code>null</code>.
     * @throws IOException in case of error
     * @throws EntityProviderException in case of error
     */
    public static ODataErrorContext retrieveODataErrorDocumentFromResponse(final Response response)
            throws IOException, EntityProviderException {
        try (InputStream content = response.readEntity(InputStream.class)) {
            return EntityProvider.readErrorDocument(content, response.getMediaType()
                                                                     .toString());
        }
    }

    /**
     * Validates an ODataFeed against a list of Maps, which represents the expected properties of each
     * entry. The expected properties define only a minimal set of properties, which have to be
     * contained in the ODataEntrys. The ODataEntrys can contain more entries. The properties of the
     * entries itself can contain entries or maps, which are validated recursively. The validation is
     * independent of the order of the entries. The result is returned as a {@link Pair}, where the
     * first member gives the result of the validation as an Boolean value and the second the reason, if
     * the validation has failed.
     *
     * @param expectedEntries a list of maps which represent the expected properties of the ODataEntrys
     * @param oDataFeed an ODataFeed, which shall be validated
     * @return a Pair where the first member is true or false depending on the result of the validation
     *         and the second gives the reason, if the validation has failed
     */
    public static Pair<Boolean, String> validateODataFeed(final List<Map<String, Object>> expectedEntries, final ODataFeed oDataFeed) {
        List<ODataEntry> actualEntries = oDataFeed.getEntries();
        return validateODataFeed(expectedEntries, actualEntries);
    }

    /**
     * Validates an ODataEntry against a map of expected properties (key/value pairs). The properties of
     * the entry itself can contain entries or maps, which are validated recursively. The result is
     * returned as a Pair, where the first member gives the result of the validation as an Boolean value
     * and the second the reason, if the validation has failed.
     *
     * @param expectedProperties a map which represent the expected properties of the ODataEntry
     * @param oDataEntry an ODataEntry, which shall be validated
     * @return a Pair, where the first member is true or false depending on the result of the validation
     *         and the second gives the reason, if the validation has failed
     */
    public static Pair<Boolean, String> validateODataEntry(final Map<String, Object> expectedProperties, final ODataEntry oDataEntry) {
        Map<String, Object> actualProperties = oDataEntry.getProperties();
        return validateODataEntry(expectedProperties, actualProperties);
    }

    /**
     * Validate expected entry recursively.
     *
     * @param expectedValue the expected value
     * @param actualValue the actual value
     * @param key the key
     * @return the pair
     */
    @SuppressWarnings("unchecked")
    private static Pair<Boolean, String> validateExpectedEntryRecursively(final Object expectedValue, final Object actualValue,
            final String key) {
        if (expectedValue == null) {
            if (actualValue == null)
                return new Pair<Boolean, String>(true, "Success");
            return new Pair<Boolean, String>(false, "Actual value of key " + key + " must be NULL");
        }
        if (actualValue == null)
            return new Pair<Boolean, String>(false, "Actual value of key " + key + " must not be NULL");
        else if (expectedValue instanceof Map) {
            Map<String, Object> actualProperties = null;
            if (actualValue instanceof ODataEntry) {
                actualProperties = ((ODataEntry) actualValue).getProperties();
            } else if (actualValue instanceof Map) {
                actualProperties = ((Map<String, Object>) actualValue);
            } else
                return new Pair<Boolean, String>(false, "Actual value of key " + key + " is neither of type ODataEntry nor of type Map");
            Map<String, Object> expectedEntrySet = (Map<String, Object>) expectedValue;
            return validateODataEntry(expectedEntrySet, actualProperties);
        } else if (expectedValue instanceof List) {
            List<ODataEntry> actualValueList = null;
            if (actualValue instanceof ODataFeed) {
                actualValueList = ((ODataFeed) actualValue).getEntries();
            } else if (actualValue instanceof List) {
                actualValueList = (List<ODataEntry>) actualValue;
            } else
                return new Pair<Boolean, String>(false, "Actual value of key " + key + " is neither of type ODataFeed nor of type List");
            List<Map<String, Object>> expectedValueList = (List<Map<String, Object>>) expectedValue;
            return validateODataFeed(expectedValueList, actualValueList);
        } else
            return new Pair<Boolean, String>(expectedValue.equals(actualValue),
                    "Property '" + key + "' not correct, expected: " + expectedValue + ", was: " + actualValue);
    }

    /**
     * Validate O data feed.
     *
     * @param expectedValueList expected
     * @param actualValueList actual
     * @return Pair
     */
    private static Pair<Boolean, String> validateODataFeed(final List<Map<String, Object>> expectedValueList,
            final List<ODataEntry> actualValueList) {
        if (expectedValueList.size() != actualValueList.size())
            return new Pair<Boolean, String>(false,
                    "The number of entries does not match, expected: " + expectedValueList.size() + ", was: " + actualValueList.size());
        for (int i = 0; i < expectedValueList.size(); i++) {
            Map<String, Object> expectedProperties = expectedValueList.get(i);
            Pair<Boolean, String> result = null;
            int j = 0;
            do {
                Map<String, Object> actualProperties = actualValueList.get(j++)
                                                                      .getProperties();
                result = validateODataEntry(expectedProperties, actualProperties);
            } while (!result.getFirst() && j < actualValueList.size());
            if (!result.getFirst())
                return new Pair<Boolean, String>(false, "ODataEntry " + expectedProperties + " not found in " + actualValueList);
        }
        return new Pair<Boolean, String>(true, "Success");
    }

    /**
     * Validate O data entry.
     *
     * @param expectedProperties the expected properties
     * @param actualProperties actual
     * @return Pair
     */
    private static Pair<Boolean, String> validateODataEntry(final Map<String, Object> expectedProperties,
            final Map<String, Object> actualProperties) {
        Pair<Boolean, String> result = null;
        for (String expectedKey : expectedProperties.keySet()) {
            result = validateExpectedEntryRecursively(expectedProperties.get(expectedKey), actualProperties.get(expectedKey), expectedKey);
            if (!result.getFirst())
                return result;
        }
        return new Pair<Boolean, String>(true, "Success");
    }

    /**
     * Fqn.
     *
     * @param ns the namespace
     * @param name the name
     * @return FQN
     */
    public static String fqn(String ns, String name) {
        return ns + "." + name;
    }

    /**
     * Fqn.
     *
     * @param clazz class
     * @return FQN
     */
    public static String fqn(Class<?> clazz) {
        AnnotationHelper annotationHelper = new AnnotationHelper();
        FullQualifiedName fqn = null;
        if (clazz.getAnnotation(org.apache.olingo.odata2.api.annotation.edm.EdmEntityType.class) != null) {
            fqn = annotationHelper.extractEntityTypeFqn(clazz);
        } else if (clazz.getAnnotation(org.apache.olingo.odata2.api.annotation.edm.EdmComplexType.class) != null) {
            fqn = annotationHelper.extractComplexTypeFqn(clazz);
        }
        if (fqn == null) {
            throw new IllegalArgumentException("The class " + clazz + " does not have the annotation "
                    + org.apache.olingo.odata2.api.annotation.edm.EdmEntityType.class + " or "
                    + org.apache.olingo.odata2.api.annotation.edm.EdmComplexType.class);
        }
        return fqn.toString();
    }

    /**
     * Fqns.
     *
     * @param classes classes
     * @return FQN list
     */
    public static List<String> fqns(Class<?>... classes) {
        List<String> fqns = new ArrayList<String>();
        for (Class<?> clazz : classes) {
            fqns.add(fqn(clazz));
        }
        return fqns;
    }

    /**
     * Resources.
     *
     * @param classes classes
     * @return array
     */
    @SuppressWarnings("rawtypes")
    public static String[] resources(Class... classes) {
        List<String> resources = new ArrayList<>();
        for (Class clazzz : classes) {
            resources.add("META-INF/" + clazzz.getSimpleName() + ".json");
        }
        return resources.toArray(new String[resources.size()]);
    }


    /**
     * Resource.
     *
     * @param <T> T
     * @param clazz class
     * @return content
     */
    public static <T> String resource(Class<T> clazz) {
        return "META-INF/" + clazz.getSimpleName() + ".json";
    }

    /**
     * Stream.
     *
     * @param <T> T
     * @param clazz class
     * @return InputStream
     */
    public static <T> InputStream stream(Class<T> clazz) {
        return OData2TestUtils.class.getClassLoader()
                                    .getResourceAsStream(resource(clazz));
    }
}
