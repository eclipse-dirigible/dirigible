import { rs } from 'sdk/http';

rs.service()
    .resource("/hello-world")
    .get(function (_ctx, _request, _response) {
        _response.setContentType('application/json');
        _response.println(JSON.stringify({
            message: 'Hello World'
        }));
    })
    .produces(['application/json'])
    .execute();
