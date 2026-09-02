# 将 Fluid Air 功能合并进 TweakerMore 的研究与 PR 实施指南

研究基线：`Fallen-Breath/tweakermore@219f1cf0c34bebe80c74f94d275e2eda567b8cb5`（v3.33.0，2026-08-21 检查）。本文只给迁移和提交方案，没有修改 TweakerMore，也没有创建 PR。

## 结论

1. PR 应以 `master` 为 base。远端默认分支为 `master`；检查时 `master` 与 `dev` 都指向 `219f1cf0`，当前开放的外部 PR [#177](https://github.com/Fallen-Breath/tweakermore/pull/177) 也以 `master` 为目标。当前仓库没有 1.20.1 专用开发分支；1.20.1 是多版本工程中的 Gradle 子项目，列在 [`settings.json`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/settings.json)，具体依赖在 [`versions/1.20.1/gradle.properties`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/versions/1.20.1/gradle.properties)。
2. 不要把 Fluid Air 的独立 malilib 页面、配置存储、入口类和 Gradle 依赖原样搬过去。TweakerMore 已有统一 GUI、热键和存储体系；只需声明一个 `TweakerMoreConfigBooleanHotkeyed`，注解收集器会把它加入现有页面。
3. 推荐配置 ID 为 `disableFluidMovementEffects`，字段名为 `DISABLE_FLUID_MOVEMENT_EFFECTS`，类型为 `Config.Type.DISABLE`，分类为 `Config.Category.MC_TWEAKS`。这个名字比 `disableFluidPhysics` 更准确，因为当前实现不会取消溺水、熔岩伤害、着火、呼吸或服务端校正。
4. Mixin 不能在用户按热键后动态“注入”。TweakerMore 的惯例是启动时加载 Mixin，每个注入点在运行时检查布尔配置；热键只切换配置值。
5. 仓库没有 `CONTRIBUTING.md` 或 PR 模板，也没有规则要求先开 issue；但本功能涉及四个移动路径且仓库 CI 构建全部版本，强烈建议先用现有 [Feature Request 模板](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/.github/ISSUE_TEMPLATE/feature_request.yml)确认维护者接受的名称、范围，以及“仅 1.20.1”还是“所有支持版本”。类似的 `disableCreativeFlyLandingExitFlying` 先有 [#109](https://github.com/Fallen-Breath/tweakermore/issues/109)，随后由提交 [`343c032c`](https://github.com/Fallen-Breath/tweakermore/commit/343c032c584555d8f9ffca9bbe0058503619d0ae)实现；但也存在直接提交的外部 PR，例如 [#154](https://github.com/Fallen-Breath/tweakermore/pull/154)，所以 issue 是降低返工风险的建议，不是硬门槛。

## 仓库约定

### 配置与热键

在 [`TweakerMoreConfigs.java`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/config/TweakerMoreConfigs.java#L535-L601) 的 MC Tweaks / Disable 区域按字母顺序加入：

```java
@Config(type = Config.Type.DISABLE, category = Config.Category.MC_TWEAKS)
public static final TweakerMoreConfigBooleanHotkeyed DISABLE_FLUID_MOVEMENT_EFFECTS =
        newConfigBooleanHotkeyed("disableFluidMovementEffects");
```

[`ConfigFactory.newConfigBooleanHotkeyed(String)`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/config/ConfigFactory.java#L42-L57) 已将默认值设为 `false`、默认热键设为空字符串。配置字段由 [`TweakerMoreConfigs.loadConfigFields()`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/config/TweakerMoreConfigs.java#L1169-L1213) 反射收集；[`TweakerMoreConfigStorage`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/config/TweakerMoreConfigStorage.java#L113-L130) 自动把 `DISABLE` 项写入 `DisableHotkeys` / `DisableToggles`。因此不需要迁移 `FluidAirInitializationHandler`、`FluidAirKeybindProvider`、`FluidAirConfigScreen` 或 `FluidAirConfigs` 的文件读写代码。

### Mixin 风格

新类应放在：

`src/main/java/me/fallenbreath/tweakermore/mixins/tweaks/mc_tweaks/disableFluidMovementEffects/`

并把四个类按字母顺序登记到 [`tweakermore.mixins.json`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/resources/tweakermore.mixins.json)。现有同类实现会先检查配置，再用 `self == Minecraft.getInstance().player` 限定本地玩家，例如 [`disableHoneyBlockEffect/EntityMixin`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/mixins/tweaks/mc_tweaks/disableHoneyBlockEffect/EntityMixin.java) 和 [`disableSlimeBlockBouncing/EntityMixin`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/mixins/tweaks/mc_tweaks/disableSlimeBlockBouncing/EntityMixin.java)。当前 MC Tweaks 大量使用 MixinExtras 的 `@ModifyExpressionValue`，新代码宜用它替代 Fluid Air 里的 `@Redirect`，以降低注入冲突。

TweakerMore 1.20.1 使用 Mojang/Parchment 命名，而 Fluid Air 使用 Yarn。根据仓库的 [1.20.1 映射配置](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/versions/1.20.1/gradle.properties)和本地生成的 1.20.1 layered mappings，迁移时对应关系如下：

| Fluid Air / Yarn | TweakerMore 1.20.1 / Mojmap | 处理 |
| --- | --- | --- |
| `Entity.updateMovementInFluid` | `Entity.updateFluidHeightAndDoFluidPushing` | HEAD 保存 `getDeltaMovement()`，RETURN 恢复；只处理 `FluidTags.WATER` / `LAVA`，不要直接返回 `false`，否则会破坏浸液状态检测 |
| `onBubbleColumnSurfaceCollision` | `onAboveBubbleCol` | 本地玩家且开关开启时取消 |
| `onBubbleColumnCollision` | `onInsideBubbleColumn` | 本地玩家且开关开启时取消 |
| `LivingEntity.tickMovement` | `LivingEntity.aiStep` | 将其中的 `isInWater()` / `isInLava()` 表达式改成 `false` |
| `LivingEntity.travel` | `LivingEntity.travel` | 同上，使它走空气/地面移动分支 |
| `PlayerEntity.updateSwimming` | `Player.updateSwimming` | 本地玩家且开启时 `setSwimming(false)` 并取消原方法 |
| `ClientPlayerEntity.tickMovement` | `LocalPlayer.aiStep` | 将直接调用的 `isInWater()` / `isUnderWater()` 表达式改成 `false`，避免进入游泳姿态和终止疾跑 |
| `Vec3d` | `Vec3` | 保存/恢复速度字段类型 |

保留“先执行原版流体高度/接触计算，再恢复速度”的设计。这能去掉水流/熔岩流推力，同时保留水下视野、呼吸、伤害等其他系统依赖的接触状态。不要把 `isInWater()` 在整个实体类上全局改为 `false`。

### 多版本边界

[`build.gradle`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/build.gradle) 用 ReplayMod/Fallen-Breath preprocessor 连接全部版本节点，CI 的 [`gradle.yml`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/.github/workflows/gradle.yml) 在每个 PR 上调用全工程 `build`。`versions/1.20.1/src/main/java` 是版本边界覆盖层，不是独立长期分支。

可选方案：

- 推荐：先让维护者确认支持范围，然后把实现放进共享源码，用 `//#if MC ...` / `//$$` 适配发生变化的方法，并对所有受支持子项目运行构建和 Mixin audit。
- 仅 1.20.1：配置和每个 Mixin 使用 `@Restriction(require = @Condition(value = ModIds.minecraft, versionPredicates = "=1.20.1"))`。共享源码中放同名 `@Mixin(DummyClass.class)` 空壳；在 `versions/1.20.1/src/main/java/...` 覆盖为真实 Mixin；再在 `versions/1.20.2/src/main/java/...` 覆盖回 `DummyClass`，保证真实实现只在 1.20.1 节点存在。四个真实 Mixin 仍应保留相同的精确 `@Restriction` 作为运行时防线。这种“一版专用”方案维护成本高，未经 issue 确认不建议直接提交。

不能只在 `versions/1.20.1` 随手放四个类后就认为其他版本不受影响；预处理节点会向相邻版本传播源文件。仓库现有 `mcSpectatorEnterSinkingFixPorting` 在共享源码和 `versions/1.20.1` 覆盖层之间切换真实 Mixin / `DummyClass`，可作为边界写法参考：[`shared`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/mixins/tweaks/porting/mcSpectatorEnterSinkingFixPorting/ClientPlayNetworkHandlerMixin.java)、[`1.20.1 override`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/versions/1.20.1/src/main/java/me/fallenbreath/tweakermore/mixins/tweaks/porting/mcSpectatorEnterSinkingFixPorting/ClientPlayNetworkHandlerMixin.java)。

## 从 Fluid Air 到 TweakerMore 的迁移映射

| Fluid Air 文件 | TweakerMore 去向 | 决策 |
| --- | --- | --- |
| `config/FluidAirConfigs.java` | `config/TweakerMoreConfigs.java` | 只迁移一个 hotkeyed boolean；删除独立存储、GUI 热键和 helper |
| `config/FluidAirInitializationHandler.java` | 无 | TweakerMore 已统一初始化配置 |
| `config/FluidAirKeybindProvider.java` | 无 | 配置对象本身就是可绑定热键 |
| `gui/FluidAirConfigScreen.java` | 无 | 选项自动进入 TweakerMore 的 Disable / MC Tweaks 页面 |
| `mixin/EntityFluidPhysicsMixin.java` | `.../disableFluidMovementEffects/EntityMixin.java` | 改 Mojmap 名称并采用本地玩家等值判断 |
| `mixin/LivingEntityFluidPhysicsMixin.java` | `.../disableFluidMovementEffects/LivingEntityMixin.java` | 推荐把 `@Redirect` 改为 `@ModifyExpressionValue` |
| `mixin/PlayerEntityPoseMixin.java` | `.../disableFluidMovementEffects/PlayerMixin.java` | 强制非游泳姿态 |
| `mixin/ClientPlayerMovementMixin.java` | `.../disableFluidMovementEffects/LocalPlayerMixin.java` | 修正疾跑和水下判定 |
| `physics/FluidAirMovementPolicy.java` | 默认不迁移 | 目前只有 `!enabled` 两个表达式，内联更符合仓库风格；保留它不会真正测试 Mixin |
| `FluidAirMovementPolicyTest.java` | 默认不迁移 | TweakerMore 使用 JUnit 4，且现有测试重点是翻译一致性；如坚持保留需改为 JUnit 4，但仍需游戏内验证 |
| `fluidair.mixins.json` | `tweakermore.mixins.json` | 追加四项，不新建第二套 Mixin 配置 |
| `en_us.json` / `zh_cn.json` | `assets/tweakermore/lang/en_us.yml` / `zh_cn.yml` | 两种语言以完全相同的键顺序添加 |
| `fabric.mod.json`、`build.gradle` | 无 | 不复制独立 mod 元数据和依赖；沿用 TweakerMore 工程 |

## 翻译和文档

在 [`en_us.yml`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/resources/assets/tweakermore/lang/en_us.yml) 与 [`zh_cn.yml`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/resources/assets/tweakermore/lang/zh_cn.yml) 的 `tweakermore.config` 下、按配置 ID 字母顺序加入同一组键：

```yaml
disableFluidMovementEffects:
  .: disableFluidMovementEffects
  comment: |-
    Ignore water, lava and bubble-column movement effects for the local player
    This does not disable drowning, lava damage, fire, or server-side movement checks
  pretty_name: Disable Fluid Movement Effects
```

中文建议为“禁用流体移动效果”，注释明确“仅改变本地玩家移动；不禁用溺水、熔岩伤害、着火或服务端移动检查”。仓库的 [`TranslationOrderTest`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/test/java/me/fallenbreath/tweakermore/tests/TranslationOrderTest.java) 会检查 `en_us` / `zh_cn` 的键数量和顺序一致。

还应更新生成的 `docs/document-en_us.md` 和 `docs/document-zh_cn.md`。文档生成器由 [`DocumentGenerator`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/util/doc/DocumentGenerator.java) 驱动，开发环境可启用 `-Dtweakermore.gen_doc=true`；相似功能提交 `343c032c` 同时更新了配置、Mixin、两种语言、Mixin JSON 和两份文档。

## 实施与提交步骤

```powershell
# 1. 在 GitHub 网页 fork Fallen-Breath/tweakermore 后
git clone https://github.com/<你的账号>/tweakermore.git
Set-Location tweakermore
git remote add upstream https://github.com/Fallen-Breath/tweakermore.git
git fetch upstream
git switch -c feature/disable-fluid-movement-effects upstream/master

# 2. 完成配置、Mixin、翻译、文档修改后，先验证 1.20.1
.\gradlew.bat :1.20.1:test :1.20.1:build
.\gradlew.bat :1.20.1:runClientMixinAudit

# 3. PR 前必须跑与 CI 相同的全工程构建
.\gradlew.bat build
git diff --check
git status --short

# 4. 提交并推送 fork
git add src versions docs
git commit -m "Add disableFluidMovementEffects tweak"
git push -u origin feature/disable-fluid-movement-effects
```

`runClientMixinAudit` 来自 [`common.gradle`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/common.gradle#L154-L177)，会以 `tweakermore.mixin_audit=true` 启动客户端并在审计后退出。GitHub fork/PR 操作可按官方文档：[Fork a repository](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/fork-a-repo)、[Create a pull request from a fork](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork)。

Gradle 构建会自动执行 Java license header 格式化（见 [`common.gradle`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/common.gradle)），所以构建后必须再次检查 diff，避免把无关格式变化带进 PR。

## 验证矩阵

| 场景 | 关闭（必须保持原版） | 开启（期望） |
| --- | --- | --- |
| 水中落地疾跑 + 跳跃 | 原版游泳/阻力 | 保持疾跑、正常地面跳跃、无抽搐、不进入游泳姿态 |
| 水中悬空按跳跃 | 原版上浮/游泳 | 不上浮，按空气重力下落 |
| 流水 | 受水流推动 | 无推动、无水阻 |
| 熔岩 | 原版熔岩阻力 | 移动按空气分支；仍受伤、着火 |
| 灵魂沙气泡柱 | 上浮 | 不上浮，地面仍可疾跑跳跃 |
| 岩浆块气泡柱 | 下拉 | 不下拉 |
| 潜行/下沉 | 水中下沉 | 不触发水中下沉逻辑 |
| 热键即时切换 | N/A | 无需重启，下一 tick 生效；重启后开关和绑定持久化 |
| 单人世界 | 原版 | 行为稳定，无姿态抖动 |
| 原版专用服务器 | 原版 | 客户端预测生效；记录是否发生 rubber-banding |
| 有反作弊服务器 | 原版 | 明确可能被纠正或踢出，不承诺绕过服务端校验 |
| 载具、旁观、创造飞行、鞘翅 | 原版 | 不影响载具和其他实体；检查模式切换无回归 |
| 呼吸、溺水、熔岩伤害、相机迷雾 | 原版 | 仍按原版，证明没有全局伪造流体接触状态 |

自动验证至少包括：`:1.20.1:test`、`:1.20.1:build`、`:1.20.1:runClientMixinAudit`、全工程 `build`。当前仓库唯一直接相关的单元测试模式是翻译顺序测试，不能替代上述游戏内矩阵。

## PR 内容建议

标题：

```text
Add disableFluidMovementEffects tweak
```

正文草稿：

```markdown
## Summary

- add a disabled-by-default, hotkey-togglable `disableFluidMovementEffects` option
- make the local player use air/ground movement in water and lava
- suppress water/lava current velocity and bubble-column lift/drag
- prevent swimming pose and fluid sprint cancellation while enabled

## Scope

This is client-side movement prediction only. It does not disable drowning,
lava damage, fire, breathing, camera submersion effects, server movement checks,
or anti-cheat corrections. Servers may still correct the player's position.

## Implementation

Fluid contact/height calculation is preserved. The mixin restores the velocity
that existed before vanilla fluid pushing, so systems that need water/lava contact
state keep working. All behavior is restricted to `Minecraft.getInstance().player`.

## Testing

- [ ] `./gradlew :1.20.1:test :1.20.1:build`
- [ ] `./gradlew :1.20.1:runClientMixinAudit`
- [ ] `./gradlew build`
- [ ] sprint-jump in still/flowing water
- [ ] water and lava mid-air movement
- [ ] soul-sand and magma bubble columns
- [ ] toggle at runtime and config persistence
- [ ] integrated server and vanilla dedicated server

Closes #<feature-request-number>
```

保持一个功能提交即可；不要把独立项目脚手架、构建产物或 IDE 文件提交进来。PR 中主动说明代码来自你自己的 MIT 项目 Fluid Air，并同意以 TweakerMore 的 LGPL-3.0 条款贡献。TweakerMore 的许可证和源文件头见 [`LICENSE`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/LICENSE) 与 [`HEADER.txt`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/HEADER.txt)。如果 Fluid Air 代码还有其他实际作者，需要先确认这些作者也允许该贡献，并保留必要的 MIT 版权通知。

## 一手来源索引

- 仓库定位、默认关闭和 `K+C` 配置入口：[`README.md`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/README.md)
- 支持版本列表：[`settings.json`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/settings.json)
- 1.20.1 依赖与 MC 范围：[`versions/1.20.1/gradle.properties`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/versions/1.20.1/gradle.properties)
- 多版本预处理图：[`build.gradle`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/build.gradle)
- 配置声明/收集：[`TweakerMoreConfigs.java`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/config/TweakerMoreConfigs.java)
- 默认关闭、空热键：[`ConfigFactory.java`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/java/me/fallenbreath/tweakermore/config/ConfigFactory.java)
- Mixin 注册：[`tweakermore.mixins.json`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/main/resources/tweakermore.mixins.json)
- 翻译测试：[`TranslationOrderTest.java`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/src/test/java/me/fallenbreath/tweakermore/tests/TranslationOrderTest.java)
- PR CI：[`gradle.yml`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/.github/workflows/gradle.yml)、[`build.yml`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/.github/workflows/build.yml)
- 功能请求格式：[`feature_request.yml`](https://github.com/Fallen-Breath/tweakermore/blob/219f1cf0c34bebe80c74f94d275e2eda567b8cb5/.github/ISSUE_TEMPLATE/feature_request.yml)
