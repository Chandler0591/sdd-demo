# Spec Delta

## ADDED Requirements

### Requirement: Shelf location dictionary seed
首次部署的字典种子数据 MUST 包含「书架位置」顶层字典项（code=shelf-location）及其多级示例位置子项（区 / 排 / 号 两级），供上架确认的位置树形下拉使用。

#### Scenario: 种子执行后书架位置可选
- **WHEN** 管理员执行字典种子脚本后打开上架确认表单
- **THEN** 位置树形下拉展示种子中的示例位置层级（如 A 区 → 1 排 → 01 号）
