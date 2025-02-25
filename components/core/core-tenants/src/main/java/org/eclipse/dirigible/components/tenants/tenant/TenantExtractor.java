package org.eclipse.dirigible.components.tenants.tenant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class TenantExtractor {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantExtractor.class);

    private static final List<String> HOST_HEADERS = List.of("host", "x-forwarded-host");

    /** The Constant TENANT_SUBDOMAIN_PATTERN. */
    private static final Pattern TENANT_SUBDOMAIN_PATTERN = Pattern.compile(DirigibleConfig.TENANT_SUBDOMAIN_REGEX.getStringValue());

    /** The Constant tenantCache. */
    public static final Cache<String, Optional<Tenant>> TENANT_CACHE = Caffeine.newBuilder()
                                                                               .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                               .maximumSize(100)
                                                                               .build();

    private final TenantService tenantService;
    private final boolean multitenantModeEnabled;
    private final Gson gson;

    public TenantExtractor(TenantService tenantService) {
        this.tenantService = tenantService;
        this.multitenantModeEnabled = DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getBooleanValue();
        this.gson = new GsonBuilder().serializeNulls()
                                     .create();
    }

    /**
     * Determine current tenant.
     *
     * @param request the request
     * @return the optional
     */
    public Optional<Tenant> determineTenantSubdomain(HttpServletRequest request) {
        if (!multitenantModeEnabled) {
            LOGGER.debug("The app is in single tenant mode. Will return the default tenant.");
            return Optional.of(TenantImpl.getDefaultTenant());
        }
        List<Optional<String>> subdomains = HOST_HEADERS.stream()
                                                        .map(hostHeader -> determineTenantSubdomain(request, hostHeader))
                                                        .toList();
        if (areEmpty(subdomains)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Tenant subdomain cannot be extracted from the current request. Will return the default tenant. Headers: [{}]",
                        getHeaders(request));
            }
            return Optional.of(TenantImpl.getDefaultTenant());
        }

        for (Optional<String> subdomain : subdomains) {
            Optional<Tenant> tenant = tenantBySubdomain(subdomain);
            if (tenant.isPresent()) {
                LOGGER.debug("Found registered tenant [{}] for subdomain [{}].", tenant.get(), subdomain.get());
                return tenant;
            }
        }
        return Optional.empty();
    }

    private boolean areEmpty(List<Optional<String>> optionals) {
        for (Optional<?> optional : optionals) {
            if (optional.isPresent()) {
                return false;
            }
        }
        return true;
    }

    private Optional<Tenant> tenantBySubdomain(Optional<String> subdomainOpt) {
        if (subdomainOpt.isEmpty()) {
            return Optional.empty();
        }
        String subdomain = subdomainOpt.get();
        return TENANT_CACHE.get(subdomain, k -> {
            LOGGER.debug("Searching for tenant with subdomain [{}] from database", subdomain);
            return tenantService.findBySubdomain(subdomain)
                                .map(TenantImpl::createFromEntity);
        });
    }

    private String getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        HOST_HEADERS.forEach(h -> headers.put(h, request.getHeader(h)));
        return gson.toJson(headers);
    }

    private Optional<String> determineTenantSubdomain(HttpServletRequest request, String hostHeader) {
        String headerValue = request.getHeader(hostHeader);
        if (null == headerValue) {
            LOGGER.debug("Cannot extract subdomain from header [{}] with null value.", hostHeader);
            return Optional.empty();
        }
        Matcher matcher = TENANT_SUBDOMAIN_PATTERN.matcher(headerValue);
        if (matcher.find()) {
            String tenantSubdomain = matcher.group(1);
            LOGGER.debug("Host [{}] from header [{}] MATCHES tenant subdomain pattern [{}]. Extracted subdomain: [{}]", headerValue,
                    hostHeader, TENANT_SUBDOMAIN_PATTERN.pattern(), tenantSubdomain);

            return Optional.of(tenantSubdomain);
        }
        LOGGER.debug("Host [{}] from header [{}] does NOT match tenant subdomain pattern [{}].", headerValue, hostHeader,
                TENANT_SUBDOMAIN_PATTERN.pattern());
        return Optional.empty();
    }
}
