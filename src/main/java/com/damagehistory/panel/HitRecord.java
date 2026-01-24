package com.damagehistory.panel;

import lombok.Value;

@Value
public class HitRecord {

    String weaponName;
    int hit;
    String npcName;
    int weaponId;
    int tickCount;
    int attackSpeed;
    boolean specialAttack;
}