/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Marks the {@code "-updated"} event of a DERIVED write - a roll-up total, a keyed aggregate, an
 * expansion share, written by a generated handler rather than by a person or a workflow - with the
 * columns that write touched.
 *
 * <p>
 * A derived write keeps the {@code "-updated"} contract, because a transitive roll-up above the row
 * and the intent's own onUpdate reactions must keep cascading. But a listener whose job is to react
 * to an AUTHORED change could not tell the two apart: a settlement's payment-updated handler saw
 * the payment's allocated total drop when a person deleted an allocation, and re-settled the
 * payment onto the very document the person had just taken it off (#7557). The marker lets such a
 * listener ask which columns actually moved.
 *
 * <p>
 * The marker is one extra top-level key, {@value #MARKER}, holding the written column names. It is
 * not a Java identifier the generator emits, so it cannot collide with an entity property, and a
 * consumer that deserializes the payload into the entity class ignores it.
 */
public final class DerivedWrite {

    /** The payload key that carries the columns a derived write touched. */
    public static final String MARKER = "$derived";

    private static final ThreadLocal<Set<String>> COLUMNS = new ThreadLocal<>();

    private DerivedWrite() {}

    /**
     * Runs a write whose {@code "-updated"} event is to be marked as derived from the given columns.
     * Scoped to this thread and to the call: the event is recorded inside the write itself, on the
     * calling thread, so the marker reaches exactly that write's event.
     *
     * @param <R> the write's result type
     * @param columns the property names the derived write persists
     * @param write the write
     * @return the write's result
     */
    public static <R> R run(Collection<String> columns, Supplier<R> write) {
        Set<String> previous = COLUMNS.get();
        COLUMNS.set(new LinkedHashSet<>(columns));
        try {
            return write.get();
        } finally {
            if (previous == null) {
                COLUMNS.remove();
            } else {
                COLUMNS.set(previous);
            }
        }
    }

    /**
     * Adds the marker to an event payload when the current thread is inside {@link #run}, and only to
     * the FIRST event recorded there: that is the derived write's own row. A write the derived one
     * causes in turn - a document line resumming its master - is about another row whose columns this
     * marker does not describe, so it goes out unmarked, which reads as an authored change: the
     * conservative side, exactly what it published before the marker existed.
     *
     * @param payload the JSON of the written row
     * @return the payload, marked when it is the derived write's own event
     */
    public static String mark(String payload) {
        Set<String> columns = COLUMNS.get();
        if (columns == null || columns.isEmpty() || payload == null) {
            return payload;
        }
        JsonElement parsed = JsonParser.parseString(payload);
        if (!parsed.isJsonObject()) {
            return payload;
        }
        JsonObject object = parsed.getAsJsonObject();
        JsonArray names = new JsonArray();
        columns.forEach(names::add);
        object.add(MARKER, names);
        COLUMNS.set(Set.of());
        return object.toString();
    }

    /**
     * The columns a derived write touched, read off its event payload.
     *
     * @param payload the event payload
     * @return the written columns, or {@code null} when the event is not from a derived write
     */
    public static Set<String> columns(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(payload);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonElement marker = parsed.getAsJsonObject()
                                       .get(MARKER);
            if (marker == null || !marker.isJsonArray()) {
                return null;
            }
            Set<String> columns = new LinkedHashSet<>();
            marker.getAsJsonArray()
                  .forEach(e -> columns.add(e.getAsString()));
            return columns;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Whether an event payload is a derived write that touched NONE of the given columns - the one case
     * a listener reacting to authored changes of those columns can safely skip.
     *
     * @param payload the event payload
     * @param watched the columns the listener reacts to
     * @return {@code true} when the event is derived and moved none of them
     */
    public static boolean touchedNoneOf(String payload, String... watched) {
        Set<String> columns = columns(payload);
        return columns != null && columns.stream()
                                         .noneMatch(List.of(watched)::contains);
    }
}
