echo "Executing build-source.ps1"
# clean
rm dist -r -force

# build esm and cjs
Get-ChildItem -Recurse -Include *.ts -Exclude *.d.ts | ForEach-Object {
    $relativePath = $_.FullName.Substring((Get-Location).Path.Length + 1)
    $relativePath = $relativePath -replace '^src\\', ''
    $mjsOutputFile = ".\dist\esm\$relativePath" -replace '\.ts$', '.mjs'
    $cjsOutputFile = ".\dist\cjs\$relativePath" -replace '\.ts$', '.js'

    echo "--- Building mjs"
    esbuild $_.FullName --outfile=$mjsOutputFile --sourcemap=inline --format=esm --target=es2022

    echo "--- Building cjs"
    esbuild $_.FullName --outfile=$cjsOutputFile --sourcemap=inline --bundle --format=cjs --target=es2022
}

echo "--- build-source.ps1 completed"