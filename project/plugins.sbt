addSbtPlugin("com.eed3si9n"          % "sbt-assembly"           % "0.14.6")
addSbtPlugin("com.typesafe.sbt"      % "sbt-native-packager"    % "1.3.5")
addSbtPlugin("com.pigumer.sbt.cloud" % "sbt-aws-cloudformation" % "5.0.23")

libraryDependencies ++= Seq(
  "javax.xml.bind"   % "jaxb-api"  % "2.3.0",
  "com.sun.xml.bind" % "jaxb-impl" % "2.3.0.1"
)