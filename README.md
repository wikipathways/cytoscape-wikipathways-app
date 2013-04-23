wikipathways-app
================

WikiPathways app for Cytoscape to open and access pathways from WikiPathways

1) git clone https://github.com/wikipathways/cytoscape-wikipathways-app.git

2) go in new repository directory and run localsetup.sh

3) go into wikipathways.app directory and run mvn install

4) go to Eclipse -> make sure Maven plugin is installed -> Import -> Maven -> Existing Maven Project -> select repository directory


run plugin
================

Copy the jar file in wikipathways.app/target/wikipathways.app-0.0.1-SNAPSHOT.jar into 
${home}/CytoscapeConfiguration/3/apps/installed/ 

If Cytoscape 3 is started the plugin should show up or be updated.


add other dependency
================

1) add jar in localRepo

2) add one line in localsetup.sh (compare to org.pathvisio.core.jar as an example) and run localsetup.sh

3) go to wikipathways.app/pom.xml

4) add dependency under dependencies (again use org.pathvisio:pathvisio-core) as an example

5) add artifact in plugin: maven-shade-plugin - in configuration

6) run mvn install and see if the jar gets included
troubleshooting
================

git push -> if this results in the following error:
error: The requested URL returned error: 403 while accessing https://github.com/wikipathways/cytoscape-wikipathways-app.git/info/refs
fatal: HTTP request failed

Then edit the following file in the repository:
.git/config 

and change the url to
url = ssh://git@github.com/wikipathways/cytoscape-wikipathways-app.git