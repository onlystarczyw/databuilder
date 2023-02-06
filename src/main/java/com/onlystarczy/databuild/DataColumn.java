package com.onlystarczy.databuild;

public class DataColumn {
	
	private String name;

	private String type;

	private int length;
	
	public DataColumn(String name, String type, int length) {
		this.name = name;
		this.type = type;
		this.length = length;
	}

	public DataColumn(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public DataColumn() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
	
	

}
