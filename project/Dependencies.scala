import sbt.*

object Dependencies {
  private val mockitoScalaVersion = "1.17.14"

  lazy val authUtils = "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.141"
  lazy val awsDynamoDb = "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.12.472"
  lazy val awsDynamoDbClient = "uk.gov.nationalarchives" %% "da-dynamodb-client" % "0.1.12"
  lazy val awsJavaEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.11.2"
  lazy val awsLambda = "com.amazonaws" % "aws-java-sdk-lambda" % "1.12.472"
  lazy val awsLambdaCore = "com.amazonaws" % "aws-lambda-java-core" % "1.2.2"
  lazy val awsSns = "com.amazonaws" % "aws-java-sdk-sns" % "1.12.472"
  lazy val awsSnsClient = "uk.gov.nationalarchives" %% "da-sns-client" % "0.1.13"
  lazy val awsSsm = "com.amazonaws" % "aws-java-sdk-secretsmanager" % "1.12.472"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.1"
  lazy val mockitoScala = "org.mockito" %% "mockito-scala" % mockitoScalaVersion
  lazy val mockitoScalaTest = "org.mockito" %% "mockito-scala-scalatest" % mockitoScalaVersion
  lazy val preservicaClient = "uk.gov.nationalarchives" %% "preservica-client-fs2" % "0.0.10"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.16"
  lazy val sttpClient = "com.softwaremill.sttp.client3" %% "core" % "3.8.13"
  lazy val typeSafeConfig = "com.typesafe" % "config" % "1.4.2"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0"
}