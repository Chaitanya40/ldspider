# Debugging #

If a process waits infinitely, used `kill -s SIGQUIT <processid>` to get debug output of the current state of threads

# Log Analysis #

```
sed -e "s/-1/0/" access.log > access.log.1
```

```
mkdir calamaris
cat access.log.1 | calamaris -F graph -F html --image-type png -a --output-path calamaris/ --domain-report 40
```

or

```
mkdir webalizer
webalizer -F squid access.log.1 -o webalizer/
```

# Profiling #

```
java -Xmx256M -Dhttp.proxyHost=localhost -Dhttp.proxyPort=3128 -Xrunhprof -jar dist/ldspider.0.1-dev.jar -u "http://harth.org/andreas/foaf#ah" -o aha-r4.nq -t 10 -r 2 -m 2 2> logfile.txt
```