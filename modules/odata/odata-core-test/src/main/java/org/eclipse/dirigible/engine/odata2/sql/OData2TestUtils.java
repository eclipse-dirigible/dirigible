/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.odata2.sql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
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

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.ClassLoaderResourceAccessor;

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
     * {@link OData2RequestBuilder#executeRequest()}.
     *
     * @param response the response returned by {@link OData2RequestBuilder#executeRequest()}
     * @param entitySet the {@link EdmEntitySet} used to parse the response
     * @return the {@link ODataEntry}
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
     * Helper method to retrieve the {@link ODataFeed} from the response object returned by
     * {@link OData2RequestBuilder#executeRequest()}.
     *
     * @param response the response returned by {@link OData2RequestBuilder#executeRequest()}
     * @param entitySet the {@link EdmEntitySet} used to parse the response
     * @return the {@link ODataFeed}
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
     * Helper method to retrieve the {@link ODataErrorContext} representing the error response to a
     * failed call to an OData API from the response object returned by
     * {@link OData2RequestBuilder#executeRequest()}.
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
     * @param clazz the class
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
     * @param classes the classes
     * @return list
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
     * @param classes the classes
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

    /**
     * Inits the liquibase.
     *
     * @param ds data source
     * @param resourceAccessor the resource accessor
     * @throws SQLException in case of error
     */
    public static void initLiquibase(DataSource ds, AbstractResourceAccessor resourceAccessor) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                                               .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new liquibase.Liquibase("liquibase/changelog.xml", resourceAccessor, database);
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (DatabaseException e) {
            throw new SQLException("Unable to initilize liquibase", e);
        } catch (LiquibaseException e) {
            throw new SQLException("Unable to load the liquibase resources", e);
        }
    }

}
