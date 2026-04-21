import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.time.LocalDate;
import java.util.Optional;

public class App extends Application {

    // --- Task Object ---
    public static class Task {
        private final SimpleStringProperty name;
        private final SimpleStringProperty priority;
        private final SimpleStringProperty dueDate;
        private final SimpleStringProperty status;

        public Task(String name, String priority, String dueDate) {
            this.name = new SimpleStringProperty(name);
            this.priority = new SimpleStringProperty(priority);
            this.dueDate = new SimpleStringProperty(dueDate);
            this.status = new SimpleStringProperty("Pending");
        }

        public String getName() { return name.get(); }
        public SimpleStringProperty nameProperty() { return name; }
        
        public String getPriority() { return priority.get(); }
        public SimpleStringProperty priorityProperty() { return priority; }

        public String getDueDate() { return dueDate.get(); }
        public SimpleStringProperty dueDateProperty() { return dueDate; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }

        public void setName(String newName) { this.name.set(newName); }
        public void setPriority(String newPriority) { this.priority.set(newPriority); }
        public void setDueDate(String newDate) { this.dueDate.set(newDate); }
        public void setStatus(String newStatus) { this.status.set(newStatus); }
    }

    private ObservableList<Task> masterTaskList;
    private TableView<Task> taskTable;
    private ProgressBar taskProgressBar;
    private Label progressLabel;
    private boolean isDarkMode = false;
    private BorderPane root;

    private TextField taskInput;
    private ComboBox<String> priorityInput;
    private DatePicker datePicker;
    private Button addOrUpdateButton;
    private Button clearAllBtn;
    private Task currentlyEditingTask = null; 

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("To-Do List Application");

        root = new BorderPane();
        applyTheme();
        root.setPadding(new Insets(30));

        // ==========================================
        // UPPER LAYOUT
        // ==========================================
        VBox topContainer = new VBox(25);
        topContainer.setPadding(new Insets(0, 0, 30, 0));

        BorderPane headerBox = new BorderPane();
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("My Tasks Dashboard");
        titleLabel.setFont(Font.font("Inter", FontWeight.BOLD, 32));
        Label subTitle = new Label("Organize, track, and complete your daily tasks efficiently.");
        subTitle.setStyle("-fx-font-size: 14px;");
        titleBox.getChildren().addAll(titleLabel, subTitle);

        Button themeButton = new Button(" ☾ Theme Toggle");
        themeButton.setStyle("-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand; -fx-font-weight: bold;");
        themeButton.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            themeButton.setText(isDarkMode ? "☀︎ Light Mode" : "☾ Dark Mode");
            applyTheme();
        });
        headerBox.setLeft(titleBox);
        headerBox.setRight(themeButton);

        HBox inputBox = new HBox(15);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.setPadding(new Insets(25));
        inputBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #6ba9e3; -fx-border-radius: 12;");
        
        taskInput = new TextField();
        taskInput.setPromptText("What needs to be done?");
        taskInput.setPrefWidth(320);
        taskInput.setStyle("-fx-background-radius: 8; -fx-padding: 10;");

        priorityInput = new ComboBox<>();
        priorityInput.getItems().addAll("Low", "Medium", "High");
        priorityInput.setValue("Medium");
        priorityInput.setStyle("-fx-background-radius: 8;");

        datePicker = new DatePicker();
        datePicker.setPromptText("Due Date");
        datePicker.setPrefWidth(160);

        // --- RESTRICT DATE PICKER TO CURRENT DATE OR LATER ---
        final Callback<DatePicker, DateCell> dayCellFactory = (final DatePicker datePicker1) -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #99abd7;");
                }
            }
        };
        datePicker.setDayCellFactory(dayCellFactory);

        addOrUpdateButton = new Button("➕ Add Task");
        addOrUpdateButton.setStyle("-fx-background-color: #097d57; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 8; -fx-cursor: hand;");
        
        Button cancelEditBtn = new Button("✖ Cancel");
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);
        cancelEditBtn.setStyle("-fx-background-color: #a6122b; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 8;");

        inputBox.getChildren().addAll(taskInput, priorityInput, datePicker, addOrUpdateButton, cancelEditBtn);

        HBox toolBar = new HBox(20);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search tasks...");
        searchField.setPrefWidth(250);
        searchField.setStyle("-fx-background-radius: 20;");

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Sort by Priority", "Sort by Due Date", "Sort by Name");
        sortBox.setValue("Sort by Priority");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        taskProgressBar = new ProgressBar(0);
        taskProgressBar.setPrefWidth(150);
        progressLabel = new Label("0% Completed");
        
        toolBar.getChildren().addAll(searchField, sortBox, spacer, new Label("Progress: "), taskProgressBar, progressLabel);
        topContainer.getChildren().addAll(headerBox, inputBox, toolBar);
        root.setTop(topContainer);

        // ==========================================
        // CENTER LAYOUT
        // ==========================================
        taskTable = new TableView<>();
        masterTaskList = FXCollections.observableArrayList();
        taskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Task, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Task, String> priorityCol = new TableColumn<>("PRIORITY");
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));

        TableColumn<Task, String> nameCol = new TableColumn<>("TASK DESCRIPTION");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(300);

        TableColumn<Task, String> dateCol = new TableColumn<>("DUE DATE");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        TableColumn<Task, Void> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setPrefWidth(180);
        actionCol.setCellFactory(param -> new TableCell<Task, Void>() {
            private final CheckBox completeCheck = new CheckBox();
            private final Button editBtn = new Button("✎");
            private final Button deleteBtn = new Button("🗑");
            private final HBox pane = new HBox(10, completeCheck, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #db8d11; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #ac1d0d; -fx-text-fill: white; -fx-cursor: hand;");

                completeCheck.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    task.setStatus(completeCheck.isSelected() ? "Completed" : "Pending");
                    getTableView().refresh();
                    updateProgress();
                });

                editBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    taskInput.setText(task.getName());
                    priorityInput.setValue(task.getPriority());
                    datePicker.setValue(LocalDate.parse(task.getDueDate()));
                    currentlyEditingTask = task;
                    addOrUpdateButton.setText("✔ Save");
                    addOrUpdateButton.setStyle("-fx-background-color: #c27e08; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 8;");
                    cancelEditBtn.setVisible(true);
                    cancelEditBtn.setManaged(true);
                });

                deleteBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Deletion");
                    confirmAlert.setHeaderText("Delete Task");
                    confirmAlert.setContentText("Are you sure you want to delete: \"" + task.getName() + "\"?");

                    Optional<ButtonType> result = confirmAlert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        masterTaskList.remove(task);
                        updateProgress();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Task task = getTableView().getItems().get(getIndex());
                    completeCheck.setSelected("Completed".equals(task.getStatus()));
                    setGraphic(pane);
                }
            }
        });

        taskTable.getColumns().addAll(statusCol, priorityCol, nameCol, dateCol, actionCol);

        FilteredList<Task> filteredData = new FilteredList<>(masterTaskList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(task -> {
                if (newVal == null || newVal.isEmpty()) return true;
                return task.getName().toLowerCase().contains(newVal.toLowerCase());
            });
        });

        SortedList<Task> sortedData = new SortedList<>(filteredData);
        sortBox.setOnAction(e -> {
            String criteria = sortBox.getValue();
            sortedData.setComparator((t1, t2) -> {
                if ("Sort by Priority".equals(criteria)) {
                    int p1 = getWeight(t1.getPriority());
                    int p2 = getWeight(t2.getPriority());
                    return Integer.compare(p2, p1); 
                } else if ("Sort by Due Date".equals(criteria)) {
                    return t1.getDueDate().compareTo(t2.getDueDate());
                } else {
                    return t1.getName().compareToIgnoreCase(t2.getName());
                }
            });
        });

        taskTable.setItems(sortedData);
        root.setCenter(taskTable);

        // ==========================================
        // BOTTOM LAYOUT
        // ==========================================
        HBox bottomBox = new HBox();
        bottomBox.setPadding(new Insets(20, 0, 0, 0));
        bottomBox.setAlignment(Pos.CENTER);
        
        clearAllBtn = new Button("🗑 Clear All Tasks");
        clearAllBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #b3102b; -fx-border-radius: 8; -fx-text-fill: #c10c2a; -fx-padding: 10 25; -fx-cursor: hand;");
        
        clearAllBtn.setOnAction(e -> {
            if (masterTaskList.isEmpty()) {
                showAlert("There are no tasks to clear.", Alert.AlertType.INFORMATION);
                return;
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Clear All");
            alert.setHeaderText("Clear All Tasks");
            alert.setContentText("This will permanently delete all tasks. Do you want to proceed?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                masterTaskList.clear();
                updateProgress();
            }
        });
        
        bottomBox.getChildren().add(clearAllBtn);
        root.setBottom(bottomBox);

        // ==========================================
        // ADD / UPDATE LOGIC
        // ==========================================
        addOrUpdateButton.setOnAction(e -> {
            String name = taskInput.getText().trim();
            LocalDate date = datePicker.getValue();

            if (name.isEmpty() || date == null) {
                showAlert("Please enter a task name and date.", Alert.AlertType.WARNING);
                return;
            }

            // Logic check: Just in case they manually type a date (if editable)
            if (date.isBefore(LocalDate.now())) {
                showAlert("You cannot schedule a task for a past date.", Alert.AlertType.ERROR);
                return;
            }

            if (currentlyEditingTask == null) {
                masterTaskList.add(new Task(name, priorityInput.getValue(), date.toString()));
            } else {
                currentlyEditingTask.setName(name);
                currentlyEditingTask.setPriority(priorityInput.getValue());
                currentlyEditingTask.setDueDate(date.toString());
                taskTable.refresh();
                resetForm(cancelEditBtn);
            }
            
            taskInput.clear();
            datePicker.setValue(null);
            updateProgress(); 
        });

        cancelEditBtn.setOnAction(e -> resetForm(cancelEditBtn));

        Scene scene = new Scene(root, 1050, 750);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private int getWeight(String p) {
        return switch (p) {
            case "High" -> 3;
            case "Medium" -> 2;
            default -> 1;
        };
    }

    private void resetForm(Button cancelBtn) {
        currentlyEditingTask = null;
        taskInput.clear();
        datePicker.setValue(null);
        addOrUpdateButton.setText("➕ Add Task");
        addOrUpdateButton.setStyle("-fx-background-color: #0b8059; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 8;");
        cancelBtn.setVisible(false);
        cancelBtn.setManaged(false);
    }

    private void showAlert(String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("System Notification");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void updateProgress() {
        if (masterTaskList.isEmpty()) {
            taskProgressBar.setProgress(0);
            progressLabel.setText("0% Completed");
            return;
        }
        long completed = masterTaskList.stream().filter(t -> "Completed".equals(t.getStatus())).count();
        double progress = (double) completed / masterTaskList.size();
        taskProgressBar.setProgress(progress);
        progressLabel.setText(Math.round(progress * 100) + "% Completed");
    }

    private void applyTheme() {
        if (isDarkMode) {
            root.setStyle("-fx-base: #444472; -fx-background-color: #1f1f3f;");
        } else {
            root.setStyle("-fx-base: #c0daf5; -fx-background-color: #cadbeb;");
        }
    }
}