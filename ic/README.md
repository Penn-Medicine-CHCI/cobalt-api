- [IC COBALT Backend](#ic-cobalt-backend)
  - [Prerequisites](#prerequisites)
  - [Spinning it up ](#spinning-it-up)
    - [Database](#database)

# IC COBALT Backend

## Prerequisites

Before getting started, make sure you have the following installed:

1. JDK 11 [(I recommend using SDKman)](https://sdkman.io/install). Make sure it’s in your PATH. 
2. We tend to use Amazon’s Correto `sdk install java x.y.z-amzn`
3. Maven--again, [SDKman](https://sdkman.io/install). 
4. Docker `brew install --cask docker`
5. If you don’t want to run Postgres in a container during development, [Postgres.app](https://postgresapp.com/) `brew install --cask postgres `

## Spinning it up 
Running COBALT with all IC functionality enabled locally is a matter of

1. Starting the normal COBALT backend. Refer to [the COBALT README](../README.md)
2. Create database schema + tables for IC: `./bootstrap`
3. Starting the IC backend by running `mvn package exec:java`. Note that **package** is important - ebean will not generate the needed sources in a different phase.
4. Starting the front end with `npm run dev` (more details over in that repo) and navigating to http://127.0.0.1:3000/patient-sign-in

### Database
Schema is `ic` and currently lives in the `cobalt` DB.  Role is `ic`.

The script to initalize the DB user is in [the ic/db directory](./db).


For database changes, we want to use the ebean migration support.

- Update the model files
- In `src/test/java/main/GenerateDbMigration.java`, update the message and version, then re-run the `main()` method. **Make sure your working directory is `ic` otherwise the migrations will not be generated correctly**.
- A new migration should be generated, which should be run when launching the application.

To run migrations

- run `mvn package exec:java@migrate -Dmaven.test.skip=true`
  
Connect to DB:

```
PGPASSWORD=password psql -U ic -h localhost -p 5501 cobalt
```

## Connecting to webapp

Navigate to http://127.0.0.1:3000/patient-sign-in

## JWT Signing

You can generate `publicKey` and `secretKey` values for the following config using `KeyManagerTest.keyPairStringMarshaling()`:

```
jwt {
  jcaName = "SHA512withRSA"
  publicKey = "MIICI..."
  secretKey = "MIIJR..."
}
```

This is useful for creating new environments, where we need new keypairs.