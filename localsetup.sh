mvn install:install-file -Dfile=localRepo/org.pathvisio.core.jar -DgroupId=org.pathvisio -DartifactId=pathvisio-core -Dversion=3.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/com.springsource.org.jdom-1.1.0.jar -DgroupId=jdom -DartifactId=jdom -Dversion=1.1.0 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/org.bridgedb.jar -DgroupId=org.bridgedb -DartifactId=bridgedb -Dversion=1.1.0 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/org.bridgedb.bio.jar -DgroupId=org.bridgedb -DartifactId=bridgedb-bio -Dversion=1.1.0 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/derby.jar -DgroupId=derby -DartifactId=derby -Dversion=10.4 -Dpackaging=jar


