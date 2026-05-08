
import { Files } from '@aerokit/sdk/io/files';
import { Streams } from '@aerokit/sdk/io/streams';
import { Assert } from 'test/assert';

const tempFile1 = Files.createTempFile("dirigible", ".txt");
console.log('Temp file 1: ' + tempFile1);
Files.writeText(tempFile1, "Eclipse Dirigible");

const tempFile2 = Files.createTempFile("dirigible", ".txt");
console.log('Temp file 2: ' + tempFile2);

const input = Files.createInputStream(tempFile1);
const output = Files.createOutputStream(tempFile2);

Streams.copy(input, output);

const result = Files.readText(tempFile2);

Files.deleteFile(tempFile1);
Files.deleteFile(tempFile2);

Assert.assertEquals(result, "Eclipse Dirigible");
