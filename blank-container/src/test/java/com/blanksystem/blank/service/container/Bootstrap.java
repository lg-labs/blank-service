package com.blanksystem.blank.service.container;

import com.lg5.spring.testcontainer.boot.Lg5TestBoot;
import org.springframework.context.annotation.Import;

@Import(TestContainersLoader.class)
public abstract class Bootstrap extends Lg5TestBoot {
}
