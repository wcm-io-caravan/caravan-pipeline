<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Pipeline
======
[![Build Status](https://travis-ci.org/wcm-io-caravan/caravan-pipeline.png?branch=develop)](https://travis-ci.org/wcm-io-caravan/caravan-pipeline)
[![Code Coverage](https://codecov.io/gh/wcm-io-caravan/caravan-pipeline/branch/develop/graph/badge.svg)](https://codecov.io/gh/wcm-io-caravan/caravan-pipeline)

wcm.io Caravan - JSON Data Pipelining Infrastructure

![Caravan](https://github.com/wcm-io-caravan/caravan-tooling/blob/master/public_site/src/site/resources/images/caravan.gif)

JSON Data Pipelining and Caching.

Documentation: https://caravan.wcm.io/pipeline/<br/>
Issues: https://wcm-io.atlassian.net/<br/>
Wiki: https://wcm-io.atlassian.net/wiki/<br/>
Continuous Integration: https://travis-ci.org/wcm-io-caravan/caravan-pipeline/


## Build from sources

If you want to build wcm.io from sources make sure you have configured all [Maven Repositories](https://caravan.wcm.io/maven.html) in your settings.xml.

See [Travis Maven settings.xml](https://github.com/wcm-io-caravan/caravan-pipeline/blob/master/.travis.maven-settings.xml) for an example with a full configuration.

Then you can build using

```
mvn clean install
```
