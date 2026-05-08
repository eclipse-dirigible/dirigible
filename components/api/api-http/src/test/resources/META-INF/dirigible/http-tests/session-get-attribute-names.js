import { Session } from '@aerokit/sdk/http/session';
import { Assert } from 'test/assert';

Session.setAttribute('attr1', 'value1');

Assert.assertEquals(JSON.stringify(Session.getAttributeNames()), '["invocation.count","attr1"]');
