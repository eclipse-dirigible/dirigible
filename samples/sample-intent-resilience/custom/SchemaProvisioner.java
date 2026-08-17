package custom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.dirigible.sdk.log.Logger;
import org.eclipse.dirigible.sdk.log.Logging;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * The flaky remote call: fails its first two attempts per tenant and succeeds on the third, so the
 * declared retry cycle (retry: { count: 3, every: PT10S }) is what carries it to success. Produces
 * the declared dbPassword step data for the app step - which clearAfter then removes from the
 * instance once consumed.
 */
public class SchemaProvisioner implements JavaDelegate {

    private static final Logger LOG = Logging.getLogger("custom.SchemaProvisioner");

    /** Attempts per tenant id, so every created tenant demonstrates the retry recovery. */
    private static final Map<Object, AtomicInteger> ATTEMPTS = new ConcurrentHashMap<>();

    @Override
    public void execute(DelegateExecution execution) {
        Object id = execution.getVariable("Id");
        int attempt = ATTEMPTS.computeIfAbsent(id, key -> new AtomicInteger())
                              .incrementAndGet();
        if (attempt < 3) {
            LOG.warn("Schema provisioning for tenant {} failing on purpose (attempt {}) - the retry cycle re-runs it", id, attempt);
            throw new IllegalStateException("schema provisioning failed for tenant " + id + " (attempt " + attempt + ")");
        }
        LOG.info("Schema provisioned for tenant {} on attempt {} - producing dbPassword", id, attempt);
        execution.setVariable("dbPassword", "s3cret-" + id + "-" + Integer.toHexString(String.valueOf(id)
                                                                                             .hashCode()));
    }
}
