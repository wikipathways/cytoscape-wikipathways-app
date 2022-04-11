# pathvisio core
mvn install:install-file -Dfile=localRepo/org.pathvisio.core.jar -DgroupId=org.pathvisio -DartifactId=pathvisio-core -Dversion=3.3.0 -Dpackaging=jar

# bridgedb 
mvn install:install-file -Dfile=localRepo/org.bridgedb.jar -DgroupId=org.bridgedb -DartifactId=bridgedb -Dversion=3.0.13 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/org.bridgedb.bio.jar -DgroupId=org.bridgedb -DartifactId=bridgedb-bio -Dversion=3.0.13 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/derby.jar -DgroupId=derby -DartifactId=derby -Dversion=10.4 -Dpackaging=jar

# xml parsing
mvn install:install-file -Dfile=localRepo/jdom-1.1.3.jar -DgroupId=org.jdom -DartifactId=jdom -Dversion=1.1.3 -Dpackaging=jar
