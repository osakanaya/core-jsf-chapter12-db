package com.corejsf;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.sql.DataSource;

@Named("user")
@SessionScoped
public class UserBean implements Serializable {

	private static final Logger logger = Logger.getLogger("com.corejsf");
	private static final long serialVersionUID = 1L;

	private String name;
	private String password;
	private int count;
	private boolean loggedIn;
	
	@Resource(name="jdbc/corejsfdb")
	private DataSource ds;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getCount() {
		return count;
	}
	
	public String login() {
		try {
			doLogin();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "login failed", e);
			
			return "internalError";
		}
		
		if(loggedIn) {
			return "loginSuccess";
		} else {
			return "loginFailure";
		}
	}
	
	public String logout() {
		loggedIn = false;
		name = "";
		password = "";
		
		return "login";
	}
	
	private void doLogin() throws SQLException {
		if (ds == null) 
			throw new SQLException("No data source injected.");
		
		Connection conn = ds.getConnection();
		if (conn == null)
			throw new SQLException("No connection established.");
		
		try {
			conn.setAutoCommit(false);
			boolean committed = false;

			try {
				PreparedStatement passwordQuery = conn.prepareStatement(
						"SELECT passwd, logincount from Credentials WHERE username = ?");
				passwordQuery.setString(1, name);
				ResultSet result = passwordQuery.executeQuery();
				
				if (!result.next()) return;
				String storedPassword = result.getString("passwd");
				loggedIn = password.equals(storedPassword);
				if (!loggedIn) return;

				count = result.getInt("logincount") + 1;
				PreparedStatement updateCounterStat = conn.prepareStatement(
						"UPDATE Credentials SET logincount = logincount + 1 WHERE USERNAME = ?");
				updateCounterStat.setString(1, name);
				updateCounterStat.executeUpdate();
				
				conn.commit();
				committed = true;
			} finally {
				if (!committed) conn.rollback();
			}
		} finally {
			conn.close();
		}
	}
}
