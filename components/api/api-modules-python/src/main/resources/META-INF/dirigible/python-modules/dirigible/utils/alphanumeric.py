import random
import string

LOWERCASEASCII = "abcdefghijklmnopqrstuvwxyz"
UPPERCASEASCII = LOWERCASEASCII.upper()
NUMBERS = "1234567890"

def to_alphanumeric(string):
    return ''.join(c for c in string if c.isalnum())

def random_string(length=4, charset=LOWERCASEASCII + NUMBERS):
    return ''.join(random.choice(charset) for _ in range(length))

def alphanumeric(length=4, lowercase=True):
    charset = LOWERCASEASCII + NUMBERS
    if not lowercase:
        charset += UPPERCASEASCII
    return random_string(length, charset)

def alpha(length=4, lowercase=True):
    charset = LOWERCASEASCII
    if not lowercase:
        charset += UPPERCASEASCII
    return random_string(length, charset)

def numeric(length=4):
    return random_string(length, NUMBERS)

def is_numeric(string):
    return string.isnumeric()

def is_alphanumeric(string):
    return string.isalnum()
