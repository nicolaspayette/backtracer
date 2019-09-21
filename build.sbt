enablePlugins(org.nlogo.build.NetLogoExtension)

netLogoExtName := "backtracer"

netLogoClassManager := "io.payette.backtracer.BacktracerExtension"

netLogoTarget := org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value / "backtracer")

netLogoVersion := "6.1.0"

netLogoZipSources := false

version := "0.0.0"

scalaVersion := "2.12.8"

scalaSource in Compile := baseDirectory.value / "src"

resourceDirectory in Compile := baseDirectory.value / "resources"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "utf8")

libraryDependencies ++= Seq(
  "com.linkedin.urls" % "url-detector" % "0.1.17",
  "org.jbibtex" % "jbibtex" % "1.0.17",
  "com.softwaremill.sttp" %% "core" % "1.6.6",
  "com.softwaremill.sttp" %% "async-http-client-backend-future" % "1.6.7"
)
