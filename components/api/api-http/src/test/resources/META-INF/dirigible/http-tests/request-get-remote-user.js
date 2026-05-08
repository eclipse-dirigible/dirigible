import { Request } from '@aerokit/sdk/http/request';
import { Assert } from 'test/assert';

Assert.assertEquals(Request.getRemoteUser(), 'user');
