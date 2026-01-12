
import { Streams } from '@aerokit/sdk/io/streams';
import { Assert } from 'test/assert';

const baos = Streams.createByteArrayOutputStream();
baos.writeText("some text");

const bais = Streams.createByteArrayInputStream(baos.getBytes());
const result = bais.readText();

Assert.assertEquals(result, "some text");
