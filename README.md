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

In DeepaMehta's pom.xml: add the plugin's target directory (here: /home/mt/query/query.api.freifunk.net/dm4-ffapi-import) to the felix.fileinstall.dir property's CDATA section. Important: don't forget to append a comma to the previous line:

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
            /home/mt/query/query.api.freifunk.net/dm4-ffapi-import
        ]]>
    </felix.fileinstall.dir>
    ...
</project>
```

Now start DeepaMehta. In the directory deepamehta (where you've build):

```bash
mvn pax:run 
```

