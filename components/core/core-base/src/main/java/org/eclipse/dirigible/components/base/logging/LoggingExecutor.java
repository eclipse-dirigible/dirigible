package org.eclipse.dirigible.components.base.logging;

import org.eclipse.dirigible.components.base.callable.CallableNoResultAndException;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.callable.CallableResultAndNoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class LoggingExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingExecutor.class);

    public static <T extends Throwable> void executeNoResultWithException(DataSource dataSource, CallableNoResultAndException<T> callable)
            throws Throwable {
        try {
            callable.call();
        } catch (Throwable ex) {
            LOGGER.error("Failed to execute a code [{}] for data source [{}]", callable, dataSource, ex);
            throw ex;
        }
    }

    public static <R, T extends Throwable> R executeWithException(DataSource dataSource, CallableResultAndException<R, T> callable)
            throws Throwable {
        try {
            return callable.call();
        } catch (Throwable ex) {
            LOGGER.error("Failed to execute a code [{}] for data source [{}]", callable, dataSource, ex);
            throw ex;
        }
    }

    public static <R> R executeWithoutException(CallableResultAndNoException<R> callable) throws RuntimeException {
        try {
            return callable.call();
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to execute a code [{}]", callable, ex);
            throw ex;
        }
    }
}
