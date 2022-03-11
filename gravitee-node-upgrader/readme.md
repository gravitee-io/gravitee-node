There are 2 ways to populate/modify data in the database at the app startup

1- Using Initializer (when the code needs to run on every upgrade)

2- Using Upgrader (When the modification will apply only once)


**How to use:**

1- Add UpgraderConfiguration.java (gravitee-node-upgrader) to your Spring configurations
2- Implement Upgrader or Initializer interfaces (gravitee-node-api) based on your needs
3- Implement UpgraderRepository (gravitee-node-api) if you want to use the upgrader.  But this is not needed for the Initializer.
4- Add one of both of the following classes to the list of your Node components

`public List<Class<? extends LifecycleComponent>> components() {

    if (upgradeMode) {
        components.add(InitializerService.class);
        components.add(UpgraderService.class);
    }

}
`


where upgradeMode is

`@Value("${upgrade.mode:true}")
private boolean upgradeMode;`