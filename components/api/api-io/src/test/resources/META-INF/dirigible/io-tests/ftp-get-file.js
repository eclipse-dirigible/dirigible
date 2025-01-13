
const ftp = require("io/ftp");
import { Assert } from 'test/assert';

const host = "test.rebex.net";
const port = 21;
const userName = "demo";
const password = "password";

const ftpClient = ftp.getClient(host, port, userName, password);
const fileText = ftpClient.getFileText("/", "readme.txt");

Assert.assertTrue(fileText !== undefined && fileText !== null);