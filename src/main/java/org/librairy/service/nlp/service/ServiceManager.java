package org.librairy.service.nlp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ServiceManager {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    String resourceFolder;

    Map<String,IXAService> ixaServices;

    @PostConstruct
    public void setup(){
        ixaServices     = new ConcurrentHashMap<>();
    }

    public IXAService getIXAService(Thread thread) {

        String id = "thread"+thread.getId();
        if (!ixaServices.containsKey(id)){
            LOG.info("Initializing IXA service for thread: " + id);
            IXAService ixaService = new IXAService(resourceFolder);
            ixaService.setup();
            ixaServices.put(id,ixaService);
        }
        return ixaServices.get(id);

    }


}
