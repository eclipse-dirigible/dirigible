/**
 * @module junit/junit
 * @package @aerokit/sdk/junit
 * @name JUnit
 * @overview
 * 
 * The JUnit module provides a set of utility functions for defining tests and performing assertions, wrapping the native JUnit Assertions for use in TypeScript/JavaScript tests. It allows developers to write test cases in a familiar format while leveraging the powerful assertion capabilities of JUnit, making it easier to validate code behavior and ensure correctness.
 * 
 * ### Key Features:
 * - **Test Definition**: The `test` function allows developers to define individual test cases with a descriptive name and a function containing the test logic.
 * - **Assertions**: Functions such as `assertEquals`, `assertNotEquals`, `assertTrue`, `assertFalse`, and `fail` provide a variety of assertion methods to validate conditions and compare values within tests.
 * - **Result Tracking**: Test results are automatically captured and can be retrieved via the results API.
 * 
 * ### Use Cases:
 * - **Unit Testing**: These utilities are primarily used for writing unit tests to verify the functionality of individual components or functions in isolation.
 * - **Integration Testing**: They can also be used in integration tests to validate the behavior of multiple components working together, ensuring that they interact correctly.
 * 
 * ### Example Usage:
 * ```ts
 * import { test, assertEquals, assertTrue, storeResults } from "@aerokit/sdk/junit";
 * 
 * test("should add two numbers correctly", () => {
 *     const result = 2 + 3;
 *     assertEquals(5, result);
 * });
 * 
 * test("should return true for valid input", () => {
 *     const isValid = false; // Replace with actual validation logic
 *     assertTrue(isValid);
 * });
 * 
 * // After running tests, store results for later retrieval
 * storeResults();
 * ```
 */

import { Response } from '@aerokit/sdk/http';

const Assertions = Java.type('org.junit.jupiter.api.Assertions');
const TestResultsService = Java.type('org.eclipse.dirigible.components.ide.junit.service.TestResultsService');
const ArrayList = Java.type('java.util.ArrayList');
const TestResult = Java.type('org.eclipse.dirigible.components.ide.junit.domain.TestResult');

// Test Result Tracking
interface TestResult {
    name: string;
    status: 'passed' | 'failed' | 'skipped';
    duration: number;
    error?: string;
    stackTrace?: string;
    timestamp: number;
}

class TestResultCollector {
    private results: TestResult[] = [];
    private currentTest: { name: string; startTime: number } | null = null;
    private haveFailedTests: boolean = false;

    public hasFailedTests(): boolean {
        return this.haveFailedTests;
    }

    public recordTestStart(name: string): void {
        this.currentTest = { name, startTime: new Date().getTime() };
    }

    public recordTestComplete(status: 'passed' | 'failed' | 'skipped', error?: { message: string; stackTrace?: string }): void {
        if (!this.currentTest) return;

        const result: TestResult = {
            name: this.currentTest.name,
            status,
            duration: new Date().getTime() - this.currentTest.startTime,
            timestamp: new Date().getTime(),
        };

        if (error) {
            result.error = error.message;
            result.stackTrace = error.stackTrace;
        }

        this.results.push(result);
        this.currentTest = null;

        if (status === 'failed' || error) {
            this.haveFailedTests = true;
        }
    }

    public getResults(): TestResult[] {
        return [...this.results];
    }

    public storeResults(): void {
        const resultsList = new ArrayList();
        this.results.forEach(e => resultsList.add(new TestResult(e.name, e.status, e.duration, e.error, e.stackTrace, e.timestamp)));
        TestResultsService.get().storeResults(resultsList);
        Response.sendRedirect('/services/web/ide-junit-results/junit-results');
    }

    public getSummary(): {
        total: number;
        passed: number;
        failed: number;
        skipped: number;
        duration: number;
    } {
        const results = this.results;
        const total = results.length;
        const passed = results.filter(r => r.status === 'passed').length;
        const failed = results.filter(r => r.status === 'failed').length;
        const skipped = results.filter(r => r.status === 'skipped').length;
        const duration = results.reduce((sum, r) => sum + r.duration, 0);

        return { total, passed, failed, skipped, duration };
    }

    public clearResults(): void {
        this.results = [];
        this.currentTest = null;
        this.haveFailedTests = false;
    }
}

const resultCollector = new TestResultCollector();

/**
 * Defines a test case.
 *
 * @param name The name of the test case.
 * @param testFn The function containing the test logic and assertions.
 */
export function test(name: string, testFn: () => void) {
    resultCollector.recordTestStart(name);
    if (resultCollector.hasFailedTests()) {
        resultCollector.recordTestComplete('skipped');
        console.log(`- Skipping test "${name}" due to previous failures`);
    } else {
        try {
            testFn();
            resultCollector.recordTestComplete('passed');
        } catch (error: any) {
            resultCollector.recordTestComplete('failed', {
                message: error?.message || String(error),
                stackTrace: error?.stack,
            });
        }
    }
}

/**
 * Asserts that two objects or primitive values are equal.
 *
 * @template T The type of the values being compared.
 * @param expected The expected value.
 * @param actual The actual value.
 * @param message Optional message to display if the assertion fails.
 */
export function assertEquals<T>(expected: T, actual: T): void
export function assertEquals<T>(message: string, expected: T, actual: T): void
export function assertEquals<T>(messageOrExpected?: string | T, expectedOrActual?: T, actualOrUndefined?: T): void {
    if (arguments.length === 3) {
        Assertions.assertEquals(messageOrExpected, expectedOrActual, actualOrUndefined);
    } else {
        Assertions.assertEquals(messageOrExpected, expectedOrActual);
    }
}

/**
 * Asserts that two objects or primitive values are not equal.
 *
 * @template T The type of the values being compared.
 * @param unexpected The unexpected value.
 * @param actual The actual value.
 * @param message Optional message to display if the assertion fails.
 */
export function assertNotEquals<T>(unexpected: T, actual: T): void
export function assertNotEquals<T>(message: string, unexpected: T, actual: T): void
export function assertNotEquals<T>(messageOrUnexpected?: string | T, unexpectedOrActual?: T, actualOrUndefined?: T): void {
    if (arguments.length === 3) {
        Assertions.assertNotEquals(messageOrUnexpected, unexpectedOrActual, actualOrUndefined);
    } else {
        Assertions.assertNotEquals(messageOrUnexpected, unexpectedOrActual);
    }
}

/**
 * Asserts that a condition is true.
 *
 * @param condition The condition to test.
 * @param message Optional message to display if the assertion fails.
 */
export function assertTrue(condition: boolean): void
export function assertTrue(message: string, condition: boolean): void
export function assertTrue(messageOrCondition?: string | boolean, conditionOrUndefined?: boolean): void {
    if (arguments.length === 2) {
        Assertions.assertTrue(messageOrCondition, conditionOrUndefined);
    } else {
        Assertions.assertTrue(messageOrCondition);
    }
}

/**
 * Asserts that a condition is false.
 *
 * @param condition The condition to test.
 * @param message Optional message to display if the assertion fails.
 */
export function assertFalse(condition: boolean): void
export function assertFalse(message: string, condition: boolean): void
export function assertFalse(messageOrCondition?: string | boolean, conditionOrUndefined?: boolean): void {
    if (arguments.length === 2) {
        Assertions.assertFalse(messageOrCondition, conditionOrUndefined);
    } else {
        Assertions.assertFalse(messageOrCondition);
    }
}

/**
 * Fails a test immediately.
 *
 * @param message Optional message to display indicating the reason for the failure.
 */
export function fail(): void
export function fail(message: string): void
export function fail(message?: string): void {
    if (message) {
        Assertions.fail(message);
    } else {
        Assertions.fail();
    }
}

/**
 * Retrieves all test results collected during this session.
 *
 * @returns An array of test results.
 */
export function getResults(): Array<{
    name: string;
    status: 'passed' | 'failed' | 'skipped';
    duration: number;
    error?: string;
    stackTrace?: string;
    timestamp: number;
}> {
    return resultCollector.getResults();
}

/**
 * Retrieves a summary of test execution.
 *
 * @returns An object containing counts and total duration.
 */
export function getResultsSummary(): {
    total: number;
    passed: number;
    failed: number;
    skipped: number;
    duration: number;
} {
    return resultCollector.getSummary();
}

/**
 * Clears all collected test results.
 */
export function clearResults(): void {
    resultCollector.clearResults();
}

/**
 * Clears all collected test results.
 */
export function storeResults(): void {
    resultCollector.storeResults();
}