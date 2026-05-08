import { Writer } from '@aerokit/sdk/indexing/writer';
import { Searcher } from '@aerokit/sdk/indexing/searcher';
import { Assert } from 'test/assert';

Writer.add("index2", "myfile1", "apache lucene", new Date(123));
Writer.add("index2", "myfile2", "lucene - the search engine", new Date(234), {"name2":"value2"});
Writer.add("index2", "myfile3", "search engine", new Date(345), {"name2":"value2"});

const found = Searcher.between("index2", new Date(124), new Date(344));

console.log(JSON.stringify(found));

Assert.assertTrue((found !== null) && (found !== undefined) && found.length === 1);
