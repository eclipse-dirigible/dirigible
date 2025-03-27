package org.eclipse.dirigible.components.jobs.handler;

import io.opentelemetry.api.trace.Span;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.nio.file.Path;
import java.util.Date;

@Service
public class JobExecutionService {

    public static final String JOB_PARAMETER_HANDLER = "dirigible-job-handler";
    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionService.class);
    public static String JOB_PARAMETER_ENGINE = "dirigible-engine-type";

    private final JobLogService jobLogService;
    private final JobNameCreator jobNameCreator;
    private final PlatformTransactionManager transactionManager;

    JobExecutionService(JobLogService jobLogService, JobNameCreator jobNameCreator,
            @Qualifier("defaultDbTransactionManagerDataSource") PlatformTransactionManager transactionManager) {
        this.jobLogService = jobLogService;
        this.jobNameCreator = jobNameCreator;
        this.transactionManager = transactionManager;
    }

    public void executeJob(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("!!! transactionManager: {} {}", transactionManager, transactionManager.getClass());

        String tenantJobName = context.getJobDetail()
                                      .getKey()
                                      .getName();
        String name = jobNameCreator.fromTenantName(tenantJobName);

        JobDataMap params = context.getJobDetail()
                                   .getJobDataMap();
        String handler = params.getString(JOB_PARAMETER_HANDLER);
        Span.current()
            .setAttribute("handler", handler);

        JobLog triggered = registerTriggered(name, handler);
        try {
            executeJob(context, name, handler, triggered, params);
            registeredFinished(name, handler, triggered);
        } catch (Exception ex) {
            registeredFailed(name, handler, triggered, ex);

            String msg = "Failed to execute JS. Job name [" + name + "], handler [" + handler + "]";
            LOGGER.error(msg, ex);
            JobExecutionException jobExecutionException = new JobExecutionException(msg, ex);
            throw jobExecutionException;
        }
    }

    private void executeJob(JobExecutionContext context, String name, String handler, JobLog triggered, JobDataMap params)
            throws JobExecutionException {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        TransactionStatus status = transactionManager.getTransaction(transactionDefinition);
        try {

            Span.current()
                .setAttribute("handler", handler);

            if (triggered != null) {
                context.put("handler", handler);
                Path handlerPath = Path.of(handler);

                try (DirigibleJavascriptCodeRunner runner = new DirigibleJavascriptCodeRunner()) {
                    runner.run(handlerPath);
                }
            }
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
            throw ex;
        }
        transactionManager.commit(status);
    }

    /**
     * Register triggered.
     *
     * @param name the name
     * @param module the module
     * @return the job log definition
     */
    private JobLog registerTriggered(String name, String module) {
        JobLog triggered = null;
        try {
            triggered = jobLogService.jobTriggered(name, module);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return triggered;
    }

    /**
     * Registered finished.
     *
     * @param name the name
     * @param module the module
     * @param triggered the triggered
     */
    private void registeredFinished(String name, String module, JobLog triggered) {
        try {
            jobLogService.jobFinished(name, module, triggered.getId(), new Date(triggered.getTriggeredAt()
                                                                                         .getTime()));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Registered failed.
     *
     * @param name the name
     * @param module the module
     * @param triggered the triggered
     * @param ex the ex
     */
    private void registeredFailed(String name, String module, JobLog triggered, Exception ex) {
        try {
            jobLogService.jobFailed(name, module, triggered.getId(), new Date(triggered.getTriggeredAt()
                                                                                       .getTime()), ex.getMessage());
        } catch (Exception se) {
            LOGGER.error(se.getMessage(), se);
        }
    }
}
