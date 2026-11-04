/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.aggregation;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;

import tools.jackson.databind.json.JsonMapper;

/**
 * The branding script every shell loads first: it defines {@code top.PlatformBranding} from the
 * {@code DIRIGIBLE_BRANDING_*} configuration, tenant overrides included, unless a frame above has
 * defined it already.
 *
 * <p>
 * It also sets {@code top.PlatformScriptEngine}, whether the JavaScript engine is deployed, so the
 * IDE's pages know without a request of their own whether to offer what only runs there, and
 * {@code top.PlatformIssues}, whether the issues engine is, so the shells leave out "Report an
 * issue" and the Issues page where nothing answers {@code /services/issues} (#7991).
 *
 * <p>
 * Every value is written as a JSON string literal: a tenant administrator sets these values, and
 * one holding a quote, a line break or {@code </script>} must stay a string rather than become
 * script.
 */
final class BrandingScript {

    private static final String NAME_DEFAULT = "Dirigible";

    private static final String BRAND_DEFAULT = "Eclipse";

    private static final String BRAND_URL_DEFAULT = "https://www.dirigible.io/";

    private static final String FAVICON_DEFAULT = "/services/web/platform-branding/images/favicon.ico";

    private static final String LOGO_DEFAULT = "/services/web/platform-branding/images/dirigible.svg";

    private static final String THEME_DEFAULT = "blimpkit-auto";

    private static final String PREFIX_DEFAULT = "dirigible";

    private static final JsonMapper JSON = JsonMapper.builder()
                                                     .build();

    private BrandingScript() {}

    /**
     * Renders the script for the current configuration.
     *
     * @param scriptEngineAvailable whether the JavaScript engine is deployed
     * @param issuesAvailable whether the issues engine is deployed
     * @return the script
     */
    static String render(boolean scriptEngineAvailable, boolean issuesAvailable) {
        return "if (!top.hasOwnProperty('PlatformBranding')) top.PlatformBranding = {\n" //
                + "    name: " + value(DirigibleConfig.BRANDING_NAME, NAME_DEFAULT) + ",\n" //
                + "    subtitle: " + value(DirigibleConfig.BRANDING_SUBTITLE, "") + ",\n" //
                + "    brand: " + value(DirigibleConfig.BRANDING_BRAND, BRAND_DEFAULT) + ",\n" //
                + "    brandUrl: " + value(DirigibleConfig.BRANDING_BRAND_URL, BRAND_URL_DEFAULT) + ",\n" //
                + "    icons: {\n" //
                + "        favicon: " + value(DirigibleConfig.BRANDING_FAVICON, FAVICON_DEFAULT) + ",\n" //
                + "    },\n" //
                + "    logo: " + value(DirigibleConfig.BRANDING_LOGO, LOGO_DEFAULT) + ",\n" //
                + "    theme: " + value(DirigibleConfig.BRANDING_THEME, THEME_DEFAULT) + ",\n" //
                + "    prefix: " + value(DirigibleConfig.BRANDING_PREFIX, PREFIX_DEFAULT) + "\n" //
                + "};\n" //
                + "top.PlatformScriptEngine = " + scriptEngineAvailable + ";\n" //
                + "top.PlatformIssues = " + issuesAvailable + ";";
    }

    private static String value(DirigibleConfig key, String defaultValue) {
        String value = Configuration.get(key.getKey(), defaultValue);
        return JSON.writeValueAsString(value)
                   .replace("<", "\\u003c")
                   .replace("\u2028", "\\u2028")
                   .replace("\u2029", "\\u2029");
    }
}
