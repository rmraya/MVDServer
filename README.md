# MVDServer

A simple web server that supports HTTP and HTTPS.

```text
server.sh [-help] [-version] [-config config.json]
Where:

   -help:      (optional) Display this help information and exit
   -version:   (optional) Display version & build information and exit
   -config:    (optional) Load configuration from JSON file (default: config.json)
```

Edit the provided `config.json` (see below) and personalize your settings or create a new JSON file withe the ame fields.

```JSON
{
    "hostName": "mysite.com",
    "httpPort": 8080,
    "httpsPort": 8443,
    "keystore": "",
    "password": "",
    "webDir": "www",
    "stopWord": "halt"
}
```

## Requirements

- JDK 11 or newer is required for compiling and building. Pre-built binaries already include everything you need to run all options.
- Apache Ant 1.10.6 or newer

## Building

- Checkout this repository.
- Point your JAVA_HOME variable to JDK 11
- Run `ant` to generate a binary distribution in `./dist`
