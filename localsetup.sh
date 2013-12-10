# pathvisio core
mvn install:install-file -Dfile=localRepo/org.pathvisio.core.jar -DgroupId=org.pathvisio -DartifactId=pathvisio-core -Dversion=3.1.1 -Dpackaging=jar

# bridgedb 
mvn install:install-file -Dfile=localRepo/org.bridgedb.jar -DgroupId=org.bridgedb -DartifactId=bridgedb -Dversion=1.2.2 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/org.bridgedb.bio.jar -DgroupId=org.bridgedb -DartifactId=bridgedb-bio -Dversion=1.2.2 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/derby.jar -DgroupId=derby -DartifactId=derby -Dversion=10.4 -Dpackaging=jar

# xml parsing
mvn install:install-file -Dfile=localRepo/com.springsource.org.jdom-1.1.0.jar -DgroupId=com.springsource -DartifactId=org.jdom -Dversion=1.1.0 -Dpackaging=jar


