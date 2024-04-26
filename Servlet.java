package edu.cs;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/FileUploadServlet")
@MultipartConfig(fileSizeThreshold=1024*1024*10, 	// 10 MB 
               maxFileSize=1024*1024*50,      	// 50 MB
               maxRequestSize=1024*1024*100)   	// 100 MB
public class FileUploadServlet extends HttpServlet {

  private static final long serialVersionUID = 205242440643911308L;
  private static final String JDBC_URL = "jdbc:mysql://http://3.139.82.251:3306/CS381";
  private static final String DB_USER = "brand0ns";
  private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
	
  /**
   * Directory where uploaded files will be saved, its relative to
   * the web application directory.
   */
  private static final String UPLOAD_DIR = "uploads";
  
  @Override
  public void init() throws ServletException {
      super.init();
      createDatabasesAndTables(); // Initialize databases and tables when servlet is initialized
  }
  
  private Connection getConnection() throws SQLException {
      try {
          Class.forName("com.mysql.cj.jdbc.Driver");
          return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
      } catch (ClassNotFoundException e) {
          throw new SQLException("MySQL driver not found", e);
      }
  }
  
  
  
  
  protected void doPost(HttpServletRequest request,
          HttpServletResponse response) throws ServletException, IOException {
      // gets absolute path of the web application
      String applicationPath = request.getServletContext().getRealPath("");

      // constructs path of the directory to save uploaded file
      String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;

       
      // creates the save directory if it does not exists
      File fileSaveDir = new File(uploadFilePath);
      if (!fileSaveDir.exists()) {
          fileSaveDir.mkdirs();
      }
      System.out.println("Upload File Directory="+fileSaveDir.getAbsolutePath());
      
      String fileName = "";
      //Get all the parts from request and write it to the file on server
      for (Part part : request.getParts()) {
          fileName = getFileName(part);
          fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
          part.write(uploadFilePath + File.separator + fileName);
      }

    String message = "Result";
    String content = new Scanner(new File(uploadFilePath + File.separator + fileName)).useDelimiter("\\Z").next();      
    response.getWriter().write(message + "<BR>" + content);
        
    /****** Integrate remote DB connection with this servlet, uncomment and modify the code below *******
	   //ADD YOUR CODE HERE!

    ********/
    try (Connection connection = getConnection()) {
        // Perform database operations here
        // For example, you can insert data into a table
        insertDataIntoDatabase(connection, fileName, content);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

// Method to insert data into the database
private void insertDataIntoDatabase(Connection connection, String fileName, String content) throws SQLException {
    // Prepare your SQL insert statement
    String sql = "INSERT INTO YourTableName (column1, column2) VALUES (?, ?)";

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
        // Set values for parameters in the SQL statement
        statement.setString(1, fileName);
        statement.setString(2, content);

        // Execute the insert statement
        int rowsInserted = statement.executeUpdate();
        if (rowsInserted > 0) {
            System.out.println("Data inserted successfully.");
        } else {
            System.out.println("Failed to insert data.");
        }
    }
}
   
    
  
  
  
  // Method to create databases and tables
  private void createDatabasesAndTables() {
      try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD)) {
          Statement statement = connection.createStatement();

          // SQL queries for creating databases and tables
          String[] queries = {
                  // Internal Operations Database Schema
                  "CREATE DATABASE IF NOT EXISTS InternalOperations",
                  "USE InternalOperations",
                  "CREATE TABLE IF NOT EXISTS Customers (customer_id INT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255), phone VARCHAR(20), address VARCHAR(255), premises_sqft INT, schematics BLOB)",
                  "CREATE TABLE IF NOT EXISTS Vendors (vendor_id INT PRIMARY KEY, name VARCHAR(255), contact_details VARCHAR(255), address VARCHAR(255), goods_type VARCHAR(255), bank_account_info VARCHAR(255))",
                  "CREATE TABLE IF NOT EXISTS PerformanceMetrics (metric_id INT PRIMARY KEY, vendor_id INT, relationship_rating INT, quality_rating INT, FOREIGN KEY (vendor_id) REFERENCES Vendors(vendor_id))",
                  "CREATE TABLE IF NOT EXISTS Contracts (contract_id INT PRIMARY KEY, vendor_id INT, agreement_copy BLOB, regulatory_compliance VARCHAR(255), FOREIGN KEY (vendor_id) REFERENCES Vendors(vendor_id))",
                  "CREATE TABLE IF NOT EXISTS CommunicationsLogs (log_id INT PRIMARY KEY, vendor_id INT, interaction_type VARCHAR(255), interaction_details TEXT, interaction_date DATE, FOREIGN KEY (vendor_id) REFERENCES Vendors(vendor_id))",

                  // External Operations Database Schema
                  "CREATE DATABASE IF NOT EXISTS ExternalOperations",
                  "USE ExternalOperations",
                  "CREATE TABLE IF NOT EXISTS Products (product_id INT PRIMARY KEY, name VARCHAR(255), description TEXT, category VARCHAR(255), price DECIMAL(10, 2), specifications TEXT)"
          };

          // Execute each query
          for (String query : queries) {
              statement.executeUpdate(query);
          }
      } catch (SQLException e) {
          e.printStackTrace();
      }
  }

  /**
   * Utility method to get file name from HTTP header content-disposition
   */
  private String getFileName(Part part) {
      String contentDisp = part.getHeader("content-disposition");
      System.out.println("content-disposition header= "+contentDisp);
      String[] tokens = contentDisp.split(";");
      for (String token : tokens) {
          if (token.trim().startsWith("filename")) {
              return token.substring(token.indexOf("=") + 2, token.length()-1);
          }
      }
      return "";
  }
  
  
	private void writeToResponse(HttpServletResponse resp, String results) throws IOException {
		PrintWriter writer = new PrintWriter(resp.getOutputStream());
		resp.setContentType("text/plain");

		if (results.isEmpty()) {
			writer.write("No results found.");
		} else {
			writer.write(results);
		}
		writer.close();
	}	
	
	
}

