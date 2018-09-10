package com.hazelcast;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.hazelcast.client.common.BookService;


@SpringBootApplication
public class Application  implements CommandLineRunner{

    @Autowired
    private BookService bookService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        System.exit(0);
    }
    @Override
    public void run(String... arg0) throws Exception {
        this.bookService.test();
    }
}
