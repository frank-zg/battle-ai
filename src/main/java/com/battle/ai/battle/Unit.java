package com.battle.ai.battle;

import com.battle.ai.Node;
import com.battle.ai.battle.config.UnitConfig;
import com.battle.ai.battle.skill.Skill;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Unit {
    private static final int INITIAL_SKILL_CAPACITY = 4;
    private static final long ATTACK_COOLDOWN_TIME = 5; // 1秒攻击冷却
    private static final double SPLASH_RADIUS = 32.0; // 1个格子的溅射范围
    private static final double SPLASH_DAMAGE_RATIO = 0.5; // 溅射伤害为50%

    private String id;
    private int tempId;
    private Node position;
    private double hp;
    private int team;
    private double attackRange;
    private List<Skill> skills;
    private UnitConfig config;
    private boolean isAttackCooldown;
    private double moveSpeed;
    private List<Node> path;
    private double attackDamage;
    private UnitState state;
    private Map<Skill, Long> skillCooldowns;
    private long lastAttackTime;
    //特殊固定掉血量,百分比
    private double fixedDamageHp;
    //设定某些node不能攻击
    private boolean isAttack = true;
    //多体攻击
    private boolean isMany = false;

    private List<Unit> attackTargets;

    public Unit(String id,int tempId, Node position, UnitConfig config) {
        this.id = id;
        this.position = position;
        this.config = config;
        this.hp = config.getMaxHp();
        this.attackRange = config.getAttackRange();
        this.moveSpeed = config.getMoveSpeed();
        this.attackDamage = config.getAttackDamage();
        this.skills = new ArrayList<>(INITIAL_SKILL_CAPACITY);
        this.isAttackCooldown = false;
        this.path = new ArrayList<>();
        this.state = UnitState.IDLE;
        this.skillCooldowns = new HashMap<>();
        this.lastAttackTime = 0;
        this.tempId = tempId;
    }

    // Getters and setters
    public String getId() { return id; }
    public Node getPosition() { return position; }
    public void setPosition(Node position) { this.position = position; }
    public double getHp() { return hp; }
    public void setHp(double hp) { this.hp = hp; }
    public int getTeam() { return team; }
    public void setTeam(int team) { this.team = team; }
    public double getAttackRange() { return attackRange; }
    public boolean isAlive() { return hp > 0; }
    public double getMoveSpeed() { return moveSpeed; }
    public List<Node> getPath() { return path; }
    public void setPath(List<Node> path) { this.path = path; }
    public double getAttackDamage() { return attackDamage; }

    public void addSkill(Skill skill) {
        skills.add(skill);
    }

    public List<Skill> getSkills() {
        return skills;
    }

    public double distanceTo(Node other) {
        double dx = this.position.getX() - other.getX();
        double dy = this.position.getY() - other.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isInAttackRange(Unit target) {
        if (target == null || !target.isAlive()) return false;
        double distance = distanceTo(target.getPosition());
        log.debug("[检查攻击范围] %s -> %s 距离: %.2f, 攻击范围: %.2f%n",
                id, target.getId(), distance, attackRange);
        return distance <= attackRange;
    }

    public boolean canAttack() {
        if (!isAttack) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        boolean canAttack =
                state != UnitState.DEAD &&
                state != UnitState.STUNNED &&
                (currentTime - lastAttackTime >= ATTACK_COOLDOWN_TIME);

        if (!canAttack) {
            log.debug("[攻击检查] %s 无法攻击: 冷却=%b, 状态=%s, 冷却剩余时间=%.1f秒%n",
                    id, isAttackCooldown, state,
                    (ATTACK_COOLDOWN_TIME - (currentTime - lastAttackTime)) / 1000.0);
        }
        return canAttack;
    }

    public void attack(Unit target, List<Unit> allUnits) {
        if (canAttack()) {
            state = UnitState.ATTACKING;
            lastAttackTime = System.currentTimeMillis();
            isAttackCooldown = true;

            // 获取攻击范围内的所有敌方单位
            List<Unit> targetsInRange =  new ArrayList<>();
            if(isMany) {
                targetsInRange = allUnits.stream()
                        .filter(unit -> unit.getTeam() != this.team && // 是敌方单位
                                unit.isAlive() && // 存活
                                isInAttackRange(unit)) // 在攻击范围内
                        .collect(Collectors.toList());
            }else{
                allUnits.stream()
                        .filter(unit -> unit.getTeam() != this.team && // 是敌方单位
                                unit.isAlive() && // 存活
                                isInAttackRange(unit)) // 在攻击范围内
                        .findFirst().ifPresent(targetsInRange::add);
            }

            double tempAttackDamage = 0.0d;
            // 对范围内所有目标造成全额伤害
            for (Unit rangeTarget : targetsInRange) {
                tempAttackDamage = rangeTarget.getHp();
                rangeTarget.takeDamage(hp);
                log.debug("[攻击] %s 攻击 %s，造成 %.1f 伤害%n",
                        id, rangeTarget.getId(), attackDamage);
            }
            takeDamage(tempAttackDamage);

            if (!targetsInRange.isEmpty()) {
                log.debug("[范围攻击] %s 的攻击影响了 %d 个目标%n",
                        id, targetsInRange.size());
                attackTargets = targetsInRange;
            }
        }
    }

    public void takeDamage(double damage) {
        //如果是不动的node,按照固定的扣血量计算
        this.hp -= damage;
        log.debug("[受伤] %s 受到 %.1f 伤害，剩余血量: %.1f%n",
                id, damage, hp);
        if (this.hp <= 0) {
            this.hp = 0;
            this.state = UnitState.DEAD;
            log.debug("[死亡] %s 被击败!%n", id);
        }
    }

    public void resetAttackCooldown() {
        isAttackCooldown = false;
    }

    public void move(Node destination) {
        this.position = destination;
    }

    public void updatePathMovement() {
        if (path != null && path.size() > 1) {
            Node nextNode = path.get(1);
            double distance = position.distanceTo(nextNode);

            if (distance <= moveSpeed) {
                // 可以直接到达下一个节点
                position = nextNode;
                path.remove(0);
            } else {
                // 按照移动速度移动
                double ratio = moveSpeed / distance;
                double newX = position.getX() + (nextNode.getX() - position.getX()) * ratio;
                double newY = position.getY() + (nextNode.getY() - position.getY()) * ratio;
                position = new Node(newX, newY);
            }
        }
    }

    public void moveTowards(Node target) {
        double dx = target.getX() - position.getX();
        double dy = target.getY() - position.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > moveSpeed) {
            double ratio = moveSpeed / distance;
            double newX = position.getX() + dx * ratio;
            double newY = position.getY() + dy * ratio;
            position = new Node(newX, newY);
        } else {
            position = target;
        }
    }

    public void useSkill(Skill skill, Unit target, List<Unit> allUnits) {
        if (!skillCooldowns.containsKey(skill) ||
                System.currentTimeMillis() - skillCooldowns.get(skill) >= skill.getCooldownTime()) {
            state = UnitState.CASTING;
            skill.use(this, target, allUnits);
            skillCooldowns.put(skill, System.currentTimeMillis());
            log.debug("[技能] %s 使用技能 %s 目标 %s%n",
                    id, skill.getName(), target.getId());
        } else {
            log.debug("[技能冷却] %s 的技能 %s 还在冷却中%n",
                    id, skill.getName());
        }
    }

    public void updateState() {
        state = determineNewState();
    }

    private UnitState determineNewState() {
        if (!isAlive()) return UnitState.DEAD;
        if (isMoving()) return UnitState.MOVING;
        if (isAttacking()) return UnitState.ATTACKING;
        if (isCasting()) return UnitState.CASTING;
        return UnitState.IDLE;
    }

    private boolean isMoving() {
        return path != null && !path.isEmpty();
    }

    private boolean isAttacking() {
        return System.currentTimeMillis() - lastAttackTime < ATTACK_COOLDOWN_TIME;
    }

    private boolean isCasting() {
        return skillCooldowns.values().stream()
                .anyMatch(time -> System.currentTimeMillis() - time < 1000);
    }

    public void setMoveSpeed(double moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public void updateCooldowns() {
        long currentTime = System.currentTimeMillis();
        if (isAttackCooldown && currentTime - lastAttackTime >= ATTACK_COOLDOWN_TIME) {
            isAttackCooldown = false;
            log.debug("[冷却] %s 攻击冷却结束%n", id);
        }

        // 更新技能冷却
        for (Map.Entry<Skill, Long> entry : skillCooldowns.entrySet()) {
            if (currentTime - entry.getValue() >= entry.getKey().getCooldownTime()) {
                entry.getKey().resetCooldown();
                log.debug("[冷却] %s 技能 %s 冷却结束%n",
                        id, entry.getKey().getName());
            }
        }
    }

    public UnitState getState() {
        return state;
    }

    public List<Unit> getAttackTargets() {
        return attackTargets;
    }

    public int getTempId() {
        return tempId;
    }

    public void setTempId(int tempId) {
        this.tempId = tempId;
    }

    public double getFixedDamageHp() {
        return fixedDamageHp;
    }

    public void setFixedDamageHp(double fixedDamageHp) {
        this.fixedDamageHp = fixedDamageHp;
    }

    public boolean isAttack() {
        return isAttack;
    }

    public void setAttack(boolean attack) {
        isAttack = attack;
    }

    public boolean isMany() {
        return isMany;
    }

    public void setMany(boolean many) {
        isMany = many;
    }
}