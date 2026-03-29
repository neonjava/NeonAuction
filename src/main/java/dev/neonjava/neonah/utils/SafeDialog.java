package dev.neonjava.neonah.utils;

public class SafeDialog {
    private static Boolean supported = null;

    public static boolean isSupported() {
        if (supported == null) {
            try {
                Class.forName("io.papermc.paper.dialog.Dialog");
                supported = true;
            } catch (ClassNotFoundException e) {
                supported = false;
            }
        }
        return supported;
    }
}
