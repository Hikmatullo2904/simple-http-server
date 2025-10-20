package uz.hikmatullo.httpserver.config;

import com.fasterxml.jackson.databind.JsonNode;
import uz.hikmatullo.httpserver.exception.HttpConfigurationException;
import uz.hikmatullo.httpserver.util.JsonParser;

import java.io.FileReader;
import java.io.IOException;

public class ConfigManager {
    private static volatile ConfigManager configManager;
    private static Configuration configuration;
    private ConfigManager() {

    }

    public static ConfigManager getInstance() {
        if (configManager == null) {
            synchronized (ConfigManager.class) {
                if (configManager == null) {
                    configManager = new ConfigManager();
                }
            }
        }
        return configManager;
    }

    /*
    * we load configuration class using given filepath. we read data from filepath and covert it
    * into our configuration file.
    * */
    public void loadConfiguration(String filePath) {
        try(FileReader fileReader = new FileReader(filePath)) {
            StringBuffer sb = new StringBuffer();
            int i ;
            while( (i = fileReader.read()) != -1 ) {
                sb.append((char) i);
            }
            JsonNode json = JsonParser.parse(sb.toString());
            configuration = JsonParser.fromJson(json, Configuration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Configuration getCurrentConfiguration() {
        if (configuration == null) {
            throw new HttpConfigurationException("No Current Configuration set");
        }
        return configuration;
    }
}
