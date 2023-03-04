package diskord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Basic database tool. It facilitates the connection to a database and enable
 * to execute a statement.
 * 
 * @author soulet
 *
 */
public class Database {

	/**
	 * Returns a connection to the database host.
	 * 
	 * @param host		internet address of the host
	 * @param user		user name for the connection
	 * @param pwd		password of the user
	 * @param dbname	name of the used database 
	 * @return	the new database connection
	 */
	public synchronized static Connection getConnection(String host, String user, String pwd, String dbname) {
		Connection connection = null;
		try {
			Properties props = new Properties();
			props.setProperty("user", user);
			props.setProperty("password", pwd);
			Class.forName("org.mariadb.jdbc.Driver");
			connection = DriverManager.getConnection(
					"jdbc:mariadb://" + host + "/" + dbname
							+ "?autoReconnect=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
					props);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * Execute the statement strStmt using the given connection.
	 * 
	 * @param connection	a database connection
	 * @param strStmt		a statement to execute
	 */
	public static void executeStatement(Connection connection, String strStmt) {
		try {
			java.sql.Statement stmt = connection.createStatement();
			stmt.execute(strStmt);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
