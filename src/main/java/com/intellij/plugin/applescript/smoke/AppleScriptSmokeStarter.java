package com.intellij.plugin.applescript.smoke;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Headless smoke command entry point.
 */
public final class AppleScriptSmokeStarter implements ApplicationStarter {
  private static final String FIXTURE_ROOT_PROPERTY = "applescript.smoke.fixtureRoot";
  private static final Logger LOG = Logger.getInstance("#" + AppleScriptSmokeStarter.class.getName());

  @Override
  @SuppressWarnings("java:S1147") // ApplicationStarter callback is named main by platform contract.
  public void main(@NotNull List<String> args) {
    String fixtureRoot = System.getProperty(FIXTURE_ROOT_PROPERTY);
    File fixtureDirectory = fixtureDirectoryOrExit(fixtureRoot);
    if (fixtureDirectory == null) {
      return;
    }

    ApplicationManager.getApplication().executeOnPooledThread(
        () -> {
          AppleScriptSmokeRunner runner = new AppleScriptSmokeRunner(fixtureRoot, fixtureDirectory);
          exitSmoke(runner.run());
        });
  }

  private static @Nullable File fixtureDirectoryOrExit(@Nullable String fixtureRoot) {
    if (fixtureRoot == null || fixtureRoot.isBlank()) {
      LOG.error("[applescript-smoke] FAIL: -D" + FIXTURE_ROOT_PROPERTY + " not set");
      exitSmoke(1);
      return null;
    }

    File fixtureDirectory = new File(fixtureRoot);
    if (!fixtureDirectory.isDirectory()) {
      LOG.error("[applescript-smoke] FAIL: fixture root is not a directory: " + fixtureRoot);
      exitSmoke(1);
      return null;
    }

    return fixtureDirectory;
  }

  private static void exitSmoke(int code) {
    try {
      ApplicationManager.getApplication().exit(true, true, false, code);
    } catch (ProcessCanceledException exception) {
      throw exception;
    } catch (IllegalStateException | IllegalArgumentException exception) {
      LOG.warn("ApplicationManager.exit threw", exception);
    }
  }
}
