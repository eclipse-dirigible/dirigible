{
    "description": "command description",
    "contentType": "text/plain",
    "set": {
        "GREETING": "hello world!"
    },
    "unset": [
        "BYE"
    ],
    "commands": [
        {
            "os": "linux",
            "command": "sh print.sh"
        },
        {
            "os": "mac",
            "command": "sh print.sh"
        },
        {
            "os": "windows",
            "command": "print.bat"
        }
    ]
}