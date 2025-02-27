echo --- Executing build-source.bat
echo --- Calling build-source.ps1

set ERROR_OUTPUT_FILE=error-output.log
del %ERROR_OUTPUT_FILE% /Q
PowerShell -NoProfile -ExecutionPolicy unrestricted -Command ./build-source.ps1 2> %ERROR_OUTPUT_FILE%

set ERROR_TEXT=X [ERROR]
find "%ERROR_TEXT%" %ERROR_OUTPUT_FILE% && (
    echo --- FOUND an error which matches [%ERROR_TEXT%] in error output file: %ERROR_OUTPUT_FILE%
    echo --- File content:
    type %ERROR_OUTPUT_FILE%
    exit 1
) || (
    echo There are NO error [%%ERROR_TEXT%] in error output file %ERROR_OUTPUT_FILE%
    echo --- File content:
    type %ERROR_OUTPUT_FILE%
    echo --- build-source.bat completed
    exit 0
)
