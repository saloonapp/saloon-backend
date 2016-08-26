# SalooN

An app for tracking & organizing events !

## Requirements

This project require Java 8, Scala 2.11.5, sbt & mongo.

## Configuration

TODO

## Running the Application

### Running tests

TODO

### Running localy

- start mongo
- $ sbt run

### Deploying in prod

- Setup a heroku instance
- add addon for MongoDB (mLab)
- set config vars
- Optional :
    - add addon for scheduler (Temporize)
    - schedule events according to `conferences.controllers.Batch.scheduler`

## Continuous integration

TODO

## Credits

Inspiration taken from https://github.com/MfgLabs/PSUG
