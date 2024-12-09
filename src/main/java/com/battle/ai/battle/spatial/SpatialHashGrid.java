package com.battle.ai.battle.spatial;

import com.battle.ai.Node;
import com.battle.ai.battle.Unit;

import java.util.*;

public class SpatialHashGrid {
    private static final int CELL_SIZE = 128; // 4个格子大小作为一个分区
    private final Map<Long, List<Unit>> grid = new HashMap<>();
    
    public void addUnit(Unit unit) {
        long cellId = getCellId(unit.getPosition());
        grid.computeIfAbsent(cellId, k -> new ArrayList<>()).add(unit);
    }
    
    public void updateUnit(Unit unit, Node oldPosition) {
        long oldCellId = getCellId(oldPosition);
        long newCellId = getCellId(unit.getPosition());
        
        if (oldCellId != newCellId) {
            grid.get(oldCellId).remove(unit);
            addUnit(unit);
        }
    }
    
    public void removeUnit(Unit unit) {
        long cellId = getCellId(unit.getPosition());
        List<Unit> cell = grid.get(cellId);
        if (cell != null) {
            cell.remove(unit);
        }
    }
    
    public List<Unit> getNearbyUnits(Node position, double radius) {
        Set<Unit> result = new HashSet<>();
        int cellRadius = (int)(radius / CELL_SIZE) + 1;
        int baseX = (int)(position.getX() / CELL_SIZE);
        int baseY = (int)(position.getY() / CELL_SIZE);
        
        for (int x = -cellRadius; x <= cellRadius; x++) {
            for (int y = -cellRadius; y <= cellRadius; y++) {
                long cellId = getCellId(baseX + x, baseY + y);
                List<Unit> cell = grid.get(cellId);
                if (cell != null) {
                    for (Unit unit : cell) {
                        if (unit.getPosition().distanceTo(position) <= radius) {
                            result.add(unit);
                        }
                    }
                }
            }
        }
        
        return new ArrayList<>(result);
    }
    
    private long getCellId(Node position) {
        return getCellId((int)(position.getX() / CELL_SIZE), 
                        (int)(position.getY() / CELL_SIZE));
    }
    
    private long getCellId(int x, int y) {
        return ((long)x << 32) | (y & 0xFFFFFFFFL);
    }
    
    public void clear() {
        grid.clear();
    }
} 