package gsu;

import com.sun.jna.Platform;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class DiskFormatter extends Application {
    // Определение элементов GUI
    private ChoiceBox<DriveInfo> drivesChoiceBox;
    private TextField volumeLabelTextField;
    private ChoiceBox<String> fileSystemChoiceBox;
    private ChoiceBox<String> clusterChoiceBox;
    private CheckBox quickFormatCheckBox;
    private Button formatButton;
    private TextArea logArea;
    private ProgressBar progressBar;

    // Вход в JavaFX приложение
    @Override
    public void start(Stage stage) {
        if (!Platform.isWindows()) {
            showErrorAlert("Ошибка", "Эта утилита работает только на Windows.");
            return;
        }

        initializeComponents();
        loadAvailableDrives();
        VBox mainLayout = createMainLayout();

        Scene scene = new Scene(mainLayout, 600, 500);
        stage.setScene(scene);
        stage.setTitle("Утилита форматирования накопителей");
        stage.setResizable(false);
        stage.show();
    }

    // Метод инициализации компонентов GUI
    private void initializeComponents() {
        // ChoiceBox для выбора накопителя
        drivesChoiceBox = new ChoiceBox<>();
        drivesChoiceBox.setOnAction(e -> updateClusterSizes());

        // Текстовое поле для метки тома
        volumeLabelTextField = new TextField();
        volumeLabelTextField.setPrefColumnCount(15);

        // ChoiceBox для выбора файловой системы
        fileSystemChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList("NTFS", "FAT32", "exFAT"));
        fileSystemChoiceBox.setValue("NTFS");
        fileSystemChoiceBox.setOnAction(e -> updateClusterSizes());

        // ChoiceBox для выбора размера кластера
        clusterChoiceBox = new ChoiceBox<>();

        // CheckBox для быстрого форматирования
        quickFormatCheckBox = new CheckBox("Быстрое форматирование");
        quickFormatCheckBox.setSelected(true);

        // Кнопка для запуска форматирования
        formatButton = new Button("Начать форматирование");
        formatButton.setOnAction(e -> startFormatting());

        // Поле логов
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(8);

        // Прогресс бар
        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
    }

    // Метод создания макета GUI
    private VBox createMainLayout() {
        // Основная сетка для параметров
        GridPane parametersGrid = new GridPane();
        parametersGrid.setHgap(15);
        parametersGrid.setVgap(15);
        parametersGrid.setPadding(new Insets(20));

        // Размещение элементов в сетке
        parametersGrid.add(new Label("Накопитель:"), 0, 0);
        parametersGrid.add(drivesChoiceBox, 1, 0);
        parametersGrid.add(new Label("Метка тома:"), 0, 1);
        parametersGrid.add(volumeLabelTextField, 1, 1);
        parametersGrid.add(new Label("Файловая система:"), 0, 2);
        parametersGrid.add(fileSystemChoiceBox, 1, 2);
        parametersGrid.add(new Label("Размер кластера:"), 0, 3);
        parametersGrid.add(clusterChoiceBox, 1, 3);

        // Область для чекбокса и кнопки
        HBox controlsBox = new HBox(15);
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.getChildren().addAll(quickFormatCheckBox, formatButton);

        // Область для логов
        VBox logBox = new VBox(5);
        logBox.getChildren().addAll(new Label("Журнал операций:"), logArea, progressBar);

        // Основной контейнер
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(10));
        mainLayout.getChildren().addAll(parametersGrid, new Separator(), controlsBox, new Separator(), logBox);

        return mainLayout;
    }

    // Загрузка доступных накопителей
    private void loadAvailableDrives() {
        List<DriveInfo> drivesList = DriveManager.getAvailableDrives();

        drivesChoiceBox.setItems(FXCollections.observableList(drivesList));
        if (!drivesList.isEmpty()) {
            drivesChoiceBox.setValue(drivesList.get(0));
            updateClusterSizes();
        }
    }

    // Обновление выбора размера кластера
    private void updateClusterSizes() {
        DriveInfo selectedDrive = drivesChoiceBox.getValue();
        String fileSystem = fileSystemChoiceBox.getValue();

        if (selectedDrive == null || fileSystem == null) {
            return;
        }

        int[] allowedSizes = ClusterUtils.getAllowedClusterSizes(fileSystem, selectedDrive.size);
        var clusterOptions = FXCollections.observableArrayList("По умолчанию");

        for (int size : allowedSizes) {
            String sizeDesc = size >= 1024 ? (size / 1024) + " КБ" : size + " байт";
            clusterOptions.add(sizeDesc);
        }

        clusterChoiceBox.setItems(clusterOptions);
        clusterChoiceBox.setValue("По умолчанию");
    }

    // Метод запуска форматирования
    private void startFormatting() {
        DriveInfo selectedDrive = drivesChoiceBox.getValue();
        if (selectedDrive == null) {
            showErrorAlert("Ошибка", "Выберите накопитель для форматирования.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Форматирование удалит все данные на диске " + selectedDrive.letter + ":\n\nВы уверены?",
                ButtonType.YES, ButtonType.NO);
        confirmAlert.setTitle("Подтверждение форматирования");
        confirmAlert.setHeaderText("Подтверждение операции");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                performFormatting();
            }
        });
    }

    // Метод выполнения форматирования в отдельном потоке;
    private void performFormatting() {
        DriveInfo selectedDrive = drivesChoiceBox.getValue();
        String fileSystem = fileSystemChoiceBox.getValue();
        String volumeLabel = volumeLabelTextField.getText().trim();
        boolean isQuickFormat = quickFormatCheckBox.isSelected();
        int clusterSize = getSelectedClusterSize();

        // Отключение интерфейса во время форматирования
        setUIEnabled(false);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        logArea.clear();

        appendLog("Начинаем форматирование диска " + selectedDrive.letter + ":");
        appendLog("Файловая система: " + fileSystem);
        appendLog("Метка тома: " + (volumeLabel.isEmpty() ? "(пустая)" : volumeLabel));
        appendLog("Быстрое форматирование: " + (isQuickFormat ? "Да" : "Нет"));
        if (clusterSize > 0) {
            appendLog("Размер кластера: " + (clusterSize >= 1024 ? (clusterSize / 1024) + " КБ" : clusterSize + " байт"));
        }
        appendLog("");

        // Создание задачи для форматирования в отдельном потоке
        Task<Boolean> formatTask = new Task<>() {
            @Override
            protected Boolean call() {
                return Formatter.formatDrive(selectedDrive.letter, fileSystem, volumeLabel, isQuickFormat,
                        DiskFormatter.this::appendLog);
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                progressBar.setVisible(false);
                setUIEnabled(true);

                if (getValue()) {
                    appendLog("Форматирование успешно завершено!");
                    showInfoAlert("Форматирование диска " + selectedDrive.letter + ": успешно завершено!");
                } else {
                    appendLog("Ошибка форматирования!");
                    showErrorAlert("Ошибка", "Произошла ошибка при форматировании диска.");
                }

                loadAvailableDrives();
            }

            @Override
            protected void failed() {
                super.failed();
                progressBar.setVisible(false);
                setUIEnabled(true);
                appendLog("Критическая ошибка: " + getException().getMessage());
                showErrorAlert("Критическая ошибка", "Произошла критическая ошибка: " + getException().getMessage());
            }
        };

        Thread formatThread = new Thread(formatTask);
        formatThread.setDaemon(true);
        formatThread.start();
    }

    // Метод получения размера кластера
    private int getSelectedClusterSize() {
        String selected = clusterChoiceBox.getValue();
        if (selected == null || selected.equals("По умолчанию")) {
            return -1;
        }

        DriveInfo selectedDrive = drivesChoiceBox.getValue();
        String fileSystem = fileSystemChoiceBox.getValue();
        int[] allowedSizes = ClusterUtils.getAllowedClusterSizes(fileSystem, selectedDrive.size);

        int index = clusterChoiceBox.getItems().indexOf(selected) - 1;
        if (index >= 0 && index < allowedSizes.length) {
            return allowedSizes[index];
        }

        return -1;
    }

    // Активация элементов UI
    private void setUIEnabled(boolean enabled) {
        drivesChoiceBox.setDisable(!enabled);
        volumeLabelTextField.setDisable(!enabled);
        fileSystemChoiceBox.setDisable(!enabled);
        clusterChoiceBox.setDisable(!enabled);
        quickFormatCheckBox.setDisable(!enabled);
        formatButton.setDisable(!enabled);
    }

    // Вывод лога в соответствующее поле UI
    private void appendLog(String message) {
        javafx.application.Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    // Вывод информации об ошибке
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // Вывод информации об успехе
    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
