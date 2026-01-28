package com.damagehistory.panel;

import lombok.Value;

@Value
public class PlayerHitRecord {

    String playerName;
    int hit;
    String npcName;
    int weaponId;
    int tickCount;
    int attackSpeed;
    boolean specialAttack;
    Integer ticksSincePrevious;
    Integer previousAttackSpeed;
}