package com.example.milkdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class MilkdeliveryApplication {

	public static void main(String[] args) {
		boolean resetDb = false;
		for (String arg : args) {
			if ("--reset-db".equalsIgnoreCase(arg)) {
				resetDb = true;
				break;
			}
		}
		if (resetDb) {
			resetDatabase();
		}
		SpringApplication.run(MilkdeliveryApplication.class, args);
	}

	private static void resetDatabase() {
		System.out.println("Resetting database...");
		String url = "jdbc:mysql://localhost:3306/smart_milk_delivery?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
		String user = "root";
		String password = "1234567";
		try (Connection conn = DriverManager.getConnection(url, user, password)) {
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
				
				DatabaseMetaData metaData = conn.getMetaData();
				try (ResultSet rs = metaData.getTables("smart_milk_delivery", null, "%", new String[]{"TABLE"})) {
					List<String> tables = new ArrayList<>();
					while (rs.next()) {
						tables.add(rs.getString("TABLE_NAME"));
					}
					for (String table : tables) {
						System.out.println("Dropping table: " + table);
						stmt.execute("DROP TABLE IF EXISTS `" + table + "`");
					}
				}
				
				stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
				System.out.println("Database reset completed successfully.");
			}
		} catch (Exception e) {
			System.err.println("Error resetting database: " + e.getMessage());
			e.printStackTrace();
		}
	}

}

