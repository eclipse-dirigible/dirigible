import { Cmis as cmis } from 'sdk/cms/cmis';
import { Assert } from 'test/assert';

const result = cmis.getSession();

Assert.assertTrue(result !== null && result !== undefined);
