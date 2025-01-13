import { Cmis as cmis } from 'sdk/cms/cmis';
import { Assert } from 'test/assert';

const session = cmis.getSession();

const result = session.getRootFolder();

Assert.assertTrue(result !== null && result !== undefined);
