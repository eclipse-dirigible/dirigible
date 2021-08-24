/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
var response = require('http/v4/response');
response.print('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAABmJLR0QA/wD/AP+gvaeTAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4gMdCC8YfegUOAAAAPBJREFUaN7tlzEOgmAMhQtxc9GbkHgBVu/gauoRjBuLM45v8xaOegHvwA3kBrgw6CDyA4E2vLeR8Id+fe1fKkJRlGtFnw+b060aO4DneRv1OR97d4AABCAAAQgwb4CFhSBUNeQPoASw9uzAij1AAAIQYDjt9ZAMNgd+bUf/NrWuW1W6OybxskpNDLLAYVTrNX0J9d1p2cSesz9vByxkf74OWMl+6zlgKWCWUNOqN1LMX9+ZvDRUNRWRe9M7ACKzJQTgISJX7z2QuQYAUHR1wdItlHW5CMwA1C5cvM+BPNQFUwAAylAXLE7iXEQKoSiqld7/CTXqn0F+jgAAAABJRU5ErkJggg==');
response.setContentType("image/png");
response.setStatus(200);
response.flush();
response.close();
