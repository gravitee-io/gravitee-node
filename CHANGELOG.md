# [7.0.0-alpha.13](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.12...7.0.0-alpha.13) (2024-12-17)


### Features

* bump gravitee-bom ([0f1e87e](https://github.com/gravitee-io/gravitee-node/commit/0f1e87e542e4597010e8f18473ca3c7d881d8678))

# [7.0.0-alpha.12](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.11...7.0.0-alpha.12) (2024-12-16)


### Bug Fixes

* add more tcp client options ([8b30be3](https://github.com/gravitee-io/gravitee-node/commit/8b30be30b1061b338f455ebe8cc1d1727394856a))

# [7.0.0-alpha.11](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.10...7.0.0-alpha.11) (2024-12-14)


### Bug Fixes

* bump to latest secret-api ([4b024ff](https://github.com/gravitee-io/gravitee-node/commit/4b024ffc3fabd1318e8b9b71fee9389d51f5437a))

# [7.0.0-alpha.10](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.9...7.0.0-alpha.10) (2024-12-13)


### Features

* allow mock secret provider to get secrets updated ([0130c8c](https://github.com/gravitee-io/gravitee-node/commit/0130c8c3dee79d9932c5709b1e1702b468c01079))

# [7.0.0-alpha.9](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.8...7.0.0-alpha.9) (2024-12-12)


### Features

* move classes and adapt to gravitee-secret-api ([#384](https://github.com/gravitee-io/gravitee-node/issues/384)) ([f21f095](https://github.com/gravitee-io/gravitee-node/commit/f21f095d1299be5b8773003ccfe35fd29f11a1ea))

# [7.0.0-alpha.8](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.7...7.0.0-alpha.8) (2024-12-11)


### Features

* allow to configure unknown tier & pack in the license ([2af3f32](https://github.com/gravitee-io/gravitee-node/commit/2af3f32604823257ab2650286297195512ca62c4))

# [7.0.0-alpha.7](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.6...7.0.0-alpha.7) (2024-12-09)


### Features

* add new pack of features for kafka gateway ([6cfc1c4](https://github.com/gravitee-io/gravitee-node/commit/6cfc1c4db9733f16c22b653da1a31d442a81da5a))

# [7.0.0-alpha.6](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.5...7.0.0-alpha.6) (2024-12-03)


### Bug Fixes

* fix mock provider pom ([c60bb86](https://github.com/gravitee-io/gravitee-node/commit/c60bb8643538568a07e1a8e7be0d1bc353b027b3))

# [7.0.0-alpha.5](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.4...7.0.0-alpha.5) (2024-11-26)


### Features

* mock secret provider + update to secret core objects ([a2a2cf1](https://github.com/gravitee-io/gravitee-node/commit/a2a2cf1a51f95db4637bf4bf065b168d1a129e28))

# [7.0.0-alpha.4](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.3...7.0.0-alpha.4) (2024-11-21)


### Features

* add metadata property to NodeInfos ([c2098b0](https://github.com/gravitee-io/gravitee-node/commit/c2098b070efe43d59828f4a88dad42025af332ea))

# [7.0.0-alpha.3](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.2...7.0.0-alpha.3) (2024-11-13)


### Features

* share underlying span exporter ([e8bc84d](https://github.com/gravitee-io/gravitee-node/commit/e8bc84df7227afbc992dfa264757980b49fefcc0))

# [7.0.0-alpha.2](https://github.com/gravitee-io/gravitee-node/compare/7.0.0-alpha.1...7.0.0-alpha.2) (2024-11-13)


### Features

* allow to manually inject span context to carrier ([07adcde](https://github.com/gravitee-io/gravitee-node/commit/07adcde2f8c454fcb418cb776fcab9c222a9791f))

# [7.0.0-alpha.1](https://github.com/gravitee-io/gravitee-node/compare/6.5.0...7.0.0-alpha.1) (2024-11-05)


### Features

* add OpenTelemetry feature into Gravitee Node ([1c377c5](https://github.com/gravitee-io/gravitee-node/commit/1c377c5eef74205d4d51b33f562307ebbdb80d87))


### BREAKING CHANGES

* Tracing plugin has been removed and is now embedded inside node framework

# [6.5.0](https://github.com/gravitee-io/gravitee-node/compare/6.4.6...6.5.0) (2024-10-17)


### Features

* allow unsecured tcp server ([09bbb47](https://github.com/gravitee-io/gravitee-node/commit/09bbb4707b4c1de358e6fd9d53488f457ad52238))

## [6.4.6](https://github.com/gravitee-io/gravitee-node/compare/6.4.5...6.4.6) (2024-10-09)


### Bug Fixes

* declare all modules in dependency management ([c7874ed](https://github.com/gravitee-io/gravitee-node/commit/c7874ed6583e92712e89d7676a560c003176e543))
* remove unused dependency ([8b4e034](https://github.com/gravitee-io/gravitee-node/commit/8b4e0341c03784a539a253a63aed5515cbf93436))

## [6.4.5](https://github.com/gravitee-io/gravitee-node/compare/6.4.4...6.4.5) (2024-10-04)


### Bug Fixes

* **metrics:** hold strong reference to probes results in micrometer gauge ([e17fa16](https://github.com/gravitee-io/gravitee-node/commit/e17fa16f282f95678dc80c85cb8855e677cfe674))

## [6.4.4](https://github.com/gravitee-io/gravitee-node/compare/6.4.3...6.4.4) (2024-09-24)


### Bug Fixes

* make redis cache pool settings configurable ([5480516](https://github.com/gravitee-io/gravitee-node/commit/5480516936ff2db6414e26d0c66a4ce38ba123af))
* removed stacktrace from periodic error message ([7633706](https://github.com/gravitee-io/gravitee-node/commit/763370680f277ceb39f9940bc625a678a480381f))

## [6.4.3](https://github.com/gravitee-io/gravitee-node/compare/6.4.2...6.4.3) (2024-09-12)


### Bug Fixes

* return empty array of accepted issuers in trust manager ([7db0f15](https://github.com/gravitee-io/gravitee-node/commit/7db0f15fca5d734bc97ef8eac0267e4cd69085b0))

## [6.4.2](https://github.com/gravitee-io/gravitee-node/compare/6.4.1...6.4.2) (2024-09-03)


### Bug Fixes

* return unhealthy result when a filtered prob is not found ([2f585f2](https://github.com/gravitee-io/gravitee-node/commit/2f585f2e3ead2b0ef917bd327193ca68f64e7b62))

## [6.4.1](https://github.com/gravitee-io/gravitee-node/compare/6.4.0...6.4.1) (2024-08-29)


### Bug Fixes

* **deps:** update dependency com.hazelcast:hazelcast to v5.5.0 ([309e380](https://github.com/gravitee-io/gravitee-node/commit/309e3802432bb41ac574730e9b927518e113225d))
* **deps:** update gravitee-plugin.version to v4.1.0 ([893b4d9](https://github.com/gravitee-io/gravitee-node/commit/893b4d9ae95c53491e02732a8364e5db13ca1a79))
* missing default builder on http client options ([0cbdab7](https://github.com/gravitee-io/gravitee-node/commit/0cbdab7c1b8b6955663bfa3b048c1a1dcd4a79df))

# [6.4.0](https://github.com/gravitee-io/gravitee-node/compare/6.3.0...6.4.0) (2024-08-22)


### Features

* **http2:** allows changing http2 multiplexing ([9924840](https://github.com/gravitee-io/gravitee-node/commit/992484086954daf9adb11773146d386cf9cd2028))

# [6.3.0](https://github.com/gravitee-io/gravitee-node/compare/6.2.0...6.3.0) (2024-08-13)


### Features

* add capability to retrieve product name early during startup ([91dc359](https://github.com/gravitee-io/gravitee-node/commit/91dc3599edd1dd04b2179b6b36c7b11a7378f03a))

# [6.2.0](https://github.com/gravitee-io/gravitee-node/compare/6.1.0...6.2.0) (2024-08-02)


### Bug Fixes

* update parent pom version in redis cache implementation ([a08026f](https://github.com/gravitee-io/gravitee-node/commit/a08026f2e99ccd83567eb22662d9008f52a11df5))


### Features

* addition of Redis as cache implementation ([29964e1](https://github.com/gravitee-io/gravitee-node/commit/29964e1f58debddf01ba462d800bf9bd33c7d37a))

# [6.1.0](https://github.com/gravitee-io/gravitee-node/compare/6.0.4...6.1.0) (2024-08-01)


### Features

* allow merge of several gravitee.yml ([8effe2e](https://github.com/gravitee-io/gravitee-node/commit/8effe2e8f641a21169e09a1b5d741959ef079d65))

## [6.0.4](https://github.com/gravitee-io/gravitee-node/compare/6.0.3...6.0.4) (2024-07-26)


### Bug Fixes

* apply eviction policy only when the limit size is defined ([02ab72f](https://github.com/gravitee-io/gravitee-node/commit/02ab72fbd8a3a17f85380ba4e1069d40216d18c6))
* properly implement evict method for hazelcast cache ([a0ff2c5](https://github.com/gravitee-io/gravitee-node/commit/a0ff2c52c392826277da136435995b1c5f809389))

## [6.0.3](https://github.com/gravitee-io/gravitee-node/compare/6.0.2...6.0.3) (2024-07-26)


### Bug Fixes

* ensure default hazelcast instance names are different ([fe3add1](https://github.com/gravitee-io/gravitee-node/commit/fe3add1fe757f039ca47b720481ce95f44e0b4ee))

## [6.0.2](https://github.com/gravitee-io/gravitee-node/compare/6.0.1...6.0.2) (2024-07-25)


### Bug Fixes

* adding javadoc ([10b1e26](https://github.com/gravitee-io/gravitee-node/commit/10b1e2618ed0cde4405b200dc478f6e36ddfe157))

## [6.0.1](https://github.com/gravitee-io/gravitee-node/compare/6.0.0...6.0.1) (2024-07-25)


### Bug Fixes

* add default implementation for MemberListener ([b7db7be](https://github.com/gravitee-io/gravitee-node/commit/b7db7bedaa03d9cf981bd756354a55895fdbe28e))

# [6.0.0](https://github.com/gravitee-io/gravitee-node/compare/5.21.1...6.0.0) (2024-07-25)


### Bug Fixes

* NPE using rxPut and rxCompute ([3bb413a](https://github.com/gravitee-io/gravitee-node/commit/3bb413ab3b8a84e2accdfba9dd6003d87943c7b2))


### BREAKING CHANGES

* rxPut and rxCompute method signatures have changed
 from Single to Maybe. An empty Maybe means 'null' in the blocking world.

https://gravitee.atlassian.net/browse/ARCHI-392

## [5.21.1](https://github.com/gravitee-io/gravitee-node/compare/5.21.0...5.21.1) (2024-07-25)


### Bug Fixes

* revert back hazelcast new instance on cache plugin ([d3dc47c](https://github.com/gravitee-io/gravitee-node/commit/d3dc47c5cd773ab4e37b46f381c6d164e091fb85))
* wait for current queue polling to finish when removing listener ([cfd43c1](https://github.com/gravitee-io/gravitee-node/commit/cfd43c1a468582e555cea794d6516f3343932893))

# [5.21.0](https://github.com/gravitee-io/gravitee-node/compare/5.20.0...5.21.0) (2024-07-24)


### Bug Fixes

* add condition on monitoring event handler to avoid npe when cluster is not defined ([9655f7d](https://github.com/gravitee-io/gravitee-node/commit/9655f7df999ade777175d7a0ed2b99b2cf3868dd))
* add missing node cache service from node components ([7f150a4](https://github.com/gravitee-io/gravitee-node/commit/7f150a41f6e5f00746c1042cef21c8ea07486bae))
* call super.dostop and dostart no cluster and cache manager ([a01b077](https://github.com/gravitee-io/gravitee-node/commit/a01b0772f6896112e3d522f9faedad6868e40938))
* implement work around when hazelcast config is shared between cache and cluster plugins ([1a383c2](https://github.com/gravitee-io/gravitee-node/commit/1a383c21380d0b4c6046f9e7ceb5482e68059cd2))
* remove unnecessary node autowired ([2200561](https://github.com/gravitee-io/gravitee-node/commit/220056177d74d4d372b4bb5800eca97e33e8fde8))


### Features

* add node information to hazelcast member attribute ([7e39f06](https://github.com/gravitee-io/gravitee-node/commit/7e39f062a9d1fff68d0571fab515555f1f8f1d73))

# [5.20.0](https://github.com/gravitee-io/gravitee-node/compare/5.19.0...5.20.0) (2024-07-17)


### Features

* allow to unregister a management endpoint ([57facdb](https://github.com/gravitee-io/gravitee-node/commit/57facdb496aaa15e23c83f81c3da72dab6575f5a))

# [5.19.0](https://github.com/gravitee-io/gravitee-node/compare/5.18.3...5.19.0) (2024-06-26)


### Features

* add AM enterprise certificate AWS plugin ([3ef8d85](https://github.com/gravitee-io/gravitee-node/commit/3ef8d85a08a8205583c0655ef6076b24ecb415c7))

## [5.18.3](https://github.com/gravitee-io/gravitee-node/compare/5.18.2...5.18.3) (2024-06-24)


### Bug Fixes

* improve upgrade fwk ([a94c147](https://github.com/gravitee-io/gravitee-node/commit/a94c147d01dcd7850be5e10ad23bd5fafd0101fa))

## [5.18.2](https://github.com/gravitee-io/gravitee-node/compare/5.18.1...5.18.2) (2024-06-21)


### Bug Fixes

* added one-second delay before repeat to avoid log flooding ([220d7e0](https://github.com/gravitee-io/gravitee-node/commit/220d7e072768031f945ada016551e641a7ccc8ec))

## [5.18.1](https://github.com/gravitee-io/gravitee-node/compare/5.18.0...5.18.1) (2024-06-19)


### Bug Fixes

* configure repo to release plugins in download website ([0c54f7c](https://github.com/gravitee-io/gravitee-node/commit/0c54f7c3a29b9ea8f0867fd11ff295c98a3fc053))

# [5.18.0](https://github.com/gravitee-io/gravitee-node/compare/5.17.0...5.18.0) (2024-06-17)


### Features

* add secret provider aws to license model ([e1abcc2](https://github.com/gravitee-io/gravitee-node/commit/e1abcc239015eebf829fc18beaffb2b3002d306b))

# [5.17.0](https://github.com/gravitee-io/gravitee-node/compare/5.16.2...5.17.0) (2024-06-17)


### Features

* add oas validation policy feature to license ([9a2e3f8](https://github.com/gravitee-io/gravitee-node/commit/9a2e3f82ec65d1907a8a086225abba803d7c6d62))

## [5.16.2](https://github.com/gravitee-io/gravitee-node/compare/5.16.1...5.16.2) (2024-06-17)


### Bug Fixes

* recursive method call may lead to stack overflow exception ([2640c77](https://github.com/gravitee-io/gravitee-node/commit/2640c77dd3c8a8cc8eed0d9ad278c32055a0bd8f))

## [5.16.1](https://github.com/gravitee-io/gravitee-node/compare/5.16.0...5.16.1) (2024-06-14)


### Bug Fixes

* override equals and hashcode for SecretLocation ([401301c](https://github.com/gravitee-io/gravitee-node/commit/401301c0d91491ee0b05b1fad65c53b1ad6f67c5))

# [5.16.0](https://github.com/gravitee-io/gravitee-node/compare/5.15.0...5.16.0) (2024-06-14)


### Features

* reduce pressure on db due to health and monitoring ([e10d8a8](https://github.com/gravitee-io/gravitee-node/commit/e10d8a89488e6f090f47038e46d1b9e662ec81a9))

# [5.15.0](https://github.com/gravitee-io/gravitee-node/compare/5.14.7...5.15.0) (2024-06-13)


### Features

* add azure service bus feature to license ([6a54bba](https://github.com/gravitee-io/gravitee-node/commit/6a54bba6ba6be1c072861bf77fd972630b253393))

## [5.14.7](https://github.com/gravitee-io/gravitee-node/compare/5.14.6...5.14.7) (2024-06-10)


### Bug Fixes

* remove misleading options in vertx client factory ([915cd5a](https://github.com/gravitee-io/gravitee-node/commit/915cd5aca07aac9cbe6874a99df9c38697f073b9))

## [5.14.6](https://github.com/gravitee-io/gravitee-node/compare/5.14.5...5.14.6) (2024-06-06)


### Bug Fixes

* add AM HTTP resource plugin ([35ae431](https://github.com/gravitee-io/gravitee-node/commit/35ae4313ee31564b8b9ec7869f34a760aee72c4b))

## [5.14.5](https://github.com/gravitee-io/gravitee-node/compare/5.14.4...5.14.5) (2024-06-04)


### Bug Fixes

* **deps:** update dependency io.gravitee:gravitee-bom to v8.0.4 ([35858b3](https://github.com/gravitee-io/gravitee-node/commit/35858b3c3d4024de2d81984d334d4b909813d22f))
* **deps:** update dependency io.gravitee:gravitee-parent to v22.0.31 ([c5e9167](https://github.com/gravitee-io/gravitee-node/commit/c5e9167fce69da6c45a6705164f849417624d785))

## [5.14.4](https://github.com/gravitee-io/gravitee-node/compare/5.14.3...5.14.4) (2024-05-20)


### Bug Fixes

* add Interops SP plugins ([58b0389](https://github.com/gravitee-io/gravitee-node/commit/58b0389068da0f3f46825b7276696ae7f93e9f3a))

## [5.14.3](https://github.com/gravitee-io/gravitee-node/compare/5.14.2...5.14.3) (2024-05-13)


### Bug Fixes

* **deps:** update dependency com.github.ben-manes.caffeine:caffeine to v3.1.8 ([6a7d76a](https://github.com/gravitee-io/gravitee-node/commit/6a7d76ae727c4230f9c1d5e87464fd08eac08d43))
* pom.xml to reduce vulnerabilities ([489a9c2](https://github.com/gravitee-io/gravitee-node/commit/489a9c24a267ed40d0c02827184f39b25177fac2))


### Reverts

* Revert "fix(alpn): configure alpn option even if not secured" ([22e3a9c](https://github.com/gravitee-io/gravitee-node/commit/22e3a9cb299db9e379207593c9cd64bff4086dda))

## [5.14.2](https://github.com/gravitee-io/gravitee-node/compare/5.14.1...5.14.2) (2024-05-06)


### Bug Fixes

* missing Builder.Default in SslOptions on new property hostnameVerificationAlgorithm ([1aa3177](https://github.com/gravitee-io/gravitee-node/commit/1aa3177b263f27e6811d45c380da6a9c26891108))

## [5.14.1](https://github.com/gravitee-io/gravitee-node/compare/5.14.0...5.14.1) (2024-05-03)


### Bug Fixes

* **deps:** upgrade gravitee-bom to 8.x ([016068f](https://github.com/gravitee-io/gravitee-node/commit/016068f03992f232183fec8bc7818545456b78e2))

# [5.14.0](https://github.com/gravitee-io/gravitee-node/compare/5.13.0...5.14.0) (2024-05-02)


### Features

* add orange contact everyone plugin ([9dead4e](https://github.com/gravitee-io/gravitee-node/commit/9dead4eb8cbfb7d60bacd6e53517b50b0a3f37b5))

# [5.13.0](https://github.com/gravitee-io/gravitee-node/compare/5.12.4...5.13.0) (2024-04-30)


### Features

* add new option for TcpClient to manage hostname verification algorithm ([dc91f93](https://github.com/gravitee-io/gravitee-node/commit/dc91f9379eee8674c2a5f9c87cf7c8e3edd6d26e))

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
