logLevel := Level.Info

resolvers ++= Seq(
    Resolver.file("local-ivy-repo", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns),
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Delving Repository" at "http://artifactory.delving.org/artifactory/delving",
    "pentaho"         at "http://repository.pentaho.org/artifactory/repo",
    Resolver.url("sbt-buildinfo-resolver-0", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
)

//addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

addSbtPlugin("io.bernhardt" %% "groovy-templates-sbt-plugin" % "1.6.4-SNAPSHOT")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.6")
