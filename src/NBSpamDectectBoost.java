import java.io.*;
import java.util.*;
import java.lang.*;
public class NBSpamDectectBoost {
	// This a class with two counters (for ham and for spam)
	static class Multiple_Counter{
		double counterHam = 0;
		double counterSpam  = 0;
		double logHam = 0;
		double logSpam = 0;
	}

	public static void main(String[] args) throws IOException{
		String trainFile = args[0];
		String testFile = args[1];
		File dir_location = new File( trainFile);
		// Listing of the directory (should contain 2 subdirectories: ham/ and spam/)
		File[] dir_listing = new File[0];
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
		File[] listing_spam  = new File[0];

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
		
		double ph = listing_ham.length * 1.0 /(listing_ham.length + listing_spam.length);
		double ps = 1 - ph;
		int numOfEx = listing_ham.length + listing_spam.length;
	
		int k = 20;
		List<double[]> w = new ArrayList<double[]>(); //weights of 20 
		List<Hashtable<String, Multiple_Counter>> h = new ArrayList<Hashtable<String, Multiple_Counter>>();
		double[] weights = new double[numOfEx];
		double[] z = new double[k];
		//initialize weights
		for(int i = 0; i < numOfEx; i++){
			weights[i] = 1.0/numOfEx;
		}
		for(int i = 0; i < k; i++){
			w.add(i, weights.clone());
			double error = 0.0;
			Hashtable<String, Multiple_Counter> hypo = weakTrain(trainFile, weights);
			h.add(i, hypo);
			error = classify(0, trainFile, hypo, ph, ps, weights);
			if(error > 0){
				update(error, weights, trainFile, ph, ps, h.get(i));
				z[i] = Math.log((1-error)/error);
				//if the error reaches 0, the weight of the hypothesis is assumed to be 10
			}else{
				z[i] = 100;
			}

			double sum = 0.0;
			for(int j = 0; j < numOfEx; j++){
				sum += weights[j];
			}
			for(int j = 0; j < numOfEx; j++){
				weights[j] = weights[j]/sum;
			}
		}
		majorityVoteClassify(z,  h, ph, ps, testFile,  k);
		
	}

	private static Hashtable<String, Multiple_Counter> weakTrain(String trainFile, double[] w) throws IOException {
		File dir_location = new File(trainFile);
		// Listing of the directory (should contain 2 subdirectories: ham/ and spam/)
		File[] dir_listing = new File[0];
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
		File[] listing_spam  = new File[0];

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

		// Create a hash table for the vocabulary (word searching is very fast in a hash table)
		Hashtable<String, Multiple_Counter> vocab = new Hashtable<String, Multiple_Counter>();
		Multiple_Counter old_cnt = new Multiple_Counter();
		int numOfEx = listing_ham.length + listing_spam.length;
		// Read the e-mail messages
		// The ham mail

		for ( int i = 0; i < listing_ham.length; i++ ){
			FileInputStream i_s = new FileInputStream( listing_ham[i] );
			BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
			String line;
			String word;
			while ((line = in.readLine()) != null){					// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]", "");
					if (!word.equals("")) { // if string isn't empty
						if ( vocab.containsKey(word) ){ // check if word exists already in the vocabulary
							old_cnt = vocab.get(word);	// get the counter from the hashtable
							old_cnt.counterHam +=  numOfEx*w[i];			// and increment it
							vocab.put(word, old_cnt);
						}
						else{
							Multiple_Counter fresh_cnt = new Multiple_Counter();
							fresh_cnt.counterHam = 1 + numOfEx*w[i]; //add-1 smoohting to avoid zero prob
							//	System.out.println(w[i]);
							fresh_cnt.counterSpam = 1; //add-1 smoothing to avoid zero prob
							vocab.put(word, fresh_cnt);	// put the new word with its new counter into the hashtable
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
			while ((line = in.readLine()) != null){					// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]","");
					if (!word.equals("")) {	
						if (vocab.containsKey(word)){				// check if word exists already in the vocabulary
							old_cnt = vocab.get(word);	// get the counter from the hashtable
							old_cnt.counterSpam += numOfEx*w[listing_ham.length + i];			// and increment it
							vocab.put(word, old_cnt);
						}else{					
							Multiple_Counter fresh_cnt = new Multiple_Counter();
							fresh_cnt.counterHam = 1; //add-1 smoothing to avoid zero prob
							fresh_cnt.counterSpam = 1 + numOfEx*w[listing_ham.length + i]; //add-1 smoothing to avoid zero prob
							vocab.put(word, fresh_cnt);			// put the new word with its new counter into the hashtable
						}
					}
				}
			}
			in.close();
		}
		double sspam = 0;
		double sham = 0;
		// Print out the hash table
		for (Enumeration<String> e = vocab.keys(); e.hasMoreElements();){	
			String word;
			word = e.nextElement();
			old_cnt  = vocab.get(word);
			//			System.out.println(word + " | in ham: " + old_cnt.counterHam + 
			//					" in spam: "    + old_cnt.counterSpam);
			sspam += old_cnt.counterSpam;
			sham += old_cnt.counterHam;
		}

		for (Enumeration<String> e = vocab.keys(); e.hasMoreElements();){	
			String word;
			word = e.nextElement();
			old_cnt  = vocab.get(word);
			old_cnt.logHam = Math.log(old_cnt.counterHam/sham);
			old_cnt.logSpam = Math.log(old_cnt.counterSpam/sspam);

		}

		// Now all students must continue from here
		// Prior probabilities must be computed from the number of ham and spam messages
		// Conditional probabilities must be computed for every unique word
		// add-1 smoothing must be implemented
		// Probabilities must be stored as log probabilities (log likelihoods).
		// Bayes rule must be applied on new messages, followed by argmax classification (using log probabilities)
		// Errors must be computed on the test set and a confusion matrix must be generated

		return vocab;

	}

	private static void update(double error, double[] weight, String trainDir, double ph,  double ps, Hashtable<String, Multiple_Counter> v) throws IOException{
		double beta = error/(1-error);
		File dir_location = new File(trainDir);
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
			while((line = in.readLine()) != null){				// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]","");
					if (!word.equals("") ) { // if string isn't empty
						if (v.containsKey(word)){ // check if word exists already in the vocabulary
							logcpham += v.get(word).logHam;
							logcpspam += v.get(word).logSpam;
						}
					}
				}
			}
			if(logcpspam <= logcpham){
				weight[i] *= beta;
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
			while((line = in.readLine()) != null){				// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]","");
					if (!word.equals("") ) { // if string isn't empty
						if (v.containsKey(word)){ // check if word exists already in the vocabulary
							logcpham += v.get(word).logHam;
							logcpspam += v.get(word).logSpam;
						}
					}
				}
			}
			in.close();
			if(logcpspam > logcpham){
				weight[800+i] *= beta;
			}
		}
	}

	//private static void conditionalProbability(Hashtable<String, Multiple_Counter> v){
	private static double classify(int mode, String dir, Hashtable<String, Multiple_Counter> v, double ph, double ps, double[] weights) throws IOException{
		double error = 0.0;
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
			while((line = in.readLine()) != null){				// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]","");
					if (!word.equals("") ) { // if string isn't empty
						if (v.containsKey(word)){ // check if word exists already in the vocabulary
							logcpham += v.get(word).logHam;
							logcpspam += v.get(word).logSpam;
						}
					}
				}
			}
			if(logcpspam > logcpham){
				error+= weights[i];
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
			while((line = in.readLine()) != null){				// read a line
				StringTokenizer st = new StringTokenizer(line);			// parse it into words
				while (st.hasMoreTokens()){
					word = st.nextToken().replaceAll("[^a-zA-Z]","");
					if (!word.equals("") ) { // if string isn't empty
						if (v.containsKey(word)){ // check if word exists already in the vocabulary
							logcpham += v.get(word).logHam;
							logcpspam += v.get(word).logSpam;
						}
					}
				}
			}
			in.close();
			if(logcpspam <= logcpham){
				error+= weights[800+i];
			}
		}
		return error;
	}
	
	private static void majorityVoteClassify(double[] z, List<Hashtable<String, Multiple_Counter>> h, double ph, double ps, String testDir, int k) throws IOException{
		int ctSpam = 0;
		int cwSpam = 0;
		int ctHam = 0;
		int cwHam = 0;

		File dir_location = new File(testDir);
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

		for ( int i = 0; i < listing_ham.length; i++ ){
			double spamWeight = 0.0;
			double hamWeight = 0.0;
			for(int j = 0; j < k; j++){
				Hashtable<String, Multiple_Counter> v = h.get(j);
				double logcpham = Math.log(ph);
				double logcpspam = Math.log(ps);
				FileInputStream i_s = new FileInputStream( listing_ham[i] );
				BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
				String line;
				String word;
				while((line = in.readLine()) != null){				// read a line
					StringTokenizer st = new StringTokenizer(line);			// parse it into words
					while (st.hasMoreTokens()){
						word = st.nextToken().replaceAll("[^a-zA-Z]","");
						if (!word.equals("") ) { // if string isn't empty
							if (v.containsKey(word)){ // check if word exists already in the vocabulary
								logcpham += v.get(word).logHam;
								logcpspam += v.get(word).logSpam;
							}
						}
					}
				}
				if(logcpspam > logcpham){
					spamWeight += z[j];
				}else{
					hamWeight += z[j];
				}
				in.close();
			}
			if(spamWeight > hamWeight){
				cwHam ++;
			}else{
				ctHam ++;
			}

		}

		// The spam mail
		for ( int i = 0; i < listing_spam.length; i ++ ){
			double spamWeight = 0.0;
			double hamWeight = 0.0;
			for(int j = 0; j < k; j++){
				Hashtable<String, Multiple_Counter> v = h.get(j);
				double logcpham = Math.log(ph);
				double logcpspam = Math.log(ps);
				FileInputStream i_s = new FileInputStream( listing_spam[i] );
				BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
				String line;
				String word;
				while((line = in.readLine()) != null){				// read a line
					StringTokenizer st = new StringTokenizer(line);			// parse it into words
					while (st.hasMoreTokens()){
						word = st.nextToken().replaceAll("[^a-zA-Z]","");
						if (!word.equals("") ) { // if string isn't empty
							if (v.containsKey(word)){ // check if word exists already in the vocabulary
								logcpham += v.get(word).logHam;
								logcpspam += v.get(word).logSpam;
							}
						}
					}
				}
				in.close();
				if(logcpspam > logcpham){
					spamWeight += z[j];
				}else{
					hamWeight += z[j];
				}
			}
			if(spamWeight > hamWeight){
				ctSpam++;
			}else{
				cwSpam++;
			}
		}

		//
		System.out.printf("%30s%15s\n", "True Spam", "True Ham");
			System.out.printf("%s%10d%15d\n", "Classified Spam", ctSpam, cwHam);
				System.out.printf("%s%10d%17d\n", "Classified Ham", cwSpam , ctHam);
			System.out.println();
	}
}
