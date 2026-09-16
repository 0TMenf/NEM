package top.pinkoc.nem.game;

import org.bukkit.Location;

import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;
    private Team team;

    // 钥匙进度（仅 Notch 使用）
    private boolean hasUnderKey;
    private boolean hasToiletKey;
    private boolean hasBedroomKey;
    private boolean hasFrontKey;

    // 门交互
    private int doorTicks = 0;
    private int doorMsgCooldown = 0;

    // ---- 阶段 4/5/6 状态 ----
    private boolean houseExited = false;     // 已冲出大门
    private boolean atHideHouse = false;     // 已抵达 hideHouse
    private boolean hidden = false;          // 已完成 30 秒静止
    private long hideStartAt = 0;            // 抵达 hideHouse 的时间
    private Location staticPos = null;       // 静止基准位置
    private int noiseWarnCooldown = 0;

    // 结算
    private boolean won = false;
    private boolean alive = true;

    public GamePlayer(UUID uuid) { this.uuid = uuid; }

    public UUID getUuid() { return uuid; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public boolean isNotch() { return team == Team.NOTCH; }
    public boolean isGu() { return team == Team.GU; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean a) { this.alive = a; }

    public boolean hasUnderKey() { return hasUnderKey; }
    public void setHasUnderKey(boolean v) { this.hasUnderKey = v; }

    public boolean hasToiletKey() { return hasToiletKey; }
    public void setHasToiletKey(boolean v) { this.hasToiletKey = v; }

    public boolean hasBedroomKey() { return hasBedroomKey; }
    public void setHasBedroomKey(boolean v) { this.hasBedroomKey = v; }

    public boolean hasFrontKey() { return hasFrontKey; }
    public void setHasFrontKey(boolean v) { this.hasFrontKey = v; }

    public int getDoorTicks() { return doorTicks; }
    public void addDoorTick() { doorTicks++; }
    public void resetDoorTicks() { doorTicks = 0; }

    public boolean tryShowDoorMessage() {
        if (doorMsgCooldown > 0) { doorMsgCooldown--; return false; }
        doorMsgCooldown = 20;
        return true;
    }

    public boolean isHouseExited() { return houseExited; }
    public void setHouseExited(boolean b) { this.houseExited = b; }

    public boolean isAtHideHouse() { return atHideHouse; }
    public void setAtHideHouse(boolean b) { this.atHideHouse = b; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean h) { this.hidden = h; }

    public long getHideStartAt() { return hideStartAt; }
    public void setHideStartAt(long t) { this.hideStartAt = t; }

    public Location getStaticPos() { return staticPos; }
    public void setStaticPos(Location l) { this.staticPos = l; }

    public boolean tryShowNoiseWarning() {
        if (noiseWarnCooldown > 0) { noiseWarnCooldown--; return false; }
        noiseWarnCooldown = 40; // 2 秒
        return true;
    }

    public boolean isWon() { return won; }
    public void setWon(boolean w) { this.won = w; }
}