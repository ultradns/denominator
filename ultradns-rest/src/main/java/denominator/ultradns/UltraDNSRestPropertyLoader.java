package denominator.ultradns;

import org.apache.log4j.Logger;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class UltraDNSRestPropertyLoader {

    private static Properties applicationProperties;
    private static InputStream input = null;
    private static final Logger logger = Logger.getLogger(UltraDNSRestPropertyLoader.class);

    private UltraDNSRestPropertyLoader() {}

    public static Properties loadProperties() {
        try {
            applicationProperties = new Properties();
            input = UltraDNSRestPropertyLoader.class.getResourceAsStream("/application.properties");
            if (input == null) {
                throw new UltraDNSRestException("Unable to load application properties file !!", -1);
            }
            applicationProperties.load(input);
            return applicationProperties;
        } catch (IOException e) {
            logger.error("Error while loading application properties file !! Please check property configuration.");
            throw new UltraDNSRestException(e.getMessage(), -1);
        } finally{
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Error while closing application properties Resource Stream !!");
                    e.printStackTrace();
                }
            }
        }
    }

    private static Properties getProperties() {
        if(applicationProperties == null) {
            applicationProperties = loadProperties();
        }
        return applicationProperties;
    }

    public static String getProperty(String propertyName) {
        String propertyValue = getProperties().getProperty(propertyName);
        if (propertyValue == null) {
            throw new UltraDNSRestException("Could not load property with name " + propertyName + " !! Please check property configuration.", -1);
        }
        return propertyValue;
    }
}