import Dependencies._
import sbt.Keys._
import jp.pigumer.sbt.cloud.aws.cloudformation._

val BucketName = sys.env.get("BUCKET_NAME")

val listExports = taskKey[Unit]("")
val showARN = taskKey[Unit]("")

lazy val commonSettings = Seq(
  organization := "com.pigumer",
  scalaVersion := "2.12.6",
  version := "0.0.1-SNAPSHOT"
)

lazy val root = (project in file("."))
  .aggregate(pollyTask
    , castExample
    , javaSoundExample)
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
      )
    ),
    listExports := {
      awscfListExports.value.foreach(e ⇒ println(s"${e.name}, ${e.value}"))
    },
    showARN := {
      val arn = awscfGetValue.toTask(" SampleQueueARN").value
      println(arn)
    }
  )

lazy val pollyTask = (project in file("polly-task"))
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
