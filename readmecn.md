# 欢迎来到 NEM！

[English](README_EN.md)

非常非常高兴见见见见见见见见见见见见见见见见见见见见你，我的朋友:)

这里是 NEM（Notch Escape Mini）的 GitHub 项目。

---

## 这个插件是什么？

你是 Notch，Minecraft 的创始人。你听说迷你世界的创始人之一古振兴获得了侏罗纪老头留下的迷你世界代码——基于 2018 至 2022 年“第一次中国互联网世界大战”的故事，即 Minecraft 与迷你世界玩家群体之间的纷争。于是你决定在一个夜黑风高的夜晚潜入古振兴的家。

直到来到古振兴家后，你才发现古振兴的家中摆满了你的照片（Notch）。原来古振兴是一个病娇，并且非常喜欢你。这时候，古振兴悄悄来到你的背后，并用一口锅把你敲晕了。

等你重新醒来时，你发现自己被绑在古振兴的地下室里，周围阴暗潮湿，而且十分闷热。你无意听见古振兴的计划：“把 Notch 杀掉，就能让他的遗体永远陪着自己了。”

你必须在 3 天（60 分钟）内逃出古振兴的家，并到外面报警。注意逃跑过程中不要被古振兴抓住，不然你会被提前杀死。

---

## 玩法一览

NEM 是一个**多人对抗**小游戏，最多支持 **24 名玩家**（1~6 名古振兴 + 2~18 名 Notch）。

### 阵营

| 阵营 | 目标 | 人数上限 |
| --- | --- | --- |
| Notch | 逃出地下室 → 找到钥匙 → 冲出大门 → 躲进废弃屋 → 报警 | 18 |
| 古振兴 | 砍死（3 刀）或抓住所有 Notch | 6 |

### 游戏流程（Notch 视角）

1. **地下室醒来** — 找到 **地下室钥匙**
2. **上楼** — 找到 **厕所钥匙** → 打开厕所
3. **厕所内** — 找到 **卧室钥匙** → 打开卧室
4. **卧室（危险！）** — 古振兴正在睡觉。**必须潜行**，否则会吵醒他。悄悄拿到 **大门钥匙**
5. **冲出大门** — 大门发出巨响，古振兴被惊醒。**5 秒内**冲出大门
6. **逃亡** — **30 秒内**抵达废弃屋子
7. **躲藏** — 在废弃屋里**保持静止 30 秒**。只要一移动，全身会发光并且位置会被广播给所有古振兴
8. **报警** — 抵达警察局 → **获胜！**

### 阶段时序（T = 开门时刻）

```
T = 0     大门打开
T = 0~5s  必须冲出大门，超时淘汰
T = 5s    古振兴自动醒来（获得 40 秒黑暗效果）
T = 5~35s 30 秒内抵达废弃屋子
T = 35~65s 静止 30 秒（无需潜行）
           移动 → 全身发光 5 秒 + 广播坐标
T = 40s   古振兴黑暗结束
T = 65s   前往警察局
T = ?     抵达警察局 → Notch 胜利
```

### 古振兴的特殊能力

- 开局被冻结在卧室“睡觉”
- 被吵醒后获得 **40 秒黑暗效果**（暂时看不见）
- 持有 **钻石剑**（3 刀即可杀死 Notch）

---

## 新手快速上手（如何添加地图）

### 第一步：准备服务器

- Paper 服务端 **1.21.4+**
- Java **21**

把 `Notch_Escape_Mini-Alpha-0.0.1.jar` 丢进 `plugins/` 文件夹，启动服务端一次，让插件生成默认配置和语言文件。

### 第二步：新建一张地图

用管理员（OP）账号进服，执行：

```
/ne create house1
```

这会在 `plugins/Notch_Escape_Mini/arenas/house1/arena.yml` 下创建一份空配置。

### 第三步：进入编辑模式

```
/ne edit house1
```

服务器会自动创建一个名为 `ne_house1` 的世界（如果不存在），并把你的游戏模式切换成创造模式。

### 第四步：逐点标记

在 `ne_house1` 世界里**走到每个关键位置**，执行对应的命令：

| 命令 | 位置说明 |
| --- | --- |
| /ne waitLobby | 等待大厅（玩家等待开局的地方） |
| /ne playerSpawn | 醒来点（地下室的地板） |
| /ne setUnderKey | 地下室钥匙的具体位置 |
| /ne setUnderDoor | 地下室门（通往楼上的路口） |
| /ne setToiletKey | 厕所钥匙的位置 |
| /ne setToiletDoor | 厕所门 |
| /ne setBedroomKey | 卧室钥匙的位置 |
| /ne setBedroomDoor | 卧室门 |
| /ne setBedroomSleep | 古振兴睡觉的床 |
| /ne setFrontDoorKey | 大门钥匙（卧室内的抽屉/桌子） |
| /ne setFrontDoor | 大门 |
| /ne setHideHouse | 废弃屋子（躲藏点） |
| /ne setPolice | 警察局 |
| /ne setGuSpawn | 古振兴玩家的出生点（通常在卧室门口） |
| /ne addPatrol | 添加一个古振兴的巡逻点（可重复执行） |

每次执行命令后，聊天栏会显示已设置的坐标，方便确认。

### 第五步：保存并启用

```
/ne save
/ne enableSave house1
```

`save` 会把坐标写入 `arena.yml`；`enableSave` 会做完整性检查、加载世界、把地图变成“可加入”状态。

### 第六步：开一局试试

```
/ne gui
```

会打开一个箱子界面。绿色羊毛代表地图可加入，红石块代表正在加载。点击你的地图 → 选择阵营（下界合金剑 = 古振兴，钻石剑 = Notch）→ 进入等待大厅。

人数 ≥ 3（至少 1 名古振兴 + 2 名 Notch）时自动开始 30 秒倒计时。

---

## 指令一览

命令的主别名是 `/ne`，也支持 `/notchescape` 和 `/nem`。

### 玩家命令（默认所有玩家可用）

| 命令 | 说明 |
| --- | --- |
| /ne gui | 打开房间选择界面 |
| /ne leave | 离开当前房间 |
| /ne help | 查看帮助 |

### 管理员命令（默认仅 OP）

#### 地图管理

| 命令 | 说明 |
| --- | --- |
| /ne create <名称> | 创建新地图 |
| /ne delete <名称> | 删除地图（含世界文件夹） |
| /ne edit <名称> | 进入编辑模式 |
| /ne list | 列出所有地图 |
| /ne save | 保存当前正在编辑的地图 |
| /ne enableSave <名称> | 启用并加载地图 |
| /ne disable <名称> | 禁用并卸载地图 |

#### 点位设置（需先 `/ne edit`）

| 命令 | 说明 |
| --- | --- |
| /ne waitLobby | 等待大厅 |
| /ne playerSpawn | 醒来点 |
| /ne setUnderKey | 地下室钥匙 |
| /ne setUnderDoor | 地下室门 |
| /ne setToiletKey | 厕所钥匙 |
| /ne setToiletDoor | 厕所门 |
| /ne setBedroomKey | 卧室钥匙 |
| /ne setBedroomDoor | 卧室门 |
| /ne setBedroomSleep | 古振兴睡觉点 |
| /ne setFrontDoorKey | 大门钥匙 |
| /ne setFrontDoor | 大门 |
| /ne setHideHouse | 废弃屋子 |
| /ne setPolice | 警察局 |
| /ne setGuSpawn | 古振兴出生点 |
| /ne addPatrol | 添加巡逻点 |
| /ne clearPatrol | 清空巡逻点 |

#### 对局控制

| 命令 | 说明 |
| --- | --- |
| /ne start <地图> | 强制开始对局 |
| /ne stop <地图> | 结束对局并重置地图 |
| /ne status [地图] | 查看地图状态 |
| /ne reload | 重载配置和语言文件 |

---

## 权限组

| 权限节点 | 默认 | 说明 |
| --- | --- | --- |
| notchescape.player | 所有玩家 | 允许使用 /ne gui、/ne leave、/ne help |
| notchescape.admin | 仅 OP | 允许使用所有管理命令 |

### LuckPerms 配置示例

**给默认组玩家开放 GUI：**

```
/lp group default permission set notchescape.player true
```

**给某个管理员组开放管理权限：**

```
/lp group admin permission set notchescape.admin true
```

**单独给某个玩家临时管理权：**

```
/lp user Steve permission set notchescape.admin true
```

---

## 配置文件

插件目录结构：

```
plugins/Notch_Escape_Mini/
├── config.yml                          # 全局配置
├── lang.yml                            # 语言文件（可自由修改）
└── arenas/
    └── house1/
        └── arena.yml                   # 每张地图的独立配置
```

地图世界文件夹保存在服务器根目录：

```
<服务器根目录>/
└── ne_house1/
```

### 全局配置 `config.yml`

```
game:
  min-players: 3          # 最少几名玩家可开始
  max-players: 24         # 房间最多几人
  max-gu: 6               # 古振兴阵营上限
  max-notch: 18           # Notch 阵营上限
  start-countdown: 30     # 等待倒计时（秒）
```

### 地图配置 `arena.yml`（示例）

```
name: house1
world: ne_house1
enabled: true

locations:
  wait-lobby: "ne_house1,0.5,64.0,0.5,0.0,0.0"
  player-spawn: "ne_house1,10.5,60.0,10.5,90.0,0.0"
  under-key: "ne_house1,-5.5,60.0,3.5,0.0,0.0"
  # ... 其它点位 ...

game:
  pickup-radius: 2.0             # 拾取半径
  door-radius: 3.0               # 门交互半径
  capture-distance: 1.6          # 抓住判定距离
  escape-house-seconds: 5        # 冲出大门时限
  house-escape-radius: 15.0      # “冲出大门”的距离
  hide-arrival-seconds: 30       # 抵达废弃屋子时限
  hide-stay-seconds: 30          # 静止时长
  gu-blindness-seconds: 40       # 古振兴黑暗时长
  noise-glow-seconds: 5          # 移动后发光时长
```

---

## 语言配置

首次启动后，`plugins/Notch_Escape_Mini/lang.yml` 会自动生成。所有插件输出的文本都在这里：

```
prefix: "&8[&cNE&8] &r"

command:
  no-permission: "{prefix}&c你没有权限使用该命令。"
  arena-not-found: "{prefix}&c没有找到地图 &e{name}"

game:
  welcome: "&c你在冰冷的地下室里醒来。头疼得厉害……"
  pick-under-key: "&e你在砖缝里摸到一把锈迹斑斑的&6地下室钥匙&e。"
  # ...
```

- **颜色代码**：使用 `&` 开头，例如 `&a` `&c` `&e`
- **占位符**：`{xxx}` 会在运行时被替换，**不要修改花括号内容**
- **升级保留**：插件更新时新增的语言键会自动合并，**不会覆盖**你的自定义翻译

---

## 常见问题

**Q：地图删除了，但世界文件夹还在？**

A：`/ne delete <地图>` 会同时删除插件配置和世界文件夹。如果世界文件夹没被删掉，检查服务器进程是否有该文件夹的写权限。

**Q：`/ne enableSave` 报“缺少配置项”？**

A：把报错列出来的点位挨个补一遍，再 `/ne save`。

**Q：玩家一直卡在“加载中……”？**

A：这表示世界正在重置。等 3~5 秒即可。如果超过 30 秒还是“加载中”，查看控制台有没有报错。

**Q：古振兴玩家开局不能动？**

A：这是设计如此。他需要被吵醒（Notch 未潜行拿大门钥匙，或大门被打开）才能行动。

**Q：修改了 `arena.yml` 后怎么生效？**

A：管理员执行 `/ne disable <地图>` 再 `/ne enableSave <地图>`，或者直接 `/ne reload`。

---

## 构建

```
mvn clean package
```

产物：`target/Notch_Escape_Mini-Alpha-0.0.1.jar`

要求：

- JDK 21
- Maven 3.8+

---

## 贡献

欢迎提 Issue 和 PR。特别欢迎：

- 新地图的分享（`arena.yml` + 世界存档）
- `lang.yml` 的翻译改进
- Bug 报告（附控制台日志）

---

## 许可

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE)。

---

**祝你逃得出去。:)**