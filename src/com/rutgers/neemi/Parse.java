package com.rutgers.neemi;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Parse {
	
	ChannelDetails channelDet;
	Vendor vendor;
	
	String CHAN_SEP = "(#\\s+-\\s+|\\s+/\\s+)";
	String TYPE_STUB = "^((?<type>\\w+)"+CHAN_SEP+")?" ;
	String DOLLAR_AMOUNT = "(?<amount>-?\\$[-\\d\\.]+)";

	Pattern check_re = Pattern.compile("^SH DRAFT(#(\\s+-\\s+)?\\s*(?<checkno>\\d+)?)?$");
	Pattern atm_re = Pattern.compile(TYPE_STUB + "ATM (?<date>\\d{4} \\d{4}) (?<auth>\\d{6}) (?<description>.+)$");
	Pattern	credit_card_re = Pattern.compile(TYPE_STUB + "(?<date>\\d\\d-\\d\\d-\\d\\d) (?<description>.+) auth# (?<auth>\\d+)$");
	Pattern	pos_re = Pattern.compile(TYPE_STUB + "POS (?<date>\\d{4} \\d{4}) (?<auth>\\d{6}) (?<description>.+)$");
	Pattern	deposit_re = Pattern.compile("^DEPOSIT" + CHAN_SEP + "?\\s*(?<description>.*)$");
	Pattern	dividend_re = Pattern.compile("^(DIVIDEND|Dividend|Savings)(#?$|" + CHAN_SEP + "(?<description>.*)$)");
	Pattern	transfer_re = Pattern.compile("^(TRANSFER|Transfer)($|\\s*(?<acctDescr>.*)" + CHAN_SEP + "(?<description>.+)$)");
	Pattern	fee_end = Pattern.compile(CHAN_SEP + "(?<description>.*?)\\s*" + DOLLAR_AMOUNT + "?$");
	Pattern	fee_re = Pattern.compile("^(?<type>FEE)" + fee_end);
	Pattern	rev_fee_re = Pattern.compile("^(?<type>REV FEE)" + fee_end);
	Pattern	other_re = Pattern.compile(TYPE_STUB + "(?<description>.*)");

	// Sub filters for some transactions
	Pattern phone_end_re = Pattern.compile("^(?<description>.*) (?<phone>[-0-9\\.]{7,15})$");
	Pattern zip_end_re = Pattern.compile("^(?<description>.*) (?<zip>\\d{5})$");
	Pattern clean_phone_re = Pattern.compile("[^\\d]");
	


	public Parse() {
		channelDet=new ChannelDetails();
		vendor = new Vendor();
	}
	


	
	public Vendor parser_memo(String memo, Date currentDate) throws IOException {
	    //checks
		Matcher matcher = check_re.matcher(memo);
		if (matcher.find()) {
			this.channelDet.ch_type=Channel.CHECK;
			String checkno = matcher.group("checkno");
			channelDet.check_no= checkno;
			this.vendor.setDescription("CHECK "+ checkno);
		}
		//POS and ATM transactions
		parse_POS_ATM_transactions(Channel.POS,pos_re,memo,currentDate);
		parse_POS_ATM_transactions(Channel.ATM,atm_re,memo,currentDate);
		
		matcher = credit_card_re.matcher(memo);
		if (matcher.find()) {
			this.channelDet.ch_type=Channel.POS;
			this.vendor=parse_vendor(matcher.group("description"));
			return this.vendor;
		}
		
		
		//transfers
	    matcher = transfer_re.matcher(memo);
	    if (matcher.find()) {
			this.channelDet.ch_type=Channel.TRANSFER;
			if(matcher.group("acctDescr")!=null) {
				this.channelDet.account_description=matcher.group("acctDescr");
			}
		
	        vendor.description = memo;
	        return this.vendor;
	    }

	    //deposits
	    matcher = deposit_re.matcher(memo);
	    if (matcher.find()) {
			this.channelDet.ch_type=Channel.DEPOSIT;
			this.vendor.description = (matcher.group("description")==null ?  matcher.group("deposit"): matcher.group("description"));
	        return this.vendor;
	    }

	    //dividends
	    matcher = dividend_re.matcher(memo);
	    if (matcher.find()) {
			this.channelDet.ch_type=Channel.DIVIDEND;
			this.vendor.description = (matcher.group("description")==null ?  matcher.group("divident"): matcher.group("description"));
	        return this.vendor;
	    }

	    //fees and rev fees
	   // for regex in (rev_fee_re, fee_re):
        matcher = rev_fee_re.matcher(memo);
        if (matcher.find()) {
            this.channelDet.ch_type=Channel.FEE;
            if (matcher.group("amount")!=null) {
            		this.channelDet.amount= _bash_amount(matcher.group("amount"));   
            }
            vendor.description = matcher.group("description");
            return vendor;
        }
		
		//everything else
	    matcher = other_re.matcher(memo);
	    if (matcher.find() && matcher.group("type")!=null) {
	        String type = matcher.group("type").toLowerCase();
	        if (type.contains("fee")){
	        		this.channelDet.ch_type=Channel.FEE;
	        		this.vendor.description.replaceAll("\\$[-\\d\\.]+", matcher.group("description")).trim();
	            return this.vendor;

	        }else if (type.contains("withdraw") || type.contains("transfer")){
	        		this.channelDet.ch_type=Channel.ATM;
	            if (matcher.group("description")!=null) 
	                vendor.description = matcher.group("description");
	            return this.vendor;
	        }
	    }

	    //fallback
		this.vendor=parse_vendor(memo);
		if (this.vendor.description==null)
			vendor.description = memo;			       
		return this.vendor;
	     	
	}

	public float _bash_amount(String amount) {
     /*Sometimes the tx says "$-2.0-50". """*/
		amount = amount.replace("$", "");
	    if (amount.charAt(0) == '-'){
	        return -Float.parseFloat(amount.replace("-", ""));
	    }else {
	        return Float.parseFloat(amount);
		}
	}
	
	public void parse_POS_ATM_transactions(Channel channel_type, Pattern pattern, String memo, Date currentDate) throws IOException {
		
		Matcher matcher = pattern.matcher(memo);
	    if (matcher.find()) {
	    		this.channelDet.ch_type=channel_type;
            this.channelDet.setAuth(matcher.group("auth"));
            this.vendor = parse_vendor(matcher.group("description"));
            
	      
	    }
	
	}
	
	public Vendor parse_vendor(String description) throws IOException {
	    String memo = description.trim();

	    String memo_guess = memo.substring(0, memo.length()-2).trim();
	    String state_guess = memo.substring(memo.length()-2).trim();
	    ZipData zipdata = new ZipData();
	    ArrayList<String> cities = zipdata.cities_by_state.get(state_guess);
	   
	    
	    //We have a state match.
	    if (zipdata.cities_by_state.containsKey(state_guess)) {
	    		this.vendor.state=state_guess;
	    		//Does the listing end with a phone number?
	        Matcher matcher = phone_end_re.matcher(memo_guess);
	        if(matcher.find()) {
	            this.vendor.description = matcher.group("description");
	            this.vendor.phone = matcher.group("phone");
	            return this.vendor;
	        }
	    	
	    
	    
		    //Does the listing end with a zip code?
		     matcher = zip_end_re.matcher(memo_guess);
		    	 if(matcher.find()) {
		    		 this.vendor.description = matcher.group("description");
		    		 this.vendor.zip = matcher.group("zip");
		         this.vendor.city = zipdata.cities_by_zip.get(vendor.zip).get(0);
		         return this.vendor;
		    	 }
	
		      //Otherwise, try to match city.
		       String desc_city = parse_city(cities, memo_guess);
		       String[] descr_city = desc_city.split("_split_");
		       vendor.description=descr_city[0];
		       vendor.city=descr_city[1];
		       if (vendor.city!=null)
		            return this.vendor;
	    }

	    //Fall back
	    vendor.description= memo;
	    return this.vendor;
	}
	
	public String parse_city(ArrayList<String> cities, String memo) {
	    
	    /*Split off a (potentially abbreviated) city stub from the end of the
	    memo string.  Assume: 
	    1. The city will come at the end of the memo string.
	    2. The city may contain deletions from the "real" city name, but not
	       insertions -- hence, the represented city name will not be longer
	       than the real city name.
	    3. The city abbreviation will be preceded by a space or the beginning
	       of the string.
	   */
	    String [] words = memo.split(" ");
	    List<String> wordsList = Arrays.asList(words);
	    int best_score = 0;
	    String best_city = null;
	    String remainder = null;
	    for (String city: cities) {
	        ArrayList<String> pot_words=new ArrayList<String>();
	        int run_length = -1; // initial space
	        ListIterator li = wordsList.listIterator(wordsList.size());
	        while(li.hasPrevious()) {        	
	        		String word = (String) li.previous();
	            if(run_length + word.length() + 1 <= city.length()) {
	                pot_words.add(word);
	                run_length += word.length() + 1; //add one for space
	            }else {
	                break;
	            }
	        }
	        String pot_city = String.join(" ",pot_words).toUpperCase();
	        int pot_city_pos = pot_city.length() - 1;
	        int score = 0;
	        int cityLength = city.length();
	        for (int i=cityLength - 1; i>=0; i--) {
	            if (pot_city_pos < 0) {
	                score -= i;
	                break;
	            }if (city.charAt(i)==(pot_city.charAt(pot_city_pos))) {
	                score += 1;
	                pot_city_pos -= 1;
	            }else {
	                score -=1;
	            }
	        }
	        if (score > best_score) {
	            best_score = score;
	            best_city = city;
	            remainder = String.join(" ",words);
	            remainder = remainder.substring(0, remainder.length()-pot_city.length()-1);

	        }
	    }
	    if (best_city!=null && best_score > best_city.length() / 2) {
	        return remainder+"_split_"+best_city;
	    }else {
	        return memo+"_split_"+"";
	    }
	}
}

	
	
	class ZipData {
		
		public HashMap<String, ArrayList<String>> cities_by_state = new HashMap<String, ArrayList<String>> ();
        public HashMap<String, ArrayList<String>> cities_by_zip = new HashMap<String, ArrayList<String>> ();
        
        public ZipData() throws IOException{
        		readData();
        }
		
		public void readData() throws IOException {
			try (
		            Reader reader = Files.newBufferedReader(Paths.get("zips.csv"));
					CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
		        ) {
		            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
		            
	
		            for (CSVRecord csvRecord : csvRecords) {
		                // Accessing Values by Column Index
	
		                String zip = csvRecord.get(0);
		                String city = csvRecord.get(1);
		                String state = csvRecord.get(2);
		                
		                if (cities_by_state.containsKey(state)) {
		                		cities_by_state.get(state).add(city);
		                }else {
		                		ArrayList<String> cityList = new ArrayList<String>();
		                		cityList.add(city);
		                		cities_by_state.put(state, cityList);
		                }
		                	if (cities_by_zip.containsKey(zip)) {
		                		cities_by_zip.get(zip).add(city);
		                }else {
			                	ArrayList<String> cityList = new ArrayList<String>();
		                		cityList.add(city);
		                		cities_by_zip.put(zip, cityList);
		                }
		        		}
		        	}  
		}
	}
	

