package org.librairy.service.nlp.service;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import eus.ixa.ixa.pipe.pos.Annotate;
import eus.ixa.ixa.pipe.pos.CLI;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.Term;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.nlp.facade.model.Annotation;
import org.librairy.service.nlp.facade.model.Form;
import org.librairy.service.nlp.facade.model.PoS;
import org.librairy.service.nlp.facade.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class IXAService  {

    private static final Logger LOG = LoggerFactory.getLogger(org.librairy.service.nlp.service.IXAService.class);

    String resourceFolder;

    String model              ;
    String lemmatizerModel    ;
    String language           ;
    String multiwords         ;
    String dictag             ;
//    String binDictMorphoTaggerURL;
    String kafVersion         ;
    String normalize          ;
    String untokenizable      ;
    String hardParagraph      ;
    String noseg              ;

    private Properties annotateProperties;
    private Annotate posAnnotator;


    public IXAService(String resourceFolder) {
        this.resourceFolder = resourceFolder;
    }

    public void setup()  {

        model              = Paths.get(resourceFolder,"morph-models-1.5.0/de/de-pos-perceptron-autodict01-conll09.bin").toFile().getAbsolutePath();
        lemmatizerModel    = Paths.get(resourceFolder,"morph-models-1.5.0/de/de-lemma-perceptron-conll09.bin").toFile().getAbsolutePath();
        language           = "de";
        multiwords         = "false";
        dictag             = "false";
//        binDictMorphoTaggerURL = Paths.get(resourceFolder,"nerc-models-1.5.4/en/conll03").toFile().getAbsolutePath();
        kafVersion         = "1.5.0";
        normalize          = "true"; // false
        untokenizable      = "false";
        hardParagraph      = "false";
        noseg              = "false";

        this.annotateProperties = new Properties();

        annotateProperties.setProperty("normalize", normalize);
        annotateProperties.setProperty("untokenizable", untokenizable);
        annotateProperties.setProperty("hardParagraph", hardParagraph);
        annotateProperties.setProperty("noseg",noseg);
        annotateProperties.setProperty("model", model);
        annotateProperties.setProperty("lemmatizerModel", lemmatizerModel);
        annotateProperties.setProperty("language", language);
        annotateProperties.setProperty("multiwords", multiwords);
        annotateProperties.setProperty("dictag", dictag);

        try {
            this.posAnnotator    = new Annotate(annotateProperties);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing IXA service",e);
        }
    }

    public String tokens(String text, List<PoS> filter, Form form) throws AvroRemoteException {

        return analyze(text,filter).stream()
                .map(term-> {
                    switch (form){
                        case LEMMA: return Strings.isNullOrEmpty(term.getLemma())? term.getStr() : term.getLemma().toLowerCase();
                        default: return term.getStr().toLowerCase();
                    }
                })
                .collect(Collectors.joining(" "));
    }


    public List<Annotation> annotations(String text, List<PoS> filter) throws AvroRemoteException {
        List<Term> terms = new ArrayList<>();
        Matcher matcher = Pattern.compile(".{1,1000}(,|.$)").matcher(text);
        while (matcher.find()){
            terms.addAll(analyze(text, filter));
        }
        return terms.stream()
                .map(term -> {

                    Token token = new Token();
                    token.setTarget(term.getStr());
                    token.setLemma(!Strings.isNullOrEmpty(term.getLemma())?term.getLemma():"");

                    if ((CharMatcher.javaLetter().matchesAllOf(term.getStr())) && CharMatcher.javaDigit().matchesAllOf(term.getLemma())){
                        // special case:
                        // target = first
                        // lemma = 1
                        token.setLemma(term.getStr());
                    }

                    token.setMorphoFeat(!Strings.isNullOrEmpty(term.getMorphofeat())?term.getMorphofeat():"");
                    token.setPos(!Strings.isNullOrEmpty(term.getPos())?PoSTranslator.toPoSTag(term.getPos()):PoS.NOUN);
                    token.setType(!Strings.isNullOrEmpty(term.getType())?term.getType():"");

                    Annotation annotation = new Annotation();
                    if (term.getSentiment() != null) annotation.setSentiment(term.getSentiment().getPolarity());
                    annotation.setToken(token);
                    annotation.setOffset(Long.valueOf(term.getSpan().getTargets().get(0).getOffset()));

                    return annotation;
                })
                .collect(Collectors.toList());
    }

    private List<Term> analyze(String text, List<PoS> filter){
        List<Term> terms = Collections.emptyList();

        final KAFDocument kaf;
        try {

            InputStream is = new ByteArrayInputStream(text.getBytes());

            BufferedReader breader = new BufferedReader(new InputStreamReader(is));

            kaf = new KAFDocument(language, kafVersion);

            final String version        = CLI.class.getPackage().getImplementationVersion();
            final String commit         = CLI.class.getPackage().getSpecificationVersion();

            final eus.ixa.ixa.pipe.tok.Annotate tokAnnotator = new eus.ixa.ixa.pipe.tok.Annotate(breader, annotateProperties);

            // Tokenization
            tokAnnotator.tokenizeToKAF(kaf);


            // PosTagging

            final KAFDocument.LinguisticProcessor newLp = kaf.addLinguisticProcessor("terms", "ixa-pipe-pos-" + Files.getNameWithoutExtension(model), version + "-" + commit);
            newLp.setBeginTimestamp();
            posAnnotator.annotatePOSToKAF(kaf);

            // Filtering
            List<String> postags = filter.stream().flatMap( type -> PoSTranslator.toTermPoS(type).stream()).collect(Collectors.toList());

            terms = kaf.getAnnotations(KAFDocument.AnnotationType.TERM).stream()
                    .map(annotation -> (Term) annotation)
                    .filter(term -> postags.isEmpty() || postags.contains(term.getPos()))
                    .collect(Collectors.toList());

            breader.close();
        } catch (IOException e) {
            LOG.error("Error analyzing text", e);
        }

        return terms;
    }
}
