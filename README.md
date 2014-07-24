query.api.freifunk.net
======================

Freifunk API Query Client

# Build 

1. Build DeepaMehta

```bash
git clone git://github.com/jri/deepamehta.git
cd deepamehta
mvn install -P all
``` 

2. Setup for Hot-Deployment 

See here for complete Instructions: https://trac.deepamehta.de/wiki/PluginDevelopmentGuide

In DeepaMehta's pom.xml: add the plugin's target directory (here: `/home/mt/query/query.api.freifunk.net/dm4-ffapi-import/target`) to the felix.fileinstall.dir property's CDATA section. Important: don't forget to append a comma to the previous line:

```xml
<project>
    ...
    <felix.fileinstall.dir>
        <![CDATA[
            ${project.basedir}/modules/dm4-core/target,
            ${project.basedir}/modules/dm4-webservice/target,
            ${project.basedir}/modules/dm4-webclient/target,
            ...
            ${project.basedir}/modules/dm4-storage-neo4j/target,
            /home/mt/query/query.api.freifunk.net/dm4-ffapi-import/target
        ]]>
    </felix.fileinstall.dir>
    ...
</project>
```

Now start DeepaMehta. In the directory deepamehta (where you've build):

```bash
mvn pax:run 
```

In the Plugin directory (here: `/home/mt/query/query.api.freifu    nk.net/dm4-ffapi-import/target`run: 

```bash
mvn clean package
``` 

The package should now appear in DeepaMehta

```bash
[~/query/query.api.freifunk.net/dm4-ffapi-import]$ mvn clean package                       *[master] 
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building Freifunk API Data 0.1-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- maven-clean-plugin:2.5:clean (default-clean) @ ffapi-import ---
[INFO] Deleting /home/mt/query/query.api.freifunk.net/dm4-ffapi-import/target
[INFO] 
[INFO] --- maven-resources-plugin:2.6:resources (default-resources) @ ffapi-import ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 2 resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:compile (default-compile) @ ffapi-import ---
[INFO] Changes detected - recompiling the module!
[INFO] Compiling 1 source file to /home/mt/query/query.api.freifunk.net/dm4-ffapi-import/target/classes
[INFO] 
[INFO] --- maven-resources-plugin:2.6:testResources (default-testResources) @ ffapi-import ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /home/mt/query/query.api.freifunk.net/dm4-ffapi-import/src/test/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.1:testCompile (default-testCompile) @ ffapi-import ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-surefire-plugin:2.17:test (default-test) @ ffapi-import ---
[INFO] No tests to run.
[INFO] 
[INFO] --- maven-bundle-plugin:2.3.7:bundle (default-bundle) @ ffapi-import ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 5.779s
[INFO] Finished at: Thu Jul 24 21:27:46 CEST 2014
[INFO] Final Memory: 19M/155M
[INFO] ------------------------------------------------------------------------
[~/query/query.api.freifunk.net/dm4-ffapi-import]$                                         *[master]
```

