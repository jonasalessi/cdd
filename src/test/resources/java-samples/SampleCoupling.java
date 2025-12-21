package com.example;

import com.example.domain.User; // Internal Coupling (+1)
import java.util.List; // External Coupling (+0.5)
import java.util.ArrayList; // External Coupling (+0.5)

public class SampleCoupling {
    private User user; // Internal Coupling (+1)
    private List<String> names = new ArrayList<>(); // External coupling (List and ArrayList)

    public void processContent(User u) { // Internal Coupling (+1)
        String s = "test"; // External coupling (String)
    }
}
