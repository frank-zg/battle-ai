package com.battle.ai.battle.terrain;


import com.battle.ai.Node;

import java.util.ArrayList;
import java.util.List;

public class Grid {
    private static final int GRID_SIZE = 32; // 每个格子的像素大小
    private static Grid instance;
    private TerrainType[][] terrain;
    private static final int WIDTH = 20;
    private static final int HEIGHT = 20;
    
    private Grid() {
        terrain = new TerrainType[WIDTH][HEIGHT];
        // 初始化为普通地形
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                terrain[x][y] = TerrainType.PLAIN;
            }
        }
    }
    
    public static Grid getInstance() {
        if (instance == null) {
            instance = new Grid();
        }
        return instance;
    }
    
    public static void reset() {
        instance = null;
    }
    
    public TerrainType getTerrain(int x, int y) {
        if (isValidPosition(x, y)) {
            return terrain[x][y];
        }
        return TerrainType.WALL; // 边界外视为墙
    }
    
    public void setTerrain(int x, int y, TerrainType type) {
        if (isValidPosition(x, y)) {
            terrain[x][y] = type;
        }
    }
    
    public boolean isWalkable(int x, int y) {
        TerrainType terrainType = getTerrain(x, y);
        return isValidPosition(x, y) && terrainType.isWalkable();
    }
    
    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }
    
    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        int gridX = pixelToGrid((int)node.getX());
        int gridY = pixelToGrid((int)node.getY());
        
        // 8个方向
        int[][] directions = {
            {-1,-1}, {0,-1}, {1,-1},
            {-1,0},          {1,0},
            {-1,1},  {0,1},  {1,1}
        };
        
        for (int[] dir : directions) {
            int newX = gridX + dir[0];
            int newY = gridY + dir[1];
            
            if (isWalkable(newX, newY)) {
                neighbors.add(new Node(gridToPixel(newX), gridToPixel(newY)));
            }
        }
        
        return neighbors;
    }
    
    public static int gridToPixel(int gridCoord) {
        return gridCoord * GRID_SIZE;
    }
    
    public static int pixelToGrid(double pixelCoord) {
        return (int)(pixelCoord / GRID_SIZE);
    }
    
    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }
} 