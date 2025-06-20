
function stringifyValue(value) {
    try {
        if (typeof value === 'object') {
            if (Array.isArray(value)) {
                value = value.map(e => JSON.parse(e));
            }
            value = JSON.stringify(value, null, 4);
        } else {
            value = JSON.parse(value);
            value = JSON.stringify(value, null, 4);
        }
    } catch (e) {
        // Not a JSON - do nothing
    }
    return value;
}