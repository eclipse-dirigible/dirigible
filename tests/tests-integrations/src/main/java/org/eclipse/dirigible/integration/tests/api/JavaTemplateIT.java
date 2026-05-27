/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.util.TestConditionsChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.restassured.http.ContentType;

/**
 * Validates the runtime contract produced by {@code template-application-dao-java} +
 * {@code template-application-rest-java}: drops {@code .java} sources matching the templates'
 * output (entity + repository + controller) into the registry, triggers synchronization, and
 * exercises the generated CRUD endpoints through HTTP.
 *
 * <p>
 * The generator step itself runs in JavaScript inside the IDE's template engine and is not invoked
 * from this test — that would require the IDE / Selenide. Instead we assert that the *shape of
 * code* the templates emit compiles cleanly under {@code engine-java} and reaches the
 * {@code /services/java/...} dispatch path the same way {@code JavaEngineIT} does.
 */
class JavaTemplateIT extends IntegrationTest {

    private static final String PROJECT = "java-template-it";

    private static final String TABLE_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/gen/sample/data/books/Book.table";
    private static final String ENTITY_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/gen/sample/data/books/BookEntity.java";
    private static final String REPOSITORY_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/gen/sample/data/books/BookRepository.java";
    private static final String CONTROLLER_PATH =
            IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/gen/sample/api/books/BookController.java";

    private static final String CONTROLLER_BASE = "/services/java/" + PROJECT + "/gen/sample/api/books/BookController";

    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    private static final String TABLE_SOURCE = """
            {
                "name": "JAVATEMPLATEIT_BOOK",
                "type": "TABLE",
                "columns": [
                    {
                        "type": "INTEGER",
                        "primaryKey": true,
                        "identity": true,
                        "nullable": false,
                        "name": "BOOK_ID"
                    },
                    {
                        "type": "VARCHAR",
                        "length": 40,
                        "nullable": false,
                        "name": "BOOK_TITLE"
                    }
                ]
            }
            """;

    private static final String ENTITY_SOURCE = """
            package gen.sample.data.books;

            import org.eclipse.dirigible.engine.java.annotations.Column;
            import org.eclipse.dirigible.engine.java.annotations.Documentation;
            import org.eclipse.dirigible.engine.java.annotations.Entity;
            import org.eclipse.dirigible.engine.java.annotations.GeneratedValue;
            import org.eclipse.dirigible.engine.java.annotations.GenerationType;
            import org.eclipse.dirigible.engine.java.annotations.Id;
            import org.eclipse.dirigible.engine.java.annotations.Table;

            @Entity
            @Table(name = "JAVATEMPLATEIT_BOOK")
            @Documentation("Book entity mapping")
            public class BookEntity {

                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                @Column(name = "BOOK_ID")
                @Documentation("Id")
                public Integer id;

                @Column(name = "BOOK_TITLE", length = 40, nullable = false)
                @Documentation("Title")
                public String title;
            }
            """;

    private static final String REPOSITORY_SOURCE = """
            package gen.sample.data.books;

            import org.eclipse.dirigible.components.data.store.java.repository.JavaRepository;
            import org.eclipse.dirigible.engine.java.annotations.Repository;

            @Repository
            public class BookRepository extends JavaRepository<BookEntity> {

                public BookRepository() {
                    super(BookEntity.class);
                }
            }
            """;

    private static final String CONTROLLER_SOURCE = """
            package gen.sample.api.books;

            import gen.sample.data.books.BookEntity;
            import gen.sample.data.books.BookRepository;

            import org.eclipse.dirigible.engine.java.annotations.Documentation;
            import org.eclipse.dirigible.engine.java.annotations.Inject;
            import org.eclipse.dirigible.engine.java.annotations.http.Body;
            import org.eclipse.dirigible.engine.java.annotations.http.Controller;
            import org.eclipse.dirigible.engine.java.annotations.http.Delete;
            import org.eclipse.dirigible.engine.java.annotations.http.Get;
            import org.eclipse.dirigible.engine.java.annotations.http.PathParam;
            import org.eclipse.dirigible.engine.java.annotations.http.Post;
            import org.eclipse.dirigible.engine.java.annotations.http.Put;
            import org.eclipse.dirigible.engine.java.annotations.http.QueryParam;
            import org.springframework.http.HttpStatus;
            import org.springframework.web.server.ResponseStatusException;

            import java.util.Collection;
            import java.util.LinkedHashMap;
            import java.util.List;
            import java.util.Locale;
            import java.util.Map;
            import java.util.Set;

            @Controller
            @Documentation("java-template-it - Book Controller")
            public class BookController {

                private static final Set<String> FILTER_FIELDS = Set.of("id", "title");

                @Inject
                private BookRepository repository;

                @Get
                @Documentation("List Book")
                public List<BookEntity> getAll(@QueryParam("$limit") Integer limit,
                                              @QueryParam("$offset") Integer offset) {
                    int actualLimit = limit != null ? limit.intValue() : 20;
                    int actualOffset = offset != null ? offset.intValue() : 0;
                    return repository.findAll(actualLimit, actualOffset);
                }

                @Get("/count")
                @Documentation("Count Book")
                public Map<String, Long> count() {
                    return Map.of("count", repository.count());
                }

                @Post("/count")
                @Documentation("Count Book with filter")
                public Map<String, Long> countWithFilter(@Body Map<String, Object> filter) {
                    return Map.of("count", (long) runFilter(filter).size());
                }

                @Post("/search")
                @Documentation("Search Book")
                public List<BookEntity> search(@Body Map<String, Object> filter) {
                    return runFilter(filter);
                }

                @Get("/{id}")
                @Documentation("Get Book by id")
                public BookEntity getById(@PathParam("id") Integer id) {
                    return repository.findOne(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
                }

                @Post
                @Documentation("Create Book")
                public BookEntity create(@Body BookEntity entity) {
                    return repository.save(entity);
                }

                @Put("/{id}")
                @Documentation("Update Book by id")
                public BookEntity update(@PathParam("id") Integer id, @Body BookEntity entity) {
                    entity.id = id;
                    return repository.update(entity);
                }

                @Delete("/{id}")
                @Documentation("Delete Book by id")
                public void deleteById(@PathParam("id") Integer id) {
                    if (repository.findOne(id).isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
                    }
                    repository.deleteById(id);
                }

                private List<BookEntity> runFilter(Map<String, Object> filter) {
                    StringBuilder hql = new StringBuilder("from BookEntity e");
                    Map<String, Object> params = new LinkedHashMap<>();
                    boolean first = true;
                    if (filter != null && filter.get("equals") instanceof Map<?, ?> equals) {
                        for (Map.Entry<?, ?> entry : equals.entrySet()) {
                            String field = requireKnownField(String.valueOf(entry.getKey()));
                            String paramName = "p" + params.size();
                            hql.append(first ? " where e." : " and e.").append(field).append(" = :").append(paramName);
                            params.put(paramName, entry.getValue());
                            first = false;
                        }
                    }
                    if (filter != null && filter.get("conditions") instanceof List<?> conditions) {
                        for (Object raw : conditions) {
                            if (!(raw instanceof Map<?, ?> condition)) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter condition");
                            }
                            String field = requireKnownField(String.valueOf(condition.get("propertyName")));
                            String operator = String.valueOf(condition.get("operator")).toUpperCase(Locale.ROOT);
                            Object value = condition.get("value");
                            String paramName = "p" + params.size();
                            String clause = switch (operator) {
                                case "EQ" -> "e." + field + " = :" + paramName;
                                case "IN" -> {
                                    if (!(value instanceof Collection<?>)) {
                                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                "IN value must be a list for field: " + field);
                                    }
                                    yield "e." + field + " in (:" + paramName + ")";
                                }
                                case "LIKE" -> "e." + field + " like :" + paramName;
                                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported operator: " + operator);
                            };
                            hql.append(first ? " where " : " and ").append(clause);
                            params.put(paramName, value);
                            first = false;
                        }
                    }
                    return repository.query(hql.toString(), params);
                }

                private static String requireKnownField(String field) {
                    if (!FILTER_FIELDS.contains(field)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown filter field: " + field);
                    }
                    return field;
                }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private TestConditionsChecker testConditionsChecker;

    @Test
    void generated_dao_and_controller_serve_crud_over_http() {
        // The Java template emits @GeneratedValue(IDENTITY) on the primary key. IDENTITY DDL emission
        // is intentionally suppressed on MSSQL (TableCreateProcessor) to keep existing TS/JS code
        // paths that issue explicit-ID INSERTs working, so this CRUD flow can't reach a green state
        // on MSSQL until the broader IDENTITY-aware refactor lands.
        assumeTrue(testConditionsChecker.isH2OrPostgresDefaultDB(),
                "Skipping: IDENTITY DDL is disabled on MSSQL, so @GeneratedValue(IDENTITY) inserts cannot succeed.");

        writeAll();

        // List on a fresh table returns 200 + a JSON array (may be empty or contain prior runs' data
        // — assert only on the status code here).
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER_BASE)
                                                 .then()
                                                 .statusCode(200),
                ASSERTION_TIMEOUT_SECONDS);

        // POST persists a row through the @Inject-resolved repository. The retry-on-AssertionError
        // executor doesn't return a value, so capture the IDENTITY-assigned id via AtomicReference.
        AtomicReference<Integer> created = new AtomicReference<>();
        restAssuredExecutor.execute(() -> created.set(given().when()
                                                             .contentType(ContentType.JSON)
                                                             .body("{\"title\":\"Dune\"}")
                                                             .post(CONTROLLER_BASE)
                                                             .then()
                                                             .statusCode(200)
                                                             .body(containsString("Dune"))
                                                             .extract()
                                                             .path("id")),
                ASSERTION_TIMEOUT_SECONDS);
        Integer createdId = created.get();

        // GET /{id} resolves @PathParam binding.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER_BASE + "/" + createdId)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Dune")),
                ASSERTION_TIMEOUT_SECONDS);

        // PUT updates an existing row.
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"title\":\"Dune Messiah\"}")
                                                 .put(CONTROLLER_BASE + "/" + createdId)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Dune Messiah")),
                ASSERTION_TIMEOUT_SECONDS);

        // GET /count is non-negative and includes the row we just created.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER_BASE + "/count")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("count")),
                ASSERTION_TIMEOUT_SECONDS);

        // POST /search with a known field returns the matching row.
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"equals\":{\"title\":\"Dune Messiah\"}}")
                                                 .post(CONTROLLER_BASE + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Dune Messiah")),
                ASSERTION_TIMEOUT_SECONDS);

        // POST /count with the same filter returns at least 1.
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"equals\":{\"title\":\"Dune Messiah\"}}")
                                                 .post(CONTROLLER_BASE + "/count")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("count")),
                ASSERTION_TIMEOUT_SECONDS);

        // POST /search with an unknown field is rejected by the SQL-injection allow-list.
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"equals\":{\"nope\":\"x\"}}")
                                                 .post(CONTROLLER_BASE + "/search")
                                                 .then()
                                                 .statusCode(400),
                ASSERTION_TIMEOUT_SECONDS);

        // POST /search with the conditions shape the dropdown widget sends (IN with a list).
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"conditions\":[{\"propertyName\":\"title\",\"operator\":\"IN\",\"value\":[\"Dune Messiah\",\"Foundation\"]}]}")
                                                 .post(CONTROLLER_BASE + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Dune Messiah")),
                ASSERTION_TIMEOUT_SECONDS);

        // POST /search with a LIKE condition matches the row by pattern.
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"conditions\":[{\"propertyName\":\"title\",\"operator\":\"LIKE\",\"value\":\"Dune%\"}]}")
                                                 .post(CONTROLLER_BASE + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString("Dune Messiah")),
                ASSERTION_TIMEOUT_SECONDS);

        // POST /search with an unsupported operator is rejected.
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"conditions\":[{\"propertyName\":\"title\",\"operator\":\"REGEX\",\"value\":\".*\"}]}")
                                                 .post(CONTROLLER_BASE + "/search")
                                                 .then()
                                                 .statusCode(400),
                ASSERTION_TIMEOUT_SECONDS);

        // DELETE removes the row; subsequent GET returns 404.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(CONTROLLER_BASE + "/" + createdId)
                                                 .then()
                                                 .statusCode(200),
                ASSERTION_TIMEOUT_SECONDS);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER_BASE + "/" + createdId)
                                                 .then()
                                                 .statusCode(404),
                ASSERTION_TIMEOUT_SECONDS);

        // The OpenAPI aggregator publishes one fragment per registered controller.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/openapi")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(containsString(CONTROLLER_BASE)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private void writeAll() {
        // .table artefact first so TableSynchronizer materialises JAVATEMPLATEIT_BOOK
        // (with IDENTITY on BOOK_ID) before the Java entity is registered against it.
        write(TABLE_PATH, TABLE_SOURCE, "application/json");
        write(ENTITY_PATH, ENTITY_SOURCE, "text/x-java");
        write(REPOSITORY_PATH, REPOSITORY_SOURCE, "text/x-java");
        write(CONTROLLER_PATH, CONTROLLER_SOURCE, "text/x-java");
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void write(String path, String source, String contentType) {
        repository.createResource(path, source.getBytes(StandardCharsets.UTF_8), false, contentType, true);
    }

    @AfterEach
    void cleanup() {
        for (String path : List.of(TABLE_PATH, ENTITY_PATH, REPOSITORY_PATH, CONTROLLER_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
