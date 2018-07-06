import Dependencies._
import sbt.Keys._
import jp.pigumer.sbt.cloud.aws.cloudformation._
import jp.pigumer.sbt.cloud.aws.ecr.AwsecrCommands

val BucketName = sys.env.get("BUCKET_NAME")

val awsecrPush = taskKey[Unit]("Push")
val showExports = taskKey[Unit]("showExports")
val showARN = taskKey[Unit]("")

lazy val commonSettings = Seq(
  organization := "com.pigumer",
  scalaVersion := "2.12.6",
  version := "0.0.1-SNAPSHOT"
)

lazy val root = (project in file("."))
  .aggregate(exampleTask,
    castExample,
    javaSoundExample)
  .enablePlugins(CloudformationPlugin)
  .settings(commonSettings: _*)
  .settings(
    name := "sbt-aws-cloudformation-example",
    libraryDependencies ++= Seq(
      mp3spi,
      cast,
      alpakka,
      akkaStream,
      awsS3,
      akkaStreamTestKit,
      scalaTest
    )
  )
  .settings(
    awscfSettings := AwscfSettings(
      region = "ap-northeast-1",
      bucketName = BucketName,
      projectName = "sbt-aws-cloudformation-example",
      templates = Some(file("cloudformation"))
    ),
    awscfStacks := Stacks(
      Alias("sqs") → CloudformationStack(
        stackName = "example-sqs",
        template = "sqs.yaml"
      ),
      Alias("vpc") → CloudformationStack(
        stackName = "example-vpc",
        template = "vpc.yaml"
      ),
      Alias("ecr") → CloudformationStack(
        stackName = "example-ecr",
        template = "ecr.yaml"
      ),
      Alias("ecscluster") → CloudformationStack(
        stackName = "example-ecscluster",
        template = "ecscluster.yaml"
      ),
      Alias("task") → CloudformationStack(
        stackName = "example-task",
        template = "example-task.yaml",
        parameters = Map(
          "Image" → s"${(awsecrDomain in awsecr).value}/example-task:${version.value}",
          "BucketName" → BucketName.get
        ),
        capabilities = Seq("CAPABILITY_IAM")
      ),
      Alias("lambda") → CloudformationStack(
        stackName = "example-lambda",
        template = "example-lambda.yaml",
        capabilities = Seq("CAPABILITY_IAM")
      )
    ),
    showExports := {
      awscfListExports.value.foreach(e ⇒ println(s"${e.name}, ${e.value}"))
    },
    showARN := {
      val arn = awscfGetValue.toTask(" SampleQueueARN").value
      println(arn)
    },
    awsecrPush := {
      val docker = (awsecrDockerPath in awsecr).value
      val source = s"${(packageName in exampleTask).value}:${version.value}"
      val target = s"${(awsecrDomain in awsecr).value}/${awscfGetValue.toTask(" ECR").value}:${version.value}"
      AwsecrCommands.tag(docker, source, target)
      AwsecrCommands.push(docker, target)
    }
  )

lazy val exampleTask = (project in file("example-task"))
  .enablePlugins(JavaAppPackaging, AshScriptPlugin, DockerPlugin)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      akkaStream,
      awsPolly,
      awsS3,
      akkaStreamTestKit,
      scalaTest
    ),
    dockerBaseImage := "openjdk:8-jre-alpine",
    mainClass in assembly := Some("jp.pigumer.polly.Main")
  )

lazy val castExample = (project in file("cast"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      cast,
      alpakka,
      akkaStream,
      awsS3,
      akkaStreamTestKit,
      scalaTest
    ),
    mainClass in assembly := Some("jp.pigumer.cast.Cast")
  )

lazy val javaSoundExample = (project in file("java-sound"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      mp3spi,
      alpakka,
      akkaStream,
      awsS3,
      akkaStreamTestKit,
      scalaTest
    ),
    mainClass in assembly := Some("jp.pigumer.javasound.Main")
  )

