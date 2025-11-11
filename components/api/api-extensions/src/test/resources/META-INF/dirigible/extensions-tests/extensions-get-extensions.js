import { Extensions } from '@aerokit/sdk/extensions/extensions';
import { Assert } from 'test/assert';

const result = Extensions.getExtensions('test_extpoint1');

Assert.assertEquals(result[0], "/test_ext_module1");
