package com.cooper.recrev.data.neo4j;


import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Component;

@Component
public class Neo4jConnector {
	
	private Driver driver = null;
	
	public synchronized void connect() {
		if (driver == null) {
			driver = GraphDatabase.driver("bolt://10.1.1.4:7687",
					AuthTokens.basic("neo4j", "buaaxzl"));
		} 
	}
	
	public String searchUserId(String name) {
		String id = "";
		try (Session session = driver.session()) {
			try (Transaction tx = session.beginTransaction()) {
				StatementResult result = tx
						.run("Match (n:User {login:'" + name + "'})  Return n.userId");
				while (result.hasNext()) {
					Record record = result.next();
					id = record.get("n.userId").asString();
				}
				tx.success();
			}
		}
		return id;
	}
	
	public void closeConnection() {
		driver.close();
	}
	
	public static void main(String[] args) {
		Neo4jConnector n = new Neo4jConnector();
		n.connect();
		
		System.out.println(n.searchUserId("mschiller"));
		n.closeConnection();
	}
}
