#!/usr/bin/env node

'use strict';

const { existsSync } = require('fs');
const { join } = require('path');
const { execSync } = require('child_process');
const { platform } = require('os');

// Resolve jar path
const jarDir = join(__dirname, '..', 'jar');
const jarFile = join(jarDir, 'sql-cli.jar');

// Check Java installation
function checkJava() {
  try {
    const version = execSync('java -version 2>&1', { encoding: 'utf8' });
    const match = version.match(/version "(\d+)/);
    if (match && parseInt(match[1]) >= 17) {
      return true;
    }
    console.error('Error: sql-cli requires Java 17 or later.');
    console.error(`Found Java version: ${match ? match[1] : 'unknown'}`);
    console.error('Please install Java 21+: https://adoptium.net/');
    process.exit(1);
  } catch (e) {
    console.error('Error: Java is not installed or not on PATH.');
    console.error('sql-cli requires Java 17 or later.');
    console.error('Please install Java 21+: https://adoptium.net/');
    process.exit(1);
  }
}

// Check jar file
function checkJar() {
  if (!existsSync(jarFile)) {
    console.error(`Error: sql-cli.jar not found at ${jarFile}`);
    if (existsSync(join(__dirname, '..', 'node_modules'))) {
      console.error('Please reinstall: npm install -g @black-cyq/sql-cli');
    } else {
      console.error('Please build first: ./gradlew shadowJar && mkdir -p jar && cp build/libs/sql-cli.jar jar/');
    }
    process.exit(1);
  }
}

// Main
checkJava();
checkJar();

const args = process.argv.slice(2);

try {
  execSync(`java -jar "${jarFile}" ${args.map(a => `"${a}"`).join(' ')}`, {
    stdio: 'inherit',
    env: { ...process.env }
  });
} catch (e) {
  // Exit with the same code as the java process
  process.exit(e.status || 1);
}
