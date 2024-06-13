## [5.12.7](https://github.com/gravitee-io/gravitee-node/compare/5.12.6...5.12.7) (2024-06-13)


### Bug Fixes

* configure repo to release plugins in download website ([67d0fd9](https://github.com/gravitee-io/gravitee-node/commit/67d0fd91de9eacd8488684ee8e55495a4ed79618))

## [5.12.6](https://github.com/gravitee-io/gravitee-node/compare/5.12.5...5.12.6) (2024-05-22)


### Reverts

* Revert "fix(alpn): configure alpn option even if not secured" ([4cf2ce5](https://github.com/gravitee-io/gravitee-node/commit/4cf2ce5f98520829735229386d97e87707b1938c))

## [5.12.5](https://github.com/gravitee-io/gravitee-node/compare/5.12.4...5.12.5) (2024-05-20)


### Bug Fixes

* add Interops SP plugins ([7604631](https://github.com/gravitee-io/gravitee-node/commit/760463131307e620bc69712fde7e8796d38e3645))

## [5.12.4](https://github.com/gravitee-io/gravitee-node/compare/5.12.3...5.12.4) (2024-04-12)


### Bug Fixes

* **alpn:** configure alpn option even if not secured ([eb54cff](https://github.com/gravitee-io/gravitee-node/commit/eb54cffdac2fd1d6403f3da016517f020e0f92c1))

## [5.12.3](https://github.com/gravitee-io/gravitee-node/compare/5.12.2...5.12.3) (2024-04-05)


### Bug Fixes

* set http2-max-header-list-size option from the provided config ([3ee2feb](https://github.com/gravitee-io/gravitee-node/commit/3ee2febe298af1b2e475a14a759b2454c259c864))

## [5.12.2](https://github.com/gravitee-io/gravitee-node/compare/5.12.1...5.12.2) (2024-04-04)


### Bug Fixes

* **deps:** update dependency io.gravitee.alert:gravitee-alert-api to v1.9.1 ([23f7038](https://github.com/gravitee-io/gravitee-node/commit/23f703850cac115b6e77f794368d4baf4dd93370))

## [5.12.1](https://github.com/gravitee-io/gravitee-node/compare/5.12.0...5.12.1) (2024-04-04)


### Bug Fixes

* **deps:** update dependency io.gravitee:gravitee-bom to v7.0.18 ([358cab4](https://github.com/gravitee-io/gravitee-node/commit/358cab4cda9d98bf235d46f4f474cb211c6516d5))

# [5.12.0](https://github.com/gravitee-io/gravitee-node/compare/5.11.0...5.12.0) (2024-04-02)


### Features

* add ContainerInitializer concept ([00a11db](https://github.com/gravitee-io/gravitee-node/commit/00a11db674ced82f464a14efdc31607ea7dbb37f))

# [5.11.0](https://github.com/gravitee-io/gravitee-node/compare/5.10.0...5.11.0) (2024-03-28)


### Features

* handle keep-alive timeout ([56ac564](https://github.com/gravitee-io/gravitee-node/commit/56ac56483a48f48e1798676323a23d68bff818f0))

# [5.10.0](https://github.com/gravitee-io/gravitee-node/compare/5.9.1...5.10.0) (2024-03-15)


### Features

* a null license on create returns an oss license ([7f086e8](https://github.com/gravitee-io/gravitee-node/commit/7f086e8e63563e344e235eb28bd8bdc06487df18))

## [5.9.1](https://github.com/gravitee-io/gravitee-node/compare/5.9.0...5.9.1) (2024-03-07)


### Bug Fixes

* **deps:** update bcprov-jdk15on to bcprov-jdk18on and bcpkix-jdk15on to bcpkix-jdk18on ([81314e4](https://github.com/gravitee-io/gravitee-node/commit/81314e48e4fdf76e16a6a1fc027c0e8efc8b7707))

# [5.9.0](https://github.com/gravitee-io/gravitee-node/compare/5.8.1...5.9.0) (2024-03-07)


### Features

* introduce VertxHttpClientFactory ([676f458](https://github.com/gravitee-io/gravitee-node/commit/676f4583058c207661526a576f161d9a181c2875))
* introduce VertxTcpClientFactory ([52f7765](https://github.com/gravitee-io/gravitee-node/commit/52f7765dbec338184579ed58e483a27f29fb2293))

## [5.8.1](https://github.com/gravitee-io/gravitee-node/compare/5.8.0...5.8.1) (2024-02-23)


### Bug Fixes

* support private ca as truststore ([9613fbc](https://github.com/gravitee-io/gravitee-node/commit/9613fbcb4804cd6a4ce8f13283a1378dea3fe483))

# [5.8.0](https://github.com/gravitee-io/gravitee-node/compare/5.7.0...5.8.0) (2024-02-22)


### Features

* add new cache method ([bb1eabf](https://github.com/gravitee-io/gravitee-node/commit/bb1eabf297c32e2ce3bec406691f819e40e1fcb0))

# [5.7.0](https://github.com/gravitee-io/gravitee-node/compare/5.6.1...5.7.0) (2024-02-22)


### Features

* add interops features ([dae75b5](https://github.com/gravitee-io/gravitee-node/commit/dae75b5f43d4cb8bf84ebfc43d96e233a13f817a))

## [5.6.1](https://github.com/gravitee-io/gravitee-node/compare/5.6.0...5.6.1) (2024-02-20)


### Bug Fixes

* support org license expiration ([90598d2](https://github.com/gravitee-io/gravitee-node/commit/90598d2eef2ba9a8402c725fee72f3cd58baada2))

# [5.6.0](https://github.com/gravitee-io/gravitee-node/compare/5.5.2...5.6.0) (2024-02-20)


### Features

* add reactive method to Cache, Topic and Queue ([b2d292b](https://github.com/gravitee-io/gravitee-node/commit/b2d292ba6561944cc4316ed1a3a75d312f330653))

## [5.5.2](https://github.com/gravitee-io/gravitee-node/compare/5.5.1...5.5.2) (2024-02-13)


### Bug Fixes

* **license:** allow unknown plugins during validation ([58522a4](https://github.com/gravitee-io/gravitee-node/commit/58522a4db878b4b2e1558d949dcc869cfb9efecb))

## [5.5.1](https://github.com/gravitee-io/gravitee-node/compare/5.5.0...5.5.1) (2024-02-08)


### Bug Fixes

* support keystore with mix of private key and cert entries ([fcf141d](https://github.com/gravitee-io/gravitee-node/commit/fcf141d403c83c320e3aaa084e23ce367ee8527a))

# [5.5.0](https://github.com/gravitee-io/gravitee-node/compare/5.4.0...5.5.0) (2024-02-01)


### Features

* add protobuf-json and avro-protobuf features ([b9f0788](https://github.com/gravitee-io/gravitee-node/commit/b9f0788fdedba71972c2a63accec7f9282b577eb))

# [5.4.0](https://github.com/gravitee-io/gravitee-node/compare/5.3.1...5.4.0) (2024-01-25)


### Bug Fixes

* use generic for key-store-loaders factory to help spring ([744b342](https://github.com/gravitee-io/gravitee-node/commit/744b342c6014926c63943fc0d8227fa51bf1a68c))


### Features

* upgrade to latest bom version ([93d7cf0](https://github.com/gravitee-io/gravitee-node/commit/93d7cf098bbaec3f596a5a83f172323afcc240fc))

## [5.3.1](https://github.com/gravitee-io/gravitee-node/compare/5.3.0...5.3.1) (2024-01-22)


### Bug Fixes

* defer node bean instantiation ([21650b4](https://github.com/gravitee-io/gravitee-node/commit/21650b4d56710eeb899515e552b515331da805f7))

# [5.3.0](https://github.com/gravitee-io/gravitee-node/compare/5.2.0...5.3.0) (2024-01-16)


### Features

* load boot plugins ([94a290f](https://github.com/gravitee-io/gravitee-node/commit/94a290f9832145f24371f6ac880cf7f382dd1f32))

# [5.2.0](https://github.com/gravitee-io/gravitee-node/compare/5.1.0...5.2.0) (2024-01-04)


### Features

* **license:** add graphql rate limit policy feature ([e1bc497](https://github.com/gravitee-io/gravitee-node/commit/e1bc49700ab0ac43df326a42ace3f314c29ec784))

# [5.1.0](https://github.com/gravitee-io/gravitee-node/compare/5.0.2...5.1.0) (2023-12-22)


### Features

* reloadable trust store and one main keystore (re)loaded per server id ([dad0eb9](https://github.com/gravitee-io/gravitee-node/commit/dad0eb9c7f671d4c73150e1e6fece0c4693a37f4))

## [5.0.2](https://github.com/gravitee-io/gravitee-node/compare/5.0.1...5.0.2) (2023-12-21)


### Bug Fixes

* update PEM registry logic ([b02a700](https://github.com/gravitee-io/gravitee-node/commit/b02a70093c0a704636630c9c41eac2c9157b15d1))

## [5.0.1](https://github.com/gravitee-io/gravitee-node/compare/5.0.0...5.0.1) (2023-12-18)


### Bug Fixes

* add oem pack ([54c6be7](https://github.com/gravitee-io/gravitee-node/commit/54c6be7168d7db1a111bb3b93c2cbe73ca320d5c))

# [5.0.0](https://github.com/gravitee-io/gravitee-node/compare/4.8.1...5.0.0) (2023-12-11)


### Features

* introduce LicenseManager for organization license ([c307123](https://github.com/gravitee-io/gravitee-node/commit/c30712332bab4a1a18462e7d8be3c18e67f1057c))


### BREAKING CHANGES

* Node.license() not longer exists, LicenseManager is mandatory

## [4.8.1](https://github.com/gravitee-io/gravitee-node/compare/4.8.0...4.8.1) (2023-12-07)


### Bug Fixes

* remove dedicated schedulers in kubernetes keystore loader init methods ([32e675f](https://github.com/gravitee-io/gravitee-node/commit/32e675f84567153dca798a28265513e8fc7f34ff))

# [4.8.0](https://github.com/gravitee-io/gravitee-node/compare/4.7.1...4.8.0) (2023-12-06)


### Features

* improve Cache interface ([f2a5c92](https://github.com/gravitee-io/gravitee-node/commit/f2a5c920b9fef856175d0ac1324b45fdd0a78b21))

## [4.7.1](https://github.com/gravitee-io/gravitee-node/compare/4.7.0...4.7.1) (2023-12-01)


### Bug Fixes

* use local hash map to keep reference to created queue ([6ccc6d9](https://github.com/gravitee-io/gravitee-node/commit/6ccc6d9957b5536b4d71ad5c22df0e913bc17f57))

# [4.7.0](https://github.com/gravitee-io/gravitee-node/compare/4.6.1...4.7.0) (2023-11-30)


### Features

* add queue on ClusterManager ([fbd859e](https://github.com/gravitee-io/gravitee-node/commit/fbd859e42ff0a4f8ce30b68ed3bb3bffd9056f73))

## [4.6.1](https://github.com/gravitee-io/gravitee-node/compare/4.6.0...4.6.1) (2023-11-28)


### Bug Fixes

* defer some sensitive @Value resolution ([9f812f0](https://github.com/gravitee-io/gravitee-node/commit/9f812f068d8ed0c2fec623c594455645281e9263))

# [4.6.0](https://github.com/gravitee-io/gravitee-node/compare/4.5.0...4.6.0) (2023-11-24)


### Bug Fixes

* bump `gravitee-plugin` to 2.2.0 ([f0349cc](https://github.com/gravitee-io/gravitee-node/commit/f0349ccc2f204aba43740843a311a58413bf08e7))
* clean license file ([55de7ea](https://github.com/gravitee-io/gravitee-node/commit/55de7ea4699cf74b25826c2fd1b5d890e54da9d6))
* create a pack for alert-engine to avoid planet tiers to embed it by accident ([265b9b2](https://github.com/gravitee-io/gravitee-node/commit/265b9b2d099611cc3af118b591682ee6e959d831))
* reshuffle options in the right place, fix sonar issues ([2dbbb08](https://github.com/gravitee-io/gravitee-node/commit/2dbbb082e75672f26e813a9c5ca88c77b174a943))
* update k8s client version to 3.0.2 ([354ce10](https://github.com/gravitee-io/gravitee-node/commit/354ce10d0d6e20eaa46912a425bea1740e024562))
* update kubernetes client to version 3.0.0 ([28a7e26](https://github.com/gravitee-io/gravitee-node/commit/28a7e26d73e7deacdd9b59c7f46f7cf41eb84998))


### Features

* archi-222 secret provider plugin handling and configuration service ([#244](https://github.com/gravitee-io/gravitee-node/issues/244)) ([8c0fd34](https://github.com/gravitee-io/gravitee-node/commit/8c0fd347af091fee505520b2bf7794a2424cdf96))
* TCP proxy server factory and options ([161ee27](https://github.com/gravitee-io/gravitee-node/commit/161ee274a5c73c8709e2856a2293eff14fb29902))

# [4.6.0-alpha.3](https://github.com/gravitee-io/gravitee-node/compare/4.6.0-alpha.2...4.6.0-alpha.3) (2023-11-21)


### Bug Fixes

* create a pack for alert-engine to avoid planet tiers to embed it by accident ([7299d22](https://github.com/gravitee-io/gravitee-node/commit/7299d227f0cf81d096d40d01f73ea3cecd872fa6))

# [4.6.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/4.6.0-alpha.1...4.6.0-alpha.2) (2023-11-20)


### Bug Fixes

* update k8s client version to 3.0.2 ([130d5e2](https://github.com/gravitee-io/gravitee-node/commit/130d5e2d3c4870c9fc398c3097b9efc4073516be))

# [4.6.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/4.5.0...4.6.0-alpha.1) (2023-11-20)


### Bug Fixes

* clean license file ([2185607](https://github.com/gravitee-io/gravitee-node/commit/2185607244f25337f8a8ce63e5d12849df7475c2))
* reshuffle options in the right place, fix sonar issues ([0c946f5](https://github.com/gravitee-io/gravitee-node/commit/0c946f5ca949f12a6e38ca6407e0e71b66b0e68d))
* update kubernetes client to version 3.0.0 ([0014210](https://github.com/gravitee-io/gravitee-node/commit/0014210f2cd54007939c1dcb7550dfd3840b586e))


### Features

* archi-222 secret provider plugin handling and configuration service ([#244](https://github.com/gravitee-io/gravitee-node/issues/244)) ([8deffbb](https://github.com/gravitee-io/gravitee-node/commit/8deffbb29e113f25a206bee81cf9a5abf8972c7b))
* TCP proxy server factory and options ([85f0123](https://github.com/gravitee-io/gravitee-node/commit/85f0123b667fb5ea0a6259b3d5336d11dde44eec))

# [4.6.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/4.4.0-alpha.5...4.4.0-alpha.6) (2023-11-13)

### Bug Fixes

* reshuffle options in the right place, fix sonar issues ([0e17162](https://github.com/gravitee-io/gravitee-node/commit/0e1716256bcfdbda658c05552033ebdbb049e457))

* clean license file ([a4e25a1](https://github.com/gravitee-io/gravitee-node/commit/a4e25a1750aa14004b9cb37103b684ab54598bd0))

### Features

* add support for Gravitee Pem Registry ([a892edb](https://github.com/gravitee-io/gravitee-node/commit/a892edb5d0ab6e02d7bc855f0349966fe0eb55d6))

* TCP proxy server factory and options ([ee48796](https://github.com/gravitee-io/gravitee-node/commit/ee487963b1fe1e5e16c533a3984bb0586e0abf3f))


# [4.5.0](https://github.com/gravitee-io/gravitee-node/compare/4.4.0...4.5.0) (2023-11-17)


### Features

* allow AE as an enterprise feature ([476165c](https://github.com/gravitee-io/gravitee-node/commit/476165c2e40a83a2cf33f4f7c811b50aeb2487d4))


# [4.4.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/4.4.0-alpha.1...4.4.0-alpha.2) (2023-10-03)


### Bug Fixes

* update kubernetes client to version 3.0.0 ([23ca163](https://github.com/gravitee-io/gravitee-node/commit/23ca1636a4f22ebe6752f94b009c8ac583bc6e59))

# [4.4.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/4.3.1...4.4.0-alpha.1) (2023-10-02)


### Features

* archi-222 secret provider plugin handling and configuration service ([#244](https://github.com/gravitee-io/gravitee-node/issues/244)) ([f5cd242](https://github.com/gravitee-io/gravitee-node/commit/f5cd242260b149933729beea81b56812d82fdd5b))


# [4.4.0](https://github.com/gravitee-io/gravitee-node/compare/4.3.1...4.4.0) (2023-11-09)


### Features

* add support for Gravitee Pem Registry ([d97946a](https://github.com/gravitee-io/gravitee-node/commit/d97946a913820b885d8a887e701b355a4e8e398d))

## [4.3.1](https://github.com/gravitee-io/gravitee-node/compare/4.3.0...4.3.1) (2023-09-14)


### Bug Fixes

* bump gravitee-common to 3.3.3 ([f2363dc](https://github.com/gravitee-io/gravitee-node/commit/f2363dce64377a81d62adfb2c1f9b68ca8030e16))

# [4.3.0](https://github.com/gravitee-io/gravitee-node/compare/4.2.0...4.3.0) (2023-09-13)


### Features

* add AM SFR SMS resource plugin ([fca0429](https://github.com/gravitee-io/gravitee-node/commit/fca04292af31472efc8732789b9dd8cf67545587))

# [4.2.0](https://github.com/gravitee-io/gravitee-node/compare/4.1.0...4.2.0) (2023-09-11)


### Features

* add AM Account Linking policy ([6418962](https://github.com/gravitee-io/gravitee-node/commit/641896252b0b80c6e58500204324f3cd656ae63e))

# [4.1.0](https://github.com/gravitee-io/gravitee-node/compare/4.0.0...4.1.0) (2023-08-21)


### Features

* add license feature for AM ([538d8be](https://github.com/gravitee-io/gravitee-node/commit/538d8be467640badc8f442b779f376c1e3f3ed30))

# [4.0.0](https://github.com/gravitee-io/gravitee-node/compare/3.0.9...4.0.0) (2023-07-17)


### Bug Fixes

* add old feature names to catalog ([255803e](https://github.com/gravitee-io/gravitee-node/commit/255803ecfbcd59023858a30628fdd6acb13d0530))
* avoid npe when evaluating cpu load average ([61c1dd7](https://github.com/gravitee-io/gravitee-node/commit/61c1dd7d16d8b1a983340def654a02bd08eabb61))
* bump gravitee-kubernetes version to 2.0.2 ([7bec0f7](https://github.com/gravitee-io/gravitee-node/commit/7bec0f7eb78849347c6095e108c627bbd969929c))
* bump kubernetes client to 2.0.3 ([51b64c2](https://github.com/gravitee-io/gravitee-node/commit/51b64c299a688ce4b8aa9b6214e417fc25043333))
* **deps:** bump gravitee-plugin to 2.0.0-alpha1 ([850236d](https://github.com/gravitee-io/gravitee-node/commit/850236d2c4cbd9ac49e6b02bf8b92c4b9f845d44))
* license model syntax ([23859ec](https://github.com/gravitee-io/gravitee-node/commit/23859ecb1b3ad36de851f6db7009408ea0c045b2))
* **perf:** This fix rework the ExcludeTagsFilter to reduce a processing time ([4a52c0b](https://github.com/gravitee-io/gravitee-node/commit/4a52c0b4223477b949b106cdf97499a996e39e40))
* restore isFeatureEnabled in license interface ([6fb0dbe](https://github.com/gravitee-io/gravitee-node/commit/6fb0dbef2def0962da6120467b7a001e4a79b1ca))
* tmp enable standalone cache by default ([2fb8064](https://github.com/gravitee-io/gravitee-node/commit/2fb80644f91abe54c0dd1ea68b6f0277a99cffb7))
* update risk-assesment feature name ([ba156e8](https://github.com/gravitee-io/gravitee-node/commit/ba156e8fb1f05f48b62522ca786a2ea54ae13751))
* use simple hazelcast topic ([d45cac8](https://github.com/gravitee-io/gravitee-node/commit/d45cac845f992f83c1db54c5155b7d79f691886a))


### chore

* bump gravitee-parent and gravitee-bom ([e9428a7](https://github.com/gravitee-io/gravitee-node/commit/e9428a73bc14a1164a98c1ccfc805f09cc8c933a))


### Features

* add apim-policy-geoip-filtering ([4464df1](https://github.com/gravitee-io/gravitee-node/commit/4464df1111fafb58d21d852b7acf600f136b61e3))
* add apim-policy-transform-avro-json to catalog ([f9b4cdb](https://github.com/gravitee-io/gravitee-node/commit/f9b4cdbb2bd7163493da58c6931aa4b9f84ca6b6))
* add cluster id on ClusterManager interface ([153c441](https://github.com/gravitee-io/gravitee-node/commit/153c441d8c68babc0cc2cd1e6ce61e2c7dbb96fb))
* add new plugin type for cache manager ([3bf9804](https://github.com/gravitee-io/gravitee-node/commit/3bf9804b0596a70d0df7082c16b81037b5e38284))
* add support for multi-servers ([44af8ff](https://github.com/gravitee-io/gravitee-node/commit/44af8ffd125168fb4b6c4643e17d642d8ed433db))
* add v4 license model and utility services ([498dce9](https://github.com/gravitee-io/gravitee-node/commit/498dce938f683f1a6a17c9fa4add84a6b1189c4a))
* **node-notifier:** supports both javax and jakarta Inject annotation ([a909bff](https://github.com/gravitee-io/gravitee-node/commit/a909bffde7976f9e329c46db39ad787a310c2c56))
* update cluster manager to be compatible with plugin management ([f69ad89](https://github.com/gravitee-io/gravitee-node/commit/f69ad895f8b80a091a196a5e167210a73c154e5a))


### BREAKING CHANGES

* update code to JDK 17
* checking for feature enablement now relies on the NodeLicenseService

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

## [3.0.9](https://github.com/gravitee-io/gravitee-node/compare/3.0.8...3.0.9) (2023-05-25)


### Bug Fixes

* add config to disable keystore watcher ([2e94e9a](https://github.com/gravitee-io/gravitee-node/commit/2e94e9a52706741feb5a640221b753d8b1c5fec1))

## [3.0.8](https://github.com/gravitee-io/gravitee-node/compare/3.0.7...3.0.8) (2023-05-24)


### Bug Fixes

* avoid npe when evaluating cpu load average ([c54bd58](https://github.com/gravitee-io/gravitee-node/commit/c54bd58b71a96fff38c8579403fbe8c2aa123433))

## [3.0.7](https://github.com/gravitee-io/gravitee-node/compare/3.0.6...3.0.7) (2023-05-04)


### Bug Fixes

* license error in community mode ([aeef6fb](https://github.com/gravitee-io/gravitee-node/commit/aeef6fb6405ef15cac9674426368ca37ad707bd3))

## [3.0.6](https://github.com/gravitee-io/gravitee-node/compare/3.0.5...3.0.6) (2023-04-27)


### Bug Fixes

* add 'With' methods to HttpServerConfiguration ([c7210bb](https://github.com/gravitee-io/gravitee-node/commit/c7210bb7d102de5cfcde93fe29ddbd9af6b0a7a7))
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
