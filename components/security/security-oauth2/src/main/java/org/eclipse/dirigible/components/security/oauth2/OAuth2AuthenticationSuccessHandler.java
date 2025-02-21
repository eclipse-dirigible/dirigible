package org.eclipse.dirigible.components.security.oauth2;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom success handler to validate OIDC attribute.
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final String TENANT_ATTRIBUTE = "custom:tenant";

    /** The tenant service. */
    private final TenantService tenantService;

    /** The user service. */
    private final UserService userService;

    /**
     * Instantiates a new tenants endpoint.
     *
     * @param tenantService the tenant service
     */
    @Autowired
    public OAuth2AuthenticationSuccessHandler(TenantService tenantService, UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // TODO: Check if Multitenant Mode is enabled and if Active Spring profile is Cognito!
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            Map<String, Object> attributes = oidcUser.getAttributes();
            String attributeValue = Optional.ofNullable(attributes.get(TENANT_ATTRIBUTE))
                                            .map(Object::toString)
                                            .orElse(null);

            // TODO: Check the subdomain if it matches the tenant attribute!
            if (tenantService.findBySubdomain(attributeValue)
                             .isEmpty()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Invalid attribute value");
                return;
            }
        }

        // TODO: Redirect to the correct location, based on the environment variable!
        response.sendRedirect("/");
    }
}
