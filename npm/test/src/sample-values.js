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
      return '2026-07-08T10:00';
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
  return record;
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
