#!/usr/bin/env node
'use strict';

const { existsSync, mkdirSync, copyFileSync } = require('fs');
const { join, dirname } = require('path');
const { homedir } = require('os');

// 1. Create config directories
const configDir = join(homedir(), '.sql-cli');
const driversDir = join(configDir, 'drivers');

if (!existsSync(configDir)) mkdirSync(configDir, { recursive: true });
if (!existsSync(driversDir)) mkdirSync(driversDir, { recursive: true });

// 2. Generate encryption key hint
const envVar = 'SQL_CLI_SECRET';
if (!process.env[envVar]) {
  const crypto = require('crypto');
  const secret = crypto.randomBytes(32).toString('base64');
  console.log('[sql-cli] Encryption key not set. Add to your shell profile:');
  console.log(`  export ${envVar}="${secret}"`);
}

// 3. Install skill for AI coding assistants (Claude Code, etc.)
const pkgRoot = join(__dirname, '..');
const skillFile = join(pkgRoot, 'skill', 'sql-cli-skill.md');

const skillTargets = [
  // Claude Code
  join(homedir(), '.claude', 'skills'),
  // Generic agents (used by multiple AI tools)
  join(homedir(), '.agents', 'skills'),
];

if (existsSync(skillFile)) {
  for (const dir of skillTargets) {
    try {
      if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
      copyFileSync(skillFile, join(dir, 'sql-cli.md'));
    } catch (_) {
      // Non-critical, skip silently
    }
  }
  console.log('[sql-cli] AI skill installed.');
}

console.log('[sql-cli] Setup complete. Run "sql-cli init" to create default config.');
