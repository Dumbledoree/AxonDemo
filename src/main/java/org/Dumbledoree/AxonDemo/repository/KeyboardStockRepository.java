package org.Dumbledoree.AxonDemo.repository;

import org.Dumbledoree.AxonDemo.model.KeyboardStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyboardStockRepository extends JpaRepository<KeyboardStock,Long> {

    KeyboardStock findKeyboardStockByName(String name);
}
