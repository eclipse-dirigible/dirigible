import html
import json
import xml.sax.saxutils as saxutils

def escape_csv(input):
    return '"' + input.replace('"', '""') + '"'

def escape_javascript(input):
    return json.dumps(input)

def escape_html3(input):
    return html.escape(input)

def escape_html4(input):
    return html.escape(input)

def escape_java(input):
    escaped = ""
    for char in input:
        if char == '\n':
            escaped += '\\n'
        elif char == '\r':
            escaped += '\\r'
        elif char == '\t':
            escaped += '\\t'
        elif char == '\\':
            escaped += '\\\\'
        elif char == '"':
            escaped += '\\"'
        elif ord(char) < 32 or ord(char) > 126:
            escaped += "\\u{:04x}".format(ord(char))
        else:
            escaped += char
    return escaped

def escape_json(input):
    return json.dumps(input)

def escape_xml(input):
    return saxutils.escape(input, entities={'"': '&quot;', "'": '&apos;'})

def unescape_csv(input):
    if input.startswith('"') and input.endswith('"'):
        input = input[1:-1]
        return input.replace('""', '"')
    return input

def unescape_javascript(input):
    return json.loads(input)

def unescape_html3(input):
    return html.unescape(input)

def unescape_html4(input):
    return html.unescape(input)

def unescape_java(input):
    unescaped = ""
    i = 0
    while i < len(input):
        char = input[i]
        if char == '\\' and i + 1 < len(input):
            next_char = input[i + 1]
            if next_char == 'n':
                unescaped += '\n'
                i += 1
            elif next_char == 'r':
                unescaped += '\r'
                i += 1
            elif next_char == 't':
                unescaped += '\t'
                i += 1
            elif next_char == '\\':
                unescaped += '\\'
                i += 1
            elif next_char == '"':
                unescaped += '"'
                i += 1
            elif next_char == "'":
                unescaped += "'"
                i += 1
            elif next_char == 'b':
                unescaped += '\b'
                i += 1
            elif next_char == 'f':
                unescaped += '\f'
                i += 1
            elif next_char == 'u' and i + 5 < len(input):
                try:
                    unicode_char = chr(int(input[i + 2:i + 6], 16))
                    unescaped += unicode_char
                    i += 5
                except ValueError:
                    unescaped += '\\' + next_char
            else:
                unescaped += '\\' + next_char
        else:
            unescaped += char
        i += 1
    return unescaped

def unescape_json(input):
    return json.loads(input)

def unescape_xml(input):
    return saxutils.unescape(input, entities={'quot': '"', 'apos': "'"})
