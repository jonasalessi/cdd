package com.example;

public class SampleBranches {
    public void testIf(int x) {
        if (x > 0) { // +1 Branch, +1 Condition
            System.out.println("Positive");
        } else if (x < 0) { // +1 Branch (else if), +1 Condition
            System.out.println("Negative");
        } else { // +1 Branch (else)
            System.out.println("Zero");
        }
    }

    public void testSwitch(int x) {
        switch (x) { // +1 Branch
            case 1:
                System.out.println("One");
                break;
            case 2:
                System.out.println("Two");
                break;
            default:
                System.out.println("Other");
                break;
        }
    }

    public int testTernary(int x) {
        return x > 0 ? 1 : 0; // +1 Branch, +1 Condition
    }

    public void testLoops(int n) {
        for (int i = 0; i < n; i++) { // +1 Branch, +1 Condition
            while (i < 5) { // +1 Branch, +1 Condition
                System.out.println(i);
            }
        }
    }
}
