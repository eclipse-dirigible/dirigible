import hashlib

def md5(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    md5_hash = hashlib.md5(input)
    return md5_hash.digest()

def md5_hex(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    md5_hash = hashlib.md5(input)
    return md5_hash.hexdigest()

def sha1(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    sha1_hash = hashlib.sha1(input)
    return sha1_hash.digest()

def sha1_hex(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    sha1_hash = hashlib.sha1(input)
    return sha1_hash.hexdigest()

def sha256(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    sha256_hash = hashlib.sha256(input)
    return sha256_hash.digest()

def sha384(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    sha384_hash = hashlib.sha384(input)
    return sha384_hash.digest()

def sha512(input):
    if isinstance input, str:
        input = input.encode('utf-8')
    sha512_hash = hashlib.sha512(input)
    return sha512_hash.digest()
