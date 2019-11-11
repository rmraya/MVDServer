# MVDServer

A simple web server that supports HTTP and HTTPS.

```bash
server.sh [-help] [-version] -config config.json
Where:

   -help:      (optional) Display this help information and exit
   -version:   (optional) Display version & build information and exit
   -config:    Load configuration from JSON file
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
