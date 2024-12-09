package com.battle.ai.battle.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties properties = new Properties();
    
    public static void loadConfig(String configFile) {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                System.out.println("找不到配置文件: " + configFile);
                return;
            }
            properties.load(input);
            
            // 更新战斗配置
            BattleConfig config = BattleConfig.getInstance();
            config.setMaxTurns(getIntProperty("maxTurns", 100));
            config.setUpdateInterval(getLongProperty("updateInterval", 100));
            config.setAiUpdateInterval(getIntProperty("aiUpdateInterval", 5));
            config.setDebugMode(getBooleanProperty("debugMode", false));
            config.setGameSpeed(getDoubleProperty("gameSpeed", 1.0));
            
        } catch (IOException e) {
            System.err.println("加载配置文件失���: " + e.getMessage());
        }
    }
    
    private static int getIntProperty(String key, int defaultValue) {
        return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
    }
    
    private static long getLongProperty(String key, long defaultValue) {
        return Long.parseLong(properties.getProperty(key, String.valueOf(defaultValue)));
    }
    
    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }
    
    private static double getDoubleProperty(String key, double defaultValue) {
        return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
    }
} 