import json
import xml.etree.ElementTree as ET

def from_json(input):
    try:
        data = json.loads(input)
        root = ET.Element("root")
        dict_to_xml(data, root)
        xml_string = ET.tostring(root).decode()
        return xml_string
    except Exception as e:
        return str(e)

def to_json(input):
    try:
        root = ET.fromstring(input)
        data = xml_to_dict(root)
        json_string = json.dumps(data, indent=2)
        return json_string
    except Exception as e:
        return str(e)

def dict_to_xml(d, parent):
    for key, value in d.items():
        element = ET.Element(key)
        parent.append(element)
        if isinstance(value, dict):
            dict_to_xml(value, element)
        else:
            element.text = str(value)

def xml_to_dict(element):
    data = {}
    for child in element:
        if len(child) > 0:
            data[child.tag] = xml_to_dict(child)
        else:
            data[child.tag] = child.text
    return data
