import { Request } from 'sdk/http/request';
import { Assert } from 'test/assert';

Assert.assertEquals(Request.getHeader('header1'), 'header1');
