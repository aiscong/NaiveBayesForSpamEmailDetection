import java.io.*;
import java.util.*;
import java.lang.*;
public class NBSpamDetect {
	// This a class with two counters (for ham and for spam)
	static class Multiple_Counter{
		int counterHam = 0;
		int counterSpam  = 0;
		double logHam = 0;
		double logSpam = 0;
	}

	public static void main(String[] args) throws IOException{
		// Location of the directory (the path) taken from the cmd line (first arg)
		File dir_location = new File( args[0] );
		// Listing of the directory (should contain 2 subdirectories: ham/ and spam/)
		File[] dir_listing = new File[0];
		String testFileLoc = args[1];
		// Check if the cmd line arg is a directory and list it
		if ( dir_location.isDirectory()){
			dir_listing = dir_location.listFiles();
		}
		else{
			System.out.println( "- Error: cmd line arg not a directory.\n" );
			Runtime.getRuntime().exit(0);
		}

		// Listings of the two sub-directories (ham/ and spam/)
		File[] listing_ham = new File[0];
		File[] listing_spam    = new File[0];

		// Check that there are 2 sub-directories
		boolean hamFound = false; boolean spamFound = false;
		for (int i=0; i<dir_listing.length; i++) {
			if (dir_listing[i].getName().equals("ham")) { 
				listing_ham = dir_listing[i].listFiles(); hamFound = true;
			}
			else if (dir_listing[i].getName().equals("spam")) { 
				listing_spam = dir_listing[i].listFiles(); spamFound = true;
			}
		}
		if (!hamFound || !spamFound) {
			System.out.println( "- Error: specified directory does not contain ham and spam subdirectories.\n" );
			Runtime.getRuntime().exit(0);
		}

		// Print out the number of messages in ham and in spam
		//System.out.println( "\t number of ham messages is: " + listing_ham.length );
		//System.out.println( "\t number of spam messages is: " + listing_spam.length );

		// Create a hash table for the vocabulary (word searching is very fast in a hash table)
		Hashtable<String, Multiple_Counter> vocab = new Hashtable<String, Multiple_Counter>();
		Hashtable<String, Multiple_Counter> vocabLow = new Hashtable<String, Multiple_Counter>();
		Hashtable<String, Multiple_Counter> vocabExtract = new Hashtable<String, Multiple_Counter>();
		Multiple_Counter old_cnt = new Multiple_Counter();

		// Read the e-mail messages
		// The ham mail

		for ( int i = 0; i < listing_ham.length; i++ ){
			FileInputStream i_s = new FileInputStream( listing_ham[i] );
			BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
			String line;
			String word;
			String extractWord;
			boolean extractLine;
			extractLine = false;
			while ((line = in.readLine()) != null){					// read a line
				//currentLineChecking = false;
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				StringTokenizer extract = new StringTokenizer(line);
				String keyWord = "";
				if(extract.hasMoreTokens()){
					keyWord = extract.nextToken();
					if(keyWord.equals("From:") || keyWord.equals("To:") ||
							keyWord.equals("Subject:") || keyWord.equals("Cc:")){
						extractLine = true;
					}
				}
				if(!line.isEmpty()){
					if(!(line.charAt(0) == '\t' || Character.isWhitespace(line.charAt(0)))
							&& !(keyWord.equals("From:") || keyWord.equals("To:") ||
									keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
						extractLine = false;
					}
				}
				if(extractLine){
					if(!(keyWord.equals("From:") || keyWord.equals("To:") ||
							keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
						extract = new StringTokenizer(line);
					}		
					while(extract.hasMoreTokens()){
						extractWord = extract.nextToken().replaceAll("[^a-zA-Z]", "");
						if (!extractWord.equals("")) { // if string isn't empty
							//							if(extractWord.equals("linux")){
							//								System.out.println(listing_spam[i].getName());
							//							}
							//	if(extractWord.equals("sex")){
							//	System.out.println(listing_ham[i].getName());
							//	}
							//							if(listing_ham[i].getName().equals("00296.12af7606f42c491ca320c1c7a284d327")){
							//								System.out.println("extract:" + extractWord);
							//							}
							//System.out.println(extractWord);
							if (vocabExtract.containsKey(extractWord)){ // check if word exists already in the vocabulary
								old_cnt = vocabExtract.get(extractWord);	// get the counter from the hashtable
								old_cnt.counterHam++;			// and increment it
								vocabExtract.put(extractWord, old_cnt);
							}
							else{
								Multiple_Counter fresh_cnt = new Multiple_Counter();
								fresh_cnt.counterHam = 2; //add-1 smoohting to avoid zero prob
								fresh_cnt.counterSpam = 1; //add-1 smoothing to avoid zero prob
								vocabExtract.put(extractWord, fresh_cnt);// put the new word with its new counter into the hashtable
							}
						}
					}
				}
				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]", "");
					if (!word.equals("")) { // if string isn't empty
						if ( vocab.containsKey(word) ){ // check if word exists already in the vocabulary
							old_cnt = vocab.get(word);	// get the counter from the hashtable
							old_cnt.counterHam ++;			// and increment it
							vocab.put(word, old_cnt);
						}
						else{
							Multiple_Counter fresh_cnt = new Multiple_Counter();
							fresh_cnt.counterHam = 2; //add-1 smoohting to avoid zero prob
							fresh_cnt.counterSpam = 1; //add-1 smoothing to avoid zero prob
							vocab.put(word, fresh_cnt);	// put the new word with its new counter into the hashtable
						}
						String wordLow = word.toLowerCase();
						if (vocabLow.containsKey(wordLow)){
							old_cnt = vocabLow.get(wordLow);
							old_cnt.counterHam++;
							vocabLow.put(wordLow, old_cnt);
						}else{

							Multiple_Counter fresh_cnt = new Multiple_Counter();
							fresh_cnt.counterHam = 2;
							fresh_cnt.counterSpam = 1;
							vocabLow.put(wordLow, fresh_cnt);
						}
					}
				}
			}

			in.close();
		}
		// The spam mail
		for ( int i = 0; i < listing_spam.length; i ++ ){
			FileInputStream i_s = new FileInputStream( listing_spam[i] );
			BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
			String line;
			String word;
			String extractWord;
			boolean extractLine = false;
			while ((line = in.readLine()) != null){					// read a line
				//currentLineChecking = false;
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				StringTokenizer extract = new StringTokenizer(line);
				String keyWord = "";
				if(extract.hasMoreTokens()){
					keyWord = extract.nextToken();
					if(keyWord.equals("From:") || keyWord.equals("To:") ||
							keyWord.equals("Subject:") || keyWord.equals("Cc:")){
						extractLine = true;
					}
				}
				if(!line.isEmpty()){
					if(!(line.charAt(0) == '\t' || Character.isWhitespace(line.charAt(0)))
							&& !(keyWord.equals("From:") || keyWord.equals("To:") ||
									keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
						extractLine = false;
					}
				}
				if(extractLine){
					if(!(keyWord.equals("From:") || keyWord.equals("To:") ||
							keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
						extract = new StringTokenizer(line);
					}					
					while(extract.hasMoreTokens()){
						extractWord = extract.nextToken().replaceAll("[^a-zA-Z]", "");
						if (!extractWord.equals("")){ // if string isn't empty
							if (vocabExtract.containsKey(extractWord)){ // check if word exists already in the vocabulary
								old_cnt = vocabExtract.get(extractWord);	// get the counter from the hashtable
								old_cnt.counterSpam++;			// and increment it
								vocabExtract.put(extractWord, old_cnt);
							}
							else{
								Multiple_Counter fresh_cnt = new Multiple_Counter();
								fresh_cnt.counterHam = 1; //add-1 smoohting to avoid zero prob
								fresh_cnt.counterSpam = 2; //add-1 smoothing to avoid zero prob
								vocabExtract.put(extractWord, fresh_cnt);// put the new word with its new counter into the hashtable
							}
						}
					}
				}


				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]","");
					if (!word.equals("")) {	

						if (vocab.containsKey(word)){				// check if word exists already in the vocabulary
							old_cnt = vocab.get(word);	// get the counter from the hashtable
							old_cnt.counterSpam++;			// and increment it
							vocab.put(word, old_cnt);
						}else{					
							Multiple_Counter fresh_cnt = new Multiple_Counter();
							fresh_cnt.counterHam = 1; //add-1 smoothing to avoid zero prob
							fresh_cnt.counterSpam = 2; //add-1 smoothing to avoid zero prob
							vocab.put(word, fresh_cnt);			// put the new word with its new counter into the hashtable
						}
						String wordLow = word.toLowerCase();
						if (vocabLow.containsKey(wordLow)){
							old_cnt = vocabLow.get(wordLow);
							old_cnt.counterSpam++;
							vocabLow.put(wordLow, old_cnt);
						}else{
							Multiple_Counter fresh_cnt = new Multiple_Counter();
							fresh_cnt.counterHam = 1;
							fresh_cnt.counterSpam = 2;
							vocabLow.put(wordLow, fresh_cnt);
						}
					}
				}
			}
			in.close();
		}
		double sspam = 0;
		double sham = 0;
		double sspamLow = 0;
		double shamLow = 0;
		double sspamEx = 0;
		double shamEx = 0;
		// Print out the hash table
		//System.out.println("here");
		for (Enumeration<String> e = vocab.keys(); e.hasMoreElements();){	
			String word;
			word = e.nextElement();
			old_cnt  = vocab.get(word);
			sspam += old_cnt.counterSpam;
			sham += old_cnt.counterHam;
		}

		for(Enumeration<String> e = vocabLow.keys(); e.hasMoreElements();){
			String word;
			word = e.nextElement();
			old_cnt = vocabLow.get(word);
			sspamLow += old_cnt.counterSpam;
			shamLow += old_cnt.counterHam;
		}

		for (Enumeration<String> e = vocabExtract.keys(); e.hasMoreElements();){
			String word;
			word = e.nextElement();
			old_cnt = vocabExtract.get(word);
			sspamEx += old_cnt.counterSpam;
			shamEx += old_cnt.counterHam;
		}

		for (Enumeration<String> e = vocab.keys(); e.hasMoreElements();){	
			String word;
			word = e.nextElement();
			old_cnt  = vocab.get(word);
			old_cnt.logHam = Math.log(old_cnt.counterHam/sham);
			old_cnt.logSpam = Math.log(old_cnt.counterSpam/sspam);
			//	System.out.println(word + " | in ham: " + old_cnt.logHam + 
			//		" in spam: "    + old_cnt.logSpam);
		}

		int numKeys = 0;

		for (Enumeration<String> e = vocabLow.keys(); e.hasMoreElements();){
			numKeys++;
			String word;
			word = e.nextElement();
			old_cnt = vocabLow.get(word);
			old_cnt.logHam = Math.log(old_cnt.counterHam/shamLow);
			old_cnt.logSpam = Math.log(old_cnt.counterSpam/sspamLow);
			//			if(numKeys < 20000){
			//			System.out.println(numKeys + "." + word + " | in ham: " + old_cnt.counterHam + 
			//					" in spam: "    + old_cnt.counterSpam);
			//			}
		}

		for (Enumeration<String> e = vocabExtract.keys(); e.hasMoreElements();){	
			String word;
			word = e.nextElement();
			old_cnt = vocabExtract.get(word);
			old_cnt.logHam = Math.log(old_cnt.counterHam/shamEx);
			old_cnt.logSpam = Math.log(old_cnt.counterSpam/sspamEx);
		}

		double ph = listing_ham.length * 1.0 /(listing_ham.length + listing_spam.length);
		double ps = 1 - ph;
		//System.out.println("numebr of words in spam is " + sspam + "\nnumber of words in ham is " + sham);
		//System.out.println("ph is " + ph + "\nps is " +ps);
		classify(0, testFileLoc, vocab, ph, ps);
		classify(1, testFileLoc, vocabLow, ph, ps);
		classify(2, testFileLoc, vocabExtract, ph, ps);
	}

	//private static void conditionalProbability(Hashtable<String, Multiple_Counter> v){
	private static void classify(int mode, String dir, Hashtable<String, Multiple_Counter> v, double ph, double ps) throws IOException{
		int ctSpam = 0;
		int cwSpam = 0;
		int ctHam = 0;
		int cwHam = 0;
		File dir_location = new File(dir);
		// Listing of the directory (should contain 2 subdirectories: ham/ and spam/)
		File[] dir_listing = new File[0];
		// Check if the cmd line arg is a directory and list it
		if ( dir_location.isDirectory()){
			dir_listing = dir_location.listFiles();
		}
		else{
			System.out.println( "- Error: given test directory is not a directory.\n" );
			Runtime.getRuntime().exit(0);
		}

		// Listings of the two sub-directories (ham/ and spam/)
		File[] listing_ham = new File[0];
		File[] listing_spam = new File[0];

		// Check that there are 2 sub-directories
		boolean hamFound = false; boolean spamFound = false;
		for (int i=0; i<dir_listing.length; i++) {
			if (dir_listing[i].getName().equals("ham")) { 
				listing_ham = dir_listing[i].listFiles(); hamFound = true;
			}
			else if (dir_listing[i].getName().equals("spam")) { 
				listing_spam = dir_listing[i].listFiles(); spamFound = true;
			}
		}
		if (!hamFound || !spamFound) {
			System.out.println( "- Error: specified directory does not contain ham and spam subdirectories.\n" );
			Runtime.getRuntime().exit(0);
		}

		// Print out the number of messages in ham and in spam

		// Read the e-mail messages
		// The ham mail
		for ( int i = 0; i < listing_ham.length; i ++ ){
			double logcpham = Math.log(ph);
			double logcpspam = Math.log(ps);
			FileInputStream i_s = new FileInputStream( listing_ham[i] );
			BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
			String line;
			String word;
			boolean extractLine = false;
			while((line = in.readLine()) != null){				// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				if(mode == 2){
					StringTokenizer extract = new StringTokenizer(line);
					String keyWord = "";
					if(extract.hasMoreTokens()){
						keyWord = extract.nextToken();
						if(keyWord.equals("From:") || keyWord.equals("To:") ||
								keyWord.equals("Subject:") || keyWord.equals("Cc:")){
							extractLine = true;
						}
					}
					if(!line.isEmpty()){
						if(!(line.charAt(0) == '\t' || Character.isWhitespace(line.charAt(0)))
								&& !(keyWord.equals("From:") || keyWord.equals("To:") ||
										keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
							extractLine = false;
						}
					}
					if(extractLine){
						if(!(keyWord.equals("From:") || keyWord.equals("To:") ||
								keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
							extract = new StringTokenizer(line);
						}
						while(extract.hasMoreTokens()){
							word = extract.nextToken().replaceAll("[^a-zA-Z]", "");
							if (!word.equals("")) { // if string isn't empty

								if (v.containsKey(word)){ // check if word exists already in the vocabulary

									logcpham += v.get(word).logHam;
									logcpspam += v.get(word).logSpam;


								}
							}
						}
					}
				}// end of mode 2
				else{ //mode 0 or mode 1
					while (st.hasMoreTokens()){
						word = st.nextToken().replaceAll("[^a-zA-Z]","");
						if (!word.equals("") ) { // if string isn't empty
							if(mode == 1){
								word = word.toLowerCase();
							}
							if (v.containsKey(word)){ // check if word exists already in the vocabulary
								logcpham += v.get(word).logHam;
								logcpspam += v.get(word).logSpam;
							}
						}
					}
				}
			}
			if(logcpspam > logcpham){
				cwHam++;
			}else{
				ctHam++;
			}
			in.close();
		}

		// The spam mail
		for ( int i = 0; i < listing_spam.length; i ++ ){
			double logcpham = Math.log(ph);
			double logcpspam = Math.log(ps);
			FileInputStream i_s = new FileInputStream( listing_spam[i] );
			BufferedReader in = new BufferedReader(new InputStreamReader(i_s));

			String line;
			String word;
			boolean extractLine = false;
			while((line = in.readLine()) != null){				// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				if(mode == 2){
					StringTokenizer extract = new StringTokenizer(line);
					String keyWord = "";
					if(extract.hasMoreTokens()){
						keyWord = extract.nextToken();
						if(keyWord.equals("From:") || keyWord.equals("To:") ||
								keyWord.equals("Subject:") || keyWord.equals("Cc:")){
							extractLine = true;
						}
					}
					if(!line.isEmpty()){
						if(!(line.charAt(0) == '\t' || Character.isWhitespace(line.charAt(0)))
								&& !(keyWord.equals("From:") || keyWord.equals("To:") ||
										keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
							extractLine = false;
						}
					}
					if(extractLine){
						if(!(keyWord.equals("From:") || keyWord.equals("To:") ||
								keyWord.equals("Subject:") || keyWord.equals("Cc:"))){
							extract = new StringTokenizer(line);
						}
						while(extract.hasMoreTokens()){
							word = extract.nextToken().replaceAll("[^a-zA-Z]", "");
							if (!word.equals("")) { // if string isn't empty
								if (v.containsKey(word)){ // check if word exists already in the vocabulary
									logcpham += v.get(word).logHam;
									logcpspam += v.get(word).logSpam;
								}
							}
						}
					}
				}// end of mode 3
				else{ //mode 1 or mode 2
					while (st.hasMoreTokens()){
						word = st.nextToken().replaceAll("[^a-zA-Z]","");
						if (!word.equals("") ) { // if string isn't empty
							if(mode == 1){
								word = word.toLowerCase();
							}
							if (v.containsKey(word)){ // check if word exists already in the vocabulary
								logcpham += v.get(word).logHam;
								logcpspam += v.get(word).logSpam;
							}
						}
					}
				}
			}
			in.close();
			if(logcpspam > logcpham){
				ctSpam++;
			}else{
				cwSpam++;
			}

		}
		if(mode == 0){
			System.out.println("Classifying at regular mode");
		}
		if(mode == 1){
			System.out.println("Classifying using lower-case");
		}
		if(mode == 2){
			System.out.println("Classifying using extraction");
		}
		System.out.printf("%30s%15s\n", "True Spam", "True Ham");
		System.out.printf("%s%10d%15d\n", "Classified Spam", ctSpam, cwHam);
		System.out.printf("%s%10d%17d\n", "Classified Ham", cwSpam , ctHam);
		System.out.println();
	}
}