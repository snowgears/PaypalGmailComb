package com.tannerembry.metrics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigPropertyValues {
    private String endpoint;
    private String client_id;
    private String client_secret;
    private InputStream inputStream;

    public ConfigPropertyValues() throws IOException {

        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            // get the property value and store it
            this.endpoint = endpoint = prop.getProperty("endpoint");
            this.client_id = client_id = prop.getProperty("client_id");
            this.client_secret = client_secret = prop.getProperty("client_secret");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            inputStream.close();
        }
    }

    public String getEndpoint(){
        return endpoint;
    }

    public String getClientID(){
        return client_id;
    }

    public String getClientSecret(){
        return client_secret;
    }
}
