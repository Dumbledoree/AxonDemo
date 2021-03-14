package org.Dumbledoree.AxonDemo.aggregate;


import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Aggregate;
import org.Dumbledoree.AxonDemo.command.RestockCommand;
import org.Dumbledoree.AxonDemo.command.SellCommand;
import org.Dumbledoree.AxonDemo.event.RestockEvent;
import org.Dumbledoree.AxonDemo.event.SellEvent;
import org.Dumbledoree.AxonDemo.query.KeyboardQuery;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

@Aggregate
@Slf4j
public class KeyboardAggregate {

    @AggregateIdentifier
    private String id;

    @Autowired
    private QueryGateway queryGateway;


    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void on(RestockCommand command) {
        log.info("RestockCommand:{}", command);
        this.id = command.getTargetAggregateIdentifier();
        AggregateLifecycle.apply(new RestockEvent(command));
    }


    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public Boolean on(SellCommand command) throws ExecutionException, InterruptedException {
        log.info("SellCommand:{}", command);
        this.id = command.getTargetAggregateIdentifier();
        Integer ammount = queryGateway.query(new KeyboardQuery(command.getName()), Integer.class).get();
        if (command.getNumber() > ammount) {
            AggregateLifecycle.apply(new SellEvent(command));
            return true;
        } else {
            return false;
        }
    }


}
