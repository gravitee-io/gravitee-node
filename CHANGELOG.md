## [2.0.3](https://github.com/gravitee-io/gravitee-node/compare/2.0.2...2.0.3) (2023-02-16)


### Bug Fixes

* **api:** load the right property source for /configuration ([1ab5e77](https://github.com/gravitee-io/gravitee-node/commit/1ab5e77e3121e02a745975a8cb5daee7881096bb))

## [2.0.2](https://github.com/gravitee-io/gravitee-node/compare/2.0.1...2.0.2) (2023-02-10)


### Bug Fixes

* **api:** send head before writing response for /monitor ([c71fc4c](https://github.com/gravitee-io/gravitee-node/commit/c71fc4cd2212a34ef0c14a00196410eadb42e334))

## [2.0.1](https://github.com/gravitee-io/gravitee-node/compare/2.0.0...2.0.1) (2022-12-09)


### Bug Fixes

* bump reporter-api ([ebeceda](https://github.com/gravitee-io/gravitee-node/commit/ebeceda56f987105eb67cefbb1e4b14aa8fcfbe2))

# [2.0.0](https://github.com/gravitee-io/gravitee-node/compare/1.27.3...2.0.0) (2022-12-09)


### chore

* bump gravitee-kubernetes version to 1.0.0-alpha.1 ([94bbb33](https://github.com/gravitee-io/gravitee-node/commit/94bbb33c7834a08e1057876c305a92329b17d88d))
* bump to rxJava3 ([a2a1c8d](https://github.com/gravitee-io/gravitee-node/commit/a2a1c8dd6c619f67300a49df216a6688d971bf14))


### BREAKING CHANGES

* rxJava3 required
* rxJava3 required

# [2.0.0-alpha.3](https://github.com/gravitee-io/gravitee-node/compare/2.0.0-alpha.2...2.0.0-alpha.3) (2022-10-18)


### chore

* bump gravitee-kubernetes version to 1.0.0-alpha.1 ([94bbb33](https://github.com/gravitee-io/gravitee-node/commit/94bbb33c7834a08e1057876c305a92329b17d88d))


### BREAKING CHANGES

* rxJava3 required

# [2.0.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/2.0.0-alpha.1...2.0.0-alpha.2) (2022-10-18)


### Reverts

* Revert "chore: use properties for maven versionning" ([ad9bdd1](https://github.com/gravitee-io/gravitee-node/commit/ad9bdd1fe431a9d97aed02c5d5ee9dadfd96246b))

# [2.0.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/1.27.1...2.0.0-alpha.1) (2022-10-18)


### chore

* bump to rxJava3 ([a2a1c8d](https://github.com/gravitee-io/gravitee-node/commit/a2a1c8dd6c619f67300a49df216a6688d971bf14))


### BREAKING CHANGES

* rxJava3 required

## [1.27.3](https://github.com/gravitee-io/gravitee-node/compare/1.27.2...1.27.3) (2022-11-02)


### Bug Fixes

* **node-cache:** move guava to compile scope instead of provided ([88f21eb](https://github.com/gravitee-io/gravitee-node/commit/88f21eb04c611c4b6896fd3560e7d674bc57e64a))

## [1.27.2](https://github.com/gravitee-io/gravitee-node/compare/1.27.1...1.27.2) (2022-10-20)


### Bug Fixes

* add hashcode and equals to UpgraderRecord ([#155](https://github.com/gravitee-io/gravitee-node/issues/155)) ([b63f2d7](https://github.com/gravitee-io/gravitee-node/commit/b63f2d745191258a82780be8bee46cf7a1e94cf6))

## [1.27.1](https://github.com/gravitee-io/gravitee-node/compare/1.27.0...1.27.1) (2022-09-29)


### Bug Fixes

* get upgrade.mode from env variables ([237ba43](https://github.com/gravitee-io/gravitee-node/commit/237ba43ac46b3ce8cc7b6929ce45aadbc3425f0a))

# [1.27.0](https://github.com/gravitee-io/gravitee-node/compare/1.26.1...1.27.0) (2022-09-21)


### Features

* Implement upgrader framework ([96d3eb1](https://github.com/gravitee-io/gravitee-node/commit/96d3eb13aab38c6c1e5b1bea678ab88cfb55f05f))

## [1.26.1](https://github.com/gravitee-io/gravitee-node/compare/1.26.0...1.26.1) (2022-09-13)


### Bug Fixes

* Upgrade dependency to gravitee-kubernetes ([#151](https://github.com/gravitee-io/gravitee-node/issues/151)) ([97faf20](https://github.com/gravitee-io/gravitee-node/commit/97faf20a44e7ca7ca6dca1e117f4e60c70a18830))

# [1.26.0](https://github.com/gravitee-io/gravitee-node/compare/1.25.0...1.26.0) (2022-09-12)


### Features

* support yaml file for Hazelcast configuration ([43a2674](https://github.com/gravitee-io/gravitee-node/commit/43a267405afbaeb3e3f30ba32150259b9ca9d6b0))

# [1.25.0](https://github.com/gravitee-io/gravitee-node/compare/1.24.3...1.25.0) (2022-08-03)


### Features

* allows to include or exclude metrics labels ([f39f612](https://github.com/gravitee-io/gravitee-node/commit/f39f612c444031f62762ec4591634a0dd02522af)), closes [gravitee-io/issues#8218](https://github.com/gravitee-io/issues/issues/8218)

## [1.24.3](https://github.com/gravitee-io/gravitee-node/compare/1.24.2...1.24.3) (2022-08-01)


### Bug Fixes

* Add endpoints for generating heapdump and threaddump ([6c087c7](https://github.com/gravitee-io/gravitee-node/commit/6c087c7bc8c4ca30966bdc4880e8bf53e8c2ed39)), closes [gravitee-io/issues#8222](https://github.com/gravitee-io/issues/issues/8222)

## [1.24.2](https://github.com/gravitee-io/gravitee-node/compare/1.24.1...1.24.2) (2022-06-09)


### Bug Fixes

* respect TimeToLive parameter in StandaloneCache ([dacdbda](https://github.com/gravitee-io/gravitee-node/commit/dacdbda459754a4722af4b04c4814c330afb308c))

## [1.20.5](https://github.com/gravitee-io/gravitee-node/compare/1.20.4...1.20.5) (2022-07-28)


### Bug Fixes

* Add endpoints for generating heapdump and threaddump ([6c087c7](https://github.com/gravitee-io/gravitee-node/commit/6c087c7bc8c4ca30966bdc4880e8bf53e8c2ed39)), closes [gravitee-io/issues#8222](https://github.com/gravitee-io/issues/issues/8222)

## [1.20.4](https://github.com/gravitee-io/gravitee-node/compare/1.20.3...1.20.4) (2022-06-09)


### Bug Fixes

* respect TimeToLive parameter in StandaloneCache ([dacdbda](https://github.com/gravitee-io/gravitee-node/commit/dacdbda459754a4722af4b04c4814c330afb308c))

## [1.24.1](https://github.com/gravitee-io/gravitee-node/compare/1.24.0...1.24.1) (2022-06-02)


### Bug Fixes

* implement abtract plugin deployment to validate license feature ([#145](https://github.com/gravitee-io/gravitee-node/issues/145)) ([36d3bfa](https://github.com/gravitee-io/gravitee-node/commit/36d3bfa7ef6aef18e07f543c505d9ad90a701f33))

# [1.24.0](https://github.com/gravitee-io/gravitee-node/compare/1.23.0...1.24.0) (2022-05-20)


### Bug Fixes

* Upgrade dependency to gravitee-plugin ([308e6ca](https://github.com/gravitee-io/gravitee-node/commit/308e6ca628610d9ed89f3a427ce5d2f75ba0add4))


### Features

* Single distribution bundle for CE / EE ([5c0e6c4](https://github.com/gravitee-io/gravitee-node/commit/5c0e6c4d1ddb8dc690de4f57a5ef71a2acf44b34))

# [1.23.0](https://github.com/gravitee-io/gravitee-node/compare/1.22.0...1.23.0) (2022-05-12)


### Features

* Add support for the new Kubernetes Client API ([6deabbe](https://github.com/gravitee-io/gravitee-node/commit/6deabbeb30fe40860acd9e94616595931a8e15be))

# [1.22.0](https://github.com/gravitee-io/gravitee-node/compare/1.21.1...1.22.0) (2022-04-15)


### Bug Fixes

* **deps:** update bouncycastle.version to v1.70 ([6118e39](https://github.com/gravitee-io/gravitee-node/commit/6118e39729c14437a89d1821c3103cb994b277fe))


### Features

* provide access to meter registry ([6c4af53](https://github.com/gravitee-io/gravitee-node/commit/6c4af53ae753ea1c17336eb77ade7e7023ba4113))

## [1.21.1](https://github.com/gravitee-io/gravitee-node/compare/1.21.0...1.21.1) (2022-04-12)


### Bug Fixes

* configurable jetty http  header and buffer size ([8fb55b2](https://github.com/gravitee-io/gravitee-node/commit/8fb55b251fd84e5fff7aa9a5c6e327ca1447a6d7))
