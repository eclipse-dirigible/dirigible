def encode(input):
    if isinstance(input, str):
        input = input.encode("utf-8")
    return input.hex()

def encode_as_bytes(input):
    if isinstance(input, str):
        input = input.encode("utf-8")
    return bytes.fromhex(encode(input))

def encode_as_native_bytes(input):
    if isinstance(input, str):
        input = input.encode("utf-8")
    return bytes.fromhex(encode(input))

def decode(input):
    try:
        decoded_bytes = bytes.fromhex(input)
        return decoded_bytes.decode("utf-8")
    except ValueError:
        return None

def decode_as_native_bytes(input):
    try:
        return bytes.fromhex(input)
    except ValueError:
        return None
