import { Cmis as cmis } from 'sdk/cms/cmis';
import { Streams as streams } from 'sdk/io/streams';
import { Assert } from 'test/assert';

const session = cmis.getSession();

const rootFolder = session.getRootFolder();

const inputStream = streams.createByteArrayInputStream([101,102,103,104]);
const contentStream = session.getObjectFactory().createContentStream('test1.txt', 4, 'text/plain', inputStream);
const properties = {};
properties[cmis.OBJECT_TYPE_ID] = cmis.OBJECT_TYPE_DOCUMENT;
properties[cmis.NAME] = 'test1.txt';

const result = rootFolder.createDocument(properties, contentStream, cmis.VERSIONING_STATE_MAJOR);

Assert.assertTrue(result !== null && result !== undefined);
