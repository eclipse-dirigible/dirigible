/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A sign-in the identity provider refused, through a real OAuth2 login chain (#8801): the
 * provider's {@code error_description} reaches the login page once, only for a callback that
 * matched this session's authorization request.
 */
class OAuth2LoginFailuresTest {

    private static final String REFUSAL = "You need an invitation <to> sign in this way & ask the owner";
    private static final String REFUSAL_ESCAPED = "You need an invitation &lt;to&gt; sign in this way &amp; ask the owner";
    private static final String CUSTOM_LOGIN_PAGE = "/public/web/app/login.html";

    /** The chain of a deployment without a login page of its own. */
    @Nested
    @SpringJUnitWebConfig(GeneratedLoginPageChain.class)
    class GeneratedLoginPage {

        @Autowired
        private WebApplicationContext context;

        @Test
        void aRefusalShowsTheProvidersDescriptionOnceEscaped() throws Exception {
            MockMvc mvc = mvc(context);
            MockHttpSession session = new MockHttpSession();

            refuse(mvc, session, state(mvc, session), REFUSAL).andExpect(redirectedUrl("/login?error"));

            mvc.perform(get("/login?error").session(session))
               .andExpect(status().isOk())
               .andExpect(content().string(containsString(REFUSAL_ESCAPED)))
               .andExpect(content().string(not(containsString("Invalid credentials"))))
               .andExpect(content().string(not(containsString("<to>"))));
            mvc.perform(get("/login?error").session(session))
               .andExpect(content().string(containsString(GeneratedLoginPageFailureFilter.GENERIC_ALERT)));
        }

        @Test
        void aRefusalWithoutDescriptionReadsAsBefore() throws Exception {
            MockMvc mvc = mvc(context);
            MockHttpSession session = new MockHttpSession();

            refuse(mvc, session, state(mvc, session), null).andExpect(redirectedUrl("/login?error"));

            mvc.perform(get("/login?error").session(session))
               .andExpect(content().string(containsString(GeneratedLoginPageFailureFilter.GENERIC_ALERT)));
        }

        @Test
        void aCallbackThatMatchesNoAuthorizationRequestShowsNothingItCarries() throws Exception {
            MockMvc mvc = mvc(context);
            MockHttpSession session = new MockHttpSession();
            state(mvc, session);

            refuse(mvc, session, "made-up-state", REFUSAL).andExpect(redirectedUrl("/login?error"));

            mvc.perform(get("/login?error").session(session))
               .andExpect(content().string(containsString(GeneratedLoginPageFailureFilter.GENERIC_ALERT)))
               .andExpect(content().string(not(containsString("invitation"))));
        }

        @Test
        void aMadeUpDescriptionOnTheLoginPageShowsNothingOfIt() throws Exception {
            MvcResult result = mvc(context).perform(get("/login?error&error_description=" + "Call+0800+now"))
                                           .andReturn();

            assertThat(result.getResponse()
                             .getContentAsString()).doesNotContain("0800");
        }
    }

    /** The chain of a deployment with its own login page ({@code DIRIGIBLE_SECURITY_LOGIN_PAGE}). */
    @Nested
    @SpringJUnitWebConfig(CustomLoginPageChain.class)
    class CustomLoginPage {

        @Autowired
        private WebApplicationContext context;

        @Test
        void aRefusalIsHandedToTheApplicationsPageOnce() throws Exception {
            MockMvc mvc = mvc(context);
            MockHttpSession session = new MockHttpSession();

            refuse(mvc, session, state(mvc, session), REFUSAL).andExpect(redirectedUrl(CUSTOM_LOGIN_PAGE + "?error"));

            mvc.perform(get("/login/oauth2/failure").session(session))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.error").value("access_denied"))
               .andExpect(jsonPath("$.description").value(REFUSAL));
            mvc.perform(get("/login/oauth2/failure").session(session))
               .andExpect(status().isNoContent());
        }

        @Test
        void noRefusalIsNoContent() throws Exception {
            mvc(context).perform(get("/login/oauth2/failure"))
                        .andExpect(status().isNoContent());
        }
    }

    private static MockMvc mvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                              .apply(springSecurity())
                              .build();
    }

    /** Starts a sign-in, as the browser does, and answers the {@code state} it sent the provider. */
    private static String state(MockMvc mvc, MockHttpSession session) throws Exception {
        String location = mvc.perform(get("/oauth2/authorization/test").session(session))
                             .andExpect(status().is3xxRedirection())
                             .andReturn()
                             .getResponse()
                             .getRedirectedUrl();
        String state = UriComponentsBuilder.fromUriString(location)
                                           .build()
                                           .getQueryParams()
                                           .getFirst("state");
        // the browser decodes the state the provider echoes back
        return URLDecoder.decode(state, StandardCharsets.UTF_8);
    }

    /** The provider sends the browser back with a refusal. */
    private static ResultActions refuse(MockMvc mvc, MockHttpSession session, String state, String description) throws Exception {
        var callback = get("/login/oauth2/code/test").session(session)
                                                     .param("error", "access_denied")
                                                     .param("state", state);
        if (description != null) {
            callback.param("error_description", description);
        }
        return mvc.perform(callback);
    }

    private static ClientRegistrationRepository clients() {
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("test")
                                                                          .clientId("client")
                                                                          .clientSecret("secret")
                                                                          .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                                                          .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                                                                          .scope("profile")
                                                                          .authorizationUri("https://idp.example/authorize")
                                                                          .tokenUri("https://idp.example/token")
                                                                          .userInfoUri("https://idp.example/userinfo")
                                                                          .userNameAttributeName("sub")
                                                                          .build());
    }

    private static SecurityFilterChain chain(HttpSecurity http, String loginPage) throws Exception {
        http.authorizeHttpRequests(authz -> authz.requestMatchers("/oauth2/**", "/login/**")
                                                 .permitAll()
                                                 .anyRequest()
                                                 .authenticated())
            .oauth2Login(oauth2 -> {
                if (loginPage != null) {
                    oauth2.loginPage(loginPage);
                }
            });
        OAuth2LoginFailures.configure(http, loginPage, OAuth2LoginFailureMessages.descriptions());
        return http.build();
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class GeneratedLoginPageChain {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return clients();
        }

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return chain(http, null);
        }
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class CustomLoginPageChain {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return clients();
        }

        @Bean
        OAuth2LoginFailureEndpoint oauth2LoginFailureEndpoint() {
            return new OAuth2LoginFailureEndpoint();
        }

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return chain(http, CUSTOM_LOGIN_PAGE);
        }
    }
}
