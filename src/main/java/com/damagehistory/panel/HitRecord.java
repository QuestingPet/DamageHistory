package com.damagehistory.panel;

public class HitRecord {
    private final String weaponName;
    private final int hit;
    private final String npcName;
    private final int weaponId;
    private final int tickCount;
    private final int attackSpeed;

    public HitRecord(String weaponName, int hit, String npcName, int weaponId, int tickCount, int attackSpeed) {
        this.weaponName = weaponName;
        this.hit = hit;
        this.npcName = npcName;
        this.weaponId = weaponId;
        this.tickCount = tickCount;
        this.attackSpeed = attackSpeed;
    }

    public String getWeaponName() {
        return weaponName;
    }

    public int getHit() {
        return hit;
    }

    public String getNpcName() {
        return npcName;
    }

    public int getWeaponId() {
        return weaponId;
    }

    public int getTickCount() {
        return tickCount;
    }

    public int getAttackSpeed() {
        return attackSpeed;
    }
}