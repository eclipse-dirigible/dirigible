package gen.money.data.invoiceline;

import org.eclipse.dirigible.components.data.store.java.repository.JavaRepository;
import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.component.Repository;
import org.eclipse.dirigible.sdk.db.ValidationException;
import org.eclipse.dirigible.sdk.component.Beans;
import org.eclipse.dirigible.sdk.log.Logger;
import org.eclipse.dirigible.sdk.log.Logging;
// custom imports
import custom.LineVatAction;


@Repository
@Component("money_InvoiceLineRepository")
public class InvoiceLineRepository extends JavaRepository<InvoiceLineEntity> {

    /**
     * Reports a system-owned column a full-row update() carried a different value for - see update() -
     * and an aggregate guard skipped for a row that belongs to no key-tuple - see save().
     */
    private static final Logger LOG = Logging.getLogger("gen.money.data.invoiceline.InvoiceLineRepository");

    public InvoiceLineRepository() {
        super(InvoiceLineEntity.class);
    }

    @Override
    public InvoiceLineEntity save(InvoiceLineEntity entity) {
        entity.VatAmount = Beans.get(LineVatAction.class).calculate(entity);
        if (entity.Description == null) {
            throw new ValidationException("InvoiceLine.Description is required");
        }
        if (entity.Quantity == null) {
            throw new ValidationException("InvoiceLine.Quantity is required");
        }
        if (entity.UnitPrice == null) {
            throw new ValidationException("InvoiceLine.UnitPrice is required");
        }
        if (entity.VatRate == null) {
            throw new ValidationException("InvoiceLine.VatRate is required");
        }
        // The create event is handed to the write itself, which records it in the tenant's event outbox
        // INSIDE the insert's transaction: row and event commit together, so a broker outage can
        // neither swallow an event whose row exists nor fail a call whose row was written. Listeners
        // (intent process triggers, the reactions under gen/events) see it the moment it is durable.
        InvoiceLineEntity saved = super.save(entity, "sample-intent-money-InvoiceLine-InvoiceLine");
        return saved;
    }

    @Override
    public InvoiceLineEntity update(InvoiceLineEntity entity) {
        // System-owned fields survive a partial payload: a caller's update carries only the fields
        // its form edits, and a value it leaves null must not erase what the write path computed -
        // the roll-up/aggregate targets (an invoice's Paid, a timesheet's total hours) and the
        // platform-managed identifiers (document number, uuid). The #6226/#6306 lost-update family,
        // user-update edition: read the stored row once and take the system-owned values from THERE,
        // never from the payload. System writers stay on the targeted primitives (updateProperty /
        // updateProperties / updateDerived), so this preservation cannot fight a workflow write.
        InvoiceLineEntity existingRow = entity.Id == null ? null : findById(entity.Id);
        if (existingRow != null) {
            // ...but a caller that computed a DIFFERENT value for one of them is not sending a partial
            // payload - it is a system writer on the wrong path, and its work is being thrown away.
            // Preserving silently made that indistinguishable from success: a hand-written listener
            // maintaining a pool via full-row update() kept returning 200 while the column stayed at
            // its created value forever (#6937). So the discard is reported, once per write, naming
            // what was dropped; a payload that carries the stored value back - every ordinary form
            // round-trip - says nothing.
            java.util.List<String> discarded = new java.util.ArrayList<>();
            if (isDiscardedByPreservation(entity.VatAmount, existingRow.VatAmount)) {
                discarded.add("VatAmount=" + entity.VatAmount);
            }
            entity.VatAmount = existingRow.VatAmount;
            if (!discarded.isEmpty()) {
                LOG.warn("A full-row update of InvoiceLine {} carried system-owned {} - discarded, the stored"
                        + " values were kept. update() never writes these columns; a writer that computes"
                        + " them must use the targeted updateProperty / updateProperties / updateDerived.",
                        entity.Id, discarded);
            }
        }
        entity.VatAmount = Beans.get(LineVatAction.class).calculate(entity);
        if (entity.Description == null) {
            throw new ValidationException("InvoiceLine.Description is required");
        }
        if (entity.Quantity == null) {
            throw new ValidationException("InvoiceLine.Quantity is required");
        }
        if (entity.UnitPrice == null) {
            throw new ValidationException("InvoiceLine.UnitPrice is required");
        }
        if (entity.VatRate == null) {
            throw new ValidationException("InvoiceLine.VatRate is required");
        }
        // The update event is recorded in the event outbox inside this write's own transaction - see
        // save() - so the intent reactions under gen/events see exactly the changes that committed.
        InvoiceLineEntity updated = super.update(entity, "sample-intent-money-InvoiceLine-InvoiceLine-updated");
        return updated;
    }

    /**
     * Persists changes WITHOUT publishing the "-updated" event. Intended for system-managed
     * back-references — e.g. an intent process trigger writing ProcessId back onto the entity that
     * started it. Going through {@link #update} would re-publish "InvoiceLine-updated" and spuriously
     * re-fire onUpdate reactions (notifications, roll-ups, integrations) for a change the user never made.
     */
    public InvoiceLineEntity updateWithoutEvent(InvoiceLineEntity entity) {
        if (entity.Description == null) {
            throw new ValidationException("InvoiceLine.Description is required");
        }
        if (entity.Quantity == null) {
            throw new ValidationException("InvoiceLine.Quantity is required");
        }
        if (entity.UnitPrice == null) {
            throw new ValidationException("InvoiceLine.UnitPrice is required");
        }
        if (entity.VatRate == null) {
            throw new ValidationException("InvoiceLine.VatRate is required");
        }
        InvoiceLineEntity updated = super.update(entity);
        return updated;
    }

    /**
     * Targeted write of DERIVED columns - roll-up totals and keyed aggregates maintained by the generated
     * handlers under {@code gen/events} - that keeps the {@code "-updated"} event contract.
     *
     * <p>
     * Only the named columns are persisted, so a concurrent user write to any other column of the row
     * cannot be reverted: a handler that read the row, recomputed one total and then merged the WHOLE row
     * back silently reverted whatever the user changed in that window (the lost-update family of #6226 /
     * #6306). The {@code "-updated"} event still fires so downstream reactions - notably a transitive
     * roll-up whose own parent sits above this row - keep cascading exactly as they did when the handler
     * persisted the full row, carrying the written column names under {@code DerivedWrite.MARKER} so an
     * authored-change listener can skip a recompute that moved nothing it watches. Routed through
     * {@link #updateProperties} so an entity that declares
     * {@code checks:} still runs its gate, a labelled entity still refreshes its stored display Name,
     * and a document line still resums the master it belongs to.
     */
    public int updateDerived(Object id, java.util.Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        // The event travels with the write: the resulting row is read back on the write's own
        // connection, before its commit, and recorded in the event outbox alongside it. It is MARKED
        // with the columns written, so a listener that reacts to authored changes only - a settlement
        // re-allocating a corrected payment - can tell this recompute from an edit (#7557).
        return org.eclipse.dirigible.components.data.store.java.repository.DerivedWrite.run(values.keySet(),
                () -> updateProperties(id, values, "sample-intent-money-InvoiceLine-InvoiceLine-updated"));
    }

    /**
     * Whether the preservation of a system-owned column is DISCARDING work: the payload carried a
     * value of its own and it is not the stored one.
     *
     * <p>
     * An absent value is the ordinary case the preservation exists for - a form PUTs only the fields it
     * edits - and is never reported. A numeric value is compared by amount, not by representation:
     * {@code 120.00} and {@code 120} are the same total, and a JSON round-trip through the browser's
     * number type routinely changes the scale. Everything else is compared by value, arrays included
     * (a BLOB column's identity would differ on every round-trip).
     *
     * @param payloadValue the value the caller sent, may be null
     * @param storedValue the value the stored row holds, may be null
     * @return true when the payload value is present and differs from the stored one
     */
    private static boolean isDiscardedByPreservation(Object payloadValue, Object storedValue) {
        if (payloadValue == null || java.util.Objects.deepEquals(payloadValue, storedValue)) {
            return false;
        }
        if (payloadValue instanceof Number payloadNumber && storedValue instanceof Number storedNumber) {
            return new java.math.BigDecimal(String.valueOf(payloadNumber))
                    .compareTo(new java.math.BigDecimal(String.valueOf(storedNumber))) != 0;
        }
        return true;
    }

    /**
     * The record this create would store, as far as its calculated fields go - the row the controller's
     * checks read before the write (#7544). The calculated expressions are assigned inside save(), so a
     * check reading the submitted record saw whatever the caller sent for a calculated field (usually
     * nothing, or the value of a previous save) and a soft or hard check on one never fired for a REST
     * write. The defaults the create applies come first, as in save(), since an expression may read
     * them. Only the neutral expressions are evaluated: a calculated ACTION may mint or mutate state,
     * and runs on the write path alone. The submitted record itself is not touched - what is persisted
     * is still computed by save().
     *
     * @param submitted the record about to be created
     * @return a copy carrying the computed values, or the record itself when nothing is calculated
     */
    public InvoiceLineEntity calculatedForCreate(InvoiceLineEntity submitted) {
        return submitted;
    }

    /**
     * The record this update would store, as far as its calculated and system-owned fields go - the row
     * the controller's checks read before the write (#7544). Built as update() builds it: the
     * system-owned fields taken from the stored row, the document totals recomputed, then the
     * update-time expressions - so an expression such as Balance = Total - Paid reads the stored Paid,
     * never the payload's. The same holds for a check that reads a system-owned field directly (#7633):
     * a forbidWhen over a readOnly counter a roll-up maintains judges the stored counter, so a payload
     * that omits the column, or carries a harmless value for it, cannot walk past the refusal. Only the
     * neutral expressions are evaluated (an action runs on the write path alone), and the submitted
     * record itself is not touched.
     *
     * @param submitted the record about to be updated, carrying its id
     * @return a copy carrying the stored and computed values, or the record itself when the entity has
     *         nothing calculated and nothing system-owned
     */
    public InvoiceLineEntity calculatedForUpdate(InvoiceLineEntity submitted) {
        InvoiceLineEntity entity = copyOf(submitted);
        InvoiceLineEntity stored = entity.Id == null ? null : findById(entity.Id);
        if (stored != null) {
            entity.VatAmount = stored.VatAmount;
        }
        return entity;
    }

    private static InvoiceLineEntity copyOf(InvoiceLineEntity source) {
        InvoiceLineEntity copy = new InvoiceLineEntity();
        copy.Id = source.Id;
        copy.Description = source.Description;
        copy.Quantity = source.Quantity;
        copy.UnitPrice = source.UnitPrice;
        copy.VatRate = source.VatRate;
        copy.VatAmount = source.VatAmount;
        return copy;
    }

    @Override
    public void delete(InvoiceLineEntity entity) {
        // The delete event is recorded in the event outbox inside this write's own transaction - see
        // save() - so the intent reactions under gen/events cannot miss a row that is really gone.
        super.delete(entity, "sample-intent-money-InvoiceLine-InvoiceLine-deleted");
    }

    @Override
    public void deleteById(Object id) {
        // The payload is the row as the deleting transaction read it, recorded in the event outbox
        // beside the delete itself - see save().
        super.deleteById(id, "sample-intent-money-InvoiceLine-InvoiceLine-deleted");
    }

}
