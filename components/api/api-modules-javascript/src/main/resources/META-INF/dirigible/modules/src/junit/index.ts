const Assert = Java.type('org.junit.Assert');

export function test(name: string, testFn: () => void) {
    (globalThis as any).test(name, testFn);
}

export function assertEquals<T>(expected: T, actual: T): void
export function assertEquals<T>(message: string, expected: T, actual: T): void
export function assertEquals<T>(messageOrExpected?: string, expectedOrActual?: T, actualOrUndefined?: T): void {
    if (arguments.length === 3) {
        Assert.assertEquals(messageOrExpected, expectedOrActual, actualOrUndefined);
    } else {
        Assert.assertEquals(messageOrExpected, expectedOrActual);
    }
}

export function assertNotEquals<T>(unexpected: T, actual: T): void
export function assertNotEquals<T>(message: string, unexpected: T, actual: T): void
export function assertNotEquals<T>(messageOrUnexpected?: string, unexpectedOrActual?: T, actualOrUndefined?: T): void {
    if (arguments.length === 3) {
        Assert.assertNotEquals(messageOrUnexpected, unexpectedOrActual, actualOrUndefined);
    } else {
        Assert.assertNotEquals(messageOrUnexpected, unexpectedOrActual);
    }
}

export function assertTrue(condition: boolean): void
export function assertTrue(message: string, condition: boolean): void
export function assertTrue(messageOrCondition?: any /* what? */, conditionOrUndefine?: boolean): void {
    if (arguments.length == 2) {
        Assert.assertTrue(messageOrCondition, conditionOrUndefine);
    } else {
        Assert.assertTrue(messageOrCondition);
    }
}

export function assertFalse(condition: boolean): void
export function assertFalse(message: string, condition: boolean): void
export function assertFalse(messageOrCondition?: any /* what? */, conditionOrUndefine?: boolean): void {
    if (arguments.length == 2) {
        Assert.assertFalse(messageOrCondition, conditionOrUndefine);
    } else {
        Assert.assertFalse(messageOrCondition);
    }
}

export function fail(): void
export function fail(message: string): void
export function fail(message?: string): void {
    if (message) {
        Assert.fail(message);
    } else {
        Assert.fail();
    }
}