package com.congyre.trade.rest;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.congyre.trade.entity.User;
import com.congyre.trade.service.UserService;

@RestController
public class UserController {
	

	@Autowired
	private UserService service;
	
	@RequestMapping(method = RequestMethod.GET, value="/user")
	public List<User> getAllUsers(){
		return service.getAllUsers();
	}
	
	@RequestMapping("/user/{userId}")
	public User getUser(@PathVariable ObjectId userId) {
		return service.getUser(userId);
	}
	
	@RequestMapping(method=RequestMethod.POST, value="/user")
	public void addUser(@RequestBody User user) {
		service.updateUser(user);
	}
	
	@RequestMapping(method=RequestMethod.DELETE, value="/user/{userId}")
	public void deleteClient(@PathVariable ObjectId userId) {
		service.deleteUser(userId);
	}
}
