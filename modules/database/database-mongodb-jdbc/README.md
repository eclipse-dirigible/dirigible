# mongo-jdbc
JDBC channel for MongoDB.
While this driver mostly adheres to the JDBC ways of connections, statements and resultsets, note that this driver does not translate to/from SQL. It uses Mongo's query format instead. 
You have the option to choose between browsing result sets using the JDBC API or directly get the underlying document and operate on it.

Example:

	MongodbConnection conn = new MongodbConnection("jdbc:mongodb://localhost/mydb", null);
	try {
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("{ find: \"testCollection\",filter: {$text:{$search:\"name\"}} }");
		while(rs.next()){
			System.out.println("get string by column name: " + rs.getString("name"));
			System.out.println("get string by column index: " + rs.getString(2));
			System.out.println("raw document: " + rs.getString(-100));
		}
		rs.close();
		st.close();
	} catch (SQLException e) {
		e.printStackTrace();
	} finally {
		try{
			conn.close();
		} catch(SQLException sqlEx){
			sqlEx.printStackTrace();
		}
	}

### Tips, tricks, fancy features

#### Get the raw Document from ResultSet iteration as JSON

	String doc = rs.getObject(-100);
	
### List of Commands

- [General](https://www.mongodb.com/docs/manual/reference/command/)
- [Find](https://docs.mongodb.org/manual/reference/command/find/#dbcmd.find)
- [Update](https://docs.mongodb.org/manual/reference/command/update/#dbcmd.update)
- [Operators](https://www.mongodb.com/docs/manual/reference/operator/query/)


##### Specify a collection to query

	‘{find:"testCollection"}’

##### Specify a count of a collection

	‘{count:"testCollection"}’
	
##### Specify a create collection

	‘{create:"testCollection"}’
	
##### Specify a drop of a collection

	‘{drop:"testCollection"}’
	
##### Insert documents

	‘{insert: "testCollection", documents: [
		{"name":"foo", "age": 1},
		{"name":"bar", "age": 2},
		{"name":"mix", "age": 3}
	]}’
	
##### Find document(s)

	‘{ find: "testCollection",filter: {"name": "foo"} }’
	
##### Update document(s)

	‘update: {"update":"testCollection",
	    	"updates":[
	        {
	            q:{"name":"foo"},
	            u:{$set:{"name":"foo2"}}
	        }
	    ]
	}‘

##### Delete document(s)

	‘{delete: "testCollection", deletes: [
		{ q: {"name": "foo2"}, limit: 0}
	]}’
	
    
	
