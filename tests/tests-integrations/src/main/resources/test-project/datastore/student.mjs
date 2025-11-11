import { store } from "@aerokit/sdk/db";
import { response } from "@aerokit/sdk/http";

let entry = { 'name': 'John', 'address': 'Sofia, Bulgaria' };

store.save('Student', entry);

let list = store.list('Student');

response.println(JSON.stringify(list));
response.flush();
response.close();
