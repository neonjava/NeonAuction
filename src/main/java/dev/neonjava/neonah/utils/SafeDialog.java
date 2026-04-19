package dev.neonjava.neonah.utils;

public class SafeDialog {
    private static Boolean supported = null;

    public static boolean isSupported() {
        if (supported == null) {
            try {
                Class.forName("io.papermc.paper.dialog.Dialog");
                Class.forName("org.bukkit.entity.Player").getMethod("showDialog",
                        Class.forName("io.papermc.paper.dialog.Dialog"));
                supported = true;
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                supported = false;
            }
        }
        return supported;
    }
}
