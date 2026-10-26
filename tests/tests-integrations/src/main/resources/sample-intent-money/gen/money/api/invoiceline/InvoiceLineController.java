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
        // counted in the database, not by loading every matching row (dirigible #7982)
        Map<String, Object> params = new LinkedHashMap<>();
        String where = filterWhere(filter, params);
        return Map.of("count", repository.countQuery("select count(e) from money_InvoiceLineEntity e" + where, params));
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

    @Get("/access")
    @Documentation("What the CURRENT caller may do with InvoiceLine: read and write, by the same gates every endpoint applies - what the generated pages show or hide their write controls by; the 403 of each write stays authoritative")
    public Map<String, Boolean> access() {
        // Never a 403: a caller who may not read is told so, the way a reader is told it may not write.
        return Map.of("read", mayRead(), "write", mayWrite());
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
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder hql = new StringBuilder("from money_InvoiceLineEntity e").append(filterWhere(filter, params));
        // a stable order, so that a page of the result means the same rows on every call
        hql.append(" order by e.Id");
        // The page the caller asked for, in the body next to the conditions (dirigible #7982): the
        // shell's report widgets ask for $limit 1 or 5 and were handed every matching row.
        return repository.query(hql.toString(), params, pageParameter(filter, "$limit", -1), pageParameter(filter, "$offset", 0));
    }

    /**
     * The {@code $limit} / {@code $offset} of a search body - a number or a numeric string.
     */
    private static int pageParameter(Map<String, Object> filter, String key, int absent) {
        Object value = filter == null ? null : filter.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + key + ": " + text);
            }
        }
        return absent;
    }

    /**
     * The {@code where} clause of a search body's {@code equals} and {@code conditions}, binding their
     * values into {@code params} - shared by the search and its count.
     */
    private String filterWhere(Map<String, Object> filter, Map<String, Object> params) {
        StringBuilder hql = new StringBuilder();
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
                    // A half-open range [from, to) - what the slot picker asks for the days it shows (#7837).
                    case "GTE" -> "e." + field + " >= :" + paramName;
                    case "LT" -> "e." + field + " < :" + paramName;
                    default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported operator: " + operator);
                };
                hql.append(first ? " where " : " and ").append(clause);
                params.put(paramName, operator.equals("IN") ? value : coerceValue(field, value));
                first = false;
            }
        }
        return hql.toString();
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
        if (!("write".equals(op) ? mayWrite() : mayRead())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /** The read gate every read endpoint applies - and what GET /access answers as {@code read}. */
    private static boolean mayRead() {
        return isInAnyRole("sample-intent-money.InvoiceLine.InvoiceLineReadOnly") || isInAnyRole("sample-intent-money.InvoiceLine.InvoiceLineFullAccess");
    }

    /** The write gate every write endpoint applies - and what GET /access answers as {@code write}. */
    private static boolean mayWrite() {
        return isInAnyRole("sample-intent-money.InvoiceLine.InvoiceLineFullAccess");
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
        if (entity.Description == null || entity.Description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The 'Description' property is required");
        }
        if (entity.Description.length() > 200) {
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
