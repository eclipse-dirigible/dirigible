import base64

def encode(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    encoded_bytes = base64.b64encode(input)
    return encoded_bytes.decode('utf-8')

def encode_as_bytes(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    return base64.b64encode(input)

def encode_as_native_bytes(input):
    if isinstance(input, str):
        input = input.encode('utf-8')
    return input

def decode(input):
    try:
        decoded_bytes = base64.b64decode(input)
        return decoded_bytes.decode('utf-8')
    except Exception as e:
        return str(e)

def decode_as_native_bytes(input):
    try:
        return base64.b64decode(input)
    except Exception as e:
        return str(e)
