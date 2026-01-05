import { Writer } from '@aerokit/sdk/indexing/writer';
import { Searcher } from '@aerokit/sdk/indexing/searcher';
import { Assert } from 'test/assert';

Writer.add("index3", "myfile1", "apache lucene", new Date(), {"name1":"value1"});
Writer.add("index3", "myfile2", "lucene - the search engine", new Date(), {"name2":"value2"});

const found = Searcher.search("index3", "engine");

console.log(JSON.stringify(found));

Assert.assertTrue((found !== null) && (found !== undefined) && found.length === 1);
