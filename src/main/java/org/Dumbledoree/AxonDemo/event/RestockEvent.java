package org.Dumbledoree.AxonDemo.event;

import org.Dumbledoree.AxonDemo.command.RestockCommand;
import org.axonframework.serialization.Revision;


@Revision("1.0")
public class RestockEvent extends RestockCommand {

    public RestockEvent(RestockCommand command) {
        super(null, command.getName(), command.getNumber());
    }
}
