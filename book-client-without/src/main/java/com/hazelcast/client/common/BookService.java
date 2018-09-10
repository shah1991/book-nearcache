package com.hazelcast.client.common;

import static java.lang.Long.parseLong;
import static java.lang.String.format;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hazelcast.commons.Book;
import com.hazelcast.commons.MyConstants;
import com.hazelcast.commons.User;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.NearCacheStats;

/**
 * Shared amongst the clients, generates test data and validates it.
 * <p>
 * Most of the work is in the {@link #test()} method.
 */

@Service
public class BookService {

    private static final int NUMBER_OF_TEST_ITERATIONS = 100000;
    private static final String LANG ="ENG";
    private static final int USER_COUNT = 10;

    @Autowired
    private HazelcastInstance hazelcastInstance;

    private int calls;
    private int founds;
    private IMap<String, Book> booksMap;
    private Random pseudoRandom;
    
    
	public HazelcastInstance getHazelcastInstance() {
		return hazelcastInstance;
	}

	public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
		this.hazelcastInstance = hazelcastInstance;
	}
	
	

	public Random getPseudoRandom() {
		return pseudoRandom;
	}

	public void setPseudoRandom(Random pseudoRandom) {
		this.pseudoRandom = pseudoRandom;
	}

	public int getCalls() {
		return calls;
	}

	public void setCalls(int calls) {
		this.calls = calls;
	}

	public int getFounds() {
		return founds;
	}

	public void setFounds(int founds) {
		this.founds = founds;
	}

	public IMap<String, Book> getBooksMap() {
		return booksMap;
	}

	public void setBooksMap(IMap<String, Book> booksMap) {
		this.booksMap = booksMap;
	}

	
	public BookService() {
		String r=null;
    	
        try {
        	
            r="20180909124233";
            long seed = parseLong(r);
            pseudoRandom = new Random(seed);
        } catch (NumberFormatException nfe) {
            String message = format("Cannot parse '%s' as seed", r);
            System.out.println(message+" : "+ nfe);
            throw new RuntimeException("Check 'application.properties' file post build");
        }
    }

    
    public void test() throws Exception {
    	
    	Instant before = Instant.now();
        booksMap = hazelcastInstance.getMap(MyConstants.MAP_NAME_BOOKS);
        IMap<Integer, User> usersMap = hazelcastInstance.getMap(MyConstants.MAP_NAME_USERS);

        // Ensure only one clients run at a time as they interfere on usersMap
        ILock iLock = hazelcastInstance.getLock("client");
        if (!iLock.tryLock(1, TimeUnit.SECONDS)) {
            System.err.printf("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! %n");
            System.err.printf("!!!           E R R O R           !!! %n");
            System.err.printf("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! %n");
            System.err.printf("!!! Another client is running %n");
            System.err.printf("!!! Need exclusive access to `userMap` %n");
            System.err.printf("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! %n");
        } else {
            try {
                // Remove output from previous run
                usersMap.clear();

                // Find the books, sorted
                Object[] bookCodes = new TreeSet(booksMap.keySet()).toArray(new String[0]);
                int books = bookCodes.length;

                // Now, run the transaction simulator
                for (int i = 1; i <= NUMBER_OF_TEST_ITERATIONS; i++) {

                    // Generate the user id and language for this transaction
                    int userId = pseudoRandom.nextInt(USER_COUNT);
                    String nextBookCode = (String)bookCodes[pseudoRandom.nextInt(books)];

                    // Retrieve the last known location for the user, or build if not known
                    User user = usersMap.get(userId);
                    if (user == null) {
                        user = new User();
                        user.setUserId(userId);
                        user.setLanguage(LANG);
                    }

                    // Validate 
                    boolean valid = validate(user, nextBookCode);
                    
                    if (valid) {
                    	
                    	user.setCount(user.getCount()+1);
                        usersMap.set(user.getUserId(), user);
                    } 

                    // Progress ticker
                    if (i % 25000 == 0) {
                        System.out.println("Test iteration {}/{} "+ i+" : " + NUMBER_OF_TEST_ITERATIONS);
                    }
                }
                
            } catch (Exception e) {
            	e.printStackTrace();
                System.out.println("test()"+" : "+ e);
            } finally {
                iLock.unlock();
                statistics(before);
            }
        }
    }


   
    private boolean validate(User user,String bookCode) {

        // ----------------------------------------------------
        // Retrieve book locations, possibly uses Near Cache
        // ----------------------------------------------------
        Book nextbook = booksMap.get(bookCode);
        calls += 1;
        
        return nextbook.getLanguage().equals(user.getLanguage());
        	
    }
    
    private void statistics(Instant before) {
    	try {
    	Instant after = Instant.now();
    	Duration duration = Duration.between(before, after);

        IMap<String, Book> airportsMap = hazelcastInstance.getMap(MyConstants.MAP_NAME_BOOKS);

        NearCacheStats airportsMapNearCacheStats = airportsMap.getLocalMapStats().getNearCacheStats();
        

        System.out.printf("===================================== %n");
        System.out.printf("===         R E S U L T S         === %n");
        System.out.printf("===================================== %n");
        System.out.printf("=== Map : '%s'%n", airportsMap.getName());

        System.out.printf("===  Calls............. : '%d'%n", getCalls());
        System.out.printf("===  Alerts............ : '%d'%n", getFounds());
        

        if (airportsMapNearCacheStats != null) {
            System.out.printf("===  Near Cache hits... : '%d'%n", airportsMapNearCacheStats.getHits());
            System.out.printf("===  Near Cache misses. : '%d'%n", airportsMapNearCacheStats.getMisses());
            System.out.printf("===  Number of Entries............ : '%d'%n", airportsMapNearCacheStats.getOwnedEntryCount());
        }

        System.out.printf("===================================== %n");
        System.out.printf("===  Run time for tests : '%s'%n", duration);
        System.out.printf("===================================== %n");
    
    }
    catch(Exception e) {
    	e.printStackTrace();
    }
    }
    
}
