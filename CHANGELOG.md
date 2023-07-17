# [4.0.0-alpha.5](https://github.com/gravitee-io/gravitee-node/compare/4.0.0-alpha.4...4.0.0-alpha.5) (2023-07-17)


### Features

* **node-notifier:** supports both javax and jakarta Inject annotation ([a909bff](https://github.com/gravitee-io/gravitee-node/commit/a909bffde7976f9e329c46db39ad787a310c2c56))

# [4.0.0-alpha.4](https://github.com/gravitee-io/gravitee-node/compare/4.0.0-alpha.3...4.0.0-alpha.4) (2023-07-11)


### chore

* bump gravitee-parent and gravitee-bom ([e9428a7](https://github.com/gravitee-io/gravitee-node/commit/e9428a73bc14a1164a98c1ccfc805f09cc8c933a))


### BREAKING CHANGES

* update code to JDK 17

# [4.0.0-alpha.3](https://github.com/gravitee-io/gravitee-node/compare/4.0.0-alpha.2...4.0.0-alpha.3) (2023-07-07)


### Bug Fixes

* license model syntax ([23859ec](https://github.com/gravitee-io/gravitee-node/commit/23859ecb1b3ad36de851f6db7009408ea0c045b2))

# [4.0.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/4.0.0-alpha.1...4.0.0-alpha.2) (2023-07-05)


### Features

* add apim-policy-transform-avro-json to catalog ([f9b4cdb](https://github.com/gravitee-io/gravitee-node/commit/f9b4cdbb2bd7163493da58c6931aa4b9f84ca6b6))

# [4.0.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.11...4.0.0-alpha.1) (2023-07-03)


### Bug Fixes

* add old feature names to catalog ([255803e](https://github.com/gravitee-io/gravitee-node/commit/255803ecfbcd59023858a30628fdd6acb13d0530))
* restore isFeatureEnabled in license interface ([6fb0dbe](https://github.com/gravitee-io/gravitee-node/commit/6fb0dbef2def0962da6120467b7a001e4a79b1ca))
* update risk-assesment feature name ([ba156e8](https://github.com/gravitee-io/gravitee-node/commit/ba156e8fb1f05f48b62522ca786a2ea54ae13751))


### Features

* add apim-policy-geoip-filtering ([4464df1](https://github.com/gravitee-io/gravitee-node/commit/4464df1111fafb58d21d852b7acf600f136b61e3))
* add v4 license model and utility services ([498dce9](https://github.com/gravitee-io/gravitee-node/commit/498dce938f683f1a6a17c9fa4add84a6b1189c4a))


### BREAKING CHANGES

* checking for feature enablement now relies on the NodeLicenseService

# [3.1.0-alpha.11](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.10...3.1.0-alpha.11) (2023-06-17)


### Bug Fixes

* **deps:** bump gravitee-plugin to 2.0.0-alpha1 ([850236d](https://github.com/gravitee-io/gravitee-node/commit/850236d2c4cbd9ac49e6b02bf8b92c4b9f845d44))

# [3.1.0-alpha.10](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.9...3.1.0-alpha.10) (2023-06-12)


### Bug Fixes

* **perf:** This fix rework the ExcludeTagsFilter to reduce a processing time ([4a52c0b](https://github.com/gravitee-io/gravitee-node/commit/4a52c0b4223477b949b106cdf97499a996e39e40))

# [3.1.0-alpha.9](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.8...3.1.0-alpha.9) (2023-05-29)


### Features

* add cluster id on ClusterManager interface ([153c441](https://github.com/gravitee-io/gravitee-node/commit/153c441d8c68babc0cc2cd1e6ce61e2c7dbb96fb))

# [3.1.0-alpha.8](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.7...3.1.0-alpha.8) (2023-05-25)


### Bug Fixes

* avoid npe when evaluating cpu load average ([61c1dd7](https://github.com/gravitee-io/gravitee-node/commit/61c1dd7d16d8b1a983340def654a02bd08eabb61))

# [3.1.0-alpha.7](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.6...3.1.0-alpha.7) (2023-05-16)


### Bug Fixes

* bump kubernetes client to 2.0.3 ([51b64c2](https://github.com/gravitee-io/gravitee-node/commit/51b64c299a688ce4b8aa9b6214e417fc25043333))

# [3.1.0-alpha.6](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.5...3.1.0-alpha.6) (2023-05-03)


### Bug Fixes

* bump gravitee-kubernetes version to 2.0.2 ([7bec0f7](https://github.com/gravitee-io/gravitee-node/commit/7bec0f7eb78849347c6095e108c627bbd969929c))

# [3.1.0-alpha.5](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.4...3.1.0-alpha.5) (2023-04-19)


### Features

* add new plugin type for cache manager ([3bf9804](https://github.com/gravitee-io/gravitee-node/commit/3bf9804b0596a70d0df7082c16b81037b5e38284))

# [3.1.0-alpha.4](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.3...3.1.0-alpha.4) (2023-04-18)


### Bug Fixes

* tmp enable standalone cache by default ([2fb8064](https://github.com/gravitee-io/gravitee-node/commit/2fb80644f91abe54c0dd1ea68b6f0277a99cffb7))

# [3.1.0-alpha.3](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.2...3.1.0-alpha.3) (2023-04-14)


### Bug Fixes

* use simple hazelcast topic ([d45cac8](https://github.com/gravitee-io/gravitee-node/commit/d45cac845f992f83c1db54c5155b7d79f691886a))

# [3.1.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/3.1.0-alpha.1...3.1.0-alpha.2) (2023-04-14)


### Features

* update cluster manager to be compatible with plugin management ([f69ad89](https://github.com/gravitee-io/gravitee-node/commit/f69ad895f8b80a091a196a5e167210a73c154e5a))

# [3.1.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/3.0.5...3.1.0-alpha.1) (2023-04-13)


### Features

* add support for multi-servers ([44af8ff](https://github.com/gravitee-io/gravitee-node/commit/44af8ffd125168fb4b6c4643e17d642d8ed433db))

## [3.0.5](https://github.com/gravitee-io/gravitee-node/compare/3.0.4...3.0.5) (2023-04-06)


### Bug Fixes

* prevent scheduleNextAttempt to happen if trigger is stopped ([d9c2a44](https://github.com/gravitee-io/gravitee-node/commit/d9c2a44784ff7b1d861b82c02ea15a7fdf269da2))

## [3.0.4](https://github.com/gravitee-io/gravitee-node/compare/3.0.3...3.0.4) (2023-04-03)


### Bug Fixes

* rollback dry run mode on upgrader ([ed33d59](https://github.com/gravitee-io/gravitee-node/commit/ed33d591451eca4a9efacf95c031a87b7b09df20))

## [3.0.3](https://github.com/gravitee-io/gravitee-node/compare/3.0.2...3.0.3) (2023-03-31)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([8d878ba](https://github.com/gravitee-io/gravitee-node/commit/8d878ba01be2445d83e8fefefe6728383c47e945))

## [3.0.2](https://github.com/gravitee-io/gravitee-node/compare/3.0.1...3.0.2) (2023-03-28)


### Bug Fixes

* handle dry runs in upgraders ([187a585](https://github.com/gravitee-io/gravitee-node/commit/187a585e0b5503d1fe45b1cb6e8f5e64a582c5a7))

## [3.0.1](https://github.com/gravitee-io/gravitee-node/compare/3.0.0...3.0.1) (2023-03-17)


### Bug Fixes

* **deps:** remove unused gravitee-el dependency ([40344fd](https://github.com/gravitee-io/gravitee-node/commit/40344fd23522a6e25c8cb3454ab0c29d9eb965fe))

# [3.0.0](https://github.com/gravitee-io/gravitee-node/compare/2.0.5...3.0.0) (2023-03-17)


### Bug Fixes

* **api:** load the right property source for /configuration ([a896204](https://github.com/gravitee-io/gravitee-node/commit/a896204ed1ccc8ea6b77378b10f2f6f4fb27b46b))
* avoid spring loading issue with vertx and k8s client ([a46a05d](https://github.com/gravitee-io/gravitee-node/commit/a46a05d8c9a306071fdcca1f8aad2925cb8dd959))
* bump hazelcast and snake yml dependencies ([b297a0e](https://github.com/gravitee-io/gravitee-node/commit/b297a0ef2cf02d54a37eb4a1093e834d0fcadbf7))
* **deps:** upgrade dependencies ([c60db5a](https://github.com/gravitee-io/gravitee-node/commit/c60db5a500a9746ebadd928b8163546b44442c34))
* remove enforced INFO level for license ([8681528](https://github.com/gravitee-io/gravitee-node/commit/8681528c2dcf376f03197339b4bfc9967b89e565))
* update kubernetes client dependency ([bc5d835](https://github.com/gravitee-io/gravitee-node/commit/bc5d835c3c7060d5091c70453d89c4886c7249ec))


### Features

* bump gravitee-plugin version to 1.25.0 ([88b4ac0](https://github.com/gravitee-io/gravitee-node/commit/88b4ac0c097e4bd72a37893b832dde9634094206))
* bump reporter api version ([c2493b7](https://github.com/gravitee-io/gravitee-node/commit/c2493b75a04b5e38671bf68425e7a6a3915c61f8))
* move to bom v4 and vertx 4.3.8 ([29f3796](https://github.com/gravitee-io/gravitee-node/commit/29f3796259f4f0e8f248c7997fbd2bd30a6c2642))


### BREAKING CHANGES

* requires vertx 4.3.8

# [3.0.0-alpha.4](https://github.com/gravitee-io/gravitee-node/compare/3.0.0-alpha.3...3.0.0-alpha.4) (2023-03-16)


### Bug Fixes

* bump hazelcast and snake yml dependencies ([b297a0e](https://github.com/gravitee-io/gravitee-node/commit/b297a0ef2cf02d54a37eb4a1093e834d0fcadbf7))
* remove enforced INFO level for license ([8681528](https://github.com/gravitee-io/gravitee-node/commit/8681528c2dcf376f03197339b4bfc9967b89e565))

# [3.0.0-alpha.4](https://github.com/gravitee-io/gravitee-node/compare/3.0.0-alpha.3...3.0.0-alpha.4) (2023-03-09)


### Bug Fixes

* remove enforced INFO level for license ([8681528](https://github.com/gravitee-io/gravitee-node/commit/8681528c2dcf376f03197339b4bfc9967b89e565))

# [3.0.0-alpha.3](https://github.com/gravitee-io/gravitee-node/compare/3.0.0-alpha.2...3.0.0-alpha.3) (2023-03-07)


### Bug Fixes

* avoid spring loading issue with vertx and k8s client ([a46a05d](https://github.com/gravitee-io/gravitee-node/commit/a46a05d8c9a306071fdcca1f8aad2925cb8dd959))

# [3.0.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/3.0.0-alpha.1...3.0.0-alpha.2) (2023-02-22)


### Bug Fixes

* update kubernetes client dependency ([bc5d835](https://github.com/gravitee-io/gravitee-node/commit/bc5d835c3c7060d5091c70453d89c4886c7249ec))

# [3.0.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/2.1.0-alpha.2...3.0.0-alpha.1) (2023-02-16)


### Features

* bump gravitee-plugin version to 1.25.0 ([88b4ac0](https://github.com/gravitee-io/gravitee-node/commit/88b4ac0c097e4bd72a37893b832dde9634094206))
* move to bom v4 and vertx 4.3.8 ([29f3796](https://github.com/gravitee-io/gravitee-node/commit/29f3796259f4f0e8f248c7997fbd2bd30a6c2642))


### BREAKING CHANGES

* requires vertx 4.3.8

# [2.1.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/2.1.0-alpha.1...2.1.0-alpha.2) (2023-02-16)


### Bug Fixes

* **api:** load the right property source for /configuration ([a896204](https://github.com/gravitee-io/gravitee-node/commit/a896204ed1ccc8ea6b77378b10f2f6f4fb27b46b))

# [2.1.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/2.0.2...2.1.0-alpha.1) (2023-02-10)


### Features

* bump reporter api version ([c2493b7](https://github.com/gravitee-io/gravitee-node/commit/c2493b75a04b5e38671bf68425e7a6a3915c61f8))

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
