import Dependencies._
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"
ThisBuild / organization := "uk.gov.nationalarchives"
ThisBuild / organizationName := "The National Archives"

lazy val root = (project in file("."))
  .settings(
    name := "dr2-entity-event-generator",
    libraryDependencies ++= Seq(
      awsDynamoDb,
      awsDynamoDbClient,
      awsJavaEvents,
      awsLambdaCore,
      awsLambda,
      awsSns,
      awsSnsClient,
      awsSsm,
      catsEffect,
      sttpClient,
      mockitoScala,
      preservicaClient,
      scalaTest % Test,
      typeSafeConfig,
      wiremock % Test
    )
  )
(assembly / assemblyJarName) := "entityEventGenerator.jar"

scalacOptions ++= Seq("-Wunused:imports", "-Werror")

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
