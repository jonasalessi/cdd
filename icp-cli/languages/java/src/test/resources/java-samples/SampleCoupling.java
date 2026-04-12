package com.example;

import com.example.domain.User;

import java.util.ArrayList;
import java.util.List;

public class SampleCoupling {
    private User user; // Internal Coupling (+1)
    private List<String> names = new ArrayList<>(); // External coupling (List and ArrayList)

    public void processContent(User u) { // Internal Coupling (+1)
        String s = "test"; // External coupling (String)
    }
}
