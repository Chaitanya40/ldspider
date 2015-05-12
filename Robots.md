We are running the LDSpider as part of a project developing algorithms for managing LOD-RDF.

2009-12-17
> Started testing the LDSpider.

The ldspider honors the Robots Exclusion Protocol.
If not, please let us know where and when ([mailto:harth@kit.edu](mailto:harth@kit.edu) with "ldspider" in the subject line).

If you experience repeated lookups, you might want to add Expires headers to your files to allow HTTP caching.  Alternatively, you may exclude the ldspider robot by adding the following lines in your robots.txt:

```
User-agent: ldspider
Disallow: /
```