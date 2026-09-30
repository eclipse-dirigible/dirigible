const ALPHA = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
const LOWER = ALPHA.toLowerCase();
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
  if (field.unique && field.type !== 'string') return uniqueValue(field);
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
    // the input shapes of an <input type="month|week"> - a month column is VARCHAR(7), so the
    // default marker overflows it (dirigible #7545)
    case 'month':
      return '2026-07';
    case 'week':
      return '2026-W28';
    default:
      return 'APPTEST-' + rand(ALPHA + DIGITS, 6);
  }
}

// A `pattern:` field (an authored regex, or the address regex behind `format: email`) is rejected
// by the generated controller with 400 unless the value has the declared shape, so the marker value
// above has to be traded for one that matches: the candidates are tried in order and the first that
// both matches and fits the declared length wins. When nothing here matches (an IBAN-shaped regex
// mixing character classes by position), an optional field is left unset - the record is valid
// without it, and no flow needs it filled (dirigible #7525) - while a required one keeps the plain
// marker: the controller's own 400 then names the field and the pattern, which is the message the
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
    // a letters-only code (an ISO language or country code, ^[a-z]{2,3}$) - kept short, the shape
    // such a pattern declares, and within the declared length (dirigible #7545)
    rand(LOWER, Math.min(field.length ?? 3, 3)),
    rand(ALPHA, Math.min(field.length ?? 3, 3)),
  ];
  const match = candidates.find((candidate) => candidate.length <= (field.length ?? 255) && regex.test(candidate));
  return match ?? (field.required ? value : undefined);
}

// A `unique:` field refuses a second row carrying its value with 409 - the module's guard doing its
// job. The per-type constants above are the one value certain to collide: with a seeded row, with
// the row a parallel flow of the same run is creating, or with one an earlier failed run left
// behind (dirigible #7530). So a unique field draws a fresh value per record, away from the range
// an author seeds (a year, a small code): an integer in the hundred-thousands, a date in the 22nd
// century. Strings need none of this - the marker above is already random.
function uniqueValue(field) {
  const n = 100000 + Math.floor(Math.random() * 900000);
  switch (field.type) {
    case 'integer':
    case 'bigint':
      return n;
    case 'decimal':
    case 'double':
      return n + 0.25;
    case 'date':
    case 'timestamp':
    case 'datetime': {
      const at = new Date(Date.UTC(2100, 0, 1) + (n % 36500) * 86400000);
      return field.type === 'date' ? at.toISOString().slice(0, 10) : at.toISOString().replace(/\.\d{3}Z$/, 'Z');
    }
    case 'month':
      return `${2100 + (n % 900)}-${String(1 + (n % 12)).padStart(2, '0')}`;
    case 'week':
      return `${2100 + (n % 900)}-W${String(1 + (n % 52)).padStart(2, '0')}`;
    default:
      return sampleValue({ ...field, unique: false });
  }
}

// A drawn value is unlikely to collide, not certain not to - probe the live rows for each unique
// field of the record through the controller's filtered read and redraw on a hit, so the create the
// flow is about to send cannot be refused for a value that is already taken. The value goes as a
// string - the controller coerces it to the property's own type (an Integer would not bind to a Long
// or BigDecimal column). A probe that fails (an older controller without /search) keeps the value.
export async function freshUniques(client, entity, record) {
  let redrawn = false;
  for (const field of editableFields(entity)) {
    if (!field.unique || !(field.name in record)) continue;
    for (let attempt = 0; attempt < 5; attempt++) {
      let rows;
      try {
        rows = await client.search(entity, [{ propertyName: field.name, operator: 'EQ', value: String(record[field.name]) }]);
      } catch {
        break;
      }
      if (!rows?.length) break;
      record[field.name] = field.type === 'string' ? sampleValue(field) : uniqueValue(field);
      redrawn = true;
    }
  }
  // a redrawn field may be the right-hand side of a compare check - re-derive its left operand
  if (redrawn) steerCompares(entity, record);
  return record;
}

// A composite unique key (`unique: [ProjectTimesheet, Employee]` - one timesheet per employee per
// project-month) refuses a second row carrying the same combination with 409, and the first row of
// each relation is the combination a seed most likely holds already (dirigible #7545). Walk the
// candidate rows of the key's relations (and redraw its own fields) until the controller's filtered
// read finds no row with that combination, keeping every agree check satisfied. Answers the key no
// free combination was found for - the flow skips with it - or null when the record is free. Call it
// before the relation samples are copied into the record.
export async function freshUniqueKeys(client, entity, record, samples) {
  for (const key of entity.uniqueKeys ?? []) {
    const related = key.map((name) => samples.find((s) => s.relation.name === name)).filter(Boolean);
    const drawn = editableFields(entity).filter((f) => key.includes(f.name) && f.name in record);
    // a member left unset is NULL, and a NULL never collides
    if (related.length + drawn.length < key.length) continue;
    let free = false;
    for (const combination of combinations(related.map((s) => s.rows), related.length ? 50 : 5)) {
      combination.forEach((row, i) => related[i].use(row));
      if (!agreeing(entity, samples)) continue;
      const value = (name) => related.find((s) => s.relation.name === name)?.id ?? record[name];
      let rows;
      try {
        rows = await client.search(entity, key.map((name) => ({ propertyName: name, operator: 'EQ', value: String(value(name)) })));
      } catch {
        free = true; // an older controller without /search - keep the combination, as freshUniques does
        break;
      }
      if (!rows?.length) {
        free = true;
        break;
      }
      for (const field of drawn) record[field.name] = field.type === 'string' ? sampleValue(field) : uniqueValue(field);
    }
    if (!free) return key;
    if (drawn.length) steerCompares(entity, record);
  }
  return null;
}

// The first `limit` entries of the cartesian product of the candidate lists, in order - so the first
// combination is the one the samples started with. No lists yields `limit` empty combinations: a key
// of own fields only is retried by redrawing them.
function* combinations(lists, limit) {
  const index = lists.map(() => 0);
  for (let produced = 0; produced < limit; produced++) {
    yield lists.map((list, i) => list[index[i]]);
    let i = lists.length - 1;
    while (i >= 0 && ++index[i] === lists[i].length) index[i--] = 0;
    if (lists.length && i < 0) return;
  }
}

// Whether the relation samples satisfy every agree check of the entity (an unset side has nothing to
// disagree about).
export function agreeing(entity, samples) {
  return (entity.agree ?? []).every((check) => {
    const values = check.relations.map((name) => samples.find((s) => s.relation.name === name)?.row?.[check.onProperty]);
    return values.some((value) => value == null) || values.every((value) => String(value) === String(values[0]));
  });
}

export function editableFields(entity) {
  return (entity.fields ?? []).filter((f) => !f.readOnly && !f.primaryKey && !f.generated);
}

// One sample record for the entity's own fields (relations are resolved separately,
// against live target rows).
export function sampleRecord(entity) {
  const record = {};
  for (const field of editableFields(entity)) {
    const value = sampleValue(field);
    if (value !== undefined) record[field.name] = value;
  }
  // an exactlyOne check rejects a record where more than one of the named fields is set -
  // keep only the first of each declared set
  for (const set of entity.exactlyOne ?? []) {
    for (const name of set.slice(1)) delete record[name];
  }
  steerCompares(entity, record);
  return record;
}

// A compare check relates the record's own field to a second value - another of its fields, or a
// literal - and the sample values above are per-type constants, so two dates come out EQUAL and a
// strict comparison (gt/lt/ne) would be rejected with 400, just as a sample quantity of 7 fails a
// `le 5`. Derive the left operand from whichever right-hand side the check names, by the smallest
// step that satisfies the declared operator (equality satisfies ge/le/eq).
function steerCompares(entity, record) {
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
