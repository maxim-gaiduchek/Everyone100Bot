package utils;

public class Formatter {

    private Formatter() {
    }

    public static String formatTelegramText(String text) {
        return text.replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace("[", "\\[");
    }

    public static String formatNumeralText(int number, String ending0, String ending1, String ending2) {
        String textToOutput = number + " ";

        return switch (number) {
            case 11, 12, 13, 14 -> textToOutput + ending2;
            default -> switch (number % 10) {
                case 1 -> textToOutput + ending0;
                case 2, 3, 4 -> textToOutput + ending1;
                default -> textToOutput + ending2;
            };
        };
    }
}
