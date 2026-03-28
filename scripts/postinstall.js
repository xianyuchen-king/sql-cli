#!/usr/bin/env node
'use strict';

const { existsSync, mkdirSync } = require('fs');
const { join } = require('path');
const { homedir } = require('os');
const { execSync } = require('child_process');

const configDir = join(homedir(), '.sql-cli');
const driversDir = join(configDir, 'drivers');

// Create directories if not exist
if (!existsSync(configDir)) {
  mkdirSync(configDir, { recursive: true });
  console.log('[sql-cli] Created config directory: ' + configDir);
}

if (!existsSync(driversDir)) {
  mkdirSync(driversDir, { recursive: true });
  console.log('[sql-cli] Created drivers directory: ' + driversDir);
}

// Generate encryption key if not set
const envVar = 'SQL_CLI_SECRET';
if (!process.env[envVar]) {
  const crypto = require('crypto');
  const secret = crypto.randomBytes(32).toString('base64');
  console.log('[sql-cli] Encryption key not set. Add to your shell profile:');
  console.log(`  export ${envVar}="${secret}"`);
}

console.log('[sql-cli] Setup complete. Run "sql-cli init" to create default config.');
