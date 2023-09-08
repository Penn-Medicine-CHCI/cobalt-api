# Cobalt API

## Running Locally

### Prerequisites

* [Java 17+](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
* [Maven 3.6+](http://maven.apache.org/download.cgi)
* [Docker 3.8+](https://www.docker.com/products/docker-desktop)

### Getting Started: Start + Configure Local Environment

1. Start [Localstack](https://github.com/localstack/localstack) and [Postgres](https://www.postgresql.org)

This runs a local simulator for AWS services and starts up a local Postgres instance via Docker Compose.

```
cobalt-api$ ./start-localstack
```

...you can later stop it like this:

```
cobalt-api$ ./stop-localstack
```

2. Bootstrap Localstack And Postgres

This script creates and runs transient Docker containers that stand up Localstack resources and create + seed our Postgres DB.

To get the full seed dataset, you'll want to download the latest `bootstrap.sql` file from [Google Drive](https://drive.google.com/file/d/1Rl5CCvD86SQ6zmojjhJohyswhRmUNTap/view?usp=drive_link) and place it in `sql/initial/bootstrap.sql`.  This is helpful but not strictly required to run the backend.

```
cobalt-api$ ./bootstrap
```

3. Start the server

```
cobalt-api$ ./start-backend
```

Press any key to stop the server.

You can specify an alternate port and environment via environment variables, like this:

```
cobalt-api$ COBALT_API_ENV=local COBALT_API_PORT=4000 ./start-backend
```

## Unit and Integration Tests

Run unit tests:

```
cobalt-api$ mvn test -Dgroups="com.cobaltplatform.api.UnitTest"
```

Run integration tests (currently requires Localstack/Postgres/Redis to be running):

```
cobalt-api$ mvn test -Dgroups="com.cobaltplatform.api.IntegrationTest"
```

## Development

If you're using [IntelliJ IDEA](https://www.jetbrains.com/idea/), please import the [Formatting Scheme File](misc/intellij-codestyle.xml) in `Editor → Code Style → Java` to keep your code formatted consistently so diffs are easy to read.

### SAML Key Generation

We need our own keypair for signing and verifying JWTs, for our role as a SAML Service Provider (SP), and so on.

Here is how to generate your own keypair for testing, if needed (one has already been provided for you and checked into source control so you normally will not have to do this).

The private key generated is `cobalt.test.pem` and the public key is `cobalt.test.crt`.  

```shell
$ openssl req -new -x509 -days 3650 -nodes -sha256 -out cobalt.test.crt -keyout cobalt.test.pem
```

Your answers can be whatever you'd like so long as you provide _something_. Here is an example:

```text
Country Name (2 letter code) []:US
State or Province Name (full name) []:PA
Locality Name (eg, city) []:Conshohocken
Organization Name (eg, company) []:Transmogrify     
Organizational Unit Name (eg, section) []:
Common Name (eg, fully qualified host name) []:com.cobaltplatform
Email Address []:maa@xmog.com
```

### EPIC Key Generation

```shell
$ openssl genrsa -out cobalt.epic.nonprod.orig.pem 2048
$ openssl req -days 3650 -new -x509 -key cobalt.epic.nonprod.orig.pem -out cobalt.epic.nonprod.crt -subj '/CN=cobalt'
$ openssl pkcs8 -topk8 -inform PEM -in cobalt.epic.nonprod.orig.pem -out cobalt.epic.nonprod.pem -nocrypt
```