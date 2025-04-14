package org.example.replica.replica2;

import org.example.sequencer.Config;

import javax.xml.ws.Endpoint;
import java.util.ArrayList;
import java.util.List;

public class StockMarketPublisher {
    public static void main(String[] args) {
        List<Thread> threads = new ArrayList<>();
        Config config = Config.REPLICA2;

        for (ShareMarket shareMarket : ShareMarket.values()) {
            StockMarketServiceImpl stockMarketService = new StockMarketServiceImpl(shareMarket);
            Endpoint.publish("http://" + config.getIpAddress() + ":" + config.getPortNumber() + "/" + shareMarket.getCode(), stockMarketService);
            threads.add(new Thread(stockMarketService));
            System.out.println(shareMarket.getName() + " Service is published!");
        }

        threads.forEach(Thread::start);
    }
}
