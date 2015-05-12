# Introduction #

Some of the available Linked Data sources are not geared towards high-rate lookups.
To shield these systems from crawlers and make sites more stable, admins can put measures into place to slow down access in order to provide stable service.

# Squid Configuration #

Squid is a HTTP proxy which could be also used as reverse-proxy, i.e. put between a web server and user agents at the web server side.
Squid offers so-called delay classes to slow down access to IP addresses, cf. http://www.deckle.co.za/squid-users-guide/Access_Control_and_Access_Control_Operators#Delay_Classes

# Apache Configuration #

Apache's httpd also has means to allow for limiting access rates.
cf. http://stackoverflow.com/questions/131681/apache-rate-limiting-options