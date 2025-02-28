package org.eclipse.dirigible.components.security.cognito;

import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

@Profile("cognito")
@Controller
@RequestMapping("/login")
public class CognitoLoginController {

    private final TenantService tenantService;

    public CognitoLoginController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/{registrationId}")
    public String login(@PathVariable String registrationId, HttpServletRequest request) {
        Set<String> provisionedClients = tenantService.findByStatus(TenantStatus.PROVISIONED)
                                                      .stream()
                                                      .map(e -> e.getSubdomain())
                                                      .collect(Collectors.toSet());
        if (!provisionedClients.contains(registrationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid OAuth2 client");
        }

        return "redirect:/oauth2/authorization/" + registrationId;
    }
}
