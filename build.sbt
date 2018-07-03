import Dependencies._
import sbt.Keys._
import jp.pigumer.sbt.cloud.aws.cloudformation._

val BucketName = sys.env.get("BUCKET_NAME")
val listExports = taskKey[Unit]("")
val showARN = taskKey[Unit]("")

lazy val root = (project in file("."))
  .enablePlugins(CloudformationPlugin)
  .settings(
    organization := "com.pigumer",
    name := "sbt-aws-cloudformation-example",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      mp3spi,
      cast,
      alpakka,
      akkaActor,
      akkaStream,
      awsS3,
      awsPolly,
      akkaStreamTestKit,
      scalaTest
    ),
    mainClass in assembly := Some("jp.pigumer.cast.Cast")
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
        stackName = "sqs",
        template = "sqs.yaml"
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