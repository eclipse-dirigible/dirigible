/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package gen.money.api.invoiceline;

import gen.money.data.invoiceline.InvoiceLineEntity;
import gen.money.data.invoiceline.InvoiceLineRepository;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.sdk.platform.Documentation;
import org.eclipse.dirigible.sdk.http.Body;
import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.http.Controller;
import org.eclipse.dirigible.sdk.http.Delete;
import org.eclipse.dirigible.sdk.http.Get;
import org.eclipse.dirigible.sdk.http.PathParam;
import org.eclipse.dirigible.sdk.http.Post;
import org.eclipse.dirigible.sdk.http.Put;
import org.eclipse.dirigible.sdk.http.QueryParam;
import org.eclipse.dirigible.sdk.log.Logger;
import org.eclipse.dirigible.sdk.log.Logging;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
@Component("money_InvoiceLineController")
@Documentation("sample-intent-money - InvoiceLine Controller")
public class InvoiceLineController {

    private static final Set<String> FILTER_FIELDS = Set.of("Id", "Description", "Quantity", "UnitPrice", "VatRate", "VatAmount");

    /** Reports a system-owned column a create carried a value for - see dropSystemOwnedOnCreate. */
    private static final Logger LOG = Logging.getLogger("gen.money.api.invoiceline.InvoiceLineController");

    private final InvoiceLineRepository repository;

    public InvoiceLineController(InvoiceLineRepository repository) {
        this.repository = repository;
    }

    @Get
    @Documentation("List InvoiceLine")
    public List<InvoiceLineEntity> getAll(@QueryParam("$limit") Integer limit,
                                      @QueryParam("$offset") Integer offset) {
        checkPermissions("read");
        int actualLimit = limit != null ? limit.intValue() : 20;
        int actualOffset = offset != null ? offset.intValue() : 0;
        List<InvoiceLineEntity> result = repository.findAll(actualLimit, actualOffset);
        return result;
    }

    @Get("/count")
    @Documentation("Count InvoiceLine")
    public Map<String, Long> count() {
        checkPermissions("read");
        return Map.of("count", repository.count());
    }

    @Post("/count")
    @Documentation("Count InvoiceLine with filter")
    public Map<String, Long> countWithFilter(@Body Map<String, Object> filter) {
        checkPermissions("read");
        return Map.of("count", (long) runFilter(filter).size());
    }

    @Post("/search")
    @Documentation("Search InvoiceLine")
    public List<InvoiceLineEntity> search(@Body Map<String, Object> filter) {
        checkPermissions("read");
        List<InvoiceLineEntity> result = runFilter(filter);
        return result;
    }

    @Get("/{id}")
    @Documentation("Get InvoiceLine by id")
    public InvoiceLineEntity getById(@PathParam("id") Integer id) {
        checkPermissions("read");
        InvoiceLineEntity entity = repository.findOne(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "InvoiceLine not found"));
        return entity;
    }

    @Post
    @Documentation("Create InvoiceLine")
    public InvoiceLineEntity create(@Body InvoiceLineEntity entity) {
        checkPermissions("write");
        // The checks read the record as the write will store it - its calculated fields computed, not
        // taken from the payload (#7544). What is persisted is still computed by the repository.
        InvoiceLineEntity checked = repository.calculatedForCreate(entity);
        validate(checked);
        // After every check above, so what a create refuses is unchanged; before the save, so the
        // repository never sees a caller's value for a column it does not own (#7549, #7620).
        dropSystemOwnedOnCreate(entity);
        InvoiceLineEntity saved = repository.save(entity);
        return repository.findOne(saved.Id).orElse(saved);
    }

    @Put("/{id}")
    @Documentation("Update InvoiceLine by id")
    public InvoiceLineEntity update(@PathParam("id") Integer id, @Body InvoiceLineEntity entity) {
        checkPermissions("write");
        entity.Id = id;
        // The checks read the record as the write will store it - its calculated fields computed, not
        // taken from the payload (#7544). What is persisted is still computed by the repository.
        InvoiceLineEntity checked = repository.calculatedForUpdate(entity);
        validate(checked);
        return repository.update(entity);
    }

    @Delete("/{id}")
    @Documentation("Delete InvoiceLine by id")
    public void deleteById(@PathParam("id") Integer id) {
        checkPermissions("write");
        if (repository.findOne(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "InvoiceLine not found");
        }
        try {
            repository.deleteById(id);
        } catch (RuntimeException e) {
            // A foreign-key violation means other records still reference this one - a data-integrity
            // conflict the user can act on, not a server error.
            if (isConstraintViolation(e)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This InvoiceLine is referenced by other records and cannot be deleted");
            }
            throw e;
        }
    }

    /** Whether a persistence failure is (caused by) a database constraint violation. */
    private static boolean isConstraintViolation(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause() == cause ? null : cause.getCause()) {
            String type = cause.getClass().getName();
            if (type.contains("ConstraintViolation") || type.contains("DataIntegrityViolation")) {
                return true;
            }
        }
        return false;
    }

    private List<InvoiceLineEntity> runFilter(Map<String, Object> filter) {
        StringBuilder hql = new StringBuilder("from money_InvoiceLineEntity e");
        Map<String, Object> params = new LinkedHashMap<>();
        boolean first = true;
        if (filter != null && filter.get("equals") instanceof Map<?, ?> equals) {
            for (Map.Entry<?, ?> entry : equals.entrySet()) {
                String field = requireKnownField(String.valueOf(entry.getKey()));
                String paramName = "p" + params.size();
                hql.append(first ? " where e." : " and e.").append(field).append(" = :").append(paramName);
                params.put(paramName, coerceValue(field, entry.getValue()));
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
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IN value must be a list for field: " + field);
                        }
                        yield "e." + field + " in (:" + paramName + ")";
                    }
                    case "LIKE" -> "e." + field + " like :" + paramName;
                    default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported operator: " + operator);
                };
                hql.append(first ? " where " : " and ").append(clause);
                params.put(paramName, operator.equals("IN") ? value : coerceValue(field, value));
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

    // Coerce a JSON-sourced filter value to the entity property's Java type so Hibernate can bind it.
    // Date/timestamp filters arrive as ISO strings and must become LocalDate/LocalDateTime/Instant,
    // else binding fails with "argument [2026-07-01] is not assignable to java.time.LocalDate". Reflects
    // the entity field type (the field is already validated against FILTER_FIELDS). LIKE/string values
    // and already-typed numbers pass through unchanged.
    private static Object coerceValue(String field, Object value) {
        if (!(value instanceof String s) || s.isBlank()) {
            return value;
        }
        try {
            Class<?> type = InvoiceLineEntity.class.getField(field)
                                                    .getType();
            if (type == java.time.LocalDate.class) {
                return java.time.LocalDate.parse(s);
            }
            if (type == java.time.LocalDateTime.class) {
                return java.time.LocalDateTime.parse(s);
            }
            if (type == java.time.Instant.class) {
                return java.time.Instant.parse(s);
            }
            if (type == Integer.class) {
                return Integer.valueOf(s);
            }
            if (type == Long.class) {
                return Long.valueOf(s);
            }
            if (type == java.math.BigDecimal.class) {
                return new java.math.BigDecimal(s);
            }
            if (type == Double.class) {
                return Double.valueOf(s);
            }
            if (type == Boolean.class || type == boolean.class) {
                return Boolean.valueOf(s);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Unknown field or unparseable value: leave as-is and let Hibernate's validator report it.
        }
        return value;
    }

    private void checkPermissions(String op) {
        if ("read".equals(op) && !(isInAnyRole("sample-intent-money.InvoiceLine.InvoiceLineReadOnly") || isInAnyRole("sample-intent-money.InvoiceLine.InvoiceLineFullAccess"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if ("write".equals(op) && !isInAnyRole("sample-intent-money.InvoiceLine.InvoiceLineFullAccess")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Drops the system-owned columns a create carried (#7549): the readOnly fields, the roll-up and
     * aggregate targets, ProcessId(s), a label's Name - what update() preserves from the stored row.
     * The platform or a system writer fills them on create, never the request. Reported like
     * update()'s discard (#6937), so a client computing its own value for one of them is told rather
     * than silently ignored; a date is dropped but not reported, its round-trip being lossy.
     *
     * <p>
     * This lives on the REST surface and NOT in the repository's save() on purpose: save() is the
     * create path of every system writer - the attachment upload, the snapshot mint, a create-from, a
     * posting - and those assign exactly these columns by design (#7620).
     */
    private static void dropSystemOwnedOnCreate(InvoiceLineEntity entity) {
        java.util.List<String> discarded = new java.util.ArrayList<>();
        if (entity.VatAmount != null) {
            discarded.add("VatAmount=" + entity.VatAmount);
        }
        entity.VatAmount = null;
        if (!discarded.isEmpty()) {
            LOG.warn("A create of InvoiceLine carried system-owned {} - discarded. These columns are filled by"
                    + " the platform or a system writer, never from the request.", discarded);
        }
    }

    // Public so a process task's write-back (the generated <Process><Step>Write delegate) holds the
    // reviewer's edits to exactly these rules and messages before it writes them (#7552).
    public static void validate(InvoiceLineEntity entity) {
        if (entity.Description == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The 'Description' property is required");
        }
        if (entity.Description != null && entity.Description.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The 'Description' exceeds the maximum length of 200");
        }
        if (entity.Quantity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The 'Quantity' property is required");
        }
        if (entity.UnitPrice == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The 'UnitPrice' property is required");
        }
        if (entity.VatRate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The 'VatRate' property is required");
        }
    }

    /**
     * Whether the caller holds ANY of a comma-separated role list. One role name is what the entity
     * modeler's per-property read/write role and the convention-derived entity gates carry; a gate the
     * intent's `permissions[].can:` tokens authorized, and the intent's `visibleTo: [A, B]`
     * allow-list, arrive here as "A,B" and holding either of them is enough.
     */
    private static boolean isInAnyRole(String roles) {
        for (String role : roles.split(",")) {
            String name = role.trim();
            if (!name.isEmpty() && UserFacade.isInRole(name)) {
                return true;
            }
        }
        return false;
    }
}
