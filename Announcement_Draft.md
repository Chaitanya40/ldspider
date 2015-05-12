We are happy to announce the release of ldSpider 1.1. ldSpider is an extensible Linked Data crawling framework, enabling client applications to traverse and to consume the Web of Linked Data. ldSpider provides an easy to use command line application as well as a Java API which allows applications to configure and control the details of the crawling process.

The project is a co-operation between Andreas Harth at AIFB and Juergen Umbrich at DERI. Aidan Hogan and Robert Isele are contributing.

ldSpider 1.1 introduces a couple of new features including:
  * In addition to the native support of RDF/XML, ldSpider 1.1 can utilize an Any23 server to handle all kinds of input formats including Turtle, Notation 3, RDFa and many microformats
  * The crawled data can be written to a Triple Store using SPARQL/Update
  * It can be configured to follow only ABox and/or TBox links. This can be used for example to configure the crawler to get the schema together with the primary data.

More information about ldSpider 1.1, including the download, can be found at http://code.google.com/p/ldspider/