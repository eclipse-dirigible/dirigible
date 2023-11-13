import urllib.parse

def encode(input, charset):
    return urllib.parse.quote(input, charset)

def decode(input, charset):
    return urllib.parse.unquote(input, charset)

def escape(input):
    return urllib.parse.quote(input)

def escapePath(input):
    return urllib.parse.quote(input, safe='/')

def escapeForm(input):
    return urllib.parse.quote_plus(input)
