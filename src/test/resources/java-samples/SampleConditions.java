package com.example;

public class SampleConditions {
    public void complexConditions(boolean a, boolean b, boolean c) {
        if (a && b || c) { // +1 Branch (if), +3 Conditions (a, b, c)
            System.out.println("Match");
        }

        if ((a || b) && !c) { // +1 Branch (if), +3 Conditions (a, b, c)
            System.out.println("Match 2");
        }
    }
}
