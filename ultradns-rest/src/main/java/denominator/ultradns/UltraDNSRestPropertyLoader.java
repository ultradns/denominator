package denominator.ultradns;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class UltraDNSRestPropertyLoader {

    private static Properties applicationProperties;
    private static boolean isPropertyLoaded = false;
    private static final Logger logger = Logger.getLogger(UltraDNSRestPropertyLoader.class);

    public static void loadProperties() {
        applicationProperties = new Properties();
        try {
            applicationProperties.load(UltraDNSRestPropertyLoader.class.getResourceAsStream("/application.properties"));
            isPropertyLoaded = true;
        } catch (Exception e) {
            logger.info("Error while loading application properties file !! Please check property configuration.");
            throw new UltraDNSRestException(e.getMessage(), -1);
        }
    }

    public static String getProperty(String propertyName) {
        if (!isPropertyLoaded) {
            loadProperties();
        }
        return applicationProperties.getProperty(propertyName);
    }
}