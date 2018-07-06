sbt-aws-cloudformation-example
==============================

# Install Docker for Mac

* [Install Docker for Mac](https://docs.docker.com/docker-for-mac/install/)

# Create AWS Resources

```
$ BUCKET_NAME=<S3 Bucket Name> sbt
sbt> awscfCreateBucket create-bucket-stackname
sbt> awscfUploadTemplates
sbt> awscfCreateStack vpc
sbt> awscfCreateStack ecscluster
sbt> awscfCreateStack ecr
sbt> awscfCreateStack sqs
sbt> awscfCreateStack task
sbt> awscfCreateStack lambda
sbt> assembly
sbt> exampleTask/docker:stage
sbt> exampleTask/docker:publishLocal
sbt> awsecr::awsecrLogin
sbt> awsecrPush
sbt> exit
```

# Run task

* Test lambda function: example-lambda

# Google Home

* [chromecast-java-api-v2](https://github.com/vitalidze/chromecast-java-api-v2)

```
$ cd cast/target/scala-2.12
$ BUCKET_NAME=<YOUR BUCKET NAME> java -jar castExample-assembly-0.0.1-SNAPSHOT.jar
```

Send any message to SampleQueue

# Java Sound API

```
$ cd java-sound/target/scala-2.12
$ BUCKET_NAME=<YOUR BUCKET NAME> java -jar javaSoundExample-assembly-0.0.1-SNAPSHOT.jar
```

Send any message to SampleQueue
