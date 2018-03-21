package com.rutgers.neemi;

public class ChannelDetails {
	
	String check_no;
	String auth;
	String auth_date; //"YYYY-MM-DD"
	String auth_time; //"HH:MM"
	Channel ch_type;
	String account_description;
	float amount;
	
	
	public ChannelDetails() {
		
	}
	
	public String getCheck_no() {
		return check_no;
	}
	public void setCheck_no(String check_no) {
		this.check_no = check_no;
	}
	public String getAuth() {
		return auth;
	}
	public void setAuth(String auth) {
		this.auth = auth;
	}
	public String getAuth_date() {
		return auth_date;
	}
	public void setAuth_date(String auth_date) {
		this.auth_date = auth_date;
	}
	public String getAuth_time() {
		return auth_time;
	}
	public void setAuth_time(String auth_time) {
		this.auth_time = auth_time;
	}
	
	
	

}
