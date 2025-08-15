package loginandsignup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttemptQuizDB extends JFrame {
    private List<QuizQuestion> quizQuestions;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int quizId;
    
    private JLabel questionLabel;
    private JRadioButton[] optionButtons;
    private ButtonGroup optionGroup;
    private JButton nextButton;
    private JPanel quizPanel;
    private JLabel timerLabel;
    private Timer quizTimer;
    private int timeRemaining;
    
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/quiztopia";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public AttemptQuizDB(int quizId) {
        this.quizId = quizId;
        this.quizQuestions = new ArrayList<>();
        
        // Load questions from database
        loadQuestionsFromDB();
        
        // Initialize UI
        initializeUI();
        
        // Start timer (30 minutes)
        startTimer(30 * 60);
    }

    private void loadQuestionsFromDB() {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String query = "SELECT q.question_text, q.option1, q.option2, q.option3, q.option4, q.correct_answer " +
                           "FROM questions q " +
                           "WHERE q.quiz_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, quizId);
            ResultSet rs = pstmt.executeQuery();

            // Check if ResultSet has any rows
            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                String questionText = rs.getString("question_text"); 
                String[] options = {
                    rs.getString("option1"),
                    rs.getString("option2"),
                    rs.getString("option3"),
                    rs.getString("option4")
                };
                int correctAnswer = rs.getInt("correct_answer") - 1; // Adjusting for 0-based index
                quizQuestions.add(new QuizQuestion(questionText, options, correctAnswer));
            }
            
            if (!hasRows) {
                System.out.println("No questions found for quiz_id: " + quizId);
            }
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error loading questions: " + e.getMessage(),
                                      "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}


    private void initializeUI() {
        setTitle("QUIZTOPIA - Attempt Quiz");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(800, 60));
        headerPanel.setBackground(new Color(0, 0, 102));

        JLabel headerLabel = new JLabel("QUIZTOPIA", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 32));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Timer Label
        timerLabel = new JLabel("Time Remaining: 30:00", SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        headerPanel.add(timerLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Quiz Panel
        quizPanel = new JPanel();
        quizPanel.setLayout(new BoxLayout(quizPanel, BoxLayout.Y_AXIS));
        quizPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JScrollPane(quizPanel), BorderLayout.CENTER);

        // Question
        questionLabel = new JLabel();
        questionLabel.setFont(new Font("Arial", Font.BOLD, 16));
        questionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quizPanel.add(questionLabel);
        quizPanel.add(Box.createVerticalStrut(20));

        // Options
        optionGroup = new ButtonGroup();
        optionButtons = new JRadioButton[4];
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 14));
            optionButtons[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            optionGroup.add(optionButtons[i]);
            quizPanel.add(optionButtons[i]);
            quizPanel.add(Box.createVerticalStrut(10));
        }

        // Navigation Panel
        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nextButton = new JButton("Next");
        nextButton.setFont(new Font("Arial", Font.BOLD, 14));
        nextButton.addActionListener(e -> handleNextButton());
        navigationPanel.add(nextButton);
        add(navigationPanel, BorderLayout.SOUTH);

        if (!quizQuestions.isEmpty()) {
            displayQuestion();
        } else {
            JOptionPane.showMessageDialog(this, "No questions available for this quiz.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startTimer(int seconds) {
        timeRemaining = seconds;
        quizTimer = new Timer(1000, e -> {
            timeRemaining--;
            updateTimerLabel();
            if (timeRemaining <= 0) {
                ((Timer)e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "Time's up!");
                submitQuiz();
            }
        });
        quizTimer.start();
    }

    private void updateTimerLabel() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("Time Remaining: %02d:%02d", minutes, seconds));
    }

    private void handleNextButton() {
        if (!isAnswerSelected()) {
            JOptionPane.showMessageDialog(this, "Please select an answer before proceeding.",
                                        "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        checkAnswer();
        if (currentQuestionIndex < quizQuestions.size() - 1) {
            currentQuestionIndex++;
            displayQuestion();
        } else {
            submitQuiz();
        }
    }

    private boolean isAnswerSelected() {
        for (JRadioButton button : optionButtons) {
            if (button.isSelected()) return true;
        }
        return false;
    }

    private void displayQuestion() {
        QuizQuestion currentQuestion = quizQuestions.get(currentQuestionIndex);
        questionLabel.setText((currentQuestionIndex + 1) + ". " + currentQuestion.question);
        
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(currentQuestion.options[i]);
        }
        optionGroup.clearSelection();

        nextButton.setText(currentQuestionIndex == quizQuestions.size() - 1 ? "Submit" : "Next");
    }

    private void checkAnswer() {
        for (int i = 0; i < 4; i++) {
            if (optionButtons[i].isSelected() && 
                i == quizQuestions.get(currentQuestionIndex).correctAnswer) {
                score++;
                break;
            }
        }
    }

    private void submitQuiz() {
        if (quizTimer != null) {
            quizTimer.stop();
        }
        
        saveQuizResult();
        showResult();
    }

    private void saveQuizResult() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String query = "INSERT INTO quiz_results (quiz_id, user_id, score, total_questions) VALUES (?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, quizId);
                pstmt.setInt(2, getCurrentUserId()); // You'll need to implement this method
                pstmt.setInt(3, score);
                pstmt.setInt(4, quizQuestions.size());
                pstmt.executeUpdate();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving results: " + e.getMessage(),
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getCurrentUserId() {
        // Implement this method to return the current user's ID
        // This could be stored in a session or passed to the constructor
        return 1; // Placeholder
    }

    private void showResult() {
        quizPanel.removeAll();
        int totalQuestions = quizQuestions.size();
        double percentage = (double) score / totalQuestions * 100;

        // Create result panel
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
        resultPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Add result components
        addResultLabel(resultPanel, "Quiz Completed!", new Font("Arial", Font.BOLD, 24));
        resultPanel.add(Box.createVerticalStrut(20));
        
        addResultLabel(resultPanel, "Your Score: " + score + " out of " + totalQuestions, 
                      new Font("Arial", Font.BOLD, 20));
        addResultLabel(resultPanel, String.format("Percentage: %.2f%%", percentage), 
                      new Font("Arial", Font.BOLD, 20));
        addResultLabel(resultPanel, "Remarks: " + getRemarks(percentage), 
                      new Font("Arial", Font.BOLD, 20));

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        
        JButton reviewButton = new JButton("Review Answers");
        reviewButton.setFont(new Font("Arial", Font.BOLD, 14));
        reviewButton.addActionListener(e -> reviewAnswers());
        
        JButton exitButton = new JButton("Exit to Main Menu");
        exitButton.setFont(new Font("Arial", Font.BOLD, 14));
        exitButton.addActionListener(e -> exitToMainMenu());
        
        buttonPanel.add(reviewButton);
        buttonPanel.add(exitButton);
        resultPanel.add(buttonPanel);

        quizPanel.add(resultPanel);
        nextButton.setVisible(false);
        revalidate();
        repaint();
    }

    private void addResultLabel(JPanel panel, String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
    }

    private String getRemarks(double percentage) {
        if (percentage >= 90) return "Excellent!";
        if (percentage >= 80) return "Very Good!";
        if (percentage >= 70) return "Good!";
        if (percentage >= 60) return "Satisfactory";
        return "Needs Improvement";
    }

    private void reviewAnswers() {
        // Implement review functionality
        // This could show a new window with all questions and correct answers
    }

    private void exitToMainMenu() {
        // Implement exit to main menu functionality
        dispose();
        // Add code to return to main menu
    }

    public static class QuizQuestion {
        String question;
        String[] options;
        int correctAnswer;

        public QuizQuestion(String question, String[] options, int correctAnswer) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
        }
    }

    public static void main(String[] args) {
        // For testing purposes
        SwingUtilities.invokeLater(() -> {
            new AttemptQuizDB(1).setVisible(true);
        });
    }
}