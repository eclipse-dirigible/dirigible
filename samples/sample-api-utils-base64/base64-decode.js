import { Base64 } from "sdk/utils/base64";
import { Response } from "sdk/http/response";

const input = "PT4/";
const result = Base64.decode(input);

console.log("decoded: " + result);
Response.println(JSON.stringify("decoded: " + result));

Response.flush();
Response.close();
