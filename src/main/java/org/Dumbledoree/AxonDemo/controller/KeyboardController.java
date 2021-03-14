package org.Dumbledoree.AxonDemo.controller;


import io.swagger.annotations.ApiOperation;
import org.Dumbledoree.AxonDemo.command.RestockCommand;
import org.Dumbledoree.AxonDemo.command.SellCommand;
import org.Dumbledoree.AxonDemo.query.KeyboardQuery;
import org.Dumbledoree.AxonDemo.replay.ReplayService;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.common.IdentifierFactory;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
public class KeyboardController {

    private IdentifierFactory identifierFactory=IdentifierFactory.getInstance();

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private ReplayService replayService;

    @PostMapping("sell")
    @ApiOperation(value = "出售键盘")
    public Boolean sell(@RequestParam String name, @RequestParam Integer number){

        Boolean result=commandGateway.sendAndWait(new SellCommand(identifierFactory.generateIdentifier(),name,number));
        return true;
    }
    @PostMapping("restock")
    public Boolean restock(@RequestParam String name,@RequestParam Integer number){
        commandGateway.send(new RestockCommand(identifierFactory.generateIdentifier(),name,number));
        return true;
    }


    @GetMapping("queryKeyboard")
    public Integer queryKeyboard(@RequestParam String name) throws ExecutionException, InterruptedException {
        return queryGateway.query(new KeyboardQuery(name), Integer.class).get();
    }


    @PostMapping("replay")
    public Boolean replay(){
        replayService.replay();
        return true;
    }


}
