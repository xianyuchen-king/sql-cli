# Changelog

## [1.0.0] - 2026-03-28

### 新增
- 多数据库支持：MySQL、Oracle、PostgreSQL、SQLite、达梦
- 连接管理：添加、更新、删除、测试、重命名
- 密码加密（AES-256-GCM）
- 安全级别控制：严格（仅 SELECT）、正常、无
- 数据导出：CSV、JSON、SQL INSERT/UPDATE
- 数据导入：CSV、SQL
- 驱动管理：自动下载、本地注册
- 元数据浏览：数据库、表、列、索引
- npm 全局安装支持
- Claude Code Skill 集成

### 修复
- 驱动加载器 URLClassLoader 过早关闭问题
- conn add 非交互模式自动检测

## [0.1.0] - 2026-03-27

### 新增
- 项目初始化
- 基础 CLI 框架
- MySQL 连接支持
