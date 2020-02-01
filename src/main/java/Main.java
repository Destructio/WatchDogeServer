import com.google.gson.Gson;
import spark.Request;

import java.sql.*;
import java.util.ArrayList;

import static spark.Spark.*;

public class Main {

    private static final String dbURL = "jdbc:mysql://localhost:3306/new_schema?autoReconnect=true&useSSL=false";
    private static final String dbUSER = "root";
    private static final String dbPASSWORD = "mysqlroot";
    private static final Integer webServicePORT = 8080;

    public static void main(String[] args) {

        port(webServicePORT);

        get("/reg", (request, response) -> dbSignUp(request.queryParams("name"), request.queryParams("email"), request.queryParams("pass"), request));
        get("/login", (request, response) -> login(request.queryParams("email"), request.queryParams("pass"), request));

        get("/getFirstJson", (request, response) -> firstJsonSend(request.queryParams("computer"), request.queryParams("email"), request));
        get("/getJSON", (request, response) -> jsonSend(request.queryParams("computer"), request.queryParams("email"), request));
        get("/getAllJSON", (request, response) -> jsonSendAll(request.queryParams("computer"), request.queryParams("email"), request));


        post("/postClientJson", (request, response) -> clientSendJson(request.body(), request.queryParams("computer"), request.queryParams("email"), request));
        post("/postClientJsonL", (request, response) -> clientSendJsonL(request.body(), request.queryParams("computer"), request.queryParams("email"), request));

        get("/getComputers", (request, response) -> getComputers(request.queryParams("email"), request));
        get("/getUserName", (request, response) -> getUserName(request.queryParams("email"), request));
    }

    private static String getUserName(String email, Request request) {

        String username = null;

        if (email.equals("")) {
            username = "NULL";
        } else {
            try (Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
                 Statement stmt = con.createStatement();
                 ResultSet resultSet = stmt.executeQuery("SELECT username FROM new_schema.user WHERE useremail='" + email + "';")
            ) {
                while (resultSet.next()) {
                    username = resultSet.getString(1);
                }

            } catch (SQLException e) {
                System.err.println(e.getLocalizedMessage());
                username = "-1";
            }
        }

        System.out.println("Returned String User:" + username);

        consoleInfo(request, username);
        return username;
    }

    private static String getComputers(String email, Request request) {

        ArrayList<String> computerList = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
             Statement stmt = con.createStatement();
             ResultSet resultSet = stmt.executeQuery("SELECT computerjson FROM new_schema.computer WHERE useremail='" + email + "';")
        ) {
            while (resultSet.next()) {
                computerList.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        consoleInfo(request, computerList.toString());
        return new Gson().toJson(computerList);
    }

    private static String clientSendJsonL(String json, String computerName, String username, Request request) {

        String resultCode = "0";

        try (Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("INSERT INTO new_schema.statistic(namecomputer,useremail,computerjsonL) VALUES('" + computerName + "','" + username + "','" + json + "');");
            resultCode = "1";
        } catch (java.sql.SQLException e) {
            System.err.println(e.getLocalizedMessage());
            resultCode = "-1";
        }
        consoleInfo(request, resultCode);
        return resultCode;

    }

    private static String firstJsonSend(String computerName, String email, Request request) {

        String jsonFromDB = null;

        try (
                Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
                Statement stmt = con.createStatement();
                ResultSet resultSet = stmt.executeQuery("SELECT computerjson FROM new_schema.computer WHERE useremail='" + email + "' AND namecomputer ='" + computerName + "';")) {
            if (resultSet.last())//Giving a last element of computerjson label with params
                if (resultSet.getString(1) == null) jsonFromDB = "-1";
                else jsonFromDB = resultSet.getString(1);
        } catch (java.sql.SQLException e) {
            System.err.println(e.getLocalizedMessage());
            jsonFromDB = "-1";
        }
        consoleInfo(request, jsonFromDB);
        return jsonFromDB;
    } //return json string with first comp info

    private static String jsonSend(String computerName, String email, Request request) {

        String resultCode = "0";

        try (
                Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
                Statement stmt = con.createStatement();
                ResultSet resultSet = stmt.executeQuery("SELECT computerjsonL FROM new_schema.statistic WHERE useremail='" + email + "' AND namecomputer ='" + computerName + "';")) {
            if (resultSet.last())//Giving a last element of computerjson label with params
                if (resultSet.getString(1) == null) resultCode = "NULL";
                else resultCode = resultSet.getString(1);
        } catch (java.sql.SQLException e) {
            System.err.println(e.getLocalizedMessage());
            resultCode = "-1";
        }

        consoleInfo(request, resultCode);

        return resultCode;
    } //return json string with updated data

    private static String jsonSendAll(String computerName, String email, Request request) {

        String resultCode = "-1";

        ArrayList<String> allStat = new ArrayList<>();

        try (
                Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
                Statement stmt = con.createStatement();
                ResultSet resultSet = stmt.executeQuery("SELECT computerjsonL FROM new_schema.statistic WHERE useremail='" + email + "' AND namecomputer ='" + computerName + "';")) {
            while (resultSet.next()) {
                allStat.add(resultSet.getString(1));
            }

        } catch (java.sql.SQLException e) {
            System.err.println(e.getLocalizedMessage());
            resultCode = "-1";
        }

        if (!allStat.isEmpty())
            resultCode = "1";
        consoleInfo(request, resultCode);

        return new Gson().toJson(allStat);
    } //return json string with updated data

    private static String login(String email, String password, Request request) {

        String resultCode = "0";

        try (
                Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
                Statement stmt = con.createStatement();
                ResultSet resultSet = stmt.executeQuery("SELECT userpassword FROM new_schema.user WHERE useremail='" + email + "';")) {
            if (resultSet.next()) {
                if (resultSet.getString(1).equals(password)) resultCode = "true";
                else resultCode = "false";
            }
        } catch (java.sql.SQLException e) {
            System.err.println(e.getLocalizedMessage());
            resultCode = "false";
        }

        consoleInfo(request, resultCode);

        return resultCode;
    } // return true or false(isEmailWithPassword in DB)

    private static String dbSignUp(String name, String email, String password, Request request) {


        String query = "INSERT INTO new_schema.user(username,useremail,userpassword) VALUES('" + name + "','" + email + "','" + password + "');";

        String resultCode = "0";


        try (Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
             Statement st = con.createStatement()) {
            st.executeUpdate(query);
            resultCode = "1";

        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            resultCode = "-1";
        }

        consoleInfo(request, resultCode);
        return resultCode;
    }

    private static String clientSendJson(String json, String computerName, String email, Request request) {

        String resultCode;
        try (Connection con = DriverManager.getConnection(dbURL, dbUSER, dbPASSWORD);
             Statement stmt = con.createStatement()) {
            System.out.println(computerName);
            System.out.println(email);
            stmt.executeUpdate("insert into new_schema.computer(namecomputer,useremail,computerjson) values('" + computerName + "','" + email + "','" + json + "'); ");
            resultCode = "1";
        } catch (java.sql.SQLException e) {
            System.err.println(e.getLocalizedMessage());

            if (e.getLocalizedMessage().equals("Duplicate entry '" + computerName + "' for key 'namecomputer_UNIQUE'")) {
                resultCode = "DUPLICATE";
            } else {
                resultCode = "-1";
            }
        }

        consoleInfo(request, resultCode);
        return resultCode;
    }

    private static void consoleInfo(Request request, String resultCode) {
        String requestIP = request.ip();
        String requestHost = request.host();
        String requestUserAgent = request.userAgent();
        String requestURL = request.url();
        String requestBody = request.body();

        System.out.println("[" + requestIP + "] connected ,HOST [" + requestHost + "] " +
                ",UserAgent [" + requestUserAgent + "] ,URL[" + requestURL + "] Body [" + requestBody + "] " +
                "RESULT CODE = " + resultCode
        );
    }
}


