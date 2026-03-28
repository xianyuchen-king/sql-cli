# 贡献指南 / Contributing Guide

感谢你对 sql-cli 的关注！欢迎通过以下方式参与项目。

## 反馈与建议

- **Bug 报告**：[提交 Bug](https://github.com/cyq/sql-cli/issues/new?template=bug_report.yml)，请附上复现步骤和环境信息
- **功能建议**：[提交建议](https://github.com/cyq/sql-cli/issues/new?template=feature_request.yml)
- **一般讨论**：[GitHub Discussions](https://github.com/cyq/sql-cli/discussions)

## 开发环境搭建

### 前置要求

- Java 21+
- Gradle（项目自带 wrapper，无需单独安装）

### 构建

```bash
git clone https://github.com/cyq/sql-cli.git
cd sql-cli
./gradlew shadowJar    # 构建 fat jar
./gradlew test         # 运行测试
```

### 项目结构

```
src/main/java/com/sqlcli/
├── SqlCliApp.java          # 入口，Picocli 顶层命令
├── cli/                    # CLI 命令（每个命令一个类）
├── config/                 # 配置管理、加密
├── connection/             # JDBC 连接管理
├── dialect/                # 数据库方言
├── executor/               # SQL 执行引擎
├── output/                 # 输出格式化（Markdown/JSON/CSV）
└── safety/                 # SQL 安全分析
```

## 提交 PR

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/my-feature`
3. 提交更改：确保通过测试 (`./gradlew test`)
4. 推送分支：`git push origin feature/my-feature`
5. 创建 Pull Request

### PR 规范

- 一个 PR 只做一件事
- 包含必要的测试
- 描述清楚改动内容和原因

## 代码规范

- 标准 Java 命名规范
- 新功能需要添加对应的测试
- 命令类放在 `cli/` 包下，遵循 Picocli 模式
- 新数据库支持通过 `dialect/` 包扩展

## 添加新数据库支持

1. 在 `dialect/` 下创建新的方言类，实现 `Dialect` 接口
2. 在 `DialectFactory` 中注册
3. 在 `ConfigManager` 的默认驱动映射中添加条目
4. 添加对应的测试

## License

提交代码即表示你同意以 MIT License 授权。
