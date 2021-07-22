import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import entity.User;

public class Main {
    public static void main(String[] args) throws SQLException, JSchException{

        List<User> userList = new ArrayList<User>();

        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        String strToday = simpleDateFormat.format(date);

        String selectQuery = "SELECT LOGINID, USERNAME, DEPTNAME, SABUN, POSITIONNAME FROM USER "; // sample select query

        String insertQuery = "INSERT INTO user (LOGINID, USERNAME, DEPTNAME, SABUN, POSITIONNAME) " +
                             "VALUES(?, ?, ?, ?, ?)"; // sample insert query

        oracleConnection(userList,selectQuery); // select data oracle
        mysqldbConnection(userList,insertQuery); // insert data mysql

        System.exit(0);

        return;
    }

    static void mysqldbConnection(List<User> userList, String query) throws SQLException, JSchException{
        final String serverIp = "input your server ip";
        final int serverPort = 22;
        final String serverUser = "input your server user";
        final String serverPassword = "input your server password";
        final String dbName = "input your database name";
        final String dbUser = "input your database user";
        final String dbPassword = "input your database password";
        final int dbPort = 3306;

        StringWriter errors = new StringWriter();

        Connection conn = null;
        PreparedStatement pstm = null;

        JSch jsch = new JSch();

        Session session = jsch.getSession(serverUser, serverIp,serverPort);
        session.setPassword(serverPassword);
        session.setConfig("StrictHostKeyChecking", "no");

        session.connect();

        int tunnelPort = session.setPortForwardingL(0,"localhost",dbPort); //random tunneling port create

        try{
            Class.forName("com.mysql.jdbc.Driver"); // use mysql driver

            conn = DriverManager.getConnection("jdbc:mysql://localhost:"+ tunnelPort +"/"+dbName, dbUser, dbPassword);

            pstm = conn.prepareStatement(query);

            for(User user : userList){
                pstm.setString(1, user.getLoginid());
                pstm.setString(2, user.getUsername());
                pstm.setString(3, user.getDeptname());
                pstm.setString(4, user.getSabun());
                pstm.setString(5, user.getPostionname());

                pstm.executeUpdate();
            }
            createLog("_mysql_success","Mysql DB Insert success.");
        }catch(ClassNotFoundException cnfe){
            cnfe.printStackTrace(new PrintWriter(errors));
            createLog("_mysql_error","Mysql DB driver load failed.\r\n"+errors.toString());
        }catch(SQLException sqle){
            sqle.printStackTrace(new PrintWriter(errors));
            createLog("_mysql_error","Mysql DB connect failed.\r\n"+errors.toString());
        }catch(Exception e){
            e.printStackTrace(new PrintWriter(errors));
            createLog("_mysql_error","Mysql DB unknown error occured.\r\n"+errors.toString());
        }
        finally{
            conn.close();
            session.disconnect();
            errors = null;
        }
        return ;
    }

    public static void oracleConnection(List<User> userList, String query) throws SQLException{
        final String dbIp = "input your db ip";
        final String dbName = "input your db name";
        final String dbUser = "input your db user";
        final String dbPassword = "input your db password";
        final String dbPort = "1521";

        StringWriter errors = new StringWriter();

        Connection conn = null;
        Statement stm = null;
        ResultSet rs = null;

        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection("jdbc:oracle:thin:@"+ dbIp +":"+ dbPort +"/"+ dbName, dbUser, dbPassword);
            stm = conn.createStatement();
            rs = stm.executeQuery(query);

            while(rs.next()){
                User user = new User();
                user.setLoginid(rs.getString(1));
                user.setUsername(rs.getString(2));
                user.setDeptname(rs.getString(3));
                user.setSabun(rs.getString(4));
                user.setPostionname(rs.getString(5));

                userList.add(user);
            }
            createLog("_oracle_success","Oracle DB Select success.");
        }catch(ClassNotFoundException cnfe){
            cnfe.printStackTrace(new PrintWriter(errors));
            createLog("_oracle_error","Oracle DB driver load failed.\r\n"+errors.toString());
        }catch(SQLException sqle){
            sqle.printStackTrace(new PrintWriter(errors));
            createLog("_oracle_error","Oracle DB connect failed.\r\n"+errors.toString());
        }catch(Exception e){
            e.printStackTrace(new PrintWriter(errors));
            createLog("_oracle_error","Oracle DB unknown error occured.\r\n"+errors.toString());
        }
        finally{
            conn.close();
            errors = null;
        }

        return ;
    }


    // log create
    public static void createLog(String fileName, String message){

        File fileDir = null;
        FileWriter fileWriter = null;

        String filePath = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        String strToday = simpleDateFormat.format(new Date());

        fileDir = new File(".\\logs");

        if(!fileDir.isDirectory()){
            fileDir.mkdirs();
        }

        filePath = ".\\logs"+File.separator+strToday+fileName+".log";

        try{
            fileWriter = new FileWriter(filePath, true);
            fileWriter.write("\r\n");
            fileWriter.write("*******"+strToday+"*******");
            fileWriter.write("\r\n");
            fileWriter.write(message);
            fileWriter.write("\r\n");
            fileWriter.write("************************");
            fileWriter.flush();
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            try{
                filePath = null;
                fileDir = null;
                if(fileWriter != null)
                    fileWriter.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
