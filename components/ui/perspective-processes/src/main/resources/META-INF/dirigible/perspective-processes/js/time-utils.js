/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
function formatRelativeTime(date) {
    const now = new Date();
    const diff = date.getTime() - now.getTime();

    const units = ['year', 'month', 'week', 'day', 'hour', 'minute', 'second'];
    const divisors = {
        year: 1000 * 60 * 60 * 24 * 365,
        month: 1000 * 60 * 60 * 24 * 30,
        week: 1000 * 60 * 60 * 24 * 7,
        day: 1000 * 60 * 60 * 24,
        hour: 1000 * 60 * 60,
        minute: 1000 * 60,
        second: 1000
    };

    const rtf = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });

    for (const unit of units) {
        const diffInUnit = diff / divisors[unit];
        if (Math.abs(diffInUnit) >= 1 || unit === 'second') {
            return rtf.format(Math.round(diffInUnit), unit);
        }
    }

    return '';
}

function formatDuration(startDate, endDate) {
    let ms = Math.max(endDate.getTime() - startDate.getTime(), 0);

    const units = [
        { label: 'day', ms: 1000 * 60 * 60 * 24 },
        { label: 'hour', ms: 1000 * 60 * 60 },
        { label: 'minute', ms: 1000 * 60 },
        { label: 'second', ms: 1000 },
    ];

    const parts = [];

    for (const unit of units) {
        const value = Math.floor(ms / unit.ms);
        if (value > 0) {
            parts.push(`${value} ${unit.label}${value !== 1 ? 's' : ''}`);
            ms -= value * unit.ms;
        }
    }

    return parts.length ? parts.join(' ') : 'less than a second';
}

