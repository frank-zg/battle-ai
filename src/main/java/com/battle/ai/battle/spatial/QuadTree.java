package com.battle.ai.battle.spatial;

import com.battle.ai.Node;
import com.battle.ai.battle.Unit;
import com.battle.ai.battle.monitoring.SpatialMonitor;

import java.util.ArrayList;
import java.util.List;

public class QuadTree {
    private static final int MAX_OBJECTS = 10;
    private static final int MAX_LEVELS = 5;
    
    private int level;
    private List<Unit> units;
    private Rectangle bounds;
    private QuadTree[] nodes;
    
    public QuadTree(int level, Rectangle bounds) {
        this.level = level;
        this.bounds = bounds;
        this.units = new ArrayList<>();
        this.nodes = new QuadTree[4];
    }
    
    public void clear() {
        units.clear();
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] != null) {
                nodes[i].clear();
                nodes[i] = null;
            }
        }
    }
    
    private void split() {
        int subWidth = (int)(bounds.width / 2);
        int subHeight = (int)(bounds.height / 2);
        int x = (int)bounds.x;
        int y = (int)bounds.y;
        
        nodes[0] = new QuadTree(level + 1, new Rectangle(x + subWidth, y, subWidth, subHeight));
        nodes[1] = new QuadTree(level + 1, new Rectangle(x, y, subWidth, subHeight));
        nodes[2] = new QuadTree(level + 1, new Rectangle(x, y + subHeight, subWidth, subHeight));
        nodes[3] = new QuadTree(level + 1, new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight));
    }
    
    private int getIndex(Unit unit) {
        Node pos = unit.getPosition();
        int index = -1;
        double verticalMidpoint = bounds.x + bounds.width / 2;
        double horizontalMidpoint = bounds.y + bounds.height / 2;
        
        boolean topQuadrant = pos.getY() < horizontalMidpoint;
        boolean bottomQuadrant = pos.getY() > horizontalMidpoint;
        
        if (pos.getX() < verticalMidpoint) {
            if (topQuadrant) {
                index = 1;
            } else if (bottomQuadrant) {
                index = 2;
            }
        } else if (pos.getX() > verticalMidpoint) {
            if (topQuadrant) {
                index = 0;
            } else if (bottomQuadrant) {
                index = 3;
            }
        }
        
        return index;
    }
    
    public void insert(Unit unit) {
        if (nodes[0] != null) {
            int index = getIndex(unit);
            if (index != -1) {
                nodes[index].insert(unit);
                return;
            }
        }
        
        units.add(unit);
        
        if (units.size() > MAX_OBJECTS && level < MAX_LEVELS) {
            if (nodes[0] == null) {
                split();
            }
            
            int i = 0;
            while (i < units.size()) {
                int index = getIndex(units.get(i));
                if (index != -1) {
                    nodes[index].insert(units.remove(i));
                } else {
                    i++;
                }
            }
        }
    }
    
    public List<Unit> retrieve(List<Unit> returnUnits, Rectangle range) {
        long startTime = System.currentTimeMillis();
        
        try {
            doRetrieve(returnUnits, range);
            return returnUnits;
        } finally {
            long queryTime = System.currentTimeMillis() - startTime;
            SpatialMonitor.recordQuery(queryTime);
        }
    }
    
    private void doRetrieve(List<Unit> returnUnits, Rectangle range) {
        int index = getQuadrant(range);
        if (index != -1 && nodes[0] != null) {
            nodes[index].doRetrieve(returnUnits, range);
        } else if (nodes[0] != null) {
            for (QuadTree node : nodes) {
                if (node != null && node.bounds.intersects(range)) {
                    node.doRetrieve(returnUnits, range);
                }
            }
        }
        
        // 使用快速过滤
        for (Unit unit : units) {
            if (isInRange(unit, range)) {
                returnUnits.add(unit);
            }
        }
    }
    
    private boolean isInRange(Unit unit, Rectangle range) {
        Node pos = unit.getPosition();
        return pos.getX() >= range.x && pos.getX() <= range.x + range.width &&
               pos.getY() >= range.y && pos.getY() <= range.y + range.height;
    }
    
    private int getQuadrant(Rectangle range) {
        double verticalMidpoint = bounds.x + bounds.width / 2;
        double horizontalMidpoint = bounds.y + bounds.height / 2;
        
        boolean topQuadrant = range.y < horizontalMidpoint && range.y + range.height < horizontalMidpoint;
        boolean bottomQuadrant = range.y > horizontalMidpoint;
        
        if (range.x < verticalMidpoint && range.x + range.width < verticalMidpoint) {
            if (topQuadrant) {
                return 1;
            } else if (bottomQuadrant) {
                return 2;
            }
        } else if (range.x > verticalMidpoint) {
            if (topQuadrant) {
                return 0;
            } else if (bottomQuadrant) {
                return 3;
            }
        }
        
        return -1;
    }
    
    public static class Rectangle {
        double x, y, width, height;
        
        public Rectangle(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public boolean intersects(Rectangle other) {
            return !(other.x > this.x + this.width ||
                    other.x + other.width < this.x ||
                    other.y > this.y + this.height ||
                    other.y + other.height < this.y);
        }
    }
} 