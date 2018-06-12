package org.librairy.service.nlp.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ServiceManager {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    String resourceFolder;

    @Value("#{environment['SPOTLIGHT_ENDPOINT']?:'${spotlight.endpoint}'}")
    String endpoint;

    @Value("#{environment['SPOTLIGHT_THRESHOLD']?:${spotlight.threshold}}")
    Double threshold;

    LoadingCache<String, IXAService> ixaModels;

    LoadingCache<String, DBpediaService> dbpediaServices;

    @PostConstruct
    public void setup(){
        ixaModels = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(
                        new CacheLoader<String, IXAService>() {
                            public IXAService load(String key) {
                                LOG.info("Initializing IXA service for thread: " + key);
                                IXAService ixaService = new IXAService(resourceFolder);
                                ixaService.setup();
                                return ixaService;
                            }
                        });

        dbpediaServices = CacheBuilder.newBuilder()
                .maximumSize(100)
                .build(
                        new CacheLoader<String, DBpediaService>() {
                            public DBpediaService load(String key) {
                                LOG.info("Initializing DBpedia service for thread: " + key);
                                return new DBpediaService(endpoint, threshold);
                            }
                        });
    }

    public IXAService getIXAService(Thread thread) {
        try {
            return ixaModels.get("thread"+thread.getId());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public DBpediaService getDBpediaService(Thread thread) {

        try {
            return dbpediaServices.get("thread"+thread.getId());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }


}
