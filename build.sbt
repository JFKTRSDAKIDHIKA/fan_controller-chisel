ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "my-chisel-project",
    version := "0.1",
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % "6.5.0", // chisel 不使用 cross
      ("org.chipsalliance" %% "chisel-plugin" % "6.5.0").cross(CrossVersion.full), // chisel-plugin 使用 cross
      "org.scalatest" %% "scalatest" % "3.2.17" % Test // 添加 ScalaTest 依赖
    ),
    // 添加插件配置并进行 cross
    addCompilerPlugin(("org.chipsalliance" %% "chisel-plugin" % "6.5.0").cross(CrossVersion.full))
  )

resolvers ++= Seq(
  "Maven Central" at "https://repo1.maven.org/maven2",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
