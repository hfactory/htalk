HTalk
=====

Build
-----

Need sbt 0.13 or higher for the sbt eclipse command

To build the toplevel project and all its sub-projects:

    $ sbt compile


Test
----

    $ sbt test

You need to create a hbase-site-`username`.properties in `src/test/resources` pointing to your HBase instance
to test the application you can use the
[hfactory-server-in-docker-machine](https://github.com/hfactory/hfactory-server-in-docker-machine)
project to launch your local standalone HBase instance. Use the `hfactory-env.sh` startHBase command.
