
package com.example.datingsite.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BeanDemo {

    @Autowired
    private ApplicationContext context;

    public void demonstrate() {
        String name1 = context.getBean("applicationName", String.class);
        String name2 = context.getBean("applicationName", String.class);

        StringBuilder history1 =
                context.getBean("searchHistory", StringBuilder.class);
        StringBuilder history2 =
                context.getBean("searchHistory", StringBuilder.class);

        System.out.println("Singleton: " + (name1 == name2));
        System.out.println("Prototype: " + (history1 == history2));
    }
}
