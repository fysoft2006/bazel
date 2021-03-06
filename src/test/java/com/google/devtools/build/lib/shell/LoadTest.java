// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.shell;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests {@link Command} execution under load.
 */
@RunWith(JUnit4.class)
public class LoadTest {

  private File tempFile;

  @Before
  public void setUp() throws IOException {
    // enable all log statements to ensure there are no problems with
    // logging code
    Logger.getLogger("com.google.devtools.build.lib.shell.Command").setLevel(Level.FINEST);

    // create a temp file
    tempFile = File.createTempFile("LoadTest", "txt");
    if (tempFile.exists()) {
      tempFile.delete();
    }
    tempFile.deleteOnExit();

    // write some random numbers to the file
    try (final PrintWriter out = new PrintWriter(new FileWriter(tempFile))) {
      final Random r = new Random();
      for (int i = 0; i < 100; i++) {
        out.println(String.valueOf(r.nextDouble()));
      }
    }
  }

  @After
  public void tearDown() throws Exception {
    tempFile.delete();
  }

  @Test
  public void testLoad() throws Throwable {
    final Command command = new Command(new String[] {"/bin/cat",
                                        tempFile.getAbsolutePath()});
    Thread[] threads = new Thread[10];
    List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(new LoadThread(command, exceptions));
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].start();
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }
    if (!exceptions.isEmpty()) {
      for (Throwable t : exceptions) {
        t.printStackTrace();
      }
      throw exceptions.get(0);
    }
  }

  private static final class LoadThread implements Runnable {
    private final Command command;
    private final List<Throwable> exception;

    private LoadThread(Command command, List<Throwable> exception) {
      this.command = command;
      this.exception = exception;
    }

    @Override
    public void run() {
      try {
        for (int i = 0; i < 20; i++) {
          command.execute();
        }
      } catch (Throwable t) {
        exception.add(t);
      }
    }
  }
}
