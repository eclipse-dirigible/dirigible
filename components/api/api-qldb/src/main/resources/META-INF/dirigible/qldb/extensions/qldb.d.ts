declare module "sdk/qldb" {
    class QLDBRepository {
        /**
         *  Constructs a repository with given ledger and table.
         *  Notice: If the table does not exist you must call .createTable()
         */
        constructor(ledgerName: string, tableName: string);

        /**
         *  Creates the repository table.
         */
        createTable();

        /**
         *  Drops the repository table.
         *  Notice: Dropping tables in QLDB inactivates them.
         *  You can undo that with an UNDROP statement in PartiQL.
         */
        dropTable();

        /**
         *  Inserts the entry in the repository.
         *  On success returns the inserted entry.
         */
        insert(entry: object): object;

        /**
         *  Updates the entry in the repository.
         *  On success returns the updated entry.
         *  Notice: The entry must contain a documentId property.
         */
        update(entry: object): object;

        /**
         *  Returns the repository entry with given documentId.
         */
        getById(documentId: string): object;

        /**
         *  Returns all the repository entries.
         */
        getAll(): object[];

        /**
         *  Deletes the entry by given documentId.documentId.
         *  On success returns the documentId of the deleted object.
         */
        delete(documentId: string): string;

        /**
         *  Deletes the entry.
         *  On success returns the documentId of the deleted object.
         *  Notice: The entry must contain a documentId property.
         */
        delete(entry: object): string;

        /**
         *  Returns the entire history of transactions of the repository.
         */
        getHistory(): object[];

        /**
         *  Returns the ledger name the repository is working against.
         */
        getLedgerName(): string;

        /**
         *  Returns the table name the repository is working against.
         */
        getTableName(): string;
    }
}
