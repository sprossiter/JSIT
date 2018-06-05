# JSIT Change History

## Version 0.2

- Update to be compatible with AnyLogic 7.1 (which changed AnyLogic package
  names) and AnyLogic 8 (where use of Java 9 breaks XStream, SLF4J is bound to
  log4j, and a new compilation dependency is needed).

- Switch from SVNKit (GPL-like license) to Apache Subversion JavaHL (Apache
  license) with associated code refactoring.

- Added user guide, README, change history, and licensing details.

- Added distribution build process (Ant-based).

- Refactor logging and stochastic control to work round AnyLogic threading
  issues (with AnyLogic-specific variants).

- Tidy up exceptions (with two JSIT-specific exceptions).

- Add further probability distributions (working towards the set of those
  supported by AnyLogic and MASON).

- Add message-based events publish/subscribe alternative mechanism.

- Allow AnyLogic custom experiments to use JSIT.

- Allow for use of the stochasticity control file to be disabled in the JSIT API
  (so that it can be left for testing but 'disabled' for real runs).

## Version 0.1

- Initial internal version designed to work with AnyLogic 7, using SVNKit for
  SVN operations.
