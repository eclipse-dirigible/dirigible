package custom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.dirigible.sdk.log.Logger;
import org.eclipse.dirigible.sdk.log.Logging;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import gen.provisioning.data.tenantapplication.TenantApplicationEntity;
import gen.provisioning.data.tenantapplication.TenantApplicationRepository;

/**
 * The doomed-or-fine remote call: a tenant whose title contains "fail" is refused on EVERY attempt,
 * so its declared retry (retry: { count: 2 }) exhausts after three attempts and the onError route
 * records the FINAL attempt's message via {error}. Any other tenant succeeds immediately, consuming
 * the dbPassword the schema step produced (the `uses` declaration) and stamping the generated key
 * onto the record through the repository's targeted write.
 */
public class AppProvisioner implements JavaDelegate {

    private static final Logger LOG = Logging.getLogger("custom.AppProvisioner");

    /** Attempts per tenant id, so the recorded {error} message names the exact final attempt. */
    private static final Map<Object, AtomicInteger> ATTEMPTS = new ConcurrentHashMap<>();

    @Override
    public void execute(DelegateExecution execution) {
        Object key = execution.getVariable("Id");
        if (!(key instanceof Number id)) {
            return;
        }
        TenantApplicationRepository repository = new TenantApplicationRepository();
        TenantApplicationEntity tenant = repository.findOne(id.intValue())
                                                   .orElse(null);
        if (tenant == null) {
            return;
        }
        if (tenant.Title != null && tenant.Title.toLowerCase()
                                                .contains("fail")) {
            int attempt = ATTEMPTS.computeIfAbsent(key, k -> new AtomicInteger())
                                  .incrementAndGet();
            LOG.warn("App provisioning refused for tenant {} (attempt {}) - exhaustion will route to recordFailure", id, attempt);
            throw new IllegalStateException("no capacity for '" + tenant.Title + "' (attempt " + attempt + ")");
        }
        Object dbPassword = execution.getVariable("dbPassword");
        String generatedKey = "app-" + id + "-" + (dbPassword == null ? "missing" : Integer.toHexString(dbPassword.hashCode()));
        LOG.info("App provisioned for tenant {} using the produced credential - stamping key {}", id, generatedKey);
        repository.updateProperty(id.intValue(), "GeneratedKey", generatedKey);
    }
}
