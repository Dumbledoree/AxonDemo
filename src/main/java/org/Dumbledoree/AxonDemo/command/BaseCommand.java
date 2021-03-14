package org.Dumbledoree.AxonDemo.command;

import lombok.Getter;
import org.axonframework.modelling.command.TargetAggregateIdentifier;


@Getter
public class BaseCommand {

    @TargetAggregateIdentifier
    private String targetAggregateIdentifier;
    public BaseCommand(String targetAggregateIdentifier){
        this.targetAggregateIdentifier=targetAggregateIdentifier;
    }
}
