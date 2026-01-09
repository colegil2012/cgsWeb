package com.ua.estore.cgsWeb;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CgsWebApplicationTests {

	@Autowired
	private ProductRepository productRepository;

	@Test
	void contextLoads() {

	}

	@Test
	void testMongoConnection() {
		List<Product> products = productRepository.findAll();
		System.out.println("[DEBUG_LOG] Products found in DB: " + products.size());
		products.forEach(p -> System.out.println("[DEBUG_LOG] Product: " + p.getName()));
	}

	@Test
	void simplePassTest() {
		assertTrue(true, "This test will always pass");
	}

}
