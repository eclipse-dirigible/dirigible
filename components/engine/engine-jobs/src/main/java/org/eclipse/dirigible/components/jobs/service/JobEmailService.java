/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.service;

import java.util.List;
import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.jobs.domain.JobEmail;
import org.eclipse.dirigible.components.jobs.repository.JobEmailRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class JobEmailService.
 */
@Service
@Transactional
public class JobEmailService extends BaseArtefactService<JobEmail, Long> {

    /**
     * Instantiates a new job email service.
     *
     * @param repository the repository
     */
    public JobEmailService(JobEmailRepository repository) {
        super(repository);
    }

    /**
     * Delete all by job name.
     *
     * @param jobName the job name
     */
    public void deleteAllByJobName(String jobName) {
        JobEmail filter = new JobEmail();
        filter.setJobName(jobName);
        Example<JobEmail> example = Example.of(filter);
        List<JobEmail> jobLogs = getRepo().findAll(example);
        getRepo().deleteAll(jobLogs);
    }

    /**
     * Removes the email.
     *
     * @param id the id
     */
    public void removeEmail(Long id) {
        JobEmail jobEmail = findById(id);
        if (jobEmail != null) {
            delete(jobEmail);
        }
    }

    /**
     * Find all by job name.
     *
     * @param jobName the job name
     * @return the list
     */
    @Transactional(readOnly = true)
    public List<JobEmail> findAllByJobName(String jobName) {
        JobEmail filter = new JobEmail();
        if (jobName != null && jobName.startsWith("/")) {
            jobName = jobName.substring(1);
        }
        filter.setJobName(jobName);
        Example<JobEmail> example = Example.of(filter);
        return getRepo().findAll(example);
    }

    /**
     * Adds the email.
     *
     * @param job the job
     * @param email the email
     */
    public void addEmail(String job, String email) {
        if (job != null && job.startsWith("/")) {
            job = job.substring(1);
        }
        JobEmail jobEmail = new JobEmail(job, email, null, null, job, email);
        jobEmail.updateKey();
        save(jobEmail);
    }
}
