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
      if (len >= 16) return 'APPTEST-' + rand(ALPHA + DIGITS, 6);
      return rand(ALPHA, Math.max(1, len - 1)) + rand(DIGITS, 1);
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
  // a compare check relates two of the record's own fields, and the sample values above are
  // per-type constants - so two dates come out EQUAL and a strict comparison (gt/lt/ne) would be
  // rejected with 400. Derive the left operand from the right, by the smallest step that satisfies
  // the declared operator (equality satisfies ge/le/eq).
  for (const check of entity.compare ?? []) {
    if (!(check.field in record) || record[check.than] == null) continue;
    const type = (entity.fields ?? []).find((f) => f.name === check.field)?.type;
    record[check.field] = shifted(record[check.than], type, STEPS[check.op] ?? 0);
  }
  return record;
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
// value identifies the record in the table across the create/edit/delete flow.
// Null when the entity has no such field (all-numeric/date entities) - flows degrade:
// the UI walk is skipped and the REST flow drops its update-value assertion.
export function handleField(entity) {
  return editableFields(entity).find((f) => f.type === 'string' && (f.length ?? 64) >= 16 && f.major !== false) ?? null;
}

export function labelOf(field) {
  return field.label ?? field.name;
}
