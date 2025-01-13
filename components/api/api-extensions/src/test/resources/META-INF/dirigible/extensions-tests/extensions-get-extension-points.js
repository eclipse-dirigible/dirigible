import { Extensions } from 'sdk/extensions/extensions';
import { Assert } from 'test/assert';

const result = Extensions.getExtensionPoints();

Assert.assertEquals(result[0], "test_extpoint1");
