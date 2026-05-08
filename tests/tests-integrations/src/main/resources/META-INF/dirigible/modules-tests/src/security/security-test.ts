import { user } from "@aerokit/sdk/security";
import { assertEquals, test } from "@aerokit/sdk/junit"

test('get-user-test', () => {
	assertEquals('Unexpected user', user.getName(), 'guest');
});
