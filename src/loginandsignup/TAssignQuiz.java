package loginandsignup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TAssignQuiz extends JFrame {
    private JTextField questionField, option1Field, option2Field, option3Field, option4Field;
    private ButtonGroup correctAnswerGroup;
    private JRadioButton[] correctAnswerButtons;
    private List<QuizQuestion> quizQuestions;
    private JLabel questionCountLabel;
    private int quizId;
    
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/quiztopia";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public TAssignQuiz() {
        // Generate a new quiz ID
        generateQuizId();
        
        setTitle("QUIZTOPIA");
        setSize(800, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        quizQuestions = new ArrayList<>();

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setPreferredSize(new Dimension(800, 60));
        headerPanel.setBackground(new Color(0, 0, 102)); // Dark blue
        JLabel headerLabel = new JLabel("QUIZTOPIA");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 32));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Main content
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Question Count
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        questionCountLabel = new JLabel("Question 1");
        questionCountLabel.setFont(new Font("Arial", Font.BOLD, 16));
        contentPanel.add(questionCountLabel, gbc);

        // Question
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel questionLabel = new JLabel("Enter Question:");
        questionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        contentPanel.add(questionLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        questionField = new JTextField(50);
        questionField.setPreferredSize(new Dimension(500, 30));
        contentPanel.add(questionField, gbc);

        // Options and Correct Answer buttons
        correctAnswerGroup = new ButtonGroup();
        correctAnswerButtons = new JRadioButton[4];

        for (int i = 1; i <= 4; i++) {
            gbc.gridx = 0;
            gbc.gridy = i + 1;
            gbc.gridwidth = 1;
            JLabel optionLabel = new JLabel("Option " + i + ":");
            optionLabel.setFont(new Font("Arial", Font.BOLD, 14));
            contentPanel.add(optionLabel, gbc);

            gbc.gridx = 1;
            gbc.gridy = i + 1;
            gbc.gridwidth = 1;
            JTextField optionField = new JTextField(40);
            optionField.setPreferredSize(new Dimension(400, 30));
            contentPanel.add(optionField, gbc);

            gbc.gridx = 2;
            gbc.gridy = i + 1;
            gbc.gridwidth = 1;
            JRadioButton correctButton = new JRadioButton("Correct");
            correctAnswerGroup.add(correctButton);
            correctAnswerButtons[i-1] = correctButton;
            contentPanel.add(correctButton, gbc);

            switch (i) {
                case 1: option1Field = optionField; break;
                case 2: option2Field = optionField; break;
                case 3: option3Field = optionField; break;
                case 4: option4Field = optionField; break;
            }
        }

        add(contentPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        JButton addQuestionButton = new JButton("Add Question");
        addQuestionButton.setPreferredSize(new Dimension(150, 30));
        addQuestionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addQuestion();
            }
        });
        buttonPanel.add(addQuestionButton);

        JButton finishQuizButton = new JButton("Finish Quiz");
        finishQuizButton.setPreferredSize(new Dimension(150, 30));
        finishQuizButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finishQuiz();
            }
        });
        buttonPanel.add(finishQuizButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void generateQuizId() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "SELECT MAX(quiz_id) + 1 AS next_id FROM questions";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                if (rs.next()) {
                    quizId = rs.getInt("next_id");
                    if (rs.wasNull()) {
                        quizId = 1;
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error connecting to database: " + e.getMessage(),
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
            quizId = 1;
        }
    }

    private void addQuestion() {
        if (validateInput()) {
            if (saveQuestionToDB()) {
                QuizQuestion question = new QuizQuestion(
                    questionField.getText(),
                    new String[]{
                        option1Field.getText(),
                        option2Field.getText(),
                        option3Field.getText(),
                        option4Field.getText()
                    },
                    getCorrectAnswerIndex()
                );
                quizQuestions.add(question);
                clearFields();
                updateQuestionCount();
                JOptionPane.showMessageDialog(this, "Question added successfully!");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please fill all fields and select a correct answer.", 
                                        "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean saveQuestionToDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "INSERT INTO questions (quiz_id, question_text, option1, option2, option3, option4, correct_answer) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)";
                             
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, quizId);
                pstmt.setString(2, questionField.getText().trim());
                pstmt.setString(3, option1Field.getText().trim());
                pstmt.setString(4, option2Field.getText().trim());
                pstmt.setString(5, option3Field.getText().trim());
                pstmt.setString(6, option4Field.getText().trim());
                pstmt.setInt(7, getCorrectAnswerIndex() + 1);
                
                pstmt.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving question: " + e.getMessage(),
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void finishQuiz() {
    if (validateInput()) {
        addQuestion();  // Add the current question before finishing
    }
    
    if (quizQuestions.size() > 0) {
        JOptionPane.showMessageDialog(this, "Quiz submitted successfully!", 
                                      "Submission Confirmation", JOptionPane.INFORMATION_MESSAGE);
        this.dispose();
    } else {
        JOptionPane.showMessageDialog(this, "Please add at least one question to the quiz.",
                                      "Warning", JOptionPane.WARNING_MESSAGE);
    }
}


    private boolean validateInput() {
        return !questionField.getText().trim().isEmpty() &&
               !option1Field.getText().trim().isEmpty() &&
               !option2Field.getText().trim().isEmpty() &&
               !option3Field.getText().trim().isEmpty() &&
               !option4Field.getText().trim().isEmpty() &&
               getCorrectAnswerIndex() != -1;
    }

    private int getCorrectAnswerIndex() {
        for (int i = 0; i < correctAnswerButtons.length; i++) {
            if (correctAnswerButtons[i].isSelected()) {
                return i;
            }
        }
        return -1;
    }

    private void clearFields() {
        questionField.setText("");
        option1Field.setText("");
        option2Field.setText("");
        option3Field.setText("");
        option4Field.setText("");
        correctAnswerGroup.clearSelection();
    }

    private void updateQuestionCount() {
        questionCountLabel.setText("Question " + (quizQuestions.size() + 1));
    }

    private class QuizQuestion {
        String question;
        String[] options;
        int correctAnswer;

        QuizQuestion(String question, String[] options, int correctAnswer) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }
    }

    public static void main(String[] args) {
        createDatabaseTables();
        
        SwingUtilities.invokeLater(() -> {
            new TAssignQuiz().setVisible(true);
        });
    }

    private static void createDatabaseTables() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                Statement stmt = conn.createStatement();
                
                // Create questions table
                String createQuestionsTable = "CREATE TABLE IF NOT EXISTS questions (" +
                    "question_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "quiz_id INT," +
                    "question_text TEXT," +
                    "option1 TEXT," +
                    "option2 TEXT," +
                    "option3 TEXT," +
                    "option4 TEXT," +
                    "correct_answer INT" +
                    ")";
                stmt.executeUpdate(createQuestionsTable);
                
                // Create quiz_results table
                String createResultsTable = "CREATE TABLE IF NOT EXISTS quiz_results (" +
                    "result_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "quiz_id INT," +
                    "user_id INT," +
                    "score INT," +
                    "total_questions INT," +
                    "completion_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
                stmt.executeUpdate(createResultsTable);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error creating database tables: " + e.getMessage(),
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}