package com.onlystarczy.databuild.mysql;

import java.util.List;

import com.onlystarczy.databuild.DataColumn;

public class DataStructure4Mysql {
	
	private String name;
	
	private List<DataColumn> columns;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<DataColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<DataColumn> columns) {
		this.columns = columns;
	}
	

}
