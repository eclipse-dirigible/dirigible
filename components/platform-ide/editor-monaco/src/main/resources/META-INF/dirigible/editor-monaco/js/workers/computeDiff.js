importScripts("/webjars/diff/5.1.0/dist/diff.js");
function getNewLines(oldText, newText, isWhitespaceIgnored = false) {
    if (
        oldText[oldText.length - 1] !== "\n" ||
        newText[newText.length - 1] !== "\n"
    ) {
        oldText += "\n";
        newText += "\n";
    }
    let lineDiff;
    if (isWhitespaceIgnored) {
        lineDiff = Diff.diffTrimmedLines(oldText, newText);
    } else {
        lineDiff = Diff.diffLines(oldText, newText);
    }
    let addedCount = 0;
    let addedLines = [];
    let removedLines = [];
    for (let i = 0; i < lineDiff.length; i++) {
        let { added, removed, value } = lineDiff[i];
        let count = value.split("\n").length - 1;
        if (!added && !removed) {
            addedCount += count;
        } else if (added) {
            for (let i = 0; i < count; i++) {
                addedLines.push(addedCount + i + 1);
            }
            addedCount += count;
        } else if (removed && !addedLines.includes(addedCount + count)) {
            removedLines.push(addedCount);
        }
    }
    return { updated: addedLines, removed: removedLines };
};

function getDecorations(lines) {
    let newDecorations = [];
    for (let i = 0; i < lines.updated.length; i++) {
        newDecorations.push({
            range: { startLineNumber: lines.updated[i], startColumn: 1, endLineNumber: lines.updated[i], endColumn: 1 },
            options: {
                isWholeLine: true,
                linesDecorationsClassName: 'modified-line' + (
                    lines.removed.includes(lines.updated[i]) ? ' deleted-line' : '')
            },
        });
    };
    for (let i = 0; i < lines.removed.length; i++) {
        if (!lines.updated.includes(lines.removed[i] + 1))
            newDecorations.push({
                range: { startLineNumber: lines.removed[i], startColumn: 1, endLineNumber: lines.removed[i], endColumn: 1 },
                options: {
                    isWholeLine: true,
                    linesDecorationsClassName: 'deleted-line'
                },
            });
    }
    return newDecorations;
}

onmessage = function (event) {
    postMessage(getDecorations(
        getNewLines(event.data.oldText, event.data.newText),
    ));
};