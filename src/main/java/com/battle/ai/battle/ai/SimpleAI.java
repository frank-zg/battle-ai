package com.battle.ai.battle.ai;

import com.battle.ai.AStar;
import com.battle.ai.Node;
import com.battle.ai.battle.Unit;
import com.battle.ai.battle.skill.Skill;
import com.battle.ai.battle.terrain.Grid;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SimpleAI implements UnitAI {
    private final AStar pathFinder;
    private static final double ATTACK_RANGE_BUFFER = 16.0;

    public SimpleAI() {
        this.pathFinder = new AStar(Grid.getInstance());
    }

    @Override
    public void makeDecision(Unit unit, List<Unit> allies, List<Unit> enemies) {
        if (!unit.isAlive()) return;

        Unit target = findNearestTarget(unit, enemies);
        if (target != null) {
            double distance = unit.getPosition().distanceTo(target.getPosition());
            log.debug("[AI决策] %s -> %s 距离: %.2f, 攻击范围: %.2f%n",
                    unit.getId(), target.getId(), distance, unit.getAttackRange());

            if (distance <= unit.getAttackRange()) {
                // 在攻击范围内
                if (unit.canAttack()) {
                    // 获取所有单位列表用于范围攻击
                    List<Unit> allUnits = new ArrayList<>(allies);
                    allUnits.addAll(enemies);

                    // 执行攻击，传入所有单位列表
                    unit.attack(target, allUnits);

                    // 尝试使用技能
                    for (Skill skill : unit.getSkills()) {
                        unit.useSkill(skill, target, allUnits);
                    }
                }
            } else {
                // 需要移动
                moveTowardsTarget(unit, target);
            }
        }
    }

    private void moveTowardsTarget(Unit unit, Unit target) {
        // 如果已经有路径且路径有效，继续沿着路径移动
        if (unit.getPath() != null && !unit.getPath().isEmpty()) {
            log.debug("[AI移动] %s 继续沿路径移动，剩余路径点: %d%n",
                    unit.getId(), unit.getPath().size());
            return;
        }

        // 计算理想的攻击位置
        double effectiveRange = unit.getAttackRange() - ATTACK_RANGE_BUFFER;
        Node targetPos = calculateTargetPosition(unit, target, effectiveRange);

        // 寻找路径
        List<Node> path = pathFinder.findPath(unit.getPosition(), targetPos);
        if (path != null && !path.isEmpty()) {
            unit.setPath(path);
            log.debug("[AI路径] %s 找到通向 %s 的路径，长度: %d%n",
                    unit.getId(), target.getId(), path.size());
        } else {
            // 如果找不到路径，直接移动向目标
            unit.moveTowards(target.getPosition());
            log.debug("[AI直线] %s 直线移动向 %s%n", unit.getId(), target.getId());
        }
    }

    public Unit findNearestTarget(Unit source, List<Unit> targets) {
        if (targets == null || targets.isEmpty()) {
            return null;
        }

        Unit nearestTarget = null;
        double minDistance = Double.MAX_VALUE;

        for (Unit target : targets) {
            if (!target.isAlive()) {
                continue;
            }

            double distance = source.getPosition().distanceTo(target.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                nearestTarget = target;
            }
        }

        return nearestTarget;
    }

    private Node calculateTargetPosition(Unit unit, Unit target, double effectiveRange) {
        double dx = target.getPosition().getX() - unit.getPosition().getX();
        double dy = target.getPosition().getY() - unit.getPosition().getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= effectiveRange) {
            return unit.getPosition();
        }

        // 计算理想的攻击位置
        double ratio = effectiveRange / distance;
        double targetX = target.getPosition().getX() - dx * ratio;
        double targetY = target.getPosition().getY() - dy * ratio;

        return new Node(targetX, targetY);
    }
} 