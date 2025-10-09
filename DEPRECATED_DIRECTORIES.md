# 已废弃目录说明（Deprecated Directories)

本仓库包含一个旧版快照目录，用于迁移期间参考：

- `RoadWeaver/`：旧的多模块快照（含 `fabric/`、`neoforge/`、旧版 `build.gradle` 等）。
  - 该目录不参与当前构建，也未纳入 Gradle 子项目（`settings.gradle` 仅包含 `common`、`fabric`、`neoforge`）。
  - `.gitignore` 已忽略此目录，避免误提交和 IDE 自动索引带来的干扰。
  - 如果确需对比/参考旧实现，请只复制所需片段到 `common/` 或对应平台模块中并完成适配，不要直接修改此目录。

## 为什么保留？
- 迁移期便于检索旧代码（算法/装饰/渲染）的上下文与差异。
- 辅助验证 Architectury 迁移后行为一致性。

## 后续计划
- 当迁移完成并验证稳定后，建议彻底删除 `RoadWeaver/` 目录，避免长期混淆。

## 相关文档
- `ARCHITECTURY_MIGRATION_GUIDE.md`：包含迁移步骤、排除策略与注意事项。
- `.gitignore`：已添加 `/RoadWeaver/**` 忽略规则。
