/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.snowflake;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Profile("snowflake")
@Component
class SnowflakeAuthFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeAuthFilter.class);

    private static final String SNOWFLAKE_USER_HEADER = "Sf-Context-Current-User";

    private final SnowflakeUserDetailsService userDetailsService;

    public SnowflakeAuthFilter(SnowflakeUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String currentUser = request.getHeader(SNOWFLAKE_USER_HEADER);

        if (currentUser == null) {
            LOGGER.debug("Missing user header with name [{}]. Forwarding the request further", SNOWFLAKE_USER_HEADER);

            HttpSession session = request.getSession(false);
            if (null != session) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();

        if (authentication == null) {
            loginCurrentUser(request, currentUser);
        } else {
            validateSecurityContext(authentication, currentUser);
        }

        filterChain.doFilter(request, response);
    }

    private void validateSecurityContext(Authentication authentication, String currentUser) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            String loggedInUsername = userDetails.getUsername();
            if (!Objects.equals(currentUser, loggedInUsername)) {
                String errMessage = "Current user [" + currentUser + "] doesn't match the one in the security context: " + loggedInUsername;
                throw new InvalidSecurityContextException(errMessage);
            }
        } else {
            throw new InvalidSecurityContextException("Unexpected type [" + principal.getClass() + "] for  principal");
        }
    }

    private void loginCurrentUser(HttpServletRequest request, String currentUser) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(currentUser);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext()
                             .setAuthentication(authToken);
    }
}
