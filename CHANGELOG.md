## [2.1.1](https://github.com/gravitee-io/gravitee-node/compare/2.1.0...2.1.1) (2024-01-05)


### Bug Fixes

* update PEM registry logic ([#279](https://github.com/gravitee-io/gravitee-node/issues/279)) ([9953280](https://github.com/gravitee-io/gravitee-node/commit/995328047fa065d7eb5dcf755889b68ba6151e57))

# [2.1.0](https://github.com/gravitee-io/gravitee-node/compare/2.0.10...2.1.0) (2023-11-09)


### Features

* add support for Gravitee Pem Registry ([#257](https://github.com/gravitee-io/gravitee-node/issues/257)) ([9430497](https://github.com/gravitee-io/gravitee-node/commit/9430497e19ad9cb07ee241db0f93f27efb2e5129))

## [2.0.10](https://github.com/gravitee-io/gravitee-node/compare/2.0.9...2.0.10) (2023-05-25)


### Bug Fixes

* add config to disable keystore watcher ([6b125cf](https://github.com/gravitee-io/gravitee-node/commit/6b125cf38e61a2d7e2b30e04efe9d4edd52d4ab7))

## [2.0.9](https://github.com/gravitee-io/gravitee-node/compare/2.0.8...2.0.9) (2023-05-24)


### Bug Fixes

* avoid npe when evaluating cpu load average ([b8470f3](https://github.com/gravitee-io/gravitee-node/commit/b8470f3a7c872d4df4e34ecf689d99d7bdbd71a5))

## [2.0.8](https://github.com/gravitee-io/gravitee-node/compare/2.0.7...2.0.8) (2023-05-04)


### Bug Fixes

* license error in community mode ([7fbfcb4](https://github.com/gravitee-io/gravitee-node/commit/7fbfcb4709b73c6649e86e73f9fa90a967e581b5))

## [2.0.7](https://github.com/gravitee-io/gravitee-node/compare/2.0.6...2.0.7) (2023-04-27)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([1051060](https://github.com/gravitee-io/gravitee-node/commit/1051060a81435c7b67c69ee972e5447b6386d716))
* add 'With' methods to HttpServerConfiguration ([a0bb68f](https://github.com/gravitee-io/gravitee-node/commit/a0bb68f5b2a57bbef1709292c583c18602ce6ce7))
* **api:** load the right property source for /configuration ([935a394](https://github.com/gravitee-io/gravitee-node/commit/935a39473f78e4a0ab90b8a360694730bd4f31bf))
* **api:** load the right property source for /configuration ([f9580e2](https://github.com/gravitee-io/gravitee-node/commit/f9580e26395fc4b92d72b81d7eb5c6f10e3fc784))
* **api:** load the right property source for /configuration ([563b0f8](https://github.com/gravitee-io/gravitee-node/commit/563b0f8f62f278a52f9a355c9b7dd90b1e290465))
* **api:** send head before writing response for /monitor ([66abc90](https://github.com/gravitee-io/gravitee-node/commit/66abc903384881e8bedd4b1f4b6141ba061d9c7d))
* **api:** send head before writing response for /monitor ([08d02ea](https://github.com/gravitee-io/gravitee-node/commit/08d02ead134ee9a318ccbd58d8465d40378dbf76))
* **api:** send head before writing response for /monitor ([3700dd0](https://github.com/gravitee-io/gravitee-node/commit/3700dd0cf91db2b9b182e3e940831cf22dc61bca))
* avoid spring loading issue with vertx and k8s client ([ccbb643](https://github.com/gravitee-io/gravitee-node/commit/ccbb643f846aac4ea3e3647448bb1113cf44d6ce))
* avoid spring loading issue with vertx and k8s client ([fe829f9](https://github.com/gravitee-io/gravitee-node/commit/fe829f9fcf9b80f2525925316553aa1db54737b7))
* bump kubernetes client ([137eaf0](https://github.com/gravitee-io/gravitee-node/commit/137eaf0e48c306882cb37438a16968ece6684b24))
* **prometheus:** Stream scraping output instead of using a single String instance ([6e4271b](https://github.com/gravitee-io/gravitee-node/commit/6e4271bf66faa109bd21b078629fc1f7ed895f51))
* remove enforced INFO level for license ([0515ec5](https://github.com/gravitee-io/gravitee-node/commit/0515ec5a8acbe660cc3e9fa042edf21b535b0568))

## [1.27.8](https://github.com/gravitee-io/gravitee-node/compare/1.27.7...1.27.8) (2023-04-27)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([1051060](https://github.com/gravitee-io/gravitee-node/commit/1051060a81435c7b67c69ee972e5447b6386d716))
* add 'With' methods to HttpServerConfiguration ([a0bb68f](https://github.com/gravitee-io/gravitee-node/commit/a0bb68f5b2a57bbef1709292c583c18602ce6ce7))
* **api:** load the right property source for /configuration ([f9580e2](https://github.com/gravitee-io/gravitee-node/commit/f9580e26395fc4b92d72b81d7eb5c6f10e3fc784))
* **api:** load the right property source for /configuration ([563b0f8](https://github.com/gravitee-io/gravitee-node/commit/563b0f8f62f278a52f9a355c9b7dd90b1e290465))
* **api:** send head before writing response for /monitor ([08d02ea](https://github.com/gravitee-io/gravitee-node/commit/08d02ead134ee9a318ccbd58d8465d40378dbf76))
* **api:** send head before writing response for /monitor ([3700dd0](https://github.com/gravitee-io/gravitee-node/commit/3700dd0cf91db2b9b182e3e940831cf22dc61bca))
* avoid spring loading issue with vertx and k8s client ([fe829f9](https://github.com/gravitee-io/gravitee-node/commit/fe829f9fcf9b80f2525925316553aa1db54737b7))
* bump kubernetes client ([137eaf0](https://github.com/gravitee-io/gravitee-node/commit/137eaf0e48c306882cb37438a16968ece6684b24))
* **prometheus:** Stream scraping output instead of using a single String instance ([6e4271b](https://github.com/gravitee-io/gravitee-node/commit/6e4271bf66faa109bd21b078629fc1f7ed895f51))

## [1.25.6](https://github.com/gravitee-io/gravitee-node/compare/1.25.5...1.25.6) (2023-04-27)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([a0bb68f](https://github.com/gravitee-io/gravitee-node/commit/a0bb68f5b2a57bbef1709292c583c18602ce6ce7))
* **api:** load the right property source for /configuration ([563b0f8](https://github.com/gravitee-io/gravitee-node/commit/563b0f8f62f278a52f9a355c9b7dd90b1e290465))
* **api:** send head before writing response for /monitor ([3700dd0](https://github.com/gravitee-io/gravitee-node/commit/3700dd0cf91db2b9b182e3e940831cf22dc61bca))
* **prometheus:** Stream scraping output instead of using a single String instance ([6e4271b](https://github.com/gravitee-io/gravitee-node/commit/6e4271bf66faa109bd21b078629fc1f7ed895f51))

## [1.24.7](https://github.com/gravitee-io/gravitee-node/compare/1.24.6...1.24.7) (2023-04-24)


### Bug Fixes

* **prometheus:** Stream scraping output instead of using a single String instance ([6e4271b](https://github.com/gravitee-io/gravitee-node/commit/6e4271bf66faa109bd21b078629fc1f7ed895f51))

## [1.24.6](https://github.com/gravitee-io/gravitee-node/compare/1.24.5...1.24.6) (2023-03-31)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([a0bb68f](https://github.com/gravitee-io/gravitee-node/commit/a0bb68f5b2a57bbef1709292c583c18602ce6ce7))

## [1.24.5](https://github.com/gravitee-io/gravitee-node/compare/1.24.4...1.24.5) (2023-02-16)


### Bug Fixes

* **api:** load the right property source for /configuration ([563b0f8](https://github.com/gravitee-io/gravitee-node/commit/563b0f8f62f278a52f9a355c9b7dd90b1e290465))

## [1.24.4](https://github.com/gravitee-io/gravitee-node/compare/1.24.3...1.24.4) (2023-02-10)


### Bug Fixes

* **api:** send head before writing response for /monitor ([3700dd0](https://github.com/gravitee-io/gravitee-node/commit/3700dd0cf91db2b9b182e3e940831cf22dc61bca))

## [1.25.5](https://github.com/gravitee-io/gravitee-node/compare/1.25.4...1.25.5) (2023-04-06)


### Bug Fixes

* bump kubernetes client ([137eaf0](https://github.com/gravitee-io/gravitee-node/commit/137eaf0e48c306882cb37438a16968ece6684b24))

## [1.25.4](https://github.com/gravitee-io/gravitee-node/compare/1.25.3...1.25.4) (2023-03-31)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([1051060](https://github.com/gravitee-io/gravitee-node/commit/1051060a81435c7b67c69ee972e5447b6386d716))

## [1.25.3](https://github.com/gravitee-io/gravitee-node/compare/1.25.2...1.25.3) (2023-03-07)


### Bug Fixes

* avoid spring loading issue with vertx and k8s client ([fe829f9](https://github.com/gravitee-io/gravitee-node/commit/fe829f9fcf9b80f2525925316553aa1db54737b7))

## [1.25.2](https://github.com/gravitee-io/gravitee-node/compare/1.25.1...1.25.2) (2023-02-16)


### Bug Fixes

* **api:** load the right property source for /configuration ([f9580e2](https://github.com/gravitee-io/gravitee-node/commit/f9580e26395fc4b92d72b81d7eb5c6f10e3fc784))

## [1.25.1](https://github.com/gravitee-io/gravitee-node/compare/1.25.0...1.25.1) (2023-02-10)


### Bug Fixes

* **api:** send head before writing response for /monitor ([08d02ea](https://github.com/gravitee-io/gravitee-node/commit/08d02ead134ee9a318ccbd58d8465d40378dbf76))

## [1.27.7](https://github.com/gravitee-io/gravitee-node/compare/1.27.6...1.27.7) (2023-03-13)


### Bug Fixes

* avoid spring loading issue with vertx and k8s client ([ccbb643](https://github.com/gravitee-io/gravitee-node/commit/ccbb643f846aac4ea3e3647448bb1113cf44d6ce))

## [1.27.6](https://github.com/gravitee-io/gravitee-node/compare/1.27.5...1.27.6) (2023-03-09)


### Bug Fixes

* remove enforced INFO level for license ([0515ec5](https://github.com/gravitee-io/gravitee-node/commit/0515ec5a8acbe660cc3e9fa042edf21b535b0568))

## [1.27.5](https://github.com/gravitee-io/gravitee-node/compare/1.27.4...1.27.5) (2023-02-16)


### Bug Fixes

* **api:** load the right property source for /configuration ([935a394](https://github.com/gravitee-io/gravitee-node/commit/935a39473f78e4a0ab90b8a360694730bd4f31bf))

## [1.27.4](https://github.com/gravitee-io/gravitee-node/compare/1.27.3...1.27.4) (2023-02-10)


### Bug Fixes

* **api:** send head before writing response for /monitor ([66abc90](https://github.com/gravitee-io/gravitee-node/commit/66abc903384881e8bedd4b1f4b6141ba061d9c7d))

## [2.0.6](https://github.com/gravitee-io/gravitee-node/compare/2.0.5...2.0.6) (2023-03-31)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([c7210bb](https://github.com/gravitee-io/gravitee-node/commit/c7210bb7d102de5cfcde93fe29ddbd9af6b0a7a7))

## [2.0.5](https://github.com/gravitee-io/gravitee-node/compare/2.0.4...2.0.5) (2023-03-13)


### Bug Fixes

* avoid spring loading issue with vertx and k8s client ([4ff1b8b](https://github.com/gravitee-io/gravitee-node/commit/4ff1b8b3e6060e2303d34e39412b0edf572dfed1))

## [2.0.4](https://github.com/gravitee-io/gravitee-node/compare/2.0.3...2.0.4) (2023-02-17)


### Bug Fixes

* update kubernetes client dependency ([2b87da9](https://github.com/gravitee-io/gravitee-node/commit/2b87da9d3ac6c987a62e407f90223918add607a6))

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
