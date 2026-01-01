package com.example;

import java.io.IOException;

public class SampleExceptions {
    public void testExceptions() {
        try {
            doSomething();
        } catch (IOException e) { // +1 Exception Handling
            e.printStackTrace();
        } catch (Exception e) { // +1 Exception Handling
            e.printStackTrace();
        } finally { // +1 Exception Handling
            System.out.println("Done");
        }
    }

    private void doSomething() throws IOException {
        throw new IOException("Error");
    }
}
