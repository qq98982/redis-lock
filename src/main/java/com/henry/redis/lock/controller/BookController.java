package com.henry.redis.lock.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.henry.redis.lock.annotation.CacheLock;
import com.henry.redis.lock.annotation.CacheParam;

@RestController
@RequestMapping("/books")
public class BookController {

    @CacheLock(prefix = "books", expire = 2)
    @GetMapping
    public String query(@CacheParam(name = "token") @RequestParam String token) {
        return "success - " + token;
    }

}