package org.Dumbledoree.AxonDemo.command;


import lombok.Getter;

@Getter
public class SellCommand extends BaseCommand{

    private String name;

    private Integer number;


    public SellCommand(String targetAggregateIdentifier,String name,Integer number) {
        super(targetAggregateIdentifier);
        this.name=name;
        this.number=number;
    }
}
