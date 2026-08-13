### BiLiBiLiVideo-Fork

## 前情提要

### 在此感谢原作者 Bingzi-233 开发的BiliBiliVideo 插件，本项目基于该项目源码进行二次开发，同时感谢 TabooLib 一款强大的基于Kotlin的框架，本插件网络缓存部分由AI辅助开发，如有介意请勿使用该插件

### [插件开源地址](https://github.com/BalanceSea/BilibiliVideo-Fork)
### [最新构建版下载](https://github.com/BalanceSea/BilibiliVideo-Fork/releases)

## 📖 项目简介

**BilibiliVideo-Fork** 是一个基于 TabooLib 6.3.0-afd75a7 框架与 BiliBiliVideo Plugin 开发的 Minecraft 服务器插件，为服务器提供完整的哔哩哔哩平台集成能力。每个玩家可以独立登录自己的 B 站账号，在游戏内完成视频三连检测、领取奖励等互动功能。

### 🎯 核心亮点

- 🗺️ **游戏内二维码登录** - 创新性地使用地图物品展示 B 站登录二维码
- 💻  **游戏内GUI界面** - '傻瓜式'操作,妈妈再也不用担心玩家不会输指令啦
- 📜 **自定义语言** - 高度自定义化的语言文件
- 👥 **多账户隔离系统** - 每位玩家使用独立的 B 站账号，数据完全隔离
- ⚡ **异步架构** - 所有网络请求异步执行，不影响服务器性能
- 🎁 **灵活奖励系统** - 基于 Kether 脚本引擎，支持自定义奖励逻辑
- 🛡️ **风控规避机制** - 内置 buvid3 参数管理，保证请求稳定性
- 🗄️ **双数据库支持** - SQLite 开箱即用，可切换至 MySQL

---

## ✨ 功能特性

### 🔐 账号系统

| 功能 | 说明 |
|------|------|
| **二维码登录** | 在游戏内生成 B 站登录二维码地图物品，扫码即可绑定 |
| **账号绑定** | 每个玩家绑定独立的 B 站账号，支持查看绑定状态 |
| **Cookie 自动刷新** | 自动维护登录凭证有效性，避免过期失效 |
| **凭证管理** | 完整的凭证生命周期管理（创建、更新、过期、禁用） |

### 🎬 互动功能

| 功能 | 说明 |
|------|------|
| **三连状态检测** | 检测玩家是否完成视频的点赞、投币、收藏 |
| **奖励发放** | 基于三连状态自动发放游戏内奖励 |
| **防重复领取** | 智能记录奖励领取历史，避免重复领取 |

---

## 🚀 快速开始

### 📋 环境要求

| 组件 | 版本要求 |
|------|----------|
| **Minecraft 服务端** | Bukkit/Spigot/Paper (推荐 1.12+) |
| **Java** | JRE 8 或更高版本 |
| **TabooLib** | 6.3.0-afd75a7 (自动加载) |
| **数据库** | SQLite (默认) 或 MySQL 5.7+ |

### 📥 安装步骤

1. **下载插件**
   ```bash
   # 从 Releases 页面下载最新版本
   wget https://github.com/BalanceSea/BilibiliVideo/releases/download/v2.0.0-beta/BilibiliVideo-2.0.0-beta.jar
   ```

2. **放置插件文件**
   ```
   plugins/
   └── BilibiliVideo-Fork-2.8.0-beta.jar
   ```

    3. **启动服务器**

       插件会自动创建配置文件和数据库：
       ```
       plugins/BilibiliVideo/
       ├── config.yml          # 奖励配置
       ├── gui.yml             # GUI配置
       ├── database.yml        # 数据库配置
       ├── lang/               # 语言文件
       └── bilibili_video.db   # SQLite 数据库
       ```

4. **验证安装**

   在控制台或游戏内执行：
   ```
   /bv help
   ```

---

## 💻 命令系统

### 权限节点

| 权限节点 | 说明 | 默认许可 |
|----------|------|----------|
| `bilibili.video.use` | 玩家基础命令（qrcode/status/triple/reward） | 所有人 |
| `bilibili.video.admin` | 管理员命令（unbind/credential/reload） | 仅 OP |

### 玩家命令

| 命令                  | 功能            | 权限节点 |
|---------------------|---------------|----------|
| `/bv help`          | 查看帮助信息        | `bilibili.video.use` |
| `/bv menu`          | 打开GUI菜单       | `bilibili.video.use` |
| `/bv qrcode`        | 生成 B 站登录二维码地图 | `bilibili.video.use` |
| `/bv status`        | 查看账号绑定状态      | `bilibili.video.use` |
| `/bv triple <bvid>` | 检测视频三连状态      | `bilibili.video.use` |
| `/bv reward <bvid>` | 领取三连奖励        | `bilibili.video.use` |

### 管理员命令

| 命令 | 功能 | 权限节点 |
|------|------|----------|
| `/bv admin reload` | 重载配置文件 | `bilibili.video.admin` |
| `/bv admin unbind <target>` | 解除玩家绑定（支持玩家名/UUID/B站UID） | `bilibili.video.admin` |
| `/bv admin credential list` | 列出所有登录凭证 | `bilibili.video.admin` |
| `/bv admin credential info <label>` | 查看凭证详细信息 | `bilibili.video.admin` |
| `/bv admin credential refresh <label>` | 刷新指定凭证（待实现） | `bilibili.video.admin` |

### 🎮 使用示例

```bash
# 1. 玩家绑定 B 站账号
/bv qrcode
# 系统会给玩家一张地图物品，展示登录二维码
# 用 B 站 APP 扫码并确认登录

# 2. 查看绑定状态
/bv status
# 输出: ✓ 已绑定 B 站账号: 用户名 (UID: 123456789)

# 3. 检测视频三连
/bv triple BV1xx411c7mD
# 输出: ✓ 点赞 | ✓ 投币(2) | ✓ 收藏 - 已完成三连!

# 4. 领取奖励
/bv reward BV1xx411c7mD
# 系统执行奖励脚本并提示领取成功
```

---

## ⚙️ 配置指南

### 数据库配置 (`database.yml`)

```yaml
database:
  # 是否启用数据库（必须为 true）
  enabled: true

  # 数据库类型: sqlite (默认) 或 mysql
  type: sqlite

  # SQLite 配置（开箱即用）
  sqlite:
    file: "bilibili_video.db"

  # MySQL 配置（可选）
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "bilibili_video"
    username: "root"
    password: "your_password"
    params: "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"

  # HikariCP 连接池配置
  hikari:
    pool-name: "BilibiliVideoPool"
    maximum-pool-size: 10        # 最大连接数
    minimum-idle: 2              # 最小空闲连接
    connection-timeout: 30000    # 连接超时 (ms)
    idle-timeout: 600000         # 空闲超时 (ms)
    max-lifetime: 1800000        # 最大生命周期 (ms)

  # 数据库选项
  options:
    table-prefix: "bv_"          # 表名前缀
    show-sql: false              # 是否打印 SQL
    slow-sql-threshold-ms: 500   # 慢查询阈值 (ms)
```

### 奖励配置 (`config.yml`)

```yaml
reward:
  # 奖励模板定义
  templates:
    # 默认模板（通用奖励）
    default:
      description: "完成三连时发放的默认奖励"
      kether:
        - 'tell "&a[BilibiliVideo] &f感谢你的三连支持！"'
        - 'command papi "give %player_name% diamond 3"'

    # VIP 模板（高价值奖励）
    vip:
      description: "特殊视频的 VIP 奖励"
      kether:
        - 'tell "&6[BilibiliVideo] &f你获得了 VIP 三连奖励！"'
        - 'command papi "give %player_name% diamond 10"'
        - 'command papi "give %player_name% emerald 5"'

    # 活动模板（限时活动）
    event:
      description: "活动期间的特殊奖励"
      kether:
        - 'tell "&d[活动奖励] &f恭喜完成三连！"'
        - 'command papi "crate givekey %player_name% activity 1"'

  # 视频与奖励映射
  videos:
    # 为特定视频指定奖励模板
    BV1xx411c7mD:
      rewardKey: "vip"           # 使用 VIP 模板

    BV1yy411c7mE:
      rewardKey: "event"         # 使用活动模板

    BV1zz411c7mF: {}             # 使用默认模板（可省略 rewardKey）
```
### GUI配置 (` gui.yml `)
````yaml


Title: "&fBiliBiliVideo &7| &f三连奖励"
#不要填写过多视频占位符 请求过多B站会风控！！！
#不要填写过多视频占位符 请求过多B站会风控！！！
#不要填写过多视频占位符 请求过多B站会风控！！！
Layout:
  - "####i####"
  - "#1111111#"
  - "#p#####n#"

Icons:
  # 隔板
  '#':
    display:
      #物品名称
      name: "&7▪ 我只是个隔板"
      #物品材质
      mats: "gray_stained_glass_pane"
      #物品CustomModelData
      cmd: '-1'
      #物品描述
      lore:
        - "&7爱上一只小猪?"

  # BiliBiliVideo
  '1':
    type: VIDEO
    display:
      name: "⭐&6 %bv%"
      mats: "gold_ingot"
      lore:
        - "&8&m                        "
        - "📺&b&l 三连状态"
        - " &7▪ 点赞: &f%status_like%"
        - " &7▪ 收藏: &f%status_favorite%"
        - " &7▪ 投币: &f%status_coin%"
        - " &7▪ 三连: &f%status%"
        - "&8&m                        "
        - "🎁&b&l 三连奖励"
        - " &7▪ %description%"
        - "&8&m                        "
        - "✅&b&l 奖励状态"
        - " &7▪ %claim_status%"
        - "&8&m                        "
        - "✔&a&l 左键领取奖励 &7| &r🔗&b&l 右键查看视频链接"
        - "&8&m                        "
  'p':
    type: PREVIOUS
    display:
      name: "&f上一页"
      mats: "paper"
      lore:
        - "&7点击前往上一页"
        - "&7当前页: &e%page%"

  'n':
    type: NEXT
    display:
      name: "&f下一页"
      mats: "paper"
      lore:
        - "&7点击前往下一页"
        - "&7当前页: &e%page%"
  # 玩家信息
  'i':
    display:
      name: "📋&b&l BiliBiliVideo &7| &f三连奖励"
      mats: "player_head"
      lore:
        - "&8&m                        "
        - " &7点击查看三连奖励"
        - "&8&m                        "
````
### 语言配置(`lang/zh_CN.yml`)
````yaml
prefix: '&7[&bBiliBili三连奖励&7] &r'
no-permission: '&c⚠ 你没有权限执行该指令!'
player-only: '&c⚠ 此命令只能由玩家执行!'
unknown-command: '&c⚠ 未知指令!'

# 账号未绑定
account-not-bind: '❌&c B站账号未绑定，请通过 &e/bv qrcode &c扫码登录'

# 账号绑定提示
account-bind-tip: '📱&a 已为你生成二维码，请使用手机扫码完成绑定'

# 账号绑定成功提示
account-bind-success: '✅&a 已成功绑定 B 站账号 &e{user_name} &7(&e{user_mid}&7)'
# 账号信息
account-bind-info:
  - '&8&m                        '
  - '📋&b&l 当前绑定信息'
  - ' &7▪ 玩家: &f{player}'
  - ' &7▪ B 站 UID: &f{user_mid}'
  - ' &7▪ B 站昵称: &f{user_name}'
  - '&8&m                        '

# 凭证未保存
token-not-save: '❌&c 尚未为你保存登录凭证，请通过 &e/bv qrcode &c扫码登录'

# 凭证信息
token-info:
  - '&8&m                        '
  - '🔑&b&l 当前凭证信息'
  - ' &7▪ 标签: &f{token_tag}'
  - ' &7▪ 状态: &f{token_status}'
  - ' &7▪ 绑定 UID: &f{user_mid}'
  - '&8&m                        '

# 凭证列表
token-info-list: '&7  ▪ &fUID: {user_mid} &7| &f状态: {token_status}'

# 凭证查找
token-find-success:
  - '&8&m                        '
  - '🔍&b&l 凭证详情'
  - ' &7▪ UID: &f{user_mid}'
  - ' &7▪ 状态: &f{token_status}'
  - ' &7▪ lastUsedAt: &f{token_lastUsedAt}'
  - ' &7▪ expiredAt: &f{token_expiredAt}'
  - '&8&m                        '
token-find-fail: '&c❌ 未找到名为 &e{token_label} &c的凭证'

# 视频三连信息
video-status:
  - '&8&m                        '
  - '📺&b&l 视频 &e{bv} &b的三连状态'
  - ' &7▪ {video_like}'
  - ' &7▪ {video_coin}'
  - ' &7▪ {video_favorite}'
  - ' &7▪ {video_triple}'
  - '&8&m                        '

# 视频点赞
video-like-success: '👍&a 已点赞'
video-like-fail: '👎&c 未点赞'

# 视频投币
video-coin-success: '🪙&a 已投币'
video-coin-fail: '🪙&c 未投币'

# 视频收藏
video-favorite-success: '⭐&a 已收藏'
video-favorite-fail: '⭐&c 未收藏'

# 三连状态
video-triple-success: '🎉&a 已完成三连'
video-triple-fail: '📝&e 未完成三连'

# 三连奖励领取提示
video-claim: '🎁&a 成功领取三连奖励'
video-not-claim: '⚠&e 尚未完成该视频的点赞、投币、收藏，无法领取奖励'
video-claimed: '✅&a 已领取过该奖励'
video-claim-error: '❌&c 领取奖励失败，请稍后重试'
video-claim-warn: '⚠&e 已记录该视频三连，但并没有设置奖励'

# 视频链接提示
video-url: '🔗&b&l 视频链接&7: &fhttps://www.bilibili.com/video/{bv}'
# 视频不存在提示
video-not-find: '⚠&e 视频 &e{bv} &e并未登记'

# 二维码失效
qrcode-invalid: '❌&c 二维码已失效，请重新执行 &e/bv qrcode'

# GUI领取文本
gui-video-claim: '✅&a 点击领取奖励'
gui-video-cant-claim: '❌&c 不可领取'
gui-video-already-claim: '✔&a 已领取该奖励'

# 解绑提示
admin-unbind-success: '✅&a 已解除玩家 &e{player} &a与 B 站账号 &e{user_uid} &a的绑定'
admin-unbind-fail: '❌&c 未找到与 &e{player} &c匹配的玩家、UUID 或 B 站 UID'
````
#### 🎨 Kether 脚本语法示例

[Kether Explorer](https://taboolib.hhhhhy.kim/kether-list/)

---