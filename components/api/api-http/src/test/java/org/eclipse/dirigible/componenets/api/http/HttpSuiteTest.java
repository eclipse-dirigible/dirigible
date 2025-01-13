/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.componenets.api.http;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration
@WebAppConfiguration
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
public class HttpSuiteTest {

    @Autowired
    private JavascriptService javascriptService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    private Filter springSecurityFilterChain;

    @Test
    public void executeClientTest() throws Exception {
        javascriptService.handleRequest("http-tests", "client-get.js", null, null, false);
        javascriptService.handleRequest("http-tests", "client-get-binary.js", null, null, false);
    }

    // @WithMockUser(username = "user", roles={"role1"})
    @Test
    public void executeRequestTest() throws Exception {
        mockMvc.perform(get("/services/js/http-tests/request-get-attribute.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes()))
                                                                               .requestAttr("attr1", "val1"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        // mockMvc.perform(get("/services/js/http-tests/request-get-auth-type.js")
        // .header(HttpHeaders.AUTHORIZATION,
        // "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes())))
        // .andDo(print())
        // .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-get-header.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes()))
                                                                            .header("header1", "header1")
                                                                            .requestAttr("attr1", "val1"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-get-header-names.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes()))
                                                                                  .header("header1", "header1")
                                                                                  .header("header2", "header2")
                                                                                  .requestAttr("attr1", "val1"))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-get-method.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-get-path-info.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-get-path-translated.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-get-remote-user.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-get-server-name.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes()))
                                                                                 .with(request -> {
                                                                                     request.setServerName("server1");
                                                                                     return request;
                                                                                 }))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-is-user-in-role.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
        mockMvc.perform(get("/services/js/http-tests/request-is-valid.js").header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                                                                                                                              .encodeToString(
                                                                                                                                      "user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void executeResponseTest() throws Exception {
        mockMvc.perform(get("/services/js/http-tests/response-get-header-names.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void executeSessionTest() throws Exception {
        mockMvc.perform(get("/services/js/http-tests/session-get-attribute-names.js").header(HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder()
                                 .encodeToString("user:password".getBytes())))
               .andDo(print())
               .andExpect(status().is2xxSuccessful());
    }

    @Configuration
    static class Config {
        @Autowired
        public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                .withUser("user")
                .password("password")
                .roles("ROLE");
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return NoOpPasswordEncoder.getInstance();
        }

        @Autowired
        HttpResponseHeaderHandlerFilter httpResponseHeaderHandlerFilter;

    }


    static class HttpResponseHeaderHandlerFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            response.setHeader("header1", "val1");
            response.setHeader("header2", "val2");

            filterChain.doFilter(request, response);
        }
    }


    @SpringBootApplication
    static class TestConfiguration {

        @Bean
        HttpResponseHeaderHandlerFilter getHttpResponseHeaderHandlerFilter() {
            return new HttpResponseHeaderHandlerFilter();
        }

    }
}
