package org.Dumbledoree.AxonDemo.replay;

import org.axonframework.config.Configuration;
import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventProcessor;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.spring.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate5.SpringBeanContainer;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author Dumbledoree
 * @version 1.0
 */

@Service
public class ReplayService {

    @Autowired
    private ApplicationContext context;

    public void replay(){
        EventProcessingConfigurer configurer=context.getBean(EventProcessingConfigurer.class);
        configurer.registerTrackingEventProcessor("keyboardHandler");
        configurer.usingTrackingEventProcessors();
        Configuration configuration=context.getBean(Configuration.class);
        EventProcessingConfiguration eventProcessingConfiguration=configuration.eventProcessingConfiguration();
        Optional<TrackingEventProcessor> eventProcessorOptional=eventProcessingConfiguration.eventProcessorByProcessingGroup("keyboardHandler", TrackingEventProcessor.class);
        if(eventProcessorOptional.isPresent()){
            TrackingEventProcessor trackingEventProcessor=eventProcessorOptional.get();
            trackingEventProcessor.shutDown();
            trackingEventProcessor.resetTokens();
            trackingEventProcessor.start();
        }
        configurer.usingSubscribingEventProcessors();
    }

}
