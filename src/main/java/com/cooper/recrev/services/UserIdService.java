package com.cooper.recrev.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cooper.recrev.data.neo4j.Neo4jConnector;

@Component
public class UserIdService {
	
	@Autowired
	private Neo4jConnector con;
	
	public List<String> transferToIds(List<String> names) {
		List<String> ids = new ArrayList<String>();
		
		try {
			con.connect();
			if (names.size() == 0) return ids;
			
			//用户名 到 用户id 的转换
			for (String name : names) {
				ids.add(con.searchUserId(name));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ids;
	}
}
