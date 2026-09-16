package top.pinkoc.nem.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * lang.yml 的读取工具。
 *
 * <p>首次启动时会把 jar 内置的 {@code lang.yml} 释放到插件目录。
 * 之后每次 {@link #load()} 都会：
 * <ol>
 *   <li>读取磁盘上的 lang.yml</li>
 *   <li>与 jar 内的默认值合并，自动补齐新增键</li>
 *   <li>把合并结果写回磁盘（保留用户的自定义）</li>
 * </ol>
 *
 * <p>支持两种占位符：
 * <ul>
 *   <li>{@code {prefix}} —— 自动替换为 lang.yml 顶部的 prefix</li>
 *   <li>{@code {xxx}}    —— 调用 {@link #get(String, String...)} 时按 key 传入</li>
 * </ul>
 */
public class Lang {

    private final JavaPlugin plugin;
    private YamlConfiguration lang;
    private String prefix = "";

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // 加载
    // ------------------------------------------------------------------

    /** 从磁盘重新加载语言文件。 */
    public void load() {
        File file = new File(plugin.getDataFolder(), "lang.yml");

        // 首次启动：释放默认文件
        if (!file.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        lang = YamlConfiguration.loadConfiguration(file);

        // 用 jar 内的默认值补齐磁盘文件缺少的键
        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("lang.yml"), StandardCharsets.UTF_8)) {

            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            lang.setDefaults(defaults);
            lang.options().copyDefaults(true);

            try {
                lang.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("写回 lang.yml 失败: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("读取内置 lang.yml 失败: " + e.getMessage());
        }

        prefix = color(lang.getString("prefix", ""));
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    /**
     * 读取一条语言文本并做占位符替换。
     *
     * <p>用法示例：
     * <pre>
     *   // 无占位符
     *   lang.get("command.no-permission");
     *
     *   // 替换 {seconds}
     *   lang.get("join.countdown", "seconds", "30");
     *
     *   // 替换多个
     *   lang.get("command.arena-not-found", "name", "house1");
     * </pre>
     *
     * @param path          键路径，例如 {@code join.joined-room}
     * @param replacements  key1, value1, key2, value2, ...
     * @return 已着色并替换完成的字符串；键不存在时返回一行红字提示
     */
    public String get(String path, String... replacements) {
        String raw = lang.getString(path);
        if (raw == null) {
            return "§c缺失语言键: " + path;
        }

        // 先着色，再替换
        String out = color(raw);

        // {prefix} 是全局占位符
        out = out.replace("{prefix}", prefix);

        // 用户传入的占位符
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            out = out.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }

        return out;
    }

    /**
     * 读取原始字符串（不做 prefix 替换，但会着色）。
     * 适合在需要进一步处理的情况下使用。
     */
    public String raw(String path) {
        String s = lang.getString(path);
        return s == null ? null : color(s);
    }

    /** 当前 prefix（已着色）。 */
    public String prefix() {
        return prefix;
    }

    /** 语言文件的 YamlConfiguration 引用。 */
    public YamlConfiguration config() {
        return lang;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /** 把 {@code &} 色码转成 § 色码。 */
    public static String color(String s) {
        return s == null ? "" : s.replace('&', '§');
    }
}