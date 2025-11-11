
import { Files } from '@aerokit/sdk/io/files';
import { Assert } from 'test/assert';

const tempFile = Files.createTempFile("dirigible", ".txt");

console.log('Temp file: ' + tempFile);

Files.writeText(tempFile, "Eclipse Dirigible");
Files.deleteFile(tempFile);

Assert.assertTrue((tempFile !== null) && (tempFile !== undefined));
