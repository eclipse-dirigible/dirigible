import { Sequence } from '@aerokit/sdk/db/sequence';
import { Assert } from 'test/assert';

Sequence.create('mysequence');

const zero = Sequence.nextval('mysequence');
const one = Sequence.nextval('mysequence');

Sequence.drop('mysequence');

Assert.assertEquals(zero, 1);
Assert.assertEquals(one, 2);
