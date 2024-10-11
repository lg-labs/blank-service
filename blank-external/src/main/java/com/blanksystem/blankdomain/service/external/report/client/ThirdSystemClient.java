package com.blanksystem.blankdomain.service.external.report.client;

import com.blanksystem.blankdomain.service.external.report.dto.User;
import com.lg5.spring.client.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient(
        name = "jsonplaceholder",
        url = "${feign.client.config.jsonplaceholder.url}",
        configuration = {
                FeignClientConfiguration.class
        })
public interface ThirdSystemClient {
    @GetMapping(value = "/users")
    ResponseEntity<List<User>> getUsers();

    @PostMapping(value = "/users")
    ResponseEntity<User> createUser(@RequestBody User user);

    @GetMapping(value = "/users")
    ResponseEntity<byte[]> getUserss();

}
