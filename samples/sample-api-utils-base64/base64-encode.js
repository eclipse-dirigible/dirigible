import { Base64 } from "sdk/utils/base64";
import { Response } from "sdk/http/response";

const input = [61, 62, 63];
const result = Base64.encode(input);

console.log("encoded: " + result);

Response.println(JSON.stringify("encoded: " + result));
Response.flush();
Response.close();
