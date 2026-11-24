# Project-specific rules for reflective access not covered by library consumer rules.
# Android optimization defaults are supplied by proguard-android-optimize.txt.

# TODO: Remove this rule when LinkComposer is enabled by default in Compose runtime.
-assumevalues public class androidx.compose.runtime.ComposeRuntimeFlags {
    static boolean isLinkBufferComposerEnabled() return true;
}
