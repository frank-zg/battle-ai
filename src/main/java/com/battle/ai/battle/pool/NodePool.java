package com.battle.ai.battle.pool;

import com.battle.ai.Node;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NodePool {
    private static final int INITIAL_CAPACITY = 1000;
    private static final Queue<Node> pool = new ConcurrentLinkedQueue<>();
    
    static {
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            pool.offer(new Node(0, 0));
        }
    }
    
    public static Node obtain(double x, double y) {
        Node node = pool.poll();
        if (node == null) {
            node = new Node(x, y);
        } else {
            node.setX(x);
            node.setY(y);
        }
        return node;
    }
    
    public static void recycle(Node node) {
        if (pool.size() < INITIAL_CAPACITY) {
            pool.offer(node);
        }
    }
} 