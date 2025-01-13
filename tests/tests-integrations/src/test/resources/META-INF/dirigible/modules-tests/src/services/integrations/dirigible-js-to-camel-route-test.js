import { test, assertEquals } from "sdk/junit"
import { Integrations } from "sdk/integrations"

test('dirigible-js-to-camel-route-test', () => {
    const message = "Initial Message";
    const expected = `${message} -> camel route inbound1 handled this message`;
    const actual = Integrations.invokeRoute('direct:inbound1', message, []);
    assertEquals("Received an unexpected message from route inbound1 ", expected, actual);
});
