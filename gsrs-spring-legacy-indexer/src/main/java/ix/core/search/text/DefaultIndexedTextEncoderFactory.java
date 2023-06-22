package ix.core.search.text;

import java.util.ArrayList;
import java.util.List;

public class DefaultIndexedTextEncoderFactory implements IndexedTextEncodersFactory{
    /*
    Encoders modify a term for example with a regex.
    In the general use case, this is done 1) at index time and 2) at search time.
    For example:
        "β-carotene" gets indexed as .BETA.-carotene
        Then when "β-carotene" comes in as a search, the query is encoded as .BETA.-carotene before
        the search takes place. Thus, it is found in the index.
    In another case, the colon ":"  was problematic because lucene splits on colons when indexing.
    This had to be handled with care because the colon has special meaning in the GSRS API query search
    Term syntax.
    */
    private static final String LEVO_REGEX = "(-)";
    private static final String DEXTRO_REGEX = "(+)";
    private static final String RACEMIC_REGEX = "(+/-)";
    private static final String RACEMIC_COMBO_REGEX = "±"; // Associated with RACEMIC_WORD

    public static final String LEVO_WORD = "LEVOROTATION";
    public static final String RACEMIC_WORD = "RACEMICROTATION";
    public static final String DEXTRO_WORD = "DEXTROROTATION";

    private static final String REPLACEMENT_SOURCE_GREEK = "\u03B1;.ALPHA.;\u03B2;.BETA.;\u03B3;.GAMMA.;\u03B4;.DELTA.;\u03B5;.EPSILON.;\u03B6;.ZETA.;\u03B7;.ETA.;\u03B8;.THETA.;\u03B9;.IOTA.;\u03BA;.KAPPA.;\u03BB;.LAMBDA.;\u03BC;.MU.;\u03BD;.NU.;\u03BE;.XI.;\u03BF;.OMICRON.;\u03C0;.PI.;\u03C1;.RHO.;\u03C2;.SIGMA.;\u03C3;.SIGMA.;\u03C4;.TAU.;\u03C5;.UPSILON.;\u03C6;.PHI.;\u03C7;.CHI.;\u03C8;.PSI.;\u03C9;.OMEGA.;\u0391;.ALPHA.;\u0392;.BETA.;\u0393;.GAMMA.;\u0394;.DELTA.;\u0395;.EPSILON.;\u0396;.ZETA.;\u0397;.ETA.;\u0398;.THETA.;\u0399;.IOTA.;\u039A;.KAPPA.;\u039B;.LAMBDA.;\u039C;.MU.;\u039D;.NU.;\u039E;.XI.;\u039F;.OMICRON.;\u03A0;.PI.;\u03A1;.RHO.;\u03A3;.SIGMA.;\u03A4;.TAU.;\u03A5;.UPSILON.;\u03A6;.PHI.;\u03A7;.CHI.;\u03A8;.PSI.;\u03A9;.OMEGA.";
    // The uppercase and lowercase forms of the 24 letters are: Α α, Β β, Γ γ, Δ δ, Ε ε, Ζ ζ, Η η, Θ θ, Ι ι, Κ κ, Λ λ, Μ μ, Ν ν, Ξ ξ, Ο ο, Π π, Ρ ρ, Σ σ/ς, Τ τ, Υ υ, Φ φ, Χ χ, Ψ ψ, and Ω ω.

    private static DefaultIndexedTextEncoderFactory _INSTANCE;

    private IndexedTextEncoder encoder;

    public static DefaultIndexedTextEncoderFactory getInstance(){
        if(_INSTANCE==null)_INSTANCE=new DefaultIndexedTextEncoderFactory();
        return _INSTANCE;
    }

    @Override
    public IndexedTextEncoder getEncoder() {
        return encoder;
    }

    private DefaultIndexedTextEncoderFactory() {
        List<IndexedTextEncoder> encodings = new ArrayList<>();
        encodings.add(new RegexIndexedTextEncoder(LEVO_REGEX, LEVO_WORD));
        encodings.add(new RegexIndexedTextEncoder(DEXTRO_REGEX, DEXTRO_WORD));
        encodings.add(new RegexIndexedTextEncoder(RACEMIC_REGEX, RACEMIC_WORD));
        encodings.add(new RegexIndexedTextEncoder(RACEMIC_COMBO_REGEX, RACEMIC_WORD));

        String[] replacementTokensGreek = REPLACEMENT_SOURCE_GREEK.split(";");
        for (int i = 0; i < replacementTokensGreek.length; i = i + 2) {
            encodings.add(new RegexIndexedTextEncoder(replacementTokensGreek[i], replacementTokensGreek[i + 1]));
        }

        // Special case for colon ":" see class for details.
        encodings.add(new ColonIndexedTextEncoder());

        // This collapses the list of IndexedTextEncoder encoders into big combined IndexedTextEncoder.
        // Lookup Map Reduce Pattern for context.
        encoder = encodings.stream().reduce((a,b)->a.combine(b)).get();
    }
}
