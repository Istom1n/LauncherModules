package ru.gravit.launchermodules.addhash;

import com.github.wolf480pl.phpass.PHPass;
import ru.gravit.launcher.ClientPermissions;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.MySQLSourceConfig;
import ru.gravit.launchserver.auth.provider.AuthProvider;
import ru.gravit.launchserver.auth.provider.AuthProviderResult;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.SecurityHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MySQLPhpHashAuthProvider extends AuthProvider {
    private MySQLSourceConfig mySQLHolder;
    private String query;
    private String message;
    private String[] queryParams;
    private boolean usePermission;
    private int passIterationCountLog2 = 8; // default
    private transient PHPass pass;
    
    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws SQLException, AuthException {
        try(Connection c = mySQLHolder.getConnection())
        {
        	PreparedStatement s = c.prepareStatement(query);
        	String[] replaceParams = {"login", login, "password", password, "ip", ip};
        	for (int i = 0; i < queryParams.length; i++)
            	s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

        	// Execute SQL query
        	s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
        	try (ResultSet set = s.executeQuery()) {
            	return set.next() ? pass.checkPassword(password, set.getString(1)) ? new AuthProviderResult(set.getString(2), SecurityHelper.randomStringToken(), usePermission ? new ClientPermissions(set.getLong(3)) : LaunchServer.server.config.permissionsHandler.getPermissions(set.getString(1))) : authError(message) : authError(message);
        	}
        }
    }

    @Override
    public void init() {
    	pass = new PHPass(passIterationCountLog2);
    }
    
    @Override
    public void close() {
    	mySQLHolder.close();
    }
}
