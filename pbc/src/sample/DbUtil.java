package sample;

import java.sql.*;

/**
 * Created by kooxiv on 2017/5/8.
 */
public class DbUtil {
    private static DbUtil dbUtil;

    private DbUtil() {
        try {
            Statement statement = getConnection().createStatement();
            String crate = "CREATE TABLE IF NOT EXISTS rate (ID INTEGER PRIMARY KEY AUTOINCREMENT,DATES TEXT,TOPICS TEXT,RATES TEXT)";
            String history = "CREATE TABLE IF NOT EXISTS fetchHistory (ID INTEGER PRIMARY KEY AUTOINCREMENT,URL TEXT)";
            statement.executeUpdate(crate);
            statement.executeUpdate(history);
        } catch (Exception e) {
            System.err.println("sqlite open failed");
            e.printStackTrace();
        }
        System.out.println("sqlite open success");
    }

    public void insertRate(String date, String topic, String value) {
        String query = "select * from rate where DATES = '" + date + "' AND TOPICS = '" + topic + "' AND RATES = '" + value + "'";
        ResultSet rs = query(query);
        try {
            while (rs.next()) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql = "insert into rate (DATES,TOPICS,RATES) VALUES ('" + date + "','" + topic + "','" + value + "')";
        executeUpdate(sql);
    }

    public void insertHistory(String url) {
        if (url == null || url.length() < 3) {
            return;
        }
        String sql = "insert into fetchHistory (URL) VALUES ('" + url + "')";
        executeUpdate(sql);
    }

    public boolean queryHistoryExsist(String url) {
        String sql = "select * from fetchHistory where URL = '" + url + "'";
        ResultSet rs = query(sql);
        try {
            while (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    void executeUpdate(String sql) {
        try {
            Statement statement = getConnection().createStatement();
            debug(sql);
            statement.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ResultSet query(String sql) {
        try {
            Statement statement = getConnection().createStatement();
            return statement.executeQuery(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void debug(String s) {
        System.out.println(s);
    }

    private Connection connection;

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        if (connection == null) {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:exchangeRate.sql");
        }
        return connection;
    }

    public static DbUtil getDbUtil() {
        if (dbUtil == null) {
            synchronized (DbUtil.class) {
                if (dbUtil == null) {
                    dbUtil = new DbUtil();
                }
            }
        }
        return dbUtil;
    }

}
