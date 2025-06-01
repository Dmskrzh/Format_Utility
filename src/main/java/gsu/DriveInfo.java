package gsu;

public class DriveInfo {
    public final String letter; // Буква накопителя
    public final String description;    // Тип накопителя
    public final long size; // Объём диска
    public final int type;  // Тип файловой системы

    // Конструктор
    public DriveInfo(String letter, String description, long size, int type) {
        this.letter = letter;
        this.description = description;
        this.size = size;
        this.type = type;
    }

    // Метод для сборки строки
    @Override
    public String toString() {
        return letter + ": - " + description;
    }
}