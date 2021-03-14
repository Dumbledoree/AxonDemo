package org.Dumbledoree.AxonDemo.event;

import org.Dumbledoree.AxonDemo.command.SellCommand;
import org.axonframework.serialization.Revision;


@Revision("1.0")
public class SellEvent extends SellCommand {
    public SellEvent(SellCommand command) {
        super(null, command.getName(), command.getNumber());
    }
}
