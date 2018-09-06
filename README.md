## rules

Simple Rule Engine from Ant's blog. Able to process MVEL and Javascript rules in JVM, or run on Node.js.

Version 2.2.2:
- Updated dependency versions and copyright notices

Version 2.2.1:
- Fixed https://github.com/maxant/rules/issues/4 - added checks to log level in order to improve performance.
- Updated dependencies to latest versions

Version 2.2.0:
- See [http://blog.maxant.co.uk/pebble/2015/08/22/1440237900000.html](http://blog.maxant.co.uk/pebble/2015/08/22/1440237900000.html) => Support for Javascript rules in JVM via Nashorn.

Version 2.1.0:
- See [http://blog.maxant.co.uk/pebble/2011/11/12/1321129560000.html](http://blog.maxant.co.uk/pebble/2011/11/12/1321129560000.html) and 
 [http://blog.maxant.co.uk/pebble/2014/10/03/1412371560000.html](http://blog.maxant.co.uk/pebble/2014/10/03/1412371560000.html) and also 
[http://blog.maxant.co.uk/pebble/2014/11/15/1416087180000.html](http://blog.maxant.co.uk/pebble/2014/11/15/1416087180000.html) for Node.js.

Also see the test classes, e.g. [https://github.com/maxant/rules/tree/master/rules/src/test/java/ch/maxant/rules/blackbox](https://github.com/maxant/rules/tree/master/rules/src/test/java/ch/maxant/rules/blackbox), [https://github.com/maxant/rules/tree/master/rules-java8/src/test/java/ch/maxant/rules/blackbox](https://github.com/maxant/rules/tree/master/rules-java8/src/test/java/ch/maxant/rules/blackbox) and [https://github.com/maxant/rules/tree/master/rules-scala/src/test/scala/ch/maxant/rules/blackbox](https://github.com/maxant/rules/tree/master/rules-scala/src/test/scala/ch/maxant/rules/blackbox), or [https://github.com/maxant/rules/tree/master/rules-js/test/rules-test.js](https://github.com/maxant/rules/tree/master/rules-js/test/rules-test.js).

Release Notes and Licence (LGPL) can be found here: [https://github.com/maxant/rules/tree/master/rules/src/main/resources](https://github.com/maxant/rules/tree/master/rules/src/main/resources).

## Building

    #update versions in poms, to snapshot version, e.g. "2.3.1-SNAPSHOT" => keep SNAPSHOT, nexus will do the release automatically if "autoReleaseAfterClose" is set to true in the parent pom.

    #update dependency versions in poms

    #update copyright years in all files

    cd rules-parent

    #ensure all is working
    mvn clean package

    #make changes

    #ensure all is working
    mvn clean package

    #update release notes in "releaseNotes.txt" as well as in "README.md".

    #build
    mvn clean deploy

    # that might require you to create a gpg key
    #   gpg --gen-key
    #   gpg --list-secret-keys
    #   gpg --keyserver pgp.mit.edu --send-keys ABCDEF
    # the above takes the letters which follow the "sec" key
    # it is OK to create a brand new gpg key e.g. if your laptop explodes.
    # see http://central.sonatype.org/pages/ossrh-guide.html
    # need to ensure that servers section of maven settings.xml contains the following:
    #    <server>
    #      <id>sonatype</id>
    #      <username>YOUR_USERNAME</username>
    #      <password>YOUR_PASSWORD</password>
    #    </server>
    # note that the ID is the same as the one contained in the distributionManagement section of the parent pom
    # after deployment, check the maven logs. you'll find something like this:
    #    Uploading to sonatype: https://oss.sonatype.org/content/repositories/snapshots/ch/maxant/rules-java8/2.2.2-SNAPSHOT/maven-metadata.xml
    # Go to https://oss.sonatype.org/content/repositories/snapshots/ch/maxant/ and double check it's all there
    # Finally go have a coffee and after a while, your release will be ready here: https://oss.sonatype.org/#nexus-search;quick~maxant
