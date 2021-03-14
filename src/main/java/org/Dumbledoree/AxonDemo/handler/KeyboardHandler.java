package org.Dumbledoree.AxonDemo.handler;


import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ReplayStatus;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.Dumbledoree.AxonDemo.event.RestockEvent;
import org.Dumbledoree.AxonDemo.event.SellEvent;
import org.Dumbledoree.AxonDemo.model.KeyboardStock;
import org.Dumbledoree.AxonDemo.query.KeyboardQuery;
import org.Dumbledoree.AxonDemo.repository.KeyboardStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@ProcessingGroup("keyboardHandler")
@AllowReplay
@Slf4j
public class KeyboardHandler {


    @Autowired
    private KeyboardStockRepository keyboardStockRepository;


    @EventHandler
    public void on(RestockEvent event, ReplayStatus replayStatus) {
        KeyboardStock keyboardStock = keyboardStockRepository.findKeyboardStockByName(event.getName());
        if (Objects.isNull(keyboardStock)) {
            keyboardStockRepository.save(new KeyboardStock(null, event.getName(), event.getNumber()));
        } else {
            keyboardStock.setAccount(keyboardStock.getAccount() + event.getNumber());
            keyboardStockRepository.save(keyboardStock);
        }
    }

    @EventHandler
    public void on(SellEvent event) {
        KeyboardStock keyboardStock = keyboardStockRepository.findKeyboardStockByName(event.getName());
        keyboardStock.setAccount(keyboardStock.getAccount() - event.getNumber());
        keyboardStockRepository.save(keyboardStock);
    }

    @QueryHandler
    public Integer on(KeyboardQuery query) {
        KeyboardStock keyboardStock = keyboardStockRepository.findKeyboardStockByName(query.getName());
        return Objects.isNull(keyboardStock) ? 0 : keyboardStock.getAccount();
    }


}
