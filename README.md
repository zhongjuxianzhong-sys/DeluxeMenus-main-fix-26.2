# DeluxeMenus for Paper 26.2

这是一个面向 Minecraft 26.2 Paper 服务端适配的 DeluxeMenus 源码分支。DeluxeMenus 是一款由 YAML 配置驱动的物品栏 GUI 菜单插件，可根据权限、PlaceholderAPI 变量和条件为玩家展示不同物品，并在点击后执行命令、消息、菜单跳转等操作。

本分支基于 DeluxeMenus `1.14.2`，主要完成了 Paper 26.2 API、Java 25、Gradle 9、完整依赖打包、新版物品 `CUSTOM_DATA` 数据组件及菜单生命周期线程安全的适配。

## 主要功能

- 使用 YAML 创建自定义箱子菜单。
- 为菜单注册一个或多个打开命令。
- 支持 PlaceholderAPI 变量和菜单参数。
- 支持查看要求、物品显示要求和点击要求。
- 支持玩家命令、控制台命令、消息、声音、关闭菜单等点击动作。
- 支持动态物品名称、Lore、数量、模型数据和数据组件。
- 支持 Vault、ItemsAdder、Oraxen、Nexo、CraftEngine、MMOItems 等可选插件钩子。
- 使用 PDC 标记临时菜单物品，降低菜单物品复制风险。
- 菜单打开、刷新、关闭和重载统一在 Paper 主线程执行，避免异步 Inventory、PlaceholderAPI 和第三方 Hook 调用。

## 环境要求

| 组件 | 要求 |
| --- | --- |
| 服务端 | Paper 26.2 |
| Java | Java 25 或更高版本 |
| 必需依赖 | [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) |
| 构建工具 | Gradle 9.6.0 |

未安装 PlaceholderAPI 时，DeluxeMenus 会在启动阶段自行禁用。

## 线程安全

菜单及其依赖的 Bukkit API 并不支持异步访问。本分支将菜单生命周期统一限制在 Paper 主线程：

- `openMenu`、`refreshForAll`、`closeMenu` 和菜单卸载入口从异步线程调用时会自动投递到主线程。
- `PreOpen`、要求判断、PlaceholderAPI、Vault、JavaScript、第三方物品 Hook、物品构建、Inventory 填充和菜单事件均在同一主线程上下文完成。
- 菜单刷新和 Placeholder 更新使用同步重复任务，并在菜单关闭、玩家退出、重载和停服时取消。
- 每次刷新都会确认 Holder 仍属于该玩家当前打开的菜单；关闭后的排队刷新不会再更新 Inventory。
- 菜单注册表按玩家 UUID 保存 Holder，并向命令和 bStats 返回不可变快照，避免重载时并发遍历可变集合。

仍保留的异步操作只包含外部 HTTP（更新检查、Dump 上传）和仅操作并发冷却状态的清扫任务。它们的 Bukkit 状态、日志和玩家消息回调都会切回主线程。

这意味着第三方插件可以从异步线程调用现有公开菜单 API，而无需自行安排主线程任务；但不要在自己的异步代码中直接读取或修改 `Player`、`Inventory`、`ItemStack` 或菜单 Hook。

## 构建和测试

项目目标为 Java 25 和 Gradle 9.6。发布构建使用 Shadow 打包，输出 JAR 位于 `build/libs/`：

```text
DeluxeMenus-<version>.jar        # 可部署的 Shadow JAR
DeluxeMenus-<version>-plain.jar  # 未打包依赖的普通 JAR
```

在 Windows PowerShell 中：

```powershell
# Wrapper JAR 存在时
./gradlew.bat compileJava --no-daemon
./gradlew.bat test --no-daemon
./gradlew.bat build --no-daemon

# 当前工作区缺少 gradle/wrapper/gradle-wrapper.jar 时，使用本地 Gradle 9.6
& .\build\gradle-9.6.0\bin\gradle.bat `
  --gradle-user-home .\build\.gradle-user-local `
  build --no-daemon
```

测试使用 JUnit 5 和 Mockito，不依赖 MockBukkit，覆盖主线程投递、并发菜单注册表快照、失效 Holder 刷新、更新检查结果发布及 Dump 回调调度。

静态检查可确认菜单包不包含异步调度调用：

```powershell
rg -n "runTaskAsynchronously|runTaskTimerAsynchronously" src/main/java/com/extendedclip/deluxemenus/menu
rg -n "callSyncMethod|\.get\(\)" src/main/java/com/extendedclip/deluxemenus/hooks/MMOItemsHook.java
```

## 菜单配置

首次启动后，独立菜单配置目录为：

```text
plugins/DeluxeMenus/gui_menus/
```

最小菜单示例：

```yaml
menu_title: '&8示例菜单'
open_command:
  - examplemenu
  - emenu
size: 9

items:
  welcome:
    material: DIAMOND
    slot: 4
    display_name: '&b欢迎，%player_name%'
    lore:
      - '&7这是一个 Paper 26.2 菜单。'
    left_click_commands:
      - '[message] &a你点击了示例物品。'
      - '[close]'
```

保存配置后，可以重新加载全部菜单：

```text
/dm reload
```

也可以只重新加载指定菜单：

```text
/dm reload <菜单名>
```

更完整的菜单选项、要求和动作格式请参考 [DeluxeMenus Wiki](https://wiki.helpch.at/clips-plugins/deluxemenus/)。

## Paper 26.2 NBT 支持

Minecraft 26.2 使用物品数据组件保存自定义数据。此分支已将旧版 NMS 标签逻辑迁移到 `DataComponents.CUSTOM_DATA`，以下配置项可以继续使用：

```text
nbt_byte, nbt_bytes
nbt_short, nbt_shorts
nbt_int, nbt_ints
nbt_string, nbt_strings
```

示例：

```yaml
items:
  custom_data_item:
    material: PAPER
    slot: 0
    nbt_string: 'item_type:menu_reward'
    nbt_int: 'reward_level:5'
    nbt_bytes:
      - 'enabled:1'
```

这些值会写入物品的原生 `CUSTOM_DATA` 组件，并保留 Byte、Short、Int 和 String 类型。该兼容层通过运行时反射访问 Paper 26.2 的 Mojang 映射内部类，不会把 NMS 类直接打包进插件。

本实现已在 Paper `26.2-111` 上进行隔离启动验证，启动日志应包含：

```text
[DeluxeMenus] NMS hook has been setup successfully!
```

如果未来 Paper 改动相关内部签名，插件会输出自定义数据钩子不可用的警告；普通菜单仍可加载，但上述 `nbt_*` 项将不会生效。

## 管理命令

主命令别名：`/deluxemenus`、`/dm`、`/deluxemenu`、`/dmenu`。

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/dm help` | 显示帮助 | `deluxemenus.admin` |
| `/dm open <菜单> [玩家] [-p:变量玩家]` | 打开菜单 | `deluxemenus.open` |
| `/dm list [all/页码]` | 列出已加载菜单 | `deluxemenus.list` |
| `/dm reload [菜单]` | 重载全部配置或指定菜单 | `deluxemenus.reload` |
| `/dm refresh <菜单> [-s]` | 刷新该菜单的在线查看者 | `deluxemenus.refresh` |
| `/dm dump <菜单/config>` | 创建配置诊断转储 | `deluxemenus.admin` |
| `/dm execute <玩家> <动作>` | 为玩家执行菜单动作 | 仅 OP |
| `/dm meta <玩家> <操作> ...` | 管理玩家持久元数据 | `deluxemenus.meta` |

额外常用权限：

| 权限 | 说明 |
| --- | --- |
| `deluxemenus.open.others` | 为其他玩家打开菜单 |
| `deluxemenus.open.bypass` | 跳过菜单打开要求 |
| `deluxemenus.placeholdersfor` | 使用 `-p:<玩家>` 解析目标玩家变量 |
| `deluxemenus.placeholdersfor.exempt` | 禁止其他人以自己作为变量解析目标 |
| `deluxemenus.menu.*` | 允许访问所有带菜单权限的菜单 |
| `deluxemenus.openrequirement.bypass.*` | 绕过所有菜单打开要求 |

## 可选插件集成

`plugin.yml` 中声明了以下软依赖，未安装时不会阻止 DeluxeMenus 启动：

- Vault
- HeadDatabase / HeadDB
- CraftEngine
- ItemsAdder
- Nexo
- Oraxen
- ExecutableItems / ExecutableBlocks
- MMOItems
- SimpleItemGenerator
- Score

使用可选物品来源前，请确保对应插件本身也兼容 Paper 26.2。

## 故障排查

### 插件启动后自行禁用

确认 PlaceholderAPI 已安装并成功启用，然后完整重启服务端。

### `nbt_*` 没有生效

检查启动日志是否包含 `NMS hook has been setup successfully!`。如果出现自定义数据钩子不可用警告，请确认服务端为官方 Paper 26.2 构建或兼容其 Mojang 映射内部结构的分支。

### 不建议热加载

不要使用 PlugMan 等工具热加载或热卸载 DeluxeMenus。修改配置后使用 `/dm reload`，更新插件 JAR 时完整重启服务端。

### 出现 `AsyncCatcher`、并发修改或菜单刷新异常

本分支的菜单包不应创建异步 Bukkit 任务。先检查是否有其他插件或自定义扩展从异步代码直接操作了 `Player`、`Inventory`、`ItemStack` 或 DeluxeMenus 的内部对象。第三方调用应使用 `Menu#openMenu`、`Menu#closeMenu` 或 `Menu#refreshForAll` 等公开入口，让插件自动投递到主线程。

若问题只发生在特定物品来源，确认对应可选插件已启用且兼容 Paper 26.2；MMOItems 等 Hook 会在菜单主线程边界内调用，不应从异步线程单独调用。

## 上游项目与支持

- [DeluxeMenus GitHub](https://github.com/HelpChat/DeluxeMenus)
- [问题跟踪](https://github.com/HelpChat/DeluxeMenus/issues)
- [官方 Wiki](https://wiki.helpch.at/clips-plugins/deluxemenus/)
- [Spigot 页面](https://www.spigotmc.org/resources/11734/)
- [Discord 支持](https://helpch.at/discord)
- [bStats](https://bstats.org/plugin/bukkit/DeluxeMenus/445)

## 许可证

本项目沿用上游 DeluxeMenus 的 [MIT License](LICENSE)。
