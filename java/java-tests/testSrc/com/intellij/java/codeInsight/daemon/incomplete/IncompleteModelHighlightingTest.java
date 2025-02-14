// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.codeInsight.daemon.incomplete;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.IncompleteDependenciesService;

import static com.intellij.openapi.project.IncompleteDependenciesServiceKt.asAutoCloseable;

public final class IncompleteModelHighlightingTest extends LightDaemonAnalyzerTestCase {
  static final String BASE_PATH = "/codeInsight/daemonCodeAnalyzer/incompleteHighlighting";

  private void doTest() {
    doTest(getTestName(false) + ".java");
  }

  private void doTest(String fileName) {
    IncompleteDependenciesService service = getProject().getService(IncompleteDependenciesService.class);
    try (var ignored = asAutoCloseable(WriteAction.compute(() -> service.enterIncompleteState()))) {
      doTest(BASE_PATH + "/" + fileName, true, true);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void testSimple() { doTest(); }
  
  public void testModuleInfo() { doTest("module-info.java"); }

  public void testDefaultLoaderFactory() { doTest(); }
  
  public void testServer() { doTest(); }
}
