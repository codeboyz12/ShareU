import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// ==========================================
// PART 1: MODELS (Structure & OOP)
// ==========================================

enum ItemStatus { AVAILABLE, BORROWED, MAINTENANCE }
enum RequestStatus { PENDING, APPROVED, REJECTED }

abstract class User {
    protected String id;
    protected String name;
    protected String password; 

    public User(String id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public boolean checkPassword(String inputInfo) { return this.password.equals(inputInfo); }
}

class Student extends User {
    private String phone;
    private String email;
    
    public Student(String id, String name, String password, String phone, String email) {
        super(id, name, password);
        this.phone = phone;
        this.email = email;
    }
}

class Admin extends User {
    public Admin(String id, String name, String password) {
        super(id, name, password);
    }
    
    public int calculateFine(BorrowRecord record, LocalDate returnDate) {
        if (returnDate.isAfter(record.getDueDate())) {
            long daysLate = ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
            return (int) (daysLate * 100); 
        }
        return 0;
    }
}

class Item {
    private String itemId;
    private String name;
    private ItemStatus status;

    public Item(String itemId, String name) {
        this.itemId = itemId;
        this.name = name;
        this.status = ItemStatus.AVAILABLE;
    }

    public String getName() { return name; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public String getItemId() { return itemId; }
    
    @Override
    public String toString() { return name; }
}

class BorrowRequest {
    private Student student;
    private Item item;
    private RequestStatus status;

    public BorrowRequest(Student student, Item item) {
        this.student = student;
        this.item = item;
        this.status = RequestStatus.PENDING;
    }

    public Student getStudent() { return student; }
    public Item getItem() { return item; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
}

class BorrowRecord {
    private Student student;
    private Item item;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    public BorrowRecord(Student student, Item item) {
        this.student = student;
        this.item = item;
        this.borrowDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(7); 
    }
    
    public LocalDate getDueDate() { return dueDate; }
    public Item getItem() { return item; }
    public Student getStudent() { return student; }
    public void setReturnDate(LocalDate date) { this.returnDate = date; }
    public LocalDate getReturnDate() { return returnDate; }
}

// ==========================================
// PART 2: DATA STORE
// ==========================================
class DataStore {
    public static List<Item> items = new ArrayList<>();
    public static List<Student> students = new ArrayList<>();
    public static List<Admin> admins = new ArrayList<>();
    public static List<BorrowRequest> requests = new ArrayList<>();
    public static List<BorrowRecord> records = new ArrayList<>();

    static {
        items.add(new Item("I001", "Projector Sony"));
        items.add(new Item("I002", "MacBook Pro M2"));
        items.add(new Item("I003", "Camera Canon EOS"));
        items.add(new Item("I004", "Microphone Shure"));

        // Mock Data: Student (User: S66001, Pass: 1234)
        students.add(new Student("S66001", "Test User", "1234", "0812345678", "test@mail.com"));

        // Mock Data: Admin (User: admin, Pass: admin)
        admins.add(new Admin("admin", "Master Admin", "admin"));
    }
}

// ==========================================
// PART 3: GUI (Java Swing)
// ==========================================
public class SmartBorrowSystem extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private Student currentUser; 

    // GUI Components declared here for access
    private DefaultTableModel itemModel;
    private DefaultTableModel reqModel;
    private DefaultTableModel recModel;

    public SmartBorrowSystem() {
        setTitle("University Borrowing System (Final Fixed)");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel loginPanel = createLoginPanel();
        JPanel registerPanel = createRegisterPanel();
        JPanel studentPanel = createStudentPanel();
        JPanel adminPanel = createAdminPanel();

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");
        mainPanel.add(studentPanel, "STUDENT");
        mainPanel.add(adminPanel, "ADMIN");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
    }

    // --- 1. Login Panel ---
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Smart Borrow System");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField tfUser = new JTextField(20);
        JPasswordField pfPass = new JPasswordField(20);
        
        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Register New Student");
        JButton btnAdminLogin = new JButton("Login as Admin");
        
        btnLogin.setBackground(new Color(100, 149, 237));
        btnLogin.setForeground(Color.WHITE);
        btnRegister.setBackground(new Color(60, 179, 113)); 
        btnRegister.setForeground(Color.WHITE);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; panel.add(title, gbc);
        gbc.gridy = 1; gbc.gridwidth = 1; panel.add(new JLabel("ID / Username:"), gbc);
        gbc.gridx = 1; panel.add(tfUser, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(pfPass, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; panel.add(btnLogin, gbc);
        gbc.gridy = 4; panel.add(btnRegister, gbc);
        gbc.gridy = 5; panel.add(new JSeparator(), gbc);
        gbc.gridy = 6; panel.add(btnAdminLogin, gbc);

        // --- LOGIC LOGIN ---
        btnLogin.addActionListener(e -> {
            String user = tfUser.getText();
            String pass = new String(pfPass.getPassword());
            
            // Admin Login Check
            if (user.equals("admin") && pass.equals("admin")) {
                refreshAdminView(); // !!! FIX: Refresh data immediately !!!
                cardLayout.show(mainPanel, "ADMIN");
                return;
            }

            // Student Login Check
            boolean found = false;
            for (Student s : DataStore.students) {
                if (s.getId().equals(user) && s.checkPassword(pass)) {
                    currentUser = s;
                    found = true;
                    break;
                }
            }

            if (found) {
                refreshStudentView(); // !!! FIX: Refresh student view !!!
                cardLayout.show(mainPanel, "STUDENT");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Username or Password", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRegister.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        btnAdminLogin.addActionListener(e -> {
             tfUser.setText("admin"); pfPass.setText(""); pfPass.requestFocus();
        });

        return panel;
    }

    // --- 1.5 Register Panel ---
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Student Registration");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));

        JTextField tfId = new JTextField(20);
        JTextField tfName = new JTextField(20);
        JTextField tfPhone = new JTextField(20);
        JTextField tfEmail = new JTextField(20);
        JPasswordField pfPass = new JPasswordField(20);
        JButton btnSave = new JButton("Sign Up");
        JButton btnBack = new JButton("Back to Login");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; panel.add(lblTitle, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Student ID:"), gbc); gbc.gridx = 1; panel.add(tfId, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Name:"), gbc); gbc.gridx = 1; panel.add(tfName, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Phone:"), gbc); gbc.gridx = 1; panel.add(tfPhone, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Email:"), gbc); gbc.gridx = 1; panel.add(tfEmail, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Password:"), gbc); gbc.gridx = 1; panel.add(pfPass, gbc);
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2; panel.add(btnSave, gbc);
        gbc.gridy++; panel.add(btnBack, gbc);

        btnSave.addActionListener(e -> {
            String id = tfId.getText();
            String name = tfName.getText();
            String pass = new String(pfPass.getPassword());

            if (id.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill required fields.");
                return;
            }
            
            for (Student s : DataStore.students) {
                if (s.getId().equals(id)) {
                    JOptionPane.showMessageDialog(this, "ID already exists!");
                    return;
                }
            }

            Student newStudent = new Student(id, name, pass, tfPhone.getText(), tfEmail.getText());
            DataStore.students.add(newStudent);
            JOptionPane.showMessageDialog(this, "Registration Successful! Please Login.");
            tfId.setText(""); tfName.setText(""); pfPass.setText("");
            cardLayout.show(mainPanel, "LOGIN");
        });

        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        return panel;
    }

    // --- 2. Student Panel ---
    private JTable itemTable;
    
    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel(" Student Menu: Borrow Items"), BorderLayout.WEST);
        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            currentUser = null;
            cardLayout.show(mainPanel, "LOGIN");
        });
        header.add(btnLogout, BorderLayout.EAST);
        
        String[] cols = {"ID", "Name", "Status"};
        itemModel = new DefaultTableModel(cols, 0);
        itemTable = new JTable(itemModel);
        
        JButton btnBorrow = new JButton("Request Borrow");
        btnBorrow.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        btnBorrow.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if (row != -1) {
                // ตรวจสอบจากตารางที่เห็น (สิ่งที่ตาเห็น)
                String currentDisplayStatus = itemModel.getValueAt(row, 2).toString();
                
                if ("AVAILABLE".equals(currentDisplayStatus)) {
                    Item item = DataStore.items.get(row);
                    
                    // สร้าง Request
                    DataStore.requests.add(new BorrowRequest(currentUser, item));
                    
                    JOptionPane.showMessageDialog(this, "Sent request for " + item.getName());
                    refreshStudentView(); // !!! FIX: Update table immediately to show WAITING
                } else {
                    JOptionPane.showMessageDialog(this, "Cannot borrow. Status: " + currentDisplayStatus);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Select an item first.");
            }
        });

        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        panel.add(btnBorrow, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshStudentView() {
        itemModel.setRowCount(0);
        for (Item i : DataStore.items) {
            String statusDisplay = i.getStatus().toString();
            
            // !!! FIX: Check if this item is in Pending Request !!!
            for (BorrowRequest req : DataStore.requests) {
                if (req.getItem() == i && req.getStatus() == RequestStatus.PENDING) {
                    statusDisplay = "WAITING APPROVAL"; // เปลี่ยนคำที่โชว์ในตาราง
                    break;
                }
            }
            
            itemModel.addRow(new Object[]{i.getItemId(), i.getName(), statusDisplay});
        }
    }

    // --- 3. Admin Panel ---
    private JTable reqTable, recTable;

    private JPanel createAdminPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel approvePanel = new JPanel(new BorderLayout());
        String[] reqCols = {"Student", "Item", "Status"};
        reqModel = new DefaultTableModel(reqCols, 0);
        reqTable = new JTable(reqModel);
        
        JButton btnApprove = new JButton("Approve Selected");
        btnApprove.setBackground(new Color(144, 238, 144));
        btnApprove.addActionListener(e -> {
            int row = reqTable.getSelectedRow();
            if (row != -1) {
                // หา Object Request จริงจาก List โดยเทียบ Student Name & Item Name (Simple matching)
                // วิธีที่ดีกว่าคือเก็บ hidden ID แต่เพื่อให้ง่ายเราใช้ index
                // แต่เนื่องจากตารางอาจไม่ตรงกับ List เป๊ะๆ เราจะวนหา
                
                String sName = reqModel.getValueAt(row, 0).toString();
                String iName = reqModel.getValueAt(row, 1).toString();
                
                BorrowRequest targetReq = null;
                for(BorrowRequest r : DataStore.requests) {
                    if(r.getStudent().getName().equals(sName) && r.getItem().getName().equals(iName)) {
                        targetReq = r;
                        break;
                    }
                }

                if (targetReq != null && targetReq.getStatus() == RequestStatus.PENDING) {
                    targetReq.setStatus(RequestStatus.APPROVED);
                    targetReq.getItem().setStatus(ItemStatus.BORROWED); 
                    DataStore.records.add(new BorrowRecord(targetReq.getStudent(), targetReq.getItem()));
                    DataStore.requests.remove(targetReq); // ลบออกจากรายการขอ
                    
                    refreshAdminView(); // Refresh
                    JOptionPane.showMessageDialog(this, "Approved: " + targetReq.getItem().getName());
                }
            }
        });
        approvePanel.add(new JScrollPane(reqTable), BorderLayout.CENTER);
        approvePanel.add(btnApprove, BorderLayout.SOUTH);

        JPanel returnPanel = new JPanel(new BorderLayout());
        String[] recCols = {"Student", "Item", "Due Date", "Status"};
        recModel = new DefaultTableModel(recCols, 0);
        recTable = new JTable(recModel);

        JButton btnReturn = new JButton("Process Return");
        btnReturn.setBackground(new Color(255, 182, 193));
        btnReturn.addActionListener(e -> {
            int row = recTable.getSelectedRow();
            if (row != -1) {
                BorrowRecord rec = DataStore.records.get(row);
                String[] options = {"Return Today", "Return Late (3 Days)"};
                int choice = JOptionPane.showOptionDialog(this, "Return Date?", "Return",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                LocalDate returnDate = LocalDate.now();
                if (choice == 1) returnDate = rec.getDueDate().plusDays(3);

                rec.setReturnDate(returnDate);
                rec.getItem().setStatus(ItemStatus.AVAILABLE); 

                Admin admin = DataStore.admins.get(0);
                int fine = admin.calculateFine(rec, returnDate);

                String msg = "Returned.\n";
                if (fine > 0) msg += "❌ Late! Fine: " + fine + " THB";
                else msg += "✅ On time.";
                
                DataStore.records.remove(rec); 
                refreshAdminView();
                JOptionPane.showMessageDialog(this, msg);
            }
        });
        returnPanel.add(new JScrollPane(recTable), BorderLayout.CENTER);
        returnPanel.add(btnReturn, BorderLayout.SOUTH);

        tabbedPane.addTab("Pending Requests", approvePanel);
        tabbedPane.addTab("Active Borrows", returnPanel);

        JPanel mainAdminPanel = new JPanel(new BorderLayout());
        JButton btnLogout = new JButton("Logout Admin");
        btnLogout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        
        mainAdminPanel.add(btnLogout, BorderLayout.NORTH);
        mainAdminPanel.add(tabbedPane, BorderLayout.CENTER);
        
        return mainAdminPanel;
    }

    private void refreshAdminView() {
        // Update Pending Table
        reqModel.setRowCount(0);
        for (BorrowRequest r : DataStore.requests) {
            reqModel.addRow(new Object[]{r.getStudent().getName(), r.getItem().getName(), r.getStatus()});
        }
        
        // Update Borrowed Table
        recModel.setRowCount(0);
        for (BorrowRecord r : DataStore.records) {
            recModel.addRow(new Object[]{r.getStudent().getName(), r.getItem().getName(), r.getDueDate(), "Active"});
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SmartBorrowSystem().setVisible(true);
        });
    }
}