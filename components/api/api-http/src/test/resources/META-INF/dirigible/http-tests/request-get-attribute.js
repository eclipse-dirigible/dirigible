import { Request } from 'sdk/http/request';
import { Assert } from 'test/assert';

Assert.assertEquals(Request.getAttribute('attr1'), 'val1');
