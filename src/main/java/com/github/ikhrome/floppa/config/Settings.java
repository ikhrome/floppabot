package com.github.ikhrome.floppa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {
    public static String get(String name) throws IOException {
        Properties settings = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream("config.properties");
        settings.load(inputStream);
        return settings.getProperty(name);
    }
}
