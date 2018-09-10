package com.hazelcast.commons;


import java.io.Serializable;

public class Book implements Serializable {

	private static final long serialVersionUID = 1L;
	
    private String code;
    private String description;
    private String language;
  
     	 	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}

    
}
