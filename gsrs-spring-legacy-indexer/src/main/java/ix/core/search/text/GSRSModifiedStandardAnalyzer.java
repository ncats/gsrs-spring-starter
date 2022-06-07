package ix.core.search.text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;

public class GSRSModifiedStandardAnalyzer extends Analyzer {
    // public static final CharArraySet STOP_WORDS_SET;

    // static {
    //    STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
    // }

    private int _maxTokenLength = 255;

    private Analyzer _delegate;

    private CharArraySet _stopWords;

    GSRSModifiedStandardAnalyzer(Analyzer delegate, CharArraySet stopWords) {
        _delegate = delegate;
        _stopWords = stopWords;
//        _maxTokenLength = 255;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
        final StandardTokenizer src = new StandardTokenizer(getVersion(), reader);
        src.setMaxTokenLength(_maxTokenLength);
        TokenStream tok = new StandardFilter(getVersion(), src);
        tok = new LowerCaseFilter(getVersion(), tok);
        tok = new StopFilter(getVersion(), tok, this._stopWords);
        return new TokenStreamComponents(src, tok) {
            @Override
            protected void setReader(final Reader reader) throws IOException {
                src.setMaxTokenLength(_maxTokenLength);
                super.setReader(reader);
            }
        };
    }

    public int getPositionIncrementGap(String fieldName) {
        return _delegate.getPositionIncrementGap(fieldName);
    }

    public int getOffsetGap(String fieldName) {
        return _delegate.getOffsetGap(fieldName);
    }


    public void setVersion(Version v) {
        _delegate.setVersion(v);
    }

    public Version getVersion() {
        return _delegate.getVersion();
    }

    public void close() {
        _delegate.close();
    }



}
