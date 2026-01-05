
import { Streams } from '@aerokit/sdk/io/streams';
import { Assert } from 'test/assert';

const bais = Streams.createByteArrayInputStream([61, 62, 63]);
const baos = Streams.createByteArrayOutputStream();

Streams.copy(bais, baos);

const result = baos.getBytes();

Assert.assertEquals(result[1], 62);
