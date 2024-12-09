package com.battle.ai.battle.monitoring;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerformanceMonitor {
    private static long lastUpdateTime = System.currentTimeMillis();
    private static int frameCount = 0;
    private static double fps = 0;
    
    public static void update() {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= 1000) {
            fps = frameCount * 1000.0 / (currentTime - lastUpdateTime);
            frameCount = 0;
            lastUpdateTime = currentTime;
            log.debug("[性能] FPS: %.2f%n", fps);
        }
    }
    
    public static double getFPS() {
        return fps;
    }
} 