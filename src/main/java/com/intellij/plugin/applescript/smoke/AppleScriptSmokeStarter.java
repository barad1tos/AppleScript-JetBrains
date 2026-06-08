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
    SmokeLaunchPlan launchPlan = createLaunchPlan(fixtureRoot);
    String failureMessage = launchPlan.failureMessage();
    if (failureMessage != null) {
      LOG.error("[applescript-smoke] FAIL: " + failureMessage);
      exitSmoke(1);
      return;
    }

    String validatedFixtureRoot = launchPlan.requireFixtureRoot();
    File fixtureDirectory = launchPlan.requireFixtureDirectory();
    ApplicationManager.getApplication().executeOnPooledThread(
        () -> {
          AppleScriptSmokeRunner runner = new AppleScriptSmokeRunner(validatedFixtureRoot, fixtureDirectory);
          exitSmoke(runner.run());
        });
  }

  static @NotNull SmokeLaunchPlan createLaunchPlan(@Nullable String fixtureRoot) {
    if (fixtureRoot == null || fixtureRoot.isBlank()) {
      return new SmokeLaunchPlan(null, null, "-D" + FIXTURE_ROOT_PROPERTY + " not set");
    }

    File fixtureDirectory = new File(fixtureRoot);
    if (!fixtureDirectory.isDirectory()) {
      return new SmokeLaunchPlan(null, null, "fixture root is not a directory: " + fixtureRoot);
    }

    return new SmokeLaunchPlan(fixtureRoot, fixtureDirectory, null);
  }

  static @Nullable String fixtureRootFailure(@Nullable String fixtureRoot) {
    return createLaunchPlan(fixtureRoot).failureMessage();
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

  record SmokeLaunchPlan(
      @Nullable String fixtureRoot,
      @Nullable File fixtureDirectory,
      @Nullable String failureMessage) {
    @NotNull String requireFixtureRoot() {
      if (fixtureRoot == null) {
        throw new IllegalStateException("fixtureRoot is required for a valid smoke launch plan");
      }
      return fixtureRoot;
    }

    @NotNull File requireFixtureDirectory() {
      if (fixtureDirectory == null) {
        throw new IllegalStateException("fixtureDirectory is required for a valid smoke launch plan");
      }
      return fixtureDirectory;
    }
  }
}
