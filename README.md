# JSIT Library README

The Java Simulation Infrastructure Toolkit (JSIT) is a set of Java libraries
that provides certain software-engineering best-practice features for
simulations. The idea is that it is used to supplement a given simulation
toolkit (such as [AnyLogic](http://www.anylogic.com) or
[MASON](http://cs.gmu.edu/~eclab/projects/mason)) to provide features that they
tend to lack, and provide common design and coding for them (especially useful
for those developing simulations using a number of toolkits).

It exists as a core library supplemented by toolkit-specific helper libraries.
(It can be used without the helper library, but this requires the user to code
helper classes themselves.) Currently, a helper library exists only for
AnyLogic, though support for at least MASON and [Repast
Simphony](http://repast.sourceforge.net) is planned.

JSIT is developed by Stuart Rossiter (originally as part of research at the
University of Southampton, UK). Contact him at stuart.p.rossiter@gmail.com.

JSIT is open source software released under the LGPL license (see Licensing
below). JSIT source code is stored on GitHub at
[https://github.com/sprossiter/JSIT](https://github.com/sprossiter/JSIT).

For further information on using JSIT and how it works, see the [User
Guide](http://sprossiter.github.io/JSIT).

## Licensing

![LGPL Logo](src/main/resources/images/lgplv3-88x31.png)

JSIT is distributed under the GNU LGPL V3 license, which has a copying
permission statement as below. (See the full [LGPL license](lgpl.txt) and [GPL
license](gpl.txt) for more details.)

```
        Copyright Stuart Rossiter, University of Southampton 2018
        
        JSIT is free software: you can redistribute it and/or modify it under the terms
        of the GNU Lesser General Public License as published by the Free Software
        Foundation, either version 3 of the License, or (at your option) any later
        version.
        
        JSIT is distributed in the hope that it will be useful, but WITHOUT ANY
        WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
        PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
        
        You should have received a copy of the GNU Lesser General Public License along
        with JSIT. If not, see <http://www.gnu.org/licenses/>.
```

## Dependencies

JSIT requires Java 1.6+. It uses the following third-party libraries with
LGPL-compatible licenses (nested items show dependencies of the primary
dependencies):

  * [Logback](http://logback.qos.ch)
  
  * [SLF4J](http://www.slf4j.org)
  
  * [XStream](http://xstream.codehaus.org)
  
    - [XMLPull](http://www.xmlpull.org)
  
    - [Xpp3](http://www.extreme.indiana.edu/xgws/xsoap/xpp/mxp1)
  
  * [Apache Commons Codec](http://commons.apache.org/proper/commons-codec)
  
  * [Apache Commons IO](http://commons.apache.org/proper/commons-io)
  
  * [Apache Commons Configuration](http://commons.apache.org/proper/commons-configuration)
  
    - [Apache Commons Lang 2.x](http://commons.apache.org/proper/commons-lang)
  
    - [Apache Commons Logging](http://commons.apache.org/proper/commons-logging)
  
  * [Apache Subversion](https://subversion.apache.org/) JavaHL (Java binding for SVN)

The links above can be used to get source code for these dependencies.

Copies of all their licences and any required attribution (notice) files are
included in bundled JSIT distributions (in the `lib` folder together with the
dependencies themselves).

If you're *developing* JSIT, you can use the `getDependencies` target of the
JSIT [Ant](http://ant.apache.org) build file (`src/main/build/build.xml`) to
populate all the open source dependencies in the `lib` folder.

### AnyLogic

To **use** the JSIT AnyLogic helper library, you will need a valid copy of
AnyLogic version 7.1.1 or later. (AnyLogic code package names changed with this
version.) When running AnyLogic models using JSIT, the required runtime
libraries are automatically available.

To **develop** (compile) the JSIT AnyLogic helper library, you will need the
following AnyLogic-related libraries added to the `lib` folder:

  * AnyLogic Engine, which exists in an AnyLogic installation under the
    `plugins\com.anylogic.engine_<version stamp>` directory, or is produced by
    exporting an AnyLogic model to a Java applet. This library is covered by the
    [AnyLogic Engine Runtime License
    Agreement](src/main/resources/AnyLogicEngineRuntimeLicenseAgreement.html).

  * QueryDSL SQL library version as used by AnyLogic (needed only for Javadoc
    generation), which exists in an AnyLogic installation under the
    `plugins\com.anylogic.third_party_libraries_<version stamp>\lib\database\querydsl`
    directory. You will also need to set the `libver.querydsl-sql` Ant property
    accordingly (to reflect the version of the library AnyLogic is using).

