import { test, assertEquals, assertTrue, assertFalse } from "@aerokit/sdk/junit"
import { caches } from "@aerokit/sdk/cache";

test('cache-api-test', () => {

    const KEY = "key1";
    const VALUE = "value1";

    caches.clear();

    assertFalse(caches.contains(KEY))
    assertEquals(null, caches.get(KEY));

    caches.set(KEY, VALUE);
    assertTrue(caches.contains(KEY))
    assertEquals(VALUE, caches.get(KEY));

    caches.delete(KEY);
    assertFalse(caches.contains(KEY))
    assertEquals(null, caches.get(KEY));

    caches.set(KEY, VALUE);
    caches.clear();

    assertFalse(caches.contains(KEY))
    assertEquals(null, caches.get(KEY));
});

test('cache-api-boolean', () => {

    const KEY = "boolean-key";
    const VALUE = true;

    caches.clear();

    caches.set(KEY, VALUE);
    assertEquals(VALUE, caches.get(KEY));
});

test('cache-api-number', () => {

    const KEY = "number-key";
    const VALUE = 123;

    caches.clear();

    caches.set(KEY, VALUE);
    assertEquals(VALUE, caches.get(KEY));
});

test('cache-api-string', () => {

    const KEY = "string-key";
    const VALUE = "This is a string value";

    caches.clear();

    caches.set(KEY, VALUE);
    assertEquals(VALUE, caches.get(KEY));
});

test('cache-api-object', () => {

    const KEY = "object-key";
    const VALUE = {
        name : "Ivan",
        age: 35,
        parent:  {
           name : "Petar",
           age: 55
        }
    };

    caches.clear();

    caches.set(KEY, VALUE);

    const cachedValue = caches.get(KEY);
    assertTrue(cachedValue != null);

    assertEquals("Ivan", cachedValue.name);
    assertEquals(35, cachedValue.age);

    assertEquals("Petar", cachedValue.parent.name);
    assertEquals(55, cachedValue.parent.age);
});
