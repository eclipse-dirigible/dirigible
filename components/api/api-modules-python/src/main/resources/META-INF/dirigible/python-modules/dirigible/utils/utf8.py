def encode(input):
    return input.encode('utf-8')

def decode(input):
    return input.decode('utf-8')

def bytesToString(bytes, offset, length):
    return bytes[offset:offset + length].decode('utf-8')
