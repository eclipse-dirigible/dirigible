package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.user.CreateUserBuilder;

public class MSSQLCreateUserBuilder extends CreateUserBuilder {

    public MSSQLCreateUserBuilder(ISqlDialect dialect, String userId, String password) {
        super(dialect, userId, password);
    }

    @Override
    protected String generateCreateUserStatement(String user, String pass) {
        // create server login
        return "CREATE LOGIN " + getEscapeSymbol() + user + getEscapeSymbol() + SPACE + "WITH PASSWORD =" + getPasswordEscapeSymbol() + pass
                + getPasswordEscapeSymbol() + "; "

                // create user mapped to the login
                + "CREATE USER " + getEscapeSymbol() + user + getEscapeSymbol() + SPACE + "FOR LOGIN " + getEscapeSymbol() + user
                + getEscapeSymbol();
    }
}
