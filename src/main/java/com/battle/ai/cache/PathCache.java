package com.battle.ai.cache;


import com.battle.ai.Node;

import java.util.*;

public class PathCache {
    private static final int MAX_CACHE_SIZE = 1000;
    private static final Map<PathKey, List<Node>> cache = new LinkedHashMap<PathKey, List<Node>>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<PathKey, List<Node>> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    
    public static List<Node> getPath(Node start, Node end) {
        return cache.get(new PathKey(start, end));
    }
    
    public static void cachePath(Node start, Node end, List<Node> path) {
        cache.put(new PathKey(start, end), new ArrayList<>(path));
    }
    
    private static class PathKey {
        private final Node start;
        private final Node end;
        
        PathKey(Node start, Node end) {
            this.start = start;
            this.end = end;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathKey pathKey = (PathKey) o;
            return start.equals(pathKey.start) && end.equals(pathKey.end);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
} 