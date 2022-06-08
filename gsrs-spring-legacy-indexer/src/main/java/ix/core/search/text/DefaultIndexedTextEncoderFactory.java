package ix.core.search.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultIndexedTextEncoderFactory {

   private static final String LEVO_REGEX = "(-)";
   private static final String DEXTRO_REGEX = "(+)";
   private static final String RACEMIC_REGEX = "(+/-)";
   private static final String RACEMIC_COMBO_REGEX = "±"; // Associated with RACEMIC_WORD

   public static final String LEVO_WORD = "LEVOROTATION";
   public static final String RACEMIC_WORD = "RACEMICROTATION";
   public static final String DEXTRO_WORD = "DEXTROROTATION";
   private static final String COLON_WORD = "XCOLONX";

   private static final String REPLACEMENT_SOURCE_GREEK = "\u03B1;.ALPHA.;\u03B2;.BETA.;\u03B3;.GAMMA.;\u03B4;.DELTA.;\u03B5;.EPSILON.;\u03B6;.ZETA.;\u03B7;.ETA.;\u03B8;.THETA.;\u03B9;.IOTA.;\u03BA;.KAPPA.;\u03BB;.LAMBDA.;\u03BC;.MU.;\u03BD;.NU.;\u03BE;.XI.;\u03BF;.OMICRON.;\u03C0;.PI.;\u03C1;.RHO.;\u03C2;.SIGMA.;\u03C3;.SIGMA.;\u03C4;.TAU.;\u03C5;.UPSILON.;\u03C6;.PHI.;\u03C7;.CHI.;\u03C8;.PSI.;\u03C9;.OMEGA.;\u0391;.ALPHA.;\u0392;.BETA.;\u0393;.GAMMA.;\u0394;.DELTA.;\u0395;.EPSILON.;\u0396;.ZETA.;\u0397;.ETA.;\u0398;.THETA.;\u0399;.IOTA.;\u039A;.KAPPA.;\u039B;.LAMBDA.;\u039C;.MU.;\u039D;.NU.;\u039E;.XI.;\u039F;.OMICRON.;\u03A0;.PI.;\u03A1;.RHO.;\u03A3;.SIGMA.;\u03A4;.TAU.;\u03A5;.UPSILON.;\u03A6;.PHI.;\u03A7;.CHI.;\u03A8;.PSI.;\u03A9;.OMEGA.";
   // The uppercase and lowercase forms of the 24 letters are: Α α, Β β, Γ γ, Δ δ, Ε ε, Ζ ζ, Η η, Θ θ, Ι ι, Κ κ, Λ λ, Μ μ, Ν ν, Ξ ξ, Ο ο, Π π, Ρ ρ, Σ σ/ς, Τ τ, Υ υ, Φ φ, Χ χ, Ψ ψ, and Ω ω.

   private static DefaultIndexedTextEncoderFactory _INSTANCE;

   private List<IndexedTextEncoder> encodings = new ArrayList<IndexedTextEncoder>();

   public List<IndexedTextEncoder> getEncodings(){ return encodings; }

   public static DefaultIndexedTextEncoderFactory getInstance(){
      if(_INSTANCE==null)_INSTANCE=new DefaultIndexedTextEncoderFactory();
      return _INSTANCE;
   }

   private DefaultIndexedTextEncoderFactory() {
      encodings.add(new RegexIndexedTextEncoder(LEVO_REGEX, LEVO_WORD));
      encodings.add(new RegexIndexedTextEncoder(DEXTRO_REGEX, DEXTRO_WORD));
      encodings.add(new RegexIndexedTextEncoder(RACEMIC_REGEX, RACEMIC_WORD));
      encodings.add(new RegexIndexedTextEncoder(RACEMIC_COMBO_REGEX, RACEMIC_WORD));

      String[] replacementTokensGreek = REPLACEMENT_SOURCE_GREEK.split(";");
      for (int i = 0; i < replacementTokensGreek.length; i = i + 2) {
         encodings.add(new RegexIndexedTextEncoder(replacementTokensGreek[i], replacementTokensGreek[i + 1]));
      }

      //add special case for colon char
      encodings.add(
              (qtest)->{
         // \"root_names_name:"AZT : ABC" AND root_names_name:"AZT : DEF"\"
                 System.out.println("====> qtest:" + qtest);
         if(qtest.contains("\"") && qtest.contains(":")){
            String tmp = qtest.replace("\\\"", "QUOTE_TEMPORARY");
            String[] parts = tmp.split("\"");
            for(int i=1;i<parts.length;i+=2){
               parts[i]="\""+parts[i].replace(":", COLON_WORD)+"\"";
            }
//            String modified1 = Arrays.stream(parts).collect(Collectors.joining("\""));
            String modified1 = Arrays.stream(parts).collect(Collectors.joining(""));
//            String modified1 = Arrays.stream(parts).map(p->"\""+p+"\"").collect(Collectors.joining(""));
            String modified2 = modified1.replace("QUOTE_TEMPORARY","\\\"");

            return modified2;
         }
         return qtest;
      });
   }
}

