# sql-cli Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Java 21 CLI tool that connects to relational databases via JDBC, with connection management, safety guards, and a Claude Code skill.

**Architecture:** Layered architecture: CLI (Picocli) → Safety → Config/Connection → Executor → Dialect → Driver Loader. All JDBC drivers are external (user-managed in ~/.sql-cli/drivers/). Configuration stored in YAML with AES-encrypted passwords.

**Tech Stack:** Java 21, Gradle, Picocli, SLF4J+Logback, JUnit 5+Mockito, SnakeYAML, Gradle Shadow

---
