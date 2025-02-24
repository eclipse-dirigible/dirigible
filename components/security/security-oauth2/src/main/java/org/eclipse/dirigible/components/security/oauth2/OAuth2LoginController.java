package org.eclipse.dirigible.components.security.oauth2;

import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/login")
public class OAuth2LoginController {

    @Autowired
    private TenantService tenantService;

    @GetMapping("/{registrationId}")
    public String login(@PathVariable String registrationId) {
        if (DirigibleConfig.MULTI_TENANT_MODE_ENABLED.getBooleanValue()) {
            Set<String> tenants = tenantService.findByStatus(TenantStatus.PROVISIONED)
                                               .stream()
                                               .map(e -> e.getSubdomain())
                                               .collect(Collectors.toSet());
            if (!tenants.contains(registrationId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid OAuth2 client");
            }
        }

        return "redirect:/oauth2/authorization/" + registrationId;
    }
}
