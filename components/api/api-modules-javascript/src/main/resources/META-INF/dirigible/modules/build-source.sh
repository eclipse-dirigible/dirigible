#!/bin/sh
# fail the whole script if any command bellow fails
set -e

# clean
rm -rf ./dist

# build esm
esbuild $(find . -iname '*.ts' -not -iname '*.d.ts') '--out-extension:.js=.mjs' --sourcemap=inline --outdir=. --format=esm --target=es2022

# build cjs
esbuild $(find . -iname '*.ts' -not -iname '*.d.ts') --sourcemap=inline --outdir=. --format=cjs --target=es2022

# build dts
#tsc --emitDeclarationOnly --outDir dist/dts

# build types
tsc --emitDeclarationOnly --outDir dist
