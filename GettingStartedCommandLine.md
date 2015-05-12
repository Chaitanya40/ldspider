# Introduction #

The LDSpider projects aims to provide a lightweighted web crawler for the Linked Data Web.
The library will be optimised for a pure in-memory setup. Thus, the scalability of this library is bounded to the allocated and available memory, yet, we implemented some means to lower the main memory consumption, see various switches below.

This page provides a short overview over the LDSpider command line application. If you need more flexibility you can also use the [Java API](GettingStartedAPI.md).

# Usage #
```
$ java -jar ldspider-1.3-with-dependencies.jar 
```

```
usage:   [-a <filename[.gz]>] [-accept <MIME Types>] [-any23] [-any23ext <any23
       extractor names>] -b <depth uri-limit pld-limit> | -c <max-uris> | -d
       <directory> [-bl <extensions>]  [-ctIgnore] [-cto <time in ms>]  [-dbfq]
       [-dbfqInput <filename>] [-dbfqSave <basefilename>] [-df <base filename>]
       [-dh <filename>] [-dr] [-ds <filename>] [-e] [-f <uris> | -n | -y] [-h]
       [-hopsplit] [-lfADignore] [-m <frontier-file>] [-mapseed] [-minpld <min.
       # of active PLDs>] [-mr <max. # of redirects>]  [-o <filename[.gz]> | -oe
       <uri>]  [-polite <time in ms>] [-r <filename[.gz]>] [-resumebfc <seenfile
       redirectsfile>] [-rf] -s <file> [-sdf <sort gzip>] [-sto <time in ms>]
       [-t <threads>] [-ul <number>] [-v <filename[.gz]>]

 -a <filename[.gz]>                       name of access log file
 -accept <MIME Types>                     Change the HTTP Accept header to the
                                          one supplied
 -any23                                   Use any23 for extending the formats
                                          available to parse.
 -any23ext <any23 extractor names>        Override the defaultly selected
                                          extractors that are to be loaded with
                                          any23. Leave empty to use all any23
                                          has available. Default: [html-rdfa11,
                                          rdf-xml, rdf-turtle, rdf-nt, rdf-nq,
                                          html-script-turtle]
 -b <depth uri-limit pld-limit>           do strict breadth-first (uri-limit and
                                          pld-limit optional)
 -bl <extensions>                         overwrite default suffixes of files
                                          that are to be ignored in the crawling
                                          with the ones supplied. Note: no
                                          suffix is also an option. Default:
                                          [.txt, .html, .xhtml, .json, .ttl,
                                          .nt, .jpg, .pdf, .htm, .png, .jpeg,
                                          .gif]
 -c <max-uris>                            use load balanced crawling strategy
 -ctIgnore                                Parse and fetch disrespective of
                                          content-type
 -cto,--connection-timeout <time in ms>   Set connection timeout. Default:
                                          16000ms
 -d <directory>                           download seed URIs and archive raw
                                          data
 -dbfq                                    Uses the on-disk BreadthFirstQueue if
                                          crawling breadth-first. Does not
                                          support uri-limit and pld-limit (see
                                          -b). Needs -sdf sort to be set. Ranks
                                          URIs on the PLDs according to their
                                          in-link count.
 -dbfqInput <filename>                    For the on-disk breadth-first queue
                                          (-dbfq), use this file as input for
                                          the eternal in-link count.
 -dbfqSave <basefilename>                 For the on-disk breadth-first queue
                                          (-dbfq), save the eternal in-link
                                          count per URI after each hop to this
                                          file.
 -df <base filename>                      Dump frontier after each round to file
                                          (only breadth-first). File name
                                          format: <base filename>-<round number>
 -dh <filename>                           Dump header information to a separate
                                          file. It makes no sense to set -e at
                                          the same time.
 -dr                                      Do not use Redirects.class for
                                          Redirects handling
 -ds <filename>                           Choose the on-disk Seen
                                          implementation. Argument: basefilename
 -e                                       omit header triple in data
 -f,--follow <uris>                       only follow specific predicates
 -h,--help                                print help
 -hopsplit                                split output hopwise
 -lfADignore                              Make No difference between A and T box
                                          in crawling
 -m <frontier-file>                       memory-optimised (puts frontier on
                                          disk)
 -mapseed                                 Sets if the minimum active plds should
                                          already be taken into account
                                          downloading the seedlist.
 -minpld <min. # of active PLDs>          In order to avoid PLD starvation, set
                                          the minimum number of active plds for
                                          each breadth first queue round. The
                                          seedlist is always downloaded
                                          completely (see -mapseed).
 -mr <max. # of redirects>                Specify the length a redirects (30x)
                                          is allowed to have at max. (default:
                                          4; with seq.strategy: 1).
 -n                                       do not extract links - just follow
                                          redirects
 -o <filename[.gz]>                       name of NQuad file with output
 -oe <uri>                                SPARQL/Update endpoint for output
 -polite <time in ms>                     Time to wait between two requests to a
                                          PLD.
 -r <filename[.gz]>                       write redirects.nx file
 -resumebfc <seenfile redirectsfile>      Resume an interrupted breadth-first
                                          crawl. Requires a seen file and a
                                          redirects file. The old frontier
                                          should be the seedlist.
 -rf,--rankFrontier                       If set, the URIs in frontier are
                                          ranked according to their number of
                                          in-links, and alphabetically as second
                                          ordering. Use this option for
                                          something like a priority queue.
 -s <file>                                location of seed list
 -sdf <sort gzip>                         Use SortingDiskFrontier as frontier.
                                          If URIs are to be returned sorted, add
                                          "sort" as value, if all temp files
                                          involved are to be gzipped, add
                                          "gzip".
 -sto,--socket-timeout <time in ms>       Set socket timeout. Default: 16000ms
 -t <threads>                             number of threads (default 2)
 -ul <number>                             Sets a limit for the Uris downloaded
                                          overall containing >0 stmts. Hits the
                                          interval [limit;limit+#threads]. Not
                                          necessarily intended for load-balanced
                                          crawling.
 -v <filename[.gz]>                       name of file logging rounds
 -y,--stay                                stay on hostnames of seed uris
```

## Crawling Strategies ##

There's three modes for crawling:

  * breadth first: you specify the depth of the breadth-first traversal (number of rounds), as well as the maximum number of URIs crawled per round per pay-level domain. -1 means unlimited amount of URIs per pay-level domain.
  * load balancing: you specify the maximum number of URIs crawled, and the crawler will try to fetch up to that number of URIs as quickly as possible. Please note that the crawler might get a few more URIs as we check only occasionally if the limit has been reached.

## Proxies ##

Use java -Dhttp.proxyHost=localhost -Dhttp.proxyPort=3128 to enable proxy.
Note: standard squid does not seem to cache 303 redirects (at least not the FOAF ones).

For proxy authentication, use -Dhttp.proxyUser -Dhttp.proxyPassword

http.nonProxyHosts is not implemented

(there seems to exists a way to just use system proxy settings - http\_proxy env variable under Linux but that needs to be tested http://www.rgagnon.com/javadetails/java-0085.html,
System.setProperty("java.net.useSystemProxies", "true");)