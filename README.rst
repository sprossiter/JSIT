===================
JSIT Library README
===================

The Java Simulation Infrastructure Toolkit (JSIT) is a set of Java libraries
that provides certain software-engineering best-practice features for
simulations. The idea is that it is used to supplement a given simulation
toolkit (such as AnyLogic_ or MASON_) to provide features that they tend to
lack, and provide common design and coding for them (especially useful for
those developing simulations using a number of toolkits).

It exists as a core library supplemented by toolkit-specific helper libraries.
(It can be used without the helper library, but this requires the user to code
helper classes themselves.) Currently, a helper library exists only for
AnyLogic, though support for at least MASON and `Repast Simphony`_ is planned.

JSIT is developed by Stuart Rossiter (as part of research at the University of
Southampton, UK). Contact him at stuart.p.rossiter@gmail.com.

JSIT is open source software released under the LGPL license (see Licensing_
below). JSIT source code is stored on GitHub at
`https://github.com/sprossiter/JSIT <https://github.com/sprossiter/JSIT>`_.

For further information on using JSIT and how it works, see the `User Guide`_.

.. _Licensing:

Licensing
=========

.. image:: src/main/resources/images/lgplv3-88x31.png

JSIT is distributed under the GNU LGPL V3 license, which has a copying
permission statement as below. (See the full `LGPL license`_ and `GPL license`_
for more details.)

::

        Copyright University of Southampton 2015
        
        JSIT is free software: you can redistribute it and/or modify it under the terms
        of the GNU Lesser General Public License as published by the Free Software
        Foundation, either version 3 of the License, or (at your option) any later
        version.
        
        JSIT is distributed in the hope that it will be useful, but WITHOUT ANY
        WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
        PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
        
        You should have received a copy of the GNU Lesser General Public License along
        with JSIT. If not, see <http://www.gnu.org/licenses/>.

Dependencies
------------

JSIT requires Java 1.6+. It uses the following third-party libraries with
LGPL-compatible licenses (nested items show dependencies of the primary
dependencies):

* Logback_

* SLF4J_

* XStream_

  - XMLPull_

  - Xpp3_

* `Apache Commons Codec`_

* `Apache Commons IO`_

* `Apache Commons Configuration`_

  - `Apache Commons Lang`_ 2.x

  - `Apache Commons Logging`_

* `Apache Subversion`_ JavaHL (Java binding for SVN)

The links above can be used to get source code for these dependencies.

Copies of all their licences and any required attribution (notice) files are
included in bundled JSIT distributions (in the ``lib`` folder together with the
dependencies themselves).

If you're *developing* JSIT, you can use the ``getDependencies`` target of the
JSIT Ant_ build file (``src/main/build/build.xml``) to populate all the open
source dependencies in the ``lib`` folder.

AnyLogic
~~~~~~~~

To **use** the JSIT AnyLogic helper library, you will need a valid copy of
AnyLogic version 7.1.1 or later. (AnyLogic code package names changed with this
version.) When running AnyLogic models using JSIT, the required runtime
libraries are automatically available.

To **develop** (compile) the JSIT AnyLogic helper library, you will need to
reference the AnyLogic Engine library, which exists in an AnyLogic installation
(under the  ``plugins\com.anylogic.engine_<version stamp>`` directory), or is
produced by exporting an AnyLogic model to a Java applet. This library is
covered by the `AnyLogic Engine Runtime License Agreement`_.

.. _AnyLogic: http://www.anylogic.com
.. _MASON: http://cs.gmu.edu/~eclab/projects/mason
.. _Repast Simphony: http://repast.sourceforge.net
.. _Logback: http://logback.qos.ch
.. _SLF4J: http://www.slf4j.org
.. _XStream: http://xstream.codehaus.org
.. _XMLPull: http://www.xmlpull.org
.. _Xpp3: http://www.extreme.indiana.edu/xgws/xsoap/xpp/mxp1
.. _Apache Commons Codec: http://commons.apache.org/proper/commons-codec
.. _Apache Commons IO: http://commons.apache.org/proper/commons-io
.. _Apache Commons Configuration: http://commons.apache.org/proper/commons-configuration
.. _Apache Commons Lang: http://commons.apache.org/proper/commons-lang
.. _Apache Commons Logging: http://commons.apache.org/proper/commons-logging
.. _Apache Subversion: https://subversion.apache.org/
.. _Ant: http://ant.apache.org

.. _attributions file: attributions.txt
.. _LGPL license: lgpl.txt
.. _GPL license: gpl.txt
.. _User Guide: src/main/resources/docs/userGuide.htm
.. _AnyLogic Engine Runtime License Agreement: src/main/resources/AnyLogicEngineRuntimeLicenseAgreement.html
