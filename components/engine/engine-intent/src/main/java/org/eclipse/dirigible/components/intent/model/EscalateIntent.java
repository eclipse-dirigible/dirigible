/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.model;

/**
 * A days-past-due <b>escalation ladder</b> over a schedule's matched row (issue #7276): pick the
 * level that applies to how overdue the row is, and let the tick advance through the ladder as the
 * document ages.
 *
 * <p>
 * The ladder is an ordinary entity of the model - the {@code kind: setting} table a dunning module
 * already has ({@code ReminderLevel}: "First reminder" after 3 days, "Second reminder" after 14,
 * "Final notice" after 30). {@link #getAfter()} names the integer threshold on it and
 * {@link #getSince()} the date on the ROW the threshold is measured from; the level applied to a
 * row is the <b>highest</b> whose threshold has been passed, and a row that has passed none is left
 * alone this tick rather than mailed at the bottom level.
 *
 * <p>
 * An escalation always accompanies a {@code generate}: the chosen level is written onto the
 * generated record through {@link #getInto()}, and that record - with the level in its
 * {@code generate.unique:} natural key - is what makes each level go out exactly <b>once</b>.
 * Without a record there is nothing to tell a level already sent from one still due, which is why a
 * {@code notify}-only escalation is refused rather than silently re-sending every tick.
 *
 * <p>
 * Inside the accompanying {@code notify}, the chosen level's own fields are reachable as
 * {@code {escalation.<field>}} placeholders - the per-level wording ("a friendly reminder" vs
 * "final notice before collection") that a flat schedule cannot express.
 */
public class EscalateIntent {

    /**
     * The entity holding the levels - a local entity of this model, normally a {@code kind: setting}.
     */
    private String ladder;

    /** The integer property of {@link #ladder} holding the days-past-{@code since} threshold. */
    private String after;

    /** The {@code date} property of the queried row the threshold is measured from. */
    private String since;

    /** The property of the {@code generate} target that receives the chosen level. */
    private String into;

    public String getLadder() {
        return ladder;
    }

    public void setLadder(String ladder) {
        this.ladder = ladder;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getInto() {
        return into;
    }

    public void setInto(String into) {
        this.into = into;
    }
}
