package ix.core.chem;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.util.CachedSupplier;
import ix.utils.FortranLikeParserHelper;
import ix.utils.FortranLikeParserHelper.LineParser;
import ix.utils.FortranLikeParserHelper.LineParser.ParsedOperation;

public class ChemCleaner {
	
	
	private static LineParser MOLFILE_COUNT_LINE_PARSER=new LineParser("aaabbblllfffcccsssxxxrrrpppiiimmmvvvvvv");

    private static Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

	private static char[] alpha="aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxWyYzZ0123456789".toCharArray();
	private static String[] nalpha=IntStream.range(0, alpha.length)
			.mapToObj(i->""+alpha[i])
			.map(a->""+a+a+a+a)
			.toArray(i->new String[i]);
	private static LineParser CHG_LINE_PARSER= CachedSupplier.of(()->{
		StringBuilder sb = new StringBuilder();
		sb.append("......???");

		for(int i=0;i<nalpha.length;i+=2){
			sb.append(nalpha[i]);
			sb.append(nalpha[i+1]);
		}
		return new LineParser(sb.toString());
	}).get();

	//										  new LineParser("......nnnaaaabbbbccccdddd");
	private static String chr(int t){
        return Character.toString((char)t);
    }
	private static String symbolsToASCII(String input){
        
        input = input.replaceAll("[\u00B4\u02B9\u02BC\u02C8\u0301\u2018\u2019\u201B\u2032\u2034\u2037]", chr(39)); /* apostrophe (') */
        input = input.replaceAll("[\u00AB\u00BB\u02BA\u030B\u030E\u201C\u201D\u201E\u201F\u2033\u2036\u3003\u301D\u301E]", chr(34)); /* quotation mark (") */
        input = input.replaceAll("[\u00AD\u2010\u2011\u2012\u2013\u2014\u2212\u2015]", chr(45)); /* hyphen (-) */
        input = input.replaceAll("[\u01C3\u2762]", chr(33)); /* exclamation mark (!) */
        input = input.replaceAll("[\u266F]", chr(35)); /* music sharp sign (#) */
        input = input.replaceAll("[\u066A\u2052]", chr(37)); /* percent sign (%) */
        input = input.replaceAll("[\u066D\u204E\u2217\u2731\u00D7]", chr(42)); /* asterisk (*) */
        input = input.replaceAll("[\u201A\uFE51\uFF64\u3001]", chr(44)); /* comma (,) */
        input = input.replaceAll("[\u00F7\u0338\u2044\u2215]", chr(47)); /* slash (/) */
        input = input.replaceAll("[\u0589\u05C3\u2236]", chr(58)); /* colon (:) */
        input = input.replaceAll("[\u203D]", chr(63)); /* question mark (?) */
        input = input.replaceAll("[\u27E6]", chr(91)); /* opening square bracket ([) */
        input = input.replaceAll("[\u20E5\u2216]", chr(92)); /* backslash (\) */
        input = input.replaceAll("[\u301B]", chr(93));  /* closing square bracket ([) */
        input = input.replaceAll("[\u02C4\u02C6\u0302\u2038\u2303]", chr(94)); /* caret (^) */
        input = input.replaceAll("[\u02CD\u0331\u0332\u2017]", chr(95)); /* underscore (_) */
        input = input.replaceAll("[\u02CB\u0300\u2035]", chr(96)); /* grave accent (`) */
        input = input.replaceAll("[\u2983]", chr(123)); /* opening curly bracket ({) */
        input = input.replaceAll("[\u01C0\u05C0\u2223\u2758]", chr(124)); /* vertical bar / pipe (|) */
        input = input.replaceAll("[\u2016]", "#chr(124)##chr(124)#"); /* double vertical bar / double pipe (||) */
        input = input.replaceAll("[\u02DC\u0303\u2053\u223C\u301C]", chr(126)); /* tilde (~) */
        input = input.replaceAll("[\u2039\u2329\u27E8\u3008]", chr(60)); /* less-than sign (<) */
        input = input.replaceAll("[\u2264\u2266]", "#chr(60)##chr(61)#"); /* less-than equal-to sign (<=) */
        input = input.replaceAll("[\u203A\u232A\u27E9\u3009]", chr(62)); /* greater-than sign (>) */
        input = input.replaceAll("[\u2265\u2267]", "#chr(62)##chr(61)#"); /* greater-than equal-to sign (>=) */
        input = input.replaceAll("[\u200B\u2060\uFEFF]", chr(32)); /* space ( ) */
        input = input.replaceAll("\u2153", "1/3");
        input = input.replaceAll("\u2154", "2/3");
        input = input.replaceAll("\u2155", "1/5");
        input = input.replaceAll("\u2156", "2/5");
        input = input.replaceAll("\u2157", "3/5");
        input = input.replaceAll("\u2158", "4/5");
        input = input.replaceAll("\u2159", "1/6");
        input = input.replaceAll("\u215A", "5/6");
        input = input.replaceAll("\u215B", "1/8");
        input = input.replaceAll("\u215C", "3/8");
        input = input.replaceAll("\u215D", "5/8");
        input = input.replaceAll("\u215E", "7/8");
        input = input.replaceAll("\u2026", "...");
        return input;
    }
	private static String standardizeString(String entered) {
       
	    //TODO: use full name standardization rules
	    // including special replacements
	    String preform = symbolsToASCII(entered);
	    String normalized= Normalizer
	            .normalize(preform, Normalizer.Form.NFKD);
	    normalized=normalized.replaceAll("\\p{Mn}+", "");
	    normalized = normalized
	            .replaceAll("[^\\p{ASCII}]", "?");

	    return normalized;
	}

	public static String cleanMolfileWithNonASCIIName(String molfile) {
	    String[] lines=NEW_LINE_PATTERN.split(molfile);
	    if(!StandardCharsets.US_ASCII.newEncoder().canEncode(lines[0])) {
	        lines[0]=standardizeString(lines[0]);
	    }	    
	    return Arrays.stream(lines).collect(Collectors.joining("\n"));
	}
	
	private static boolean isLikelyCountLine(String l) {
	    if(l.contains("V2000") || l.contains("V3000")) {
	        return true;
	    }
	    return false;
	}
	
	public static String cleanMolfileWithTypicalWhiteSpaceIssues(String molfile){
		String[] lines=NEW_LINE_PATTERN.split(molfile);
		
		if(!isLikelyCountLine(lines[3])) {
		    int addLines =0;
		    if(isLikelyCountLine(lines[2])) {
		        addLines=1;
		    }else if(isLikelyCountLine(lines[1])) {
		        addLines=2;
		    }else if(isLikelyCountLine(lines[0])) {
                addLines=3;
            }
		    if(addLines>0) {
		        StringBuilder sb = new StringBuilder();
		        
		        for(int i=0;i<addLines;i++) {
		            sb.append("\n");
		        }
		        molfile=sb.toString()+molfile;
		        lines=NEW_LINE_PATTERN.split(molfile);
		    }
		}
		
		
		lines[3]=MOLFILE_COUNT_LINE_PARSER.parseAndOperate(lines[3])
				.remove("iii")
				.set("mmm", "999")
				.set("vvvvvv", "V2000")
				.toLine();

		return Arrays.stream(lines)
				.map(l->{
					if(l.startsWith("M  END")){
						return "M  END"; //ignore anything after the M  END line start, as it sometimes is added by accident in a few tools
					}else if(l.startsWith("M  CHG")){
						List<String> nlist = new ArrayList<>();


						ParsedOperation po=CHG_LINE_PARSER.parseAndOperate(l);
						int ccount=po.getAsInt("???");
						if(ccount>8){
							po.set("???", "8");
						}
												
						for(int i=16;i<Math.min(ccount*2,nalpha.length);i+=2){
							po.remove(nalpha[i]);
							po.remove(nalpha[i+1]);
						}
						nlist.add(0,po.toLine().trim());
						if(ccount>8){
							String b = ("A" +l.substring(73)).trim().substring(1);
							for(int j=0;j<b.length();j+=64){
								int endIndex=Math.min(b.length(),j+64);
								int scount=(int)Math.ceil((endIndex-j)/8.0);
								String nsection = b.substring(j, endIndex);
								ParsedOperation no=CHG_LINE_PARSER.parseAndOperate("M  CHG").set("???", ""+scount);
								nlist.add(CHG_LINE_PARSER.parseAndOperate(no.toLine().trim()+nsection).toLine().trim());
							}
						}
						return nlist.stream().collect(Collectors.joining("\n"));

					}
					return l;
				})
				.collect(Collectors.joining("\n"));

	}
	
	/**
	 * Returns a cleaner form of a molfile, with some common jsdraw
	 * polymer parts re-interpreted
	 * @param mfile
	 * @return
	 */
	public static String getCleanMolfile(String mfile) {

		if(mfile == null || !mfile.contains("M  END"))return mfile;
		
		//TODO fix the potentially missing line
		
		
		
		// JSdraw adds this to some S-GROUPS
		// that aren't always good
		if(!mfile.contains("MUL")){
			mfile = mfile.replaceAll("M  SPA[^\n]*\n", "");
		}
		
		mfile = mfile.replaceAll("M  END\n", "");
		
		Matcher m = Pattern.compile("M  STY  2 (...) GEN (...) DAT").matcher(mfile);
		List<String> addList = new ArrayList<String>();
		Map<String,String> toreplace = new HashMap<String,String>();
		while (m.find()) {
			String group1= m.group(1);
			String group2=  m.group(2);
			toreplace.put(m.group(), "M  STY  2 " + group1 + " SRU "+group2+" DAT");
			Matcher m2 = Pattern.compile("M  SED " + group2 + " ([^\n]*)").matcher(
					mfile);
			m2.find();
			String lab=m2.group(1);
			String add="M  SMT " + group1 + " " + lab + "\n";
			addList.add(add);
			//M  STY  1   2 SRU
		}
		 // JSDraw sometimes repeats SGROUP indexes by accident
		 // take inventory of all SRU sgroups, to help
		 m = Pattern.compile("M  STY  1 (...) SRU").matcher(mfile);
		
		 List<String> nolabelsrus = new ArrayList<String>();
		 while (m.find()) {
				String group1= m.group(1);
				nolabelsrus.add(group1.trim());
		 }
		 
		 if(!nolabelsrus.isEmpty()){
			 // "M  SMT   1"
			 m = Pattern.compile("M  SMT (...) ([^ ]*)").matcher(mfile);
			 
			 while (m.find()) {
				 	String group1= m.group(1).trim();
					String group2= m.group(2);
				 	//if this is real
				 	if(nolabelsrus.contains(group1)){
				 		nolabelsrus.remove(group1);
				 	}else{
				 		if(!nolabelsrus.isEmpty()){
				 			String newlabel=("  " + nolabelsrus.get(0));
				 			newlabel = newlabel.substring(newlabel.length()-3, newlabel.length());
				 			toreplace.put(m.group(), "M  SMT " + newlabel + " " + group2);
				 			nolabelsrus.remove(0);
				 		}
				 	}
			 }
		 }
		 
		 
		 
		 
		for(String key:toreplace.keySet()){
			mfile=mfile.replace(key, toreplace.get(key));
		}
		for(String add:addList){
			mfile+=add;
		}
		mfile+="M  END";
		mfile = cleanMolfileWithTypicalWhiteSpaceIssues(mfile);
		mfile = cleanMolfileWithNonASCIIName(mfile);
		return mfile;
	}
	
	private static String removeLegacyAtomListLines(String mfile) {
		StringBuilder builder = new StringBuilder(mfile.length());
		try(BufferedReader reader = new BufferedReader(new StringReader(mfile))){
			//assume valid mol file
			builder.append(reader.readLine()).append('\n');
			builder.append(reader.readLine()).append('\n');
			builder.append(reader.readLine()).append('\n');
			String countsLine = reader.readLine();
			//aaabbblllfffcccsssxxxrrrpppiiimmmvvvvvv
			Map<String, FortranLikeParserHelper.ParsedSection> map =MOLFILE_COUNT_LINE_PARSER.parse(countsLine);
			int numAtoms = Integer.parseInt(map.get("aaa").getValueTrimmed());
			int numBonds = Integer.parseInt(map.get("bbb").getValueTrimmed());
			int numAtomLists = Integer.parseInt(map.get("lll").getValueTrimmed());
			//rewrite with atomList set to 0
//			builder.append(countsLine.substring(0, 6)+"  0" + countsLine.substring(9)).append('\n');
			builder.append(countsLine).append('\n');
			int linesInAtomAndBondBlocks = numAtoms+numBonds;
			for(int i=0; i<linesInAtomAndBondBlocks; i++){
				builder.append(reader.readLine()).append('\n');
			}
			String line;
			while( (line =reader.readLine()) !=null){
				if(line.startsWith("M  ")){
					builder.append(line).append('\n');
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//remove last new line
		builder.setLength(builder.length()-1);
		return builder.toString();
	}

	/**
	 * Takes an input mol formatted String and removes all
	 * S Groups and legacy AtomList lines
	 * @param mol
	 * @return
	 */
	public static String removeSGroupsAndLegacyAtomLists(String mol){
		String withoutSgroups =  StreamUtil.lines(mol)
  				.filter(l->!l.matches("^M  S.*$"))
  				.collect(Collectors.joining("\n"));

		return removeLegacyAtomListLines(withoutSgroups);
	}

}
