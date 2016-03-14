# lucene-leveldb

Lucene的索引一般存储在文件系统（FSDirectory）或内存（RAMDirectory）。官方提供Directory接口，也就是说通过实现这个接口，可以把索引存储在其他地方(MySQL,MongoDB,LevelDB......)，甚至HDFS也没问题。[lucene-leveldb](https://github.com/wenzuojing/lucene-leveldb)实现了把索引存储到LevelDB,索引的性能跟RAMDirectory相差不大，代码不是很复杂，仅仅提供实现思路，要在实际环境中应用还用很多优化的地方。

环境/依赖：

* Java 1.7+
* Lucene 5.0+
* [Leveldb 0.7](https://github.com/dain/leveldb)


使用方式：
```java

        Path path = Paths.get("db-data");

        File indexDir = path.toFile();

        if (indexDir.exists()) {
            TestUtils.deleteDir(indexDir);
        }

        Directory directory = new LeveldbDirectory(path);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        IndexWriter writer =
                new IndexWriter(directory, indexWriterConfig);


        File resourceDir = new File(RAMDirectoryTest.class.getResource("/test-data-set").getPath());

        Long startTime = System.currentTimeMillis();
        TestUtils.indexTextFile(writer, resourceDir);
        writer.close();
        System.out.println("Index speed time : " + (System.currentTimeMillis() - startTime));

        DirectoryReader index = DirectoryReader.open(directory);

        IndexSearcher searcher = new IndexSearcher(index);

        Query query = new QueryParser("content", analyzer).parse("good");

        TopScoreDocCollector collector = TopScoreDocCollector.create(100);

        startTime = System.currentTimeMillis();
        searcher.search(query, collector);
        System.out.println("Search speed time : " + (System.currentTimeMillis() - startTime));
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        System.out.println("Found " + hits.length + " hits.");
        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            //System.out.println((i + 1) + ". " + d.get("fileName") + " score=" + hits[i].score);
        }

        directory.close();

```
