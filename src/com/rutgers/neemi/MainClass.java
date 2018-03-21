package com.rutgers.neemi;

import java.io.IOException;
import java.util.Date;

public class MainClass {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Parse p = new Parse();
		//Vendor v = p.parser_memo("PURCHASE#  - 11-25-09 SAVENORS MARKET BOSTON MA auth# 60618",new Date());
		Vendor v=p.parser_memo("PURCHASE#  - 09-19-09 CARBON FUND.ORG 240-293-2700 MD auth# 31933", new Date());
		System.out.println(v.state);
		System.out.println(v.city);
		System.out.println(v.zip);
		System.out.println(v.phone);
		System.out.println(v.description);
	}

}
