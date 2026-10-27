# Fix the first Distribute run

commit: see git log

No test was deleted, `@Ignore`d, or weakened. KSP, Kotlin, and AGP were not bumped. No secret value is in this report. This commit is the one that gets pushed; the workflow result is not in this file.

## What failed run 40433797515

https://github.com/menashsoffer/Myself-/actions/runs/40433797515 failed in **Build the signed release APK**. Both messages are in that step's log, in this order:

1. First error line, on the background AWT thread, while KSP was still running. The step continued after it:

```
Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException: Cannot invoke "ksp.com.intellij.openapi.application.Application.getService(java.lang.Class)" because the return value of "ksp.com.intellij.openapi.application.ApplicationManager.getApplication()" is null
```

2. Later, the line that failed the step:

```
> Task :app:packageRelease FAILED
```

```
com.android.ide.common.signing.KeytoolException: Failed to read key *** from store "/home/runner/work/_temp/release.keystore": Get Key failed: Given final block not properly padded. Such issues can arise if a bad key is used during decryption.
```

Gradle then printed `BUILD FAILED in 3m 30s` and the step ended with `Process completed with exit code 1`. The KeytoolException is what failed the step. The NPE did not stop Gradle.

## Root cause of the KeytoolException

Task 3 generated a PKCS12 keystore with a separate `-keypass`. On this JDK, keytool prints:

```
Warning: Different store and key passwords not supported for PKCS12 KeyStores. Ignoring user-specified -keypass value.
```

A throwaway PKCS12 file built that way opens with the store password (`openssl pkcs12` exit 0) and rejects the separate key password (`Mac verify error: invalid password?`, exit 1). `passwords.env` had recorded the ignored key password. Android's signer then tried that password and failed with the padding error. The real release keystore was not used for this check. In the agent store, `KEY_PASSWORD` and `KEYSTORE_PASSWORD` now compare equal. The values were not printed.

## Local Gradle commands

Throwaway PKCS12 keystore, key password equal to store password, alias `throwaway`. Not the release key. The local `app/google-services.json` is the fake unit-test file, so this APK is not for a phone.

`./gradlew testDebugUnitTest --no-daemon` exit 0. `testDebugUnitTest` was UP-TO-DATE:

```
BUILD SUCCESSFUL in 9s
```

`./gradlew assembleRelease --no-daemon` exit 0. That log has no AWT NPE and no KeytoolException:

```
BUILD SUCCESSFUL in 1m 48s
```

APK: `app/build/outputs/apk/release/app-release.apk`. `apksigner verify --verbose` exit 0. Verified using v2 scheme: true. Signer DN: `CN=Throwaway`.

The same NPE is a known non-fatal KSP 2.3.5 message: https://github.com/google/ksp/issues/2763 ("does not break build checks"). KSP 2.3.6 release notes call it "a KSP version 2.3.5 CI error exception that does not break build checks" (https://github.com/google/ksp/releases/tag/2.3.6). It does not break the build, so KSP stays 2.3.5, Kotlin stays 2.2.10, AGP stays 9.1.1.

## What this commit changes

`actions/checkout@v4` to `@v5`, and `actions/setup-java@v4` to `@v5`. v5 of each action runs on Node 24 and needs Actions Runner v2.327.1 or newer. GitHub-hosted runners already meet that. checkout v5.0.0 is the Node 24 move (https://github.com/actions/checkout/releases/tag/v5.0.0). setup-java v5.0.0 is the same move (https://github.com/actions/setup-java/releases/tag/v5.0.0). The v4 deprecation warning on the failed run pointed at v5. No other action inputs changed.

`docs/RULES.md` section 8 now says that for PKCS12, `KEY_PASSWORD` equals `KEYSTORE_PASSWORD`.

## Who did the work

chief-architect scoped and dispatched; executed by a Cursor cloud coding agent.

## Noticed, not changed

- The local unit-test command did not re-execute tests; every test task was UP-TO-DATE.
- `enqueuePeriodicWork` is still a 1-day period.
- The release signing block still falls back to `my-upload-key.jks` when `KEYSTORE_PATH` is unset.

## Open questions

None for this commit. If the pushed run fails again on the same KeytoolException, the `KEY_PASSWORD` secret may have been copied from `passwords.env` before the store password and the key password were made the same.
