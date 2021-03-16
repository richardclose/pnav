name := "pnav"

organization := "org.phasanix"

version := "1.0"

scalaVersion := "2.13.5"

libraryDependencies ++= Seq (
  "com.lihaoyi" %% "utest" % "0.7.7",
  "com.lihaoyi" %% "scalatags" % "0.9.3"
)

testFrameworks += new TestFramework("utest.runner.Framework")
