package com.damagehistory;

public class HitRecord {
    private final String weaponName;
    private final int hit;
    private final String npcName;
    private final int weaponId;

    public HitRecord(String weaponName, int hit, String npcName, int weaponId) {
        this.weaponName = weaponName;
        this.hit = hit;
        this.npcName = npcName;
        this.weaponId = weaponId;
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
}