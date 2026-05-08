import { Writer } from '@aerokit/sdk/indexing/writer';
import { Searcher } from '@aerokit/sdk/indexing/searcher';
import { Assert } from 'test/assert';

Writer.add("index1", "myfile1", "apache lucene", new Date(), {"name1":"value1"});
Writer.add("index1", "myfile2", "lucene - the search engine", new Date(), {"name2":"value2"});

const found = Searcher.search("index1", "lucene");

console.log(JSON.stringify(found));

Assert.assertTrue((found !== null) && (found !== undefined) && found.length === 2);
