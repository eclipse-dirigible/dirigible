#!/bin/sh
# fail the whole script if any command bellow fails
set -e

# clean
rm -rf ./dist

# build esm
esbuild $(find . -iname '*.ts' -not -iname '*.d.ts') '--out-extension:.js=.mjs' --sourcemap=inline --outdir=dist/esm --format=esm --target=es2022

# build cjs
esbuild $(find . -iname '*.ts' -not -iname '*.d.ts') --sourcemap=inline --outdir=dist/cjs --format=cjs --target=es2022

# build dts
tsc --emitDeclarationOnly --outDir dist/dts

# fail loudly if the dist is incomplete - a truncated dist ships a jar whose whole JS layer
# fails at runtime (see eclipse-dirigible/dirigible#6339)
node verify-dist.js
