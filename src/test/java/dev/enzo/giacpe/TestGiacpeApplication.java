package dev.enzo.giacpe;

import org.springframework.boot.SpringApplication;

public class TestGiacpeApplication {

	public static void main(String[] args) {
		SpringApplication.from(GiacpeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
