properties-webapp
============================

properties-webapp is a webapp that stores and serves properties.


The REST API:
| ENDPOINT |DESCRIPTION  |
|--|--|
| GET /properties/properties | get all of the properties as a JSON map |
| PUT /properties/properties | save all of the properties JSON map provided in the body|
| GET /properties/properties/keys | get all of the keys in from the properties |
| GET /properties/property/{key} | get the value of the key |
| PUT /properties/property/{key} | add the value (as the body) for the provided key |
| DELETE /properties/property/{key} | delete the value for the provided key |


# Install

```
docker pull vincentrussell/properties-webapp
```

# Development
## Install software
### homebrew (mac)
* brew install docker-machine
* brew install docker
* docker-machine create --driver virtualbox properties-webapp
* docker-machine env properties-webapp
* eval "$(docker-machine env properties-webapp)"


## build the docker container
Build and Run the docker container ...

```
docker build -t vincentrussell/properties-webapp .
docker run -p 8080:8000 -p 8443:8443 -it --rm vincentrussell/properties-webapp
```

# Use

```
docker run -d -p 80:8080 -p 443:8443 vincentrussell/properties-webapp
```

# Environment Variable Options

|Environment variable  | Description |
|--|--|
| server.port | The http (unsecured) port to use  |
| server.https.port | The https (secured) port to use  |
| https.authorized.dns | The Authorized Dns delimited by two commas (,,) that should acces  |
| javax.net.ssl.keyStore | The keystore to use  |
| javax.net.ssl.keyStorePassword | The keystore password to use  |
| javax.net.ssl.keyStoreType | The type of keystore (JKS or PKCS12)  |
| javax.net.ssl.trustStore | The truststore to use |
| javax.net.ssl.trustStorePassword| The truststore password to use  |
| javax.net.ssl.trustStoreType | The type of truststore (JKS or PKCS12)  |
| javax.net.ssl.sslProtocol | The ssl protocol to use (TLSv1.2)  |
| javax.net.ssl.keyAlias | The alias in the keystore to use  |
| javax.net.ssl.clientAuth | The tomcat client auth to use.  (optional, required, false, etc.)  |
| properties.file | The persistent properties file to store the properties. (saved to every 45 seconds)  |



## ChangeLog

Version: 1.0.0, 2020-11-29

Initial Release