# MVDServer

A simple web server that supports HTTP and HTTPS.

If you use binaries from the command line, running `.\server.bat` or `./server.sh` without parameters displays help for using MVDServer.

```text
server.sh [-help] [-version] [-config config.json]
Where:

   -help:      (optional) Display this help information and exit
   -version:   (optional) Display version & build information and exit
   -config:    (optional) Load configuration from JSON file (default: config.json)
```

Edit the provided `config.json` (see below) and personalize your settings or create a new JSON file withe the same fields.

```JSON
{
    "hostName": "mysite.com",
    "ipAddress": "",
    "httpPort": 8080,
    "httpsPort": 8443,
    "keystore": "",
    "password": "",
    "webDir": "www",
    "stopWord": "halt"
}
```

Ready to use binaries and configuration information are available at [MVDServer home page](https://www.mvdsoftware.com/tools/mvdserver.html).

You can find instructions for generating TLS/SSL certificates for using HTTPS with MVDServer at [https://mvdsoftware.com/tools/certificates.html](https://mvdsoftware.com/tools/certificates.html)

## Requirements

- JDK 17 or newer is required for compiling and building. Pre-built binaries already include everything you need to run all options.
- Apache Ant 1.10.6 or newer

## Building

- Checkout this repository.
- Point your JAVA_HOME variable to JDK 17
- Run `ant` to generate a binary distribution in `./dist`
