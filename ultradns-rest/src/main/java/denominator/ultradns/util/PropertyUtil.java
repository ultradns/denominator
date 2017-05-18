package denominator.ultradns.util;

import denominator.ultradns.exception.UltraDNSRestException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This will load application properties from application configuration file.
 */
public final class PropertyUtil {

    private static Properties applicationProperties;
    private static InputStream input = null;
    private static final Logger LOGGER = Logger.getLogger(PropertyUtil.class);

    private PropertyUtil() { }

    /**
     * Load the property key & value from configuration file & load it in to Properties object.
     *
     * @return Properties Object
     */
    public static Properties loadProperties() {
        try {
            applicationProperties = new Properties();
            input = PropertyUtil.class.getResourceAsStream("/application.properties");
            if (input == null) {
                throw new UltraDNSRestException("Unable to load application properties file !!", -1);
            }
            applicationProperties.load(input);
            return applicationProperties;
        } catch (IOException e) {
            LOGGER.error("Error while loading application properties file !! Please check property configuration.");
            throw new UltraDNSRestException(e.getMessage(), -1);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error("Error while closing application properties Resource Stream !!");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * This will either get the existing object or create a new object of Properties.
     * @return Properties Object
     */
    private static Properties getProperties() {
        if (applicationProperties == null) {
            applicationProperties = loadProperties();
        }
        return applicationProperties;
    }

    /**
     * Will return property name based on property value.
     *
     * @param propertyName name of the property
     * @return value of the property
     */
    public static String getProperty(String propertyName) {
        String propertyValue = getProperties().getProperty(propertyName);
        if (propertyValue == null) {
            throw new UltraDNSRestException("Could not load property with name " + propertyName
                    + " !! Please check property configuration.", -1);
        }
        return propertyValue;
    }
}
