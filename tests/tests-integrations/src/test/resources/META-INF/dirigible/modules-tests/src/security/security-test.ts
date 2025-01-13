import { user } from "sdk/security";
import { assertEquals, test } from "sdk/junit"

test('get-user-test', () => {
	assertEquals('Unexpected user', user.getName(), 'guest');
});
