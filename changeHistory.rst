===================
JSIT Change History
===================

Version 0.2
-----------

**SNAPSHOT RELEASE: SOME WORK BELOW STILL IN PROGRESS**

Changes for initial open source release:

- Update to be compatible with AnyLogic 7.1 (which changed AnyLogic package
names).

- Switch from SVNKit (GPL-like license) to Apache Subversion JavaHL (Apache
  license) with associated code refactoring.

- Added user guide, README, change history, and licensing details.

- Added distribution build process (Ant-based).

- Refactor logging and stochastic control to work round AnyLogic threading
  issues (with AnyLogic-specific variants).

- Tidy up exceptions (with two JSIT-specific exceptions).

- Complete set of supported probability distributions to match those supplied by
  AnyLogic and MASON.

Version 0.1
-----------

- Initial internal version designed to work with AnyLogic 7, using SVNKit for
  SVN operations.
