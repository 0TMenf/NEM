package top.pinkoc.nem.game;

/**
 * 房间对外状态，决定它在 {@code /ne gui} 里显示成什么图标。
 *
 * <pre>
 *   LOADING  → 红石块 (REDSTONE_BLOCK)     世界正在加载或重置
 *   WAITING  → 绿色羊毛 (GREEN_WOOL)      可以加入
 *   RUNNING  → 红色混凝土 (RED_CONCRETE)   正在对局中
 *   FULL     → 红色混凝土 (RED_CONCRETE)   人数已满
 *   DISABLED → 屏障 (BARRIER)             地图未启用
 * </pre>
 */
public enum RoomState {

    /** 世界正在加载/重置，暂时不可加入。 */
    LOADING,

    /** 等待玩家加入。 */
    WAITING,

    /** 已开始对局。 */
    RUNNING,

    /** 玩家数量已达到上限。 */
    FULL,

    /** 地图被管理员禁用。 */
    DISABLED;

    /** 给 GUI 用的中文描述。 */
    public String displayName() {
        return switch (this) {
            case LOADING  -> "加载中...";
            case WAITING  -> "可加入";
            case RUNNING  -> "游戏中";
            case FULL     -> "已满";
            case DISABLED -> "未启用";
        };
    }

    /** 是否允许玩家加入。 */
    public boolean isJoinable() {
        return this == WAITING;
    }
}