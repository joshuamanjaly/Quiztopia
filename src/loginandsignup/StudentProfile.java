import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import javax.imageio.ImageIO;

public class StudentProfile extends JFrame {
    private JLabel profilePicLabel;
    private JButton uploadButton, saveButton;
    private JTextField idField, nameField, emailField;
    private JFileChooser fileChooser;
    private File selectedFile = null;
    private Connection conn;
    private JPanel rightPanel;

    public StudentProfile() {
        setTitle("Student Profile");
        setSize(800, 600);
        setLayout(new BorderLayout());

        // ==== Left Panel ====
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(new Color(0, 51, 102));
        leftPanel.setPreferredSize(new Dimension(200, getHeight()));
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("QUIZTOPIA");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        leftPanel.add(titleLabel);

        profilePicLabel = new JLabel();
        profilePicLabel.setPreferredSize(new Dimension(150, 150));
        profilePicLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        profilePicLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profilePicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profilePicLabel.setOpaque(true);
        profilePicLabel.setBackground(Color.WHITE);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        leftPanel.add(profilePicLabel);

        uploadButton = new JButton("Upload Picture");
        uploadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        uploadButton.addActionListener(e -> chooseProfilePicture());
        leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        leftPanel.add(uploadButton);

        add(leftPanel, BorderLayout.WEST);

        // ==== Right Panel ====
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        idField = createField("ID:", false);
        nameField = createField("Name:", true);
        emailField = createField("Email:", true);

        saveButton = new JButton("Save Changes");
        saveButton.setFont(new Font("Arial", Font.BOLD, 16));
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.addActionListener(e -> saveChanges());

        rightPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        rightPanel.add(saveButton);

        add(rightPanel, BorderLayout.CENTER);

        // DB fetch
        fetchDataFromDatabase();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private JTextField createField(String label, boolean editable) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setMaximumSize(new Dimension(400, 40));
        panel.setBackground(Color.WHITE);

        JLabel jLabel = new JLabel(label);
        jLabel.setFont(new Font("Arial", Font.BOLD, 16));
        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 16));
        textField.setEditable(editable);

        panel.add(jLabel, BorderLayout.WEST);
        panel.add(textField, BorderLayout.CENTER);

        rightPanel.add(panel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        return textField;
    }

    private void fetchDataFromDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/quiztopia", "root", "");
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            stmt.setInt(1, 1); // Use dynamic ID in real case
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                idField.setText(String.valueOf(rs.getInt("id")));
                nameField.setText(rs.getString("name"));
                emailField.setText(rs.getString("email"));

                // Load and display profile photo
                Blob photoBlob = rs.getBlob("photo");
                if (photoBlob != null) {
                    InputStream in = photoBlob.getBinaryStream();
                    Image img = ImageIO.read(in).getScaledInstance(150, 150, Image.SCALE_SMOOTH);
                    profilePicLabel.setIcon(new ImageIcon(img));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching data: " + e.getMessage());
        }
    }

    private void chooseProfilePicture() {
        fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            ImageIcon profileImage = new ImageIcon(
                    new ImageIcon(selectedFile.getAbsolutePath()).getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH));
            profilePicLabel.setIcon(profileImage);
        }
    }

    private void saveChanges() {
        try {
            String name = nameField.getText();
            String email = emailField.getText();
            int id = Integer.parseInt(idField.getText());

            PreparedStatement stmt;
            if (selectedFile != null) {
                FileInputStream fis = new FileInputStream(selectedFile);
                stmt = conn.prepareStatement("UPDATE users SET name = ?, email = ?, photo = ? WHERE id = ?");
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setBinaryStream(3, fis, (int) selectedFile.length());
                stmt.setInt(4, id);
            } else {
                stmt = conn.prepareStatement("UPDATE users SET name = ?, email = ? WHERE id = ?");
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setInt(3, id);
            }

            int rows = stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, rows + " record(s) updated.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving changes: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StudentProfile::new);
    }
}
