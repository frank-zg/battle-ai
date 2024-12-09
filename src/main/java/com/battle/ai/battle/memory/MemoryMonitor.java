package com.battle.ai.battle.memory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryMonitor {
    private static final Runtime runtime = Runtime.getRuntime();
    private static final long MEMORY_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static long lastGCTime = System.currentTimeMillis();
    private static final long GC_INTERVAL = 60000; // 1分钟
    
    public static void checkMemory() {
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long currentTime = System.currentTimeMillis();
        
        if (usedMemory > MEMORY_THRESHOLD || currentTime - lastGCTime > GC_INTERVAL) {
            System.gc();
            lastGCTime = currentTime;
            log.debug("[内存] 已清理 - 使用: %.2fMB, 总计: %.2fMB, 空闲: %.2fMB%n",
                usedMemory / 1024.0 / 1024.0,
                runtime.totalMemory() / 1024.0 / 1024.0,
                runtime.freeMemory() / 1024.0 / 1024.0);
        }
    }
} 