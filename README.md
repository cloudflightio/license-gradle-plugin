# License Gradle Plugin

[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

This plugin is another approach to track your dependencies including their licenses.

We provide support for:

* Maven modules
* NPM modules (by using the [Gradle Node Plugin](https://github.com/node-gradle/gradle-node-plugin))
* SPDX Licenses, attempting to merge similar license names (Apache 2 vs. Apache 2-0)
* Generating NOTICE.html files to be put into your final artifact
* Handling of ambiguous licenses (multi-licenses)
* Manual override of licenses by means of a JSON file
* Support for BOM files and dependency constraints

## Installation

TBD

## Usage 

TBD


## Acknowledgements 

This plugin was inspired by a couple of other plugins, trying to mix together
the best of all of them while still keeping it simple:

* https://github.com/jaredsburrows/gradle-license-plugin
* https://github.com/jk1/Gradle-License-Report