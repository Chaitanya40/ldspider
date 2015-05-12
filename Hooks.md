# Introduction #
LDSpider uses Hooks to adapt the behaviour of the crawler.

# Contents #


# Hooks #

## FetchFilter ##
A fetch filter checks if a page is allowed to be downloaded.

The following fetch filters are available:
### FetchFilterAllow ###
A fetch filter which downloads all papges.
### FetchFilterDeny ###
A fetch filter which downloads no page at all.
### FetchFilterRdfXml ###
A fetch filter which downloads all RDF/XML pages.

## LinkFilter ##
A link filter filters links from a parsed document. Can be configured to fetch ABox and/or TBox links in the document.

Every link filter defines the following configuration methods:
| **Method** | **Description** |
|:-----------|:----------------|
|setFollowABox|Configures if the given link filter will follow ABox links.|
|setFollowTBox|Configures if the given link filter will follow TBox links.|
|setErrorHandler|Configures an error handler.|

The following link filters are available:
### LinkFilterDefault ###
Default link filter, which can be configured to fetch all ABox and/or TBox links in the document.
### LinkFilterDomain ###
Link filter which adds only uris with matching host to queue.
### LinkFilterPrefix ###
Add only uris with matching prefix to queue.

## ContentHandler ##
A content handler is used to handle documents and extract RDF statements from it.

### ContentHandlerRdfXml ###

Handles RDF/XML documents.

### ContentHandlerNx ###

Handles N-TRIPLES and N-QUADS documents.

### ContentHandlerAny23 ###

Uses Any23 to handle all kinds of documents. This includes RDF/XML, Turtle, Notation 3, RDFa and many Microformats. Needs a running Any23 server which can be downloaded from http://any23.org.

**Parameters:**

| **Parameter** | **Required** | **Description** |
|:--------------|:-------------|:----------------|
|any23Endpoint|no|The Any23 endpoint to be used. If ommitted, the default Any23 endpoint of a local server will be used (http://127.0.0.1:8080).|

### ContentHandlers ###

Combines multiple content handlers.

**Parameters:**
| **Parameter** | **Required** | **Description** |
|:--------------|:-------------|:----------------|
|handlers|yes|The content handlers. Content handlers will be prioritized by their sequence order.|

**Example:**

Combining ContentHandlerRdfXml and ContentHandlerAny23 to handle RDF/XML documents locally and other documents using the Any23 server:

```
ContantHandler h = new ContentHandlers(new ContentHandlerRdfXml(), new ContentHandlerAny23());
crawler.setContentHandler(h);
```

## Sink ##
A Sink is receives the crawled statements and processes them usually by writing them to some output.

### SinkCallback ###

Uses an Callback from the NxParser library (http://sw.deri.org/2006/08/nxparser/) to write the statements. Among others, NxParser includes callbacks to write N-QUADS and RDF/XML.

**Parameters:**

| **Parameter** | **Required** | **Description** |
|:--------------|:-------------|:----------------|
|callback|yes|The NxParser callback to receive the statements.|
|includeProvenance|no|If true, provenance information will be included in the output.|

**Example:**

Writing the crawled data as N-QUADS to the standard output.

```
Sink sink = new SinkCallback(new CallbackNQOutputStream(System.out));
crawler.setOutputCallback(sink);
```

### SinkSparul ###

Writes the content to a triple store using SPARQL/Update. Optionally uses Named Graphs to represent provenance.

**Parameters:**
| **Parameter** | **Required** | **Description** |
|:--------------|:-------------|:----------------|
|sparulEndpoint|yes|The SPARQL/Update endpoint.|
|includeProvenance|yes|If true, provenance information will be included in the output.|
|graphUri|no|The graph into which all statements are written. If not given, each dataset will be written into its own graph.|

**Example:**
```
String sparulEndpoint = "http://localhost:2020/service/update";
boolean includeProvenance = false;
String graph = "http://example.com/SinkSparulTestGraph";

Sink sink = new SinkSparul(sparulEndpoint, includeProvenance, graph);
crawler.setOutputCallback(sink);
```

## ErrorHandler ##
An error handler processes exceptions.