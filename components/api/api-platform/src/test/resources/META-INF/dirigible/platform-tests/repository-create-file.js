import { Repository } from "@aerokit/sdk/platform/repository";
import { Assert } from 'test/assert';

Repository.createResource("/registry/public/test/file.js", "console.log('Hello World');", "application/json");
const resource = Repository.getResource("/registry/public/test/file.js");
const content = resource.getText();

Assert.assertTrue(content !== undefined && content !== null);
