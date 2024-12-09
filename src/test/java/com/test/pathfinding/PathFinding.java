package com.test.pathfinding;

import java.util.List;

import com.battle.ai.Node;

public interface PathFinding {
    List<Node> findPath(Node start, Node end);
    void setMapSize(int width, int height);
    void setObstacles(List<Node> obstacles);
} 