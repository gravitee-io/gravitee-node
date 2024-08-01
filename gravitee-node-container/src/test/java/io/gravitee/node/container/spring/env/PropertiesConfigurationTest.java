package io.gravitee.node.container.spring.env;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class PropertiesConfigurationTest {

    @Test
    void should_load_gravitee_yaml() throws IOException {
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        System.setProperty(PropertiesConfiguration.GRAVITEE_CONFIGURATION, "src/test/resources/conf/gravitee.yml");
        Properties properties = propertiesConfiguration.graviteeProperties();
        assertThat(properties.getProperty("person.name")).isEqualTo("Hank");
        assertThat(properties.getProperty("person.age")).isEqualTo("42");
        assertThat(properties.getProperty("person.single")).isEqualTo("false");
        assertThat(properties.getProperty("person.addresses[0].street")).isEqualTo("Venice Beach");
        assertThat(properties.getProperty("person.addresses[0].town")).isEqualTo("Los Angeles");
        assertThat(properties.getProperty("person.addresses[1].street")).isEqualTo("1st Avenue");
        assertThat(properties.getProperty("person.addresses[1].town")).isEqualTo("Hollywood");
        assertThat(properties.getProperty("person.emails[0]")).isEqualTo("hanky@gmail.com");
        assertThat(properties.getProperty("creditCards[0]")).isEqualTo("visa");
    }

    @Test
    void should_load_two_gravitee_yaml() throws IOException {
        PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
        System.setProperty(
            PropertiesConfiguration.GRAVITEE_CONFIGURATION,
            "src/test/resources/conf/gravitee.yml,src/test/resources/conf/gravitee-override.yml"
        );
        Properties properties = propertiesConfiguration.graviteeProperties();
        assertThat(properties.getProperty("person.name")).isEqualTo("Hank Moody");
        assertThat(properties.getProperty("person.age")).isEqualTo("42");
        assertThat(properties.getProperty("person.single")).isEqualTo("false");
        assertThat(properties.getProperty("person.emails[0]")).isEqualTo("hanky@hotmail.com");
        assertThat(properties.getProperty("person.addresses[0].street")).isEqualTo("Venice Beach");
        assertThat(properties.getProperty("person.addresses[0].town")).isEqualTo("Los Angeles");
        assertThat(properties.getProperty("person.addresses[1].street")).isEqualTo("1st Avenue");
        assertThat(properties.getProperty("person.addresses[1].town")).isEqualTo("Hollywood, LA");
        assertThat(properties.getProperty("person.addresses[2].street")).isEqualTo("1, Broadway");
        assertThat(properties.getProperty("person.addresses[2].town")).isEqualTo("NY");
        assertThat(properties.getProperty("vehicle.kind")).isEqualTo("911");
        assertThat(properties.getProperty("vehicle.brand")).isEqualTo("Porsche");
        assertThat(properties.getProperty("creditCards[0]")).isEqualTo("visa");
        assertThat(properties.getProperty("creditCards[1]")).isEqualTo("mastercard");
    }
}
