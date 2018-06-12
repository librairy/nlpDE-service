package org.librairy.service.nlp.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.librairy.service.nlp.facade.model.Annotation;
import org.librairy.service.nlp.facade.model.PoS;
import org.librairy.service.nlp.facade.model.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class IXAServiceTest {


    private static final Logger LOG = LoggerFactory.getLogger(IXAServiceTest.class);


    IXAService service;

    @Before
    public void setup(){
        service = new IXAService("src/main/bin");
        service.setup();
    }

    @Test
    public void annotation() throws IOException {

        String text = "Da gab der Mentor-Kapitän das verabredete Zeichen , und mit aller Kraft und Schnelligkeit rollten jetzt die Böller von der einen Schiffsseite nach der andern hinüber und schlugen , durch die dünne Wandung hindurch , auf das unten haltende , schon siegessichere Boot , das nun , von der Wucht der schweren eisernen Kanonen in Stücke gebrochen , mit Mann und Maus zugrunde ging";

        List<PoS> filter = Collections.emptyList();

        List<Annotation> annotations = service.annotations(text, filter);

        annotations.forEach(annotation -> LOG.info(annotation.toString()));

//        Assert.assertEquals(2, annotations.size());
    }

}