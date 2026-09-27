const ALPHA = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
const DIGITS = '0123456789';

function rand(chars, n) {
  let out = '';
  for (let i = 0; i < n; i++) out += chars[Math.floor(Math.random() * chars.length)];
  return out;
}

// Type-aware sample value for one field. Long strings carry the APPTEST- prefix (the
// cleanup marker and the searchable handle); short strings always contain a digit so
// they can never collide with letter-only nomenclature seeds (ISO codes etc.).
export function sampleValue(field) {
  switch (field.type) {
    case 'string': {
      const len = field.length ?? 64;
      const value = len >= 16 ? 'APPTEST-' + rand(ALPHA + DIGITS, 6) : rand(ALPHA, Math.max(1, len - 1)) + rand(DIGITS, 1);
      return field.pattern ? shaped(field, value) : value;
    }
    case 'integer':
    case 'bigint':
      return 7;
    case 'decimal':
    case 'double':
      return 3.14;
    case 'boolean':
      return true;
    case 'date':
      return '2026-07-08';
    case 'timestamp':
    case 'datetime':
      // full ISO instant: the generated Java entities bind java.time.Instant, which rejects a
      // zone-less value; the UI fill slices this to the datetime-local shape
      return '2026-07-08T10:00:00Z';
    default:
      return 'APPTEST-' + rand(ALPHA + DIGITS, 6);
  }
}

// A `pattern:` field (an authored regex, or the address regex behind `format: email`) is rejected
// by the generated controller with 400 unless the value has the declared shape, so the marker value
// above has to be traded for one that matches: the candidates are tried in order and the first that
// both matches and fits the declared length wins. A pattern nothing here matches keeps the plain
// marker - the controller's own 400 then names the field and the pattern, which is the message the
// module author needs, and a silent near-miss would not.
function shaped(field, value) {
  let regex;
  try {
    // anchored like the generated controller's own String.matches, so an unanchored pattern cannot
    // pass here on a substring and then be refused by the server
    regex = new RegExp('^(?:' + field.pattern + ')$');
  } catch {
    return value; // not a JavaScript-parsable regex - nothing to shape the value to
  }
  const token = value.replace(/[^A-Za-z0-9]/g, '');
  const candidates = [
    value,
    `apptest.${token}@apptest.example.com`, // format: email and other address-shaped patterns
    token, // letters and digits, no separator
    rand(DIGITS, 10), // a numeric code
  ];
  return candidates.find((candidate) => candidate.length <= (field.length ?? 255) && regex.test(candidate)) ?? value;
}

export function editableFields(entity) {
  return (entity.fields ?? []).filter((f) => !f.readOnly && !f.primaryKey && !f.generated);
}

// One sample record for the entity's own fields (relations are resolved separately,
// against live target rows).
export function sampleRecord(entity) {
  const record = {};
  for (const field of editableFields(entity)) record[field.name] = sampleValue(field);
  // an exactlyOne check rejects a record where more than one of the named fields is set -
  // keep only the first of each declared set
  for (const set of entity.exactlyOne ?? []) {
    for (const name of set.slice(1)) delete record[name];
  }
  // a compare check relates the record's own field to a second value - another of its fields, or a
  // literal - and the sample values above are per-type constants, so two dates come out EQUAL and a
  // strict comparison (gt/lt/ne) would be rejected with 400, just as a sample quantity of 7 fails a
  // `le 5`. Derive the left operand from whichever right-hand side the check names, by the smallest
  // step that satisfies the declared operator (equality satisfies ge/le/eq).
  for (const check of entity.compare ?? []) {
    if (!(check.field in record)) continue;
    const type = (entity.fields ?? []).find((f) => f.name === check.field)?.type;
    const right = 'than' in check ? record[check.than] : literalValue(check.value, type);
    if (right == null) continue;
    record[check.field] = shifted(right, type, STEPS[check.op] ?? 0);
  }
  return record;
}

// The right-hand side of a compare check declared as a literal. A moment (CURRENT_DATE /
// CURRENT_TIMESTAMP / NOW) resolves against the runner's own clock, in the field's shape; a moment
// carrying an offset is left alone - the sample record keeps its constant and the check is simply
// not steered, which is safe for the ge/le/eq that a stale sample still satisfies.
function literalValue(value, type) {
  if (typeof value !== 'string') return value;
  const now = new Date();
  switch (value.trim()) {
    case 'CURRENT_DATE':
      return type === 'date' ? now.toISOString().slice(0, 10) : now.toISOString().replace(/\.\d{3}Z$/, 'Z');
    case 'CURRENT_TIMESTAMP':
    case 'NOW':
      return now.toISOString().replace(/\.\d{3}Z$/, 'Z');
    default:
      return /^(CURRENT_DATE|CURRENT_TIMESTAMP|NOW)[+-]/.test(value.trim()) ? null : value;
  }
}

// How far the left operand of a compare check has to move off the right one to satisfy it.
const STEPS = { ge: 0, le: 0, eq: 0, gt: 1, ne: 1, lt: -1 };

// One step of the value's own unit: a day for a date, an hour for a timestamp, one for a number.
function shifted(value, type, step) {
  if (step === 0) return value;
  switch (type) {
    case 'date': {
      const at = new Date(value + 'T00:00:00Z');
      at.setUTCDate(at.getUTCDate() + step);
      return at.toISOString().slice(0, 10);
    }
    case 'timestamp':
    case 'datetime': {
      const at = new Date(value);
      at.setUTCHours(at.getUTCHours() + step);
      return at.toISOString().replace(/\.\d{3}Z$/, 'Z');
    }
    default:
      return value + step;
  }
}

// The searchable "handle" field: the first long string field shown in the list. Its
// value identifies the record in the table across the create/edit/delete flow, and the flows
// flip it by appending a suffix - so a field carrying a `pattern:` cannot be it, the suffix
// breaking the very shape the controller enforces. Read-only fields are out through
// editableFields: a `number:` field is the platform's to stamp and preserve, so a write to it
// reads back unchanged (dirigible #7411).
// Null when the entity has no such field (all-numeric/date entities) - flows degrade:
// the UI walk is skipped and the REST flow drops its update-value assertion.
export function handleField(entity) {
  return editableFields(entity).find((f) => f.type === 'string' && (f.length ?? 64) >= 16 && f.major !== false && !f.pattern) ?? null;
}

export function labelOf(field) {
  return field.label ?? field.name;
}
