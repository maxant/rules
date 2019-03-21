## rules

Simple Rule Engine from Ant's blog. Able to process MVEL and Javascript rules in JVM, or run on Node.js.

Version 2.4.1:
- Updated license to Apache 2.0

Version 2.3.1:
- Removed constructors and methods which took Java 8 streams because it's not a good idea to pass streams around. Added constructors for using additional variables and static methods. See https://github.com/maxant/rules/pull/9.

Version 2.2.2:
- Updated dependency versions and copyright notices.

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

Release Notes can be found here: [https://github.com/maxant/rules/tree/master/rules/src/main/resources](https://github.com/maxant/rules/tree/master/rules/src/main/resources).

## License

See LICENCE file.

    Copyright 2011-2019 Ant Kutschera

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

## Building

    # update versions in poms, to snapshot version, e.g. "2.4.1-SNAPSHOT"

    # update dependency versions in poms

    # update copyright years in:
    #  - rules/src/main/resources/license.txt
    #  - rules-java8/src/main/resources/license.txt
    #  - rules-parent/license.txt
    #  - rules-scala/src/main/resources/license.txt
    #  - LICENSE

    cd rules-parent

    # ensure all is working
    mvn clean package

    # make changes

    # ensure all is working
    mvn clean package

    # update release notes in "releaseNotes.txt" as well as in "README.md".

    # build
    mvn clean deploy

    # when pop up requests password for gpg key, use the correct password!

    # the above might require you to create a gpg key.
    # see https://central.sonatype.org/pages/working-with-pgp-signatures.html
    #   gpg --gen-key
    #   gpg --list-keys
    #
    # now upload it:
    #   gpg --keyserver keyserver.gpg.com  --send-keys ABCDEF
    #   gpg --keyserver pool.sks-keyservers.net --send-keys ABCDEF
    #   gpg --keyserver keys.gnupg.net --send-keys ABCDEF
    #   gpg --keyserver keyserver.ubuntu.com --send-keys ABCDEF
    #
    # or maybe with hkp protocol?
    #
    #   gpg --keyserver hkp://keyserver.gpg.com  --send-keys ABCDEF
    #   gpg --keyserver hkp://pool.sks-keyservers.net --send-keys ABCDEF
    #   gpg --keyserver hkp://keys.gnupg.net --send-keys ABCDEF
    #   gpg --keyserver hkp://keyserver.ubuntu.com --send-keys ABCDEF
    #
    # then go have a coffee. check
    # http://keyserver.ubuntu.com/pks/lookup?search=ant%40maxant.co.uk&hash=on&op=vindex
    # to see if the key is available.
    #
    # if it isn't try this:
    #
    #   gpg --armor --export ant@maxant.co.uk
    #
    # and import that manually at http://keyserver.ubuntu.com
    #
    # the above takes the letters (ID) which follow the "pub" key
    # it is OK to create a brand new gpg key e.g. if your laptop explodes.
    # see http://central.sonatype.org/pages/ossrh-guide.html
    # need to ensure that servers section of maven settings.xml contains the following:
    #    <server>
    #      <id>ossrh</id>
    #      <username>YOUR_USERNAME</username>
    #      <password>YOUR_PASSWORD</password>
    #    </server>
    # note that the ID is the same as the one contained in the distributionManagement section of the parent pom

    # after deployment, check the maven logs. you'll find something like this:
    #    Uploading to sonatype: https://oss.sonatype.org/content/repositories/snapshots/ch/maxant/rules-java8/2.3.1-SNAPSHOT/maven-metadata.xml

    # Go to https://oss.sonatype.org/content/repositories/snapshots/ch/maxant/ and double check it's all there

    # Now if that all works, do the release:
    # update versions in poms, to release version, e.g. "2.4.1"

    # build
    mvn clean deploy

    # then you should either get a failed report, or logging like this:

      [INFO]  * Upload of locally staged artifacts finished.
      [INFO]  * Closing staging repository with ID "chmaxant-1012".

      Waiting for operation to complete...
      ........

      [INFO] Remote staged 1 repositories, finished with success.
      [INFO] Remote staging repositories are being released...

      Waiting for operation to complete...
      .........

      [INFO] Remote staging repositories released.
      [INFO] ------------------------------------------------------------------------
      [INFO] Reactor Summary:
      [INFO]
      [INFO] rules .............................................. SUCCESS [ 12.109 s]
      [INFO] rules .............................................. SUCCESS [ 24.242 s]
      [INFO] rules-java8 ........................................ SUCCESS [  4.704 s]
      [INFO] rules-scala ........................................ SUCCESS [01:55 min]
      [INFO] ------------------------------------------------------------------------
      [INFO] BUILD SUCCESS
      [INFO] ------------------------------------------------------------------------

    # Your release will be ready here: https://oss.sonatype.org/#nexus-search;quick~maxant

    # Finally go have a coffee and after a while, your release will also be ready in maven central:
    #   https://mvnrepository.com/artifact/ch.maxant/rules

    # don't forget to create a release in GitHub:

    git commit -a -m'...'

    git push origin master

    git tag -a v2.3.1 -m'...'

    git push origin v2.3.1

    # go here and enter details, with title = v2.3.1
    https://github.com/maxant/rules/releases/new
