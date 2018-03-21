package com.rutgers.neemi;

public class Vendor {
	
	String description;
	String city;
	String state;
	String zip;
	String phone;
	
	public Vendor() {
		
	}
	
	public Vendor(String description, String city, String state, String zip, String phone) {
		super();
		this.description = description;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.phone = phone;
	}
	
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	

}
