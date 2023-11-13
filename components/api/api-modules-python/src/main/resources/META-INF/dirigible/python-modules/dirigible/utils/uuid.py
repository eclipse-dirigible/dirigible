import uuid

def random():
    return str(uuid.uuid4())

def validate(input):
    try:
        uuid.UUID(input)
        return True
    except ValueError:
        return False
