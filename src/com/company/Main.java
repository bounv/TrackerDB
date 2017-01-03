package com.company;

import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    //public static HashMap<String, User> users = new HashMap<>();
    //public static ArrayList<Book> books = new ArrayList<>();

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

        addTestUser(conn);
        selectTestUser(conn);
        addInsertEntry(conn);
        selectTestEntry(conn);
        selectTestEntries(conn);


        Spark.init();

        Spark.get(
                "/",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");

                    //put books in this hashmap
                    HashMap m = new HashMap();
                    ArrayList<Book> books = selectEntries(conn);

                    m.put("userName", userName);
                    m.put("books", books);

                    return new ModelAndView(m, "home.html");
                }),
                new MustacheTemplateEngine()
        );


        Spark.post(
                "/login",
                ((request, response) -> {
                    String userName = request.queryParams("loginName");
                    if (userName == null) {
                        throw new Exception("Login name not found.");
                    }

                    User user = selectUser(conn, userName);
                    if (user == null) {
                        //user = new User(userName);
                        insertUser(conn, userName);
                    }

                    Session session = request.session();
                    session.attribute("userName", userName);

                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post(
                "/add-book",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");
                    if (userName == null) {
                        throw new Exception("Not logged in.");
                    }

                    String title = request.queryParams("messageTitle");
                    if (title == null) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }

                    String author = request.queryParams("messageAuthor");
                    if (author == null) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }

                    String publisher = request.queryParams("messagePublisher");
                    if (publisher == null) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }

                    int releaseYear = Integer.parseInt(request.queryParams("messageYear"));
                    if (releaseYear < 1980  && releaseYear > 2017) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }

                    User user = selectUser(conn, userName);

                    insertEntry(conn, title, author, publisher, releaseYear, user.id);

                    //Book m = new Book(books.size(), title, author, publisher, releaseYear, user.id);
                    //books.add(m);

                    response.redirect(request.headers("Referer"));
                    return "";
                })
        );

        Spark.post(
                "/update-book",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");
                    if (userName == null) {
                        throw new Exception("Not logged in.");
                    }

                    String title = request.queryParams("messageTitle");
                    if (title == null) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }

                    String author = request.queryParams("messageAuthor");
                    if (author == null) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }

                    String publisher = request.queryParams("messagePublisher");
                    if (publisher == null) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }

                    int releaseYear = Integer.parseInt(request.queryParams("messageYear"));
                    if (releaseYear < 1980  && releaseYear > 2017) {
                        throw new Exception("Didn't get necessary query parameters.");
                    }


                    int id = Integer.parseInt(request.queryParams("id"));

                    User user = selectUser(conn, userName);

                    String book = request.queryParams("updateBook");

                    Book b = selectEntry(conn, id);


                    if(b.userId == user.id) {
                        updateEntry(conn, title, author, publisher, releaseYear, user.id, id);
                    } else {
                        throw new Exception("EDIT your own book");
                    }

                    response.redirect("/");
                    return "";
                })
        );


        Spark.post(
                "/delete-book",
                ((request, response) -> {
                    Session session = request.session();
                    String userName = session.attribute("userName");
                    if (userName == null) {
                        throw new Exception("Not logged in.");
                    }

                    int id = Integer.parseInt(request.queryParams("id"));

                    User user = selectUser(conn, userName);

                    Book b = selectEntry(conn, id);

                    if(b.userId == user.id) {
                        deleteEntry(conn, id);
                    } else {
                        throw new Exception("DELETE your own book");
                    }

                    response.redirect("/");
                    return "";
                })
        );

    }

    public static void addTestUser(Connection conn) throws SQLException {
        insertUser(conn, "Harry");
        insertUser(conn, "Hermione");
        insertUser(conn, "Ron");
    }

    public static void selectTestUser(Connection conn) throws SQLException {
        selectUser(conn, "The Philosopher's Stone");
        selectUser(conn, "The Chamber of Secrets");
        selectUser(conn, "The Prisoner of Azkaban");
    }


    public static void addInsertEntry(Connection conn) throws SQLException {
        insertEntry(conn, "The Philosopher's Stone", "JK Rowling", "Bloomsbury", 1997, 1);
        insertEntry(conn, "The Chamber of Secrets", "JK Rowling", "Bloomsbury", 1998, 2);
        insertEntry(conn, "The Prisoner of Azkaban", "JK Rowling", "Bloomsbury", 1999, 3);
    }

    public static void selectTestEntry(Connection conn) throws SQLException {
        selectEntry(conn, 1);
        selectEntry(conn, 2);
        selectEntry(conn, 3);
    }

    public static void selectTestEntries(Connection conn) throws SQLException {
        selectEntries(conn);
        selectEntries(conn);
        selectEntries(conn);
    }

    private static void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users " +
                "(id IDENTITY, name VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS books " +
                "(id IDENTITY, title VARCHAR, author VARCHAR, publisher VARCHAR, releaseYear INT, userId VARCHAR)");
    }

    public static void insertUser(Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?)");
        stmt.setString(1, name);
        stmt.execute();
    }

    public static User selectUser(Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1, name);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("id");
            return new User(id, name);
        }
        return null;
    }

    public static void insertEntry(Connection conn, String title, String author, String publisher, int releaseYear, int userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO books VALUES " +
                "(NULL, ?, ?, ?, ?, ?)");
        stmt.setString(1, title);
        stmt.setString(2, author);
        stmt.setString(3, publisher);
        stmt.setInt(4, releaseYear);
        stmt.setInt(5, userId);
        stmt.execute();
    }

    public static Book selectEntry(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM books " +
                "INNER JOIN users ON books.userId = users.id " +
                "WHERE books.id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if(results.next()) {
            String title = results.getString("books.title");
            String author = results.getString("books.author");
            String publisher = results.getString("books.publisher");
            int releaseYear = results.getInt("books.releaseYear");
            int userId = results.getInt("books.userId");
            return new Book(id, title, author, publisher, releaseYear, userId);
        }
        return null;
    }


    public static ArrayList<Book> selectEntries(Connection conn) throws SQLException {
        ArrayList<Book> books = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM books " +
                "INNER JOIN users ON books.userId = users.id ");
        ResultSet results = stmt.executeQuery();
        while(results.next()) {
            int id = results.getInt("books.id");
            String title = results.getString("books.title");
            String author = results.getString("books.author");
            String publisher = results.getString("books.publisher");
            int releaseYear = results.getInt("books.releaseYear");
            int userId = results.getInt("books.userId");
            Book book = new Book(id, title, author, publisher, releaseYear, userId);
            books.add(book);
        }
        return books;
    }

    public static void updateEntry(Connection conn, String title, String author, String publisher, int releaseYear, int userId, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("UPDATE books SET title = ?, author = ?, publisher = ?, releaseYear = ?, userId = ? WHERE id = ?");
        stmt.setString(1, title);
        stmt.setString(2, author);
        stmt.setString(3, publisher);
        stmt.setInt(4, releaseYear);
        stmt.setInt(5, userId);
        stmt.setInt(6, id);
        stmt.execute();
    }

    public static void deleteEntry(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM books WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }




}


