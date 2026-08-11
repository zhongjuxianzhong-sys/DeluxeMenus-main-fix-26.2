[logo]: https://github.com/HelpChat/DeluxeMenus/assets/52609756/f24ac57d-98db-4d57-a723-791a2654e73f

[issues]: https://github.com/HelpChat/DeluxeMenus/issues
[licenseImg]: https://img.shields.io/github/license/helpchat/deluxemenus?&logo=github
[license]: https://github.com/HelpChat/DeluxeMenus/blob/master/LICENSE

[bstatsImg]: https://img.shields.io/bstats/servers/445
[bstats]: https://bstats.org/plugin/bukkit/DeluxeMenus/445

[discordImg]: https://img.shields.io/discord/164280494874165248?color=5562e9&logo=discord&logoColor=white
[discord]: https://helpch.at/discord
[spigot]: https://www.spigotmc.org/resources/11734/

[ci]: http://ci.extendedclip.com/job/DeluxeMenus/
[ciImg]: http://ci.extendedclip.com/buildStatus/icon?job=DeluxeMenus

[contributing]: https://github.com/HelpChat/DeluxeMenus/blob/main/CONTRIBUTING.md

[![logo]][spigot]

[![ciImg]][ci] [![bstatsImg]][bstats] [![discordImg]][discord] [![licenseImg]][license] [![GitBook](https://img.shields.io/static/v1?message=Documented%20on%20GitBook&logo=gitbook&logoColor=ffffff&label=%20&labelColor=5c5c5c&color=3F89A1)](https://wiki.helpch.at/helpchat-plugins/deluxemenus)


# 简介
[DeluxeMenus][spigot] 是一款一体化的物品栏 GUI 菜单插件！
你可以创建通过自定义命令打开的 GUI 菜单，用于显示统计信息，或执行与打开该菜单的玩家相关的操作。菜单支持完全配置。你可以为不同玩家显示特定物品，也可以根据特定 GUI 中某个槽位所满足的 JavaScript 条件执行不同操作。

DeluxeMenus 依赖 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)。

## Paper 26.2 支持

此源码分支面向 Minecraft 26.2 Paper 服务端构建，`plugin.yml` 的 `api-version` 为 `26.2`。

### 运行要求

- Paper `26.2` 服务端。
- Java 25 或更高版本。
- 已安装 PlaceholderAPI；插件在启动时未检测到 PlaceholderAPI 会自行禁用。

### 从源码构建

Windows PowerShell：

```powershell
.\gradlew.bat clean build
```

Linux/macOS：

```bash
./gradlew clean build
```

构建会使用 Gradle Wrapper（9.6.0）和 Shadow 打包插件。可部署的完整依赖 JAR 位于：

```text
build/libs/DeluxeMenus-<version>.jar
```

普通 JAR 会以 `-plain.jar` 结尾，仅用于开发或调试，不能放入服务端 `plugins` 目录。部署时使用不带 `-plain` 后缀的 Shadow JAR；它已包含并重定位 bStats、Adventure 和 Nashorn 等运行依赖。

### 安装

1. 完整停止 Paper 服务端。
2. 将 `build/libs/DeluxeMenus-<version>.jar` 复制到服务端的 `plugins` 目录。
3. 确保不要同时保留旧版 DeluxeMenus JAR 或 `-plain.jar`。
4. 完整启动服务端并检查控制台加载日志。

### 26.2 已知限制

Minecraft 26.2 使用新的数据组件模型，旧版 NMS 反射 NBT 实现不再兼容。因此以下菜单物品配置项在 Paper 26.2 上会被跳过：

```text
nbt_byte, nbt_bytes, nbt_short, nbt_shorts,
nbt_int, nbt_ints, nbt_string, nbt_strings
```

菜单物品防复制标记仍使用 Bukkit/Paper 的 PersistentDataContainer，可正常工作。其余常用菜单功能，例如物品名称、Lore、附魔、模型数据、PlaceholderAPI、权限和点击动作，已使用 Paper 26.2 API 编译验证。

## 参与贡献
如果你希望为 DeluxeMenus 做出贡献，请阅读我们的[贡献指南][contributing]，了解参与方式及需要注意的事项。

## 获取支持
- [问题跟踪][issues]
- [Discord 支持][discord]

## 快捷链接
- [Wiki](https://wiki.helpch.at/clips-plugins/deluxemenus/)
- [CI 服务器][ci]
- [Spigot 页面][spigot]
- [插件统计][bstats]

