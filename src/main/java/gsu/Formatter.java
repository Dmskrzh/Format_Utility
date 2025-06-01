package gsu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class Formatter {
    public static boolean formatDrive(String driveLetter, String fileSystem,
                                                  String volumeLabel, boolean isQuickFormat,
                                                  LogCallback logCallback) {
        // Создание временного файла со скриптом для diskpart
        File tempScript = null;
        try {
            tempScript = File.createTempFile("diskpart_script", ".txt");
            try (BufferedWriter writer = Files.newBufferedWriter(tempScript.toPath(), Charset.forName("CP866"))) {
                writer.write("select volume " + driveLetter);
                writer.newLine();

                // Форматируем том
                String formatCommand = "format fs=" + fileSystem.toLowerCase();
                if (!volumeLabel.isEmpty()) {
                    formatCommand += " label=\"" + volumeLabel + "\"";
                }
                if (isQuickFormat) {
                    formatCommand += " quick";
                }
                writer.write(formatCommand);
                writer.newLine();

                // Выход из diskpart
                writer.write("exit");
                writer.newLine();
            }

            // Запускаем diskpart с нашим скриптом
            ProcessBuilder processBuilder = new ProcessBuilder("diskpart.exe", "/s", tempScript.getAbsolutePath());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));
            String line;
            while ((line = reader.readLine()) != null) {
                logCallback.log(line);
            }

            int exitCode = process.waitFor();

            // Удаляем временный файл
            tempScript.delete();

            return exitCode == 0;
        } catch (Exception e) {
            logCallback.log("Ошибка при форматировании через diskpart: " + e.getMessage());
            if (tempScript != null) tempScript.delete();
            return false;
        }
    }

    public interface LogCallback {
        void log(String message);
    }
}
