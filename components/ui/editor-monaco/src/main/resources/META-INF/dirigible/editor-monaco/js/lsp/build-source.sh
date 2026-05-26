#!/bin/sh
# Copyright (c) 2010-2026 Eclipse Dirigible contributors
# SPDX-License-Identifier: EPL-2.0
set -e
npm install --prefer-offline --no-audit --no-fund
node build.js
