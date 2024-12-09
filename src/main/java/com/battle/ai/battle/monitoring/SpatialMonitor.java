package com.battle.ai.battle.monitoring;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpatialMonitor {
    private static long queryCount = 0;
    private static long totalQueryTime = 0;
    private static long maxQueryTime = 0;
    
    public static void recordQuery(long queryTime) {
        queryCount++;
        totalQueryTime += queryTime;
        maxQueryTime = Math.max(maxQueryTime, queryTime);
        
        if (queryCount % 1000 == 0) {
            printStats();
        }
    }
    
    public static void printStats() {
        log.debug("[空间查询统计] 总查询次数: %d, 平均查询时间: %.2fms, 最大查询时间: %dms%n",
            queryCount,
            totalQueryTime / (double)queryCount,
            maxQueryTime);
    }
    
    public static void reset() {
        queryCount = 0;
        totalQueryTime = 0;
        maxQueryTime = 0;
    }
} 