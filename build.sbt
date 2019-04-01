enablePlugins(org.nlogo.build.NetLogoExtension)

netLogoExtName := "qual2rule"

netLogoClassManager := "io.payette.qual2rule.Qual2RuleExtension"

netLogoTarget := org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value / "qual2rule")

netLogoVersion := "6.1.0-RC1"

netLogoZipSources := false

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.8"

scalaSource in Compile := baseDirectory.value / "src"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "utf8")
