<img src="https://wcm.io/images/favicon-16@2x.png"/> wcm.io Caravan Pipeline
======
[![Build](https://github.com/wcm-io-caravan/caravan-pipeline/workflows/Build/badge.svg?branch=develop)](https://github.com/wcm-io-caravan/caravan-pipeline/actions?query=workflow%3ABuild+branch%3Adevelop)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=wcm-io-caravan_caravan-pipeline&metric=coverage)](https://sonarcloud.io/summary/new_code?id=wcm-io-caravan_caravan-pipeline)

wcm.io Caravan - JSON Data Pipelining Infrastructure

![Caravan](https://github.com/wcm-io-caravan/caravan-tooling/blob/master/public_site/src/site/resources/images/caravan.gif)

JSON Data Pipelining and Caching.

Documentation: https://caravan.wcm.io/pipeline/<br/>
Issues: https://wcm-io.atlassian.net/<br/>
Wiki: https://wcm-io.atlassian.net/wiki/<br/>
Continuous Integration: https://github.com/wcm-io-caravan/caravan-pipeline/actions<br/>
Commercial support: https://wcm.io/commercial-support.html


## Build from sources

If you want to build wcm.io from sources make sure you have configured all [Maven Repositories](https://caravan.wcm.io/maven.html) in your settings.xml.

See [Maven Settings](https://github.com/wcm-io-caravan/caravan-pipeline/blob/develop/.maven-settings.xml) for an example with a full configuration.

Then you can build using

```
mvn clean install
```
