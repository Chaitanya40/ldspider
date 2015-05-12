# Introduction #
This tutorial provides a short walk-through on how to implement a simple parser using the LDSpider Java API. If you don't want to write code and just want to use LDSpider for a simple crawling task from the command line, have a look at [Getting started using LDSpider from the command line](GettingStartedCommandLine.md).
# Contents #

# Prerequisites #
In order to get started using the LDSpider API, get the most recent version from the [Downloads](http://code.google.com/p/ldspider/downloads/list). Alternatively, you can use the current version from the [repository](https://ldspider.googlecode.com/svn/trunk/).
## Dependencies ##
LDSpider has its dependencies managed using maven.
# Implementing a simple Crawler #
## Setting up the Crawler ##
The primary class of LDSpider is the **Crawler** class.
In order to implement a new crawler we create a new instance of it:
```
Crawler crawler = new Crawler(numberOfThreads);
Frontier frontier = new BasicFrontier();
frontier.setBlacklist(CrawlerConstants.BLACKLIST);
frontier.add(new URI(seedUri));
```
The behaviour of the crawler can be adjusted by setting hooks. In the following sections, we will create a simple crawler by setting a number of hooks.
An overview over all available Hooks can be found [here](Hooks.md)
## Setting the Link Filter ##
We can set a link filter to specify which links are retrieved from a parsed document.
In this example, we restrict the crawling to a specific domain using a Link Filter:
```
LinkFilter linkFilter = new LinkFilterDomain(frontier);  
linkFilter.addHost(hostUri);  
crawler.setLinkFilter(linkFilter);
```
An overview over all available Link Filters can be found [here](Hooks#LinkFilter.md)
## Setting the Content Handler ##
By default, LDSpider will handle all documents which use the RDF/XML format. If we want to handler different formats, we can register a Content Handler. Here we combine an RDF/XML Handler and an Nx Handler to handle the most commonly used Linked Data formats:
```
ContentHandler contentHandler = new ContentHandlers(new ContentHandlerRdfXml(), new ContentHandlerNx());
crawler.setContentHandler(contentHandler);
```
An overview over all available Content Handlers can be found [here](Hooks#ContentHandler.md)
## Setting the Sink ##
Finally, we need to define a sink, which can be used by the Crawler to write the extracted statements. In this example, we write all statements to a file using the N-QUADS format. For his purpose, we use the SinkCallback class which can use an arbitrary callback from the NxParser library (http://sw.deri.org/2006/08/nxparser/) to write the statements. Among others, NxParser includes a callback to write N-QUADS:
```
OutputStream os = new FileOutputStream(outputFile);
Sink sink = new SinkCallback(new CallbackNQOutputStream(os));
crawler.setOutputCallback(sink);
```
An overview over all available Sinks can be found [here](Hooks#Sink.md)
## Error handling ##
In the crawling process various errors can arise. We don't want to silently ignore these errors, but to log them to a file. For this purpose we register a new Error Handler which handles all exceptions:
```
//Print to Stdout
PrintStream ps = System.out;
//Print to file
FileOutputStream fos = new FileOutputStream(errorLogFile);

//Add printstream and file stream to error handler
ErrorHandler eh = new ErrorHandlerLogger(ps, rcb);
Callback rcb = new CallbackNQOutputStream(fos);
rcb.startDocument();

//Connect hooks with error handler
crawler.setErrorHandler(eh);
frontier.setErrorHandler(eh);
linkFilter.setErrorHandler(eh);     
```
The log level can be adjusted using:
```
java.util.logging.Logger.getLogger("com.ontologycentral.ldspider").setLevel(java.util.logging.Level.WARNING)
```
## Start crawling ##
We instruct the crawler to start crawling pages by calling an evaluate method.
In this example, we use the breadth first strategy, which limits the depth of the traversal (number of rounds), as well as the maximum number of URIs crawled per round.
The breadth first strategy can be configured to crawl the schema information, in which case it will do an extra round to get the schema information of the last round.
```
int depth = 2;
int maxURIs = 100;
boolean includeABox = true;
boolean includeTBox = false;

crawler.evaluateBreadthFirst(frontier, depth, maxURIs, includeABox, includeTBox);
```