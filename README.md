# License Gradle Plugin

[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](https://opensource.org/licenses/Apache-2.0)

This plugin is another approach to track your dependencies including their licenses.

We provide support for:

* Maven modules
* NPM modules (by using the [Gradle Node Plugin](https://github.com/node-gradle/gradle-node-plugin))
* SPDX Licenses, attempting to merge similar license names (Apache 2 vs. Apache 2-0)
* Generating NOTICE.html files to be put into your final artifact
* Handling of ambiguous licenses (multi-licenses) as described in [this](https://resources.whitesourcesoftware.com/blog-whitesource/license-compatibility) and [this](https://dwheeler.com/essays/floss-license-slide.pdf) link
* Manual override of licenses by means of a JSON file
* Support for BOM files and dependency constraints

## Installation

TBD after publishing at the Gradle Plugin Portal

## Usage 

This plugin provides two different tasks:

### License Report

Call `gradlew clfLicenseReport` in order to get:

* a `NOTICE.html` in the `META-INF` directory of your JAR containg all dependencies and licenses of your runtime classpath
* a machine-readable `license-report.json` with the same data in your `build/licenses` directory

Thie file is a JSON with entries of the following format:

````json
{
    "project": "IntelliJ IDEA Annotations",
    "description": "A set of annotations used for code inspection support and code documentation.",
    "version": "13.0",
    "developers": [
        "JetBrains Team"
    ],
    "url": "http://www.jetbrains.org",
    "year": "",
    "licenses": [
        {
            "license": "Apache License 2.0",
            "license_id": "Apache-2.0",
            "license_url": "https://www.apache.org/licenses/LICENSE-2.0"
        }
    ],
    "dependency": "org.jetbrains:annotations:13.0"
}
````


### Tracker Report

On top of the License Report you can also create a dependency tracker report by calling `gradlew clfCreateTrackerReport`.

This JSON file will be generated to `build/tracker/dependencies.json` and gives a list of dependencies for each of the following
configurations:

* compile (`api`, `implementation`)
* runtime (`runtimeOnly`)
* test (`testImplementation`, `testRuntimeOnly`)
* development (`provided`)

Each dependency record also contains the dependency trail, i.e. it reports for transitive
dependencies why they have been pulled:

````json
{
    "artifact": "org.jetbrains:annotations:13.0",
    "classifier": null,
    "type": "jar",
    "trail": [
        "io.cloudflight.json:json-wrapper:0.2.0",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.10",
        "org.jetbrains.kotlin:kotlin-stdlib:1.6.10"
    ]
}
````

## Support for NPM modules

As mentioned above, this plugin can not only handle Maven modules, but
also NPM modules (`package-lock.json`) if you are using the [Gradle Node Plugin](https://github.com/node-gradle/gradle-node-plugin).

Suppose you have a `package.json` like that:

````json
{
  "name": "sample-ui",
  "description": "Receive my events",
  "version": "0.0.1-3",
  "private": true,
  "dependencies": {
    "express": "3.0",
    "redis": "0.7.2",
    "jade": "0.27.2"
  },
  "subdomain": "sg-event-receive",
  "scripts": {
    "start": "app.js"
  },
  "engines": {
    "node": "0.6.x"
  },
  "bundledDependencies": [
    "redis"
  ]
}
````

And then apply both this plugin and the [Gradle Node Plugin](https://github.com/node-gradle/gradle-node-plugin), then you will 
receive a `license-report.json` with entries like the following

````json
{
    "project": "express",
    "description": "Sinatra inspired web development framework",
    "version": "3.0.6",
    "developers": [
        "TJ Holowaychuk <tj@vision-media.ca>",
        "TJ Holowaychuk",
        "Aaron Heckmann",
        "Ciaran Jessup",
        "Guillermo Rauch"
    ],
    "url": "",
    "year": null,
    "licenses": [
    ],
    "dependency": "@npm:express:3.0.6"
}
````

This way, you can create one common dependency tracker info including all licenses for a combined backend+frontend application
(i.e. Angular + Spring Boot).


## Acknowledgements 

This plugin was inspired by a couple of other plugins, trying to mix together
the best of all of them while still keeping it simple:

* https://github.com/jaredsburrows/gradle-license-plugin
* https://github.com/jk1/Gradle-License-Report