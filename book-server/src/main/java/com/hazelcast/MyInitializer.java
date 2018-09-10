package com.hazelcast;

import com.hazelcast.commons.Book;
import com.hazelcast.commons.MyConstants;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize the cluster, ensuring all maps are created
 * which is useful for monitoring, and injecting test
 * data.
 */
@Component
public class MyInitializer implements CommandLineRunner {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    public HazelcastInstance getHazelcastInstance() {
		return hazelcastInstance;
	}

	public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
		this.hazelcastInstance = hazelcastInstance;
	}

	/**
     * Access all maps, which creates them if they don't already exist. This means all
     * maps are visible on the management center if that is being used, even if they have
     * never had any content added.
     * <p>
     * Call the {@link #loadAirports()} method to populate the "{@code airports}" map.
     */
    public void run(String... arg0) {
        // Initialise all maps
        for (String mapName : MyConstants.MAP_NAMES) {
            hazelcastInstance.getMap(mapName);
        }

        loadAirports();
    }

    /**
     * Load the airport data into the airport map, once per cluster.
     */
    private void loadAirports() {
        IMap<String, Book> booksMap = hazelcastInstance.getMap(MyConstants.MAP_NAME_BOOKS);

        if (!booksMap.isEmpty()) {
//            log.info("Skip loading '{}', not empty", airportsMap.getName());
        } else {
            for (int i = 0; i < TestData.BOOKS.length; i++) {
                Object[] airportData = TestData.BOOKS[i];

                Book book = new Book();

                book.setCode(airportData[0].toString());
                book.setDescription(airportData[1].toString());
                book.setLanguage(airportData[2].toString());
                

                booksMap.put(book.getCode(), book);
            }

            System.out.println("Loaded {} into '{}'"+" : "+ TestData.BOOKS.length+" : "+ booksMap.getName());
        }
    }
}
