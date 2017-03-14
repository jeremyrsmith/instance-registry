name := "instance-registry"

organization in ThisBuild := "io.github.jeremyrsmith"
version in ThisBuild      := "0.1.0-SNAPSHOT"
scalaVersion in ThisBuild := "2.11.8"

val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
  ),

  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:experimental.macros",
    //"-Xfatal-warnings",
    "-Xlint:-adapted-args,_",
    "-Ywarn-numeric-widen",
    "-Xfuture"
  ),

  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary)
)

val macroSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

val testProjectSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
  )
)

val `instance-registry` = project.settings(
  commonSettings,
  macroSettings
)

val `instance-registry-export` = project.settings(
  commonSettings,
  macroSettings
) dependsOn `instance-registry`

val `instance-registry-export-test` = project.settings(
  commonSettings,
  macroSettings,
  scalacOptions in Compile += "-Xplugin:" + (packageBin in Compile in `instance-registry-export`).value
) dependsOn `instance-registry-export`

val `instance-registry-usage-test` = project.settings(
  commonSettings,
  dependencyClasspath in Test := ((dependencyClasspath in Test).value :+
    Attributed((packageBin in Compile in `instance-registry-export-test`).value)(AttributeMap.empty))
) dependsOn `instance-registry`

val `instance-registry-root` = project in file (".") aggregate (
  `instance-registry`,
  `instance-registry-export`,
  `instance-registry-export-test`,
  `instance-registry-usage-test`
)