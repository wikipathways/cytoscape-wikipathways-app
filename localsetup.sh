# pathvisio core
mvn install:install-file -Dfile=localRepo/org.pathvisio.core.jar -DgroupId=org.pathvisio -DartifactId=pathvisio-core -Dversion=3.1.1 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/org.wikipathways.client.jar -DgroupId=org.wikipathways -DartifactId=client -Dversion=3.1.1 -Dpackaging=jar

# bridgedb 
mvn install:install-file -Dfile=localRepo/org.bridgedb.jar -DgroupId=org.bridgedb -DartifactId=bridgedb -Dversion=1.2.2 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/org.bridgedb.bio.jar -DgroupId=org.bridgedb -DartifactId=bridgedb-bio -Dversion=1.2.2 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/derby.jar -DgroupId=derby -DartifactId=derby -Dversion=10.4 -Dpackaging=jar

# xml parsing
mvn install:install-file -Dfile=localRepo/com.springsource.org.jdom-1.1.0.jar -DgroupId=com.springsource -DartifactId=org.jdom -Dversion=1.1.0 -Dpackaging=jar

# webservice
mvn install:install-file -Dfile=localRepo/axis.jar -DgroupId=axis -DartifactId=axis-core -Dversion=1.4 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/axis.jar -DgroupId=axis -DartifactId=axis-ant -Dversion=1.4 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/commons-discovery-0.2.jar -DgroupId=org.apache.commons -DartifactId=discovery -Dversion=0.2 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/commons-logging-1.0.4.jar -DgroupId=org.apache.commons -DartifactId=logging -Dversion=1.0.4 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/jaxrpc.jar -DgroupId=jaxrpc -DartifactId=jaxrpc -Dversion=1.1 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/saaj.jar -DgroupId=javax.xml.soap -DartifactId=saaj -Dversion=1.1 -Dpackaging=jar
mvn install:install-file -Dfile=localRepo/wsdl4j-1.5.1.jar -DgroupId=wsdl4j -DartifactId=wsdl4j -Dversion=1.5.1 -Dpackaging=jar

