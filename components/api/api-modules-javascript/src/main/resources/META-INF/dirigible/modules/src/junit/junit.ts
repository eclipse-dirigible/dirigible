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
 * 
 * ### Use Cases:
 * - **Unit Testing**: These utilities are primarily used for writing unit tests to verify the functionality of individual components or functions in isolation.
 * - **Integration Testing**: They can also be used in integration tests to validate the behavior of multiple components working together, ensuring that they interact correctly.
 * 
 * ### Example Usage:
 * ```ts
 * import { test, assertEquals, assertTrue } from "@aerokit/sdk/junit";
 * 
 * test("should add two numbers correctly", () => {
 *     const result = add(2, 3);
 *     assertEquals(5, result);
 * });
 * 
 * test("should return true for valid input", () => {
 *     const isValid = validateInput("valid input");
 *     assertTrue(isValid);
 * });
 * ```
 */

const Assert = Java.type('org.junit.Assert');

/**
 * Defines a test case.
 *
 * @param name The name of the test case.
 * @param testFn The function containing the test logic and assertions.
 */
export function test(name: string, testFn: () => void) {
    // Calls the global test runner function provided by the SDK environment.
    (globalThis as any).test(name, testFn);
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
        Assert.assertEquals(messageOrExpected, expectedOrActual, actualOrUndefined);
    } else {
        Assert.assertEquals(messageOrExpected, expectedOrActual);
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
        Assert.assertNotEquals(messageOrUnexpected, unexpectedOrActual, actualOrUndefined);
    } else {
        Assert.assertNotEquals(messageOrUnexpected, unexpectedOrActual);
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
        Assert.assertTrue(messageOrCondition, conditionOrUndefined);
    } else {
        Assert.assertTrue(messageOrCondition);
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
        Assert.assertFalse(messageOrCondition, conditionOrUndefined);
    } else {
        Assert.assertFalse(messageOrCondition);
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
        Assert.fail(message);
    } else {
        Assert.fail();
    }
}