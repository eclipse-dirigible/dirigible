/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.eclipse.dirigible.commons.api.helpers.NameValuePair;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobEmail;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.eclipse.dirigible.components.jobs.service.JobEmailService;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.service.JobService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Front facing REST service serving the Jobs.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "jobs")
public class JobEndpoint extends BaseEndpoint {

    /** The job service. */
    @Autowired
    private JobService jobService;

    /** The job log service. */
    @Autowired
    private JobLogService jobLogService;

    /** The job email service. */
    @Autowired
    private JobEmailService jobEmailService;



    /**
     * Find all.
     *
     * @param size the size
     * @param page the page
     * @return the page
     */
    @GetMapping("/pages")
    public Page<Job> findAll(@Parameter(description = "The size of the page to be returned") @RequestParam(required = false) Integer size,
            @Parameter(description = "Zero-based page index") @RequestParam(required = false) Integer page) {

        if (size == null) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (page == null) {
            page = 0;
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Job> extensions = jobService.getPages(pageable);
        return extensions;

    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the response entity
     */
    @GetMapping("/search")
    public ResponseEntity<Job> findByName(@RequestParam("name") String name) {

        return ResponseEntity.ok(jobService.findByName(name));

    }

    /**
     * List jobs.
     *
     * @return the response entity
     */
    @GetMapping
    public ResponseEntity<List<Job>> listJobs() {
        return ResponseEntity.ok(jobService.getAll());
    }

    /**
     * Enable job.
     *
     * @param name the name
     * @return the response entity
     * @throws Exception the exception
     */
    @PostMapping("enable/{name}")
    public ResponseEntity<Job> enableJob(@PathVariable("name") String name) throws Exception {
        return ResponseEntity.ok(jobService.enable(IRepository.SEPARATOR + name));
    }

    /**
     * Disable job.
     *
     * @param name the name
     * @return the response entity
     * @throws Exception the exception
     */
    @PostMapping("disable/{name}")
    public ResponseEntity<Job> disableJob(@PathVariable("name") String name) throws Exception {
        return ResponseEntity.ok(jobService.disable(IRepository.SEPARATOR + name));
    }

    /**
     * List job logs.
     *
     * @param job the job
     * @return the response entity
     */
    @GetMapping(value = "/logs/{*job}", produces = "application/json")
    public ResponseEntity<List<JobLog>> listJobLogs(@PathVariable("job") String job) {
        return ResponseEntity.ok(jobLogService.findByJob(job));
    }

    /**
     * Clear job logs.
     *
     * @param job the job
     * @return the response entity
     */
    @PostMapping(value = "/clear/{*job}", produces = "application/json")
    public ResponseEntity<?> clearJobLogs(@PathVariable("job") String job) {
        jobLogService.deleteAllByJobName(job);
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * List job parameters.
     *
     * @param job the job
     * @return the response entity
     */
    @GetMapping(value = "/parameters/{*job}", produces = "application/json")
    public ResponseEntity<List<JobParameter>> getJobParameters(@PathVariable("job") String job) {
        return ResponseEntity.ok(jobService.findByName(job)
                                           .getParameters());
    }

    /**
     * Trigger job.
     *
     * @param job the job
     * @param parameters the parameters
     * @return the response entity
     * @throws Exception in case of an error
     */
    @PostMapping(value = "/trigger/{*job}", produces = "application/json")
    public ResponseEntity<?> triggerJob(@PathVariable("job") String job, @Valid @RequestBody List<NameValuePair> parameters)
            throws Exception {
        Map<String, String> parametersMap = new HashMap<String, String>();

        for (NameValuePair pair : parameters) {
            parametersMap.put(pair.getName(), pair.getValue());
        }
        return ResponseEntity.ok(jobService.trigger(job, parametersMap));
    }

    /**
     * List job emails.
     *
     * @param job the job
     * @return the response entity
     */
    @GetMapping(value = "/emails/{*job}", produces = "application/json")
    public ResponseEntity<List<JobEmail>> getJobEmails(@PathVariable("job") String job) {
        return ResponseEntity.ok(jobEmailService.findAllByJobName(job));
    }

    /**
     * Add job email.
     *
     * @param job the job
     * @param email the email
     * @return the response entity
     */
    @PostMapping(value = "/emailadd/{*job}", produces = "application/json")
    public ResponseEntity<?> addJobEmails(@PathVariable("job") String job, @Valid @RequestBody String email) {

        if (email != null && email.indexOf(',') > -1) {
            String[] emails = email.split(",");
            for (String e : emails) {
                jobEmailService.addEmail(job, e);
            }
        } else {
            jobEmailService.addEmail(job, email);
        }

        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Remove job email.
     *
     * @param id the id
     * @return the response entity
     */
    @PostMapping(value = "/emailremove/{id}", produces = "application/json")
    public ResponseEntity<?> removeJobEmail(@PathVariable("job") Long id) {

        jobEmailService.removeEmail(id);

        return ResponseEntity.ok()
                             .build();
    }

}
