package top.pinkoc.nem.game;

/**
 * 阵营枚举。
 *
 * <ul>
 *   <li>{@link #NOTCH} —— 逃亡者阵营（“Notch”）</li>
 *   <li>{@link #GU}    —— 追捕者阵营（“古振兴”）</li>
 * </ul>
 */
public enum Team {

    /** 逃亡者：Notch。目标是从地下室一路逃到警察局。 */
    NOTCH,

    /** 追捕者：古振兴。目标是阻止所有 Notch 逃脱。 */
    GU;

    /** 中文显示名，用于聊天栏 / GUI。 */
    public String displayName() {
        return this == GU ? "古振兴" : "Notch";
    }

    /** 判断是否是 Notch 阵营。 */
    public boolean isNotch() {
        return this == NOTCH;
    }

    /** 判断是否是古振兴阵营。 */
    public boolean isGu() {
        return this == GU;
    }

    /**
     * 通过名字解析阵营（大小写不敏感）。
     *
     * @return 匹配的阵营，未匹配返回 {@code null}
     */
    public static Team fromString(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase();
        return switch (n) {
            case "notch", "n", "runner", "escape" -> NOTCH;
            case "gu", "gzx", "guzhenxing", "hunter" -> GU;
            default -> null;
        };
    }
}