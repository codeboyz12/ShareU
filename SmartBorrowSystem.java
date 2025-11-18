import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// ==========================================
// 1. ENUMS & CONSTANTS (กำหนดค่าคงที่)
// ==========================================
enum UserRole { STUDENT, ADMIN }
enum ItemStatus { AVAILABLE, BORROWED, MAINTENANCE }
enum RequestType { NEW_BORROW, RENEW }
enum RequestStatus { PENDING, APPROVED, REJECTED, COMPLETED }
enum CardType { STUDENT_CARD, NATIONAL_ID }

// ==========================================
// 2. MODEL LAYER (Data Structure & OOP)
// ==========================================

// Abstract Class: User (Reusable base for Student/Admin) 
abstract class User {
    protected String id; // Username/CardID
    protected String name;
    protected String email;
    protected String phone;
    protected String password;

    public User(String id, String name, String email, String phone, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean login(String inputId, String inputPass) {
        return this.id.equals(inputId) && this.password.equals(inputPass);
    }
}

// Student Class 
class Student extends User {
    private CardType cardType;
    private int birthYear; // For National ID validation

    public Student(String id, String name, String email, String phone, String password, CardType cardType, int birthYear) {
        super(id, name, email, phone, password);
        this.cardType = cardType;
        this.birthYear = birthYear;
    }
    
    public CardType getCardType() { return cardType; }
}

// Admin Class 
class Admin extends User {
    public Admin(String username, String password) {
        super(username, "Administrator", "admin@sys.com", "-", password);
    }
}

// Item Class 
class Item {
    private String itemId;
    private String name;
    private String category;
    private ItemStatus status;

    public Item(String itemId, String name, String category) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.status = ItemStatus.AVAILABLE;
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    
    @Override
    public String toString() { return name; }
}

// BorrowRequest Class 
class BorrowRequest {
    private Student student;
    private Item item;
    private RequestType type;
    private RequestStatus status;
    private LocalDate requestDate;

    public BorrowRequest(Student student, Item item, RequestType type) {
        this.student = student;
        this.item = item;
        this.type = type;
        this.status = RequestStatus.PENDING;
        this.requestDate = LocalDate.now();
    }

    public Student getStudent() { return student; }
    public Item getItem() { return item; }
    public RequestType getType() { return type; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
}

// BorrowRecord Class 
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
        this.dueDate = LocalDate.now().plusDays(7); // ยืมได้ 7 วัน
    }

    public Student getStudent() { return student; }
    public Item getItem() { return item; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    
    public void extendDueDate(int days) {
        this.dueDate = this.dueDate.plusDays(days);
    }

    public void markReturn(LocalDate date) {
        this.returnDate = date;
    }
}

// ==========================================
// 3. SERVICE LAYER (Business Logic)
// ==========================================

// Validation Service: ตรวจสอบสิทธิ์ตามสเปค 
class ValidationService {
    public static boolean validateStudentId(String studentId) {
        // Spec: รหัส 2 ตัวแรก ต้องเป็นปีปัจจุบัน (สมมติ 68) ถอยหลังไป 3 ปี (68, 67, 66, 65)
        if (studentId.length() < 2) return false;
        try {
            int yearPrefix = Integer.parseInt(studentId.substring(0, 2));
            int currentYear = 68; // ตามสเปคระบุปีปัจจุบันคือ 68
            return yearPrefix >= (currentYear - 3) && yearPrefix <= currentYear;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean validateAge(int birthYearAD) {
        // Spec: อายุระหว่าง 18 - 22 ปี
        int currentYearAD = LocalDate.now().getYear();
        int age = currentYearAD - birthYearAD;
        return age >= 18 && age <= 22;
    }
}

// Logic การคำนวณค่าปรับ 
class FineCalculator {
    private static final int FINE_PER_DAY = 100;

    public static int calculate(LocalDate dueDate, LocalDate returnDate) {
        if (returnDate.isAfter(dueDate)) {
            long daysLate = ChronoUnit.DAYS.between(dueDate, returnDate);
            return (int) (daysLate * FINE_PER_DAY);
        }
        return 0;
    }
}

// DataStore: ทำหน้าที่แทน Database 
class DataStore {
    public static List<User> users = new ArrayList<>();
    public static List<Item> items = new ArrayList<>();
    public static List<BorrowRequest> requests = new ArrayList<>();
    public static List<BorrowRecord> records = new ArrayList<>();

    static {
        // Initial Data
        users.add(new Admin("admin", "admin")); // Admin Login 
        
        items.add(new Item("I01", "Projector Sony", "AV"));
        items.add(new Item("I02", "MacBook Pro M2", "IT"));
        items.add(new Item("I03", "Canon Camera", "AV"));
        items.add(new Item("I04", "Microphone Shure", "Audio"));
    }
}

// ==========================================
// 4. VIEW LAYER (Swing GUI)
// ==========================================

public class SmartBorrowSystem extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private User currentUser;

    public SmartBorrowSystem() {
        setTitle("University Borrowing System v1.0");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Add Panels
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRegisterPanel(), "REGISTER");
        
        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
    }

    // --- LOGIN PANEL ---
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel title = new JLabel("Smart Borrow System");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        JTextField tfUser = new JTextField(20);
        JPasswordField pfPass = new JPasswordField(20);
        JButton btnLogin = new JButton("Login");
        JButton btnReg = new JButton("Register");

        // UI Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; panel.add(title, gbc);
        gbc.gridy = 1; gbc.gridwidth = 1; panel.add(new JLabel("ID / Card ID:"), gbc);
        gbc.gridx = 1; panel.add(tfUser, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(pfPass, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; panel.add(btnLogin, gbc);
        gbc.gridy = 4; panel.add(btnReg, gbc);

        // Actions
        btnLogin.addActionListener(e -> {
            String id = tfUser.getText();
            String pass = new String(pfPass.getPassword());

            User foundUser = DataStore.users.stream()
                    .filter(u -> u.login(id, pass))
                    .findFirst().orElse(null);

            if (foundUser != null) {
                currentUser = foundUser;
                if (currentUser instanceof Admin) {
                    mainPanel.add(createAdminPanel(), "ADMIN");
                    cardLayout.show(mainPanel, "ADMIN");
                } else {
                    mainPanel.add(createStudentPanel(), "STUDENT");
                    cardLayout.show(mainPanel, "STUDENT");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Credentials");
            }
        });

        btnReg.addActionListener(e -> cardLayout.show(mainPanel, "REGISTER"));
        return panel;
    }

    // --- REGISTER PANEL ---
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JComboBox<CardType> cbType = new JComboBox<>(CardType.values());
        JTextField tfId = new JTextField(15); // Card ID
        JTextField tfName = new JTextField(15);
        JTextField tfEmail = new JTextField(15);
        JTextField tfPhone = new JTextField(15);
        JTextField tfBirthYear = new JTextField(15); // For Age Validation
        JPasswordField pfPass = new JPasswordField(15);
        JButton btnSave = new JButton("Confirm Registration");
        JButton btnBack = new JButton("Back");

        // Components Layout
        int y = 0;
        gbc.gridx = 0; gbc.gridy = y++; gbc.gridwidth = 2; panel.add(new JLabel("Register New Student"), gbc);
        
        gbc.gridwidth = 1; gbc.gridy = y; panel.add(new JLabel("Card Type:"), gbc);
        gbc.gridx = 1; panel.add(cbType, gbc);

        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("ID Number:"), gbc);
        gbc.gridx = 1; panel.add(tfId, gbc);

        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1; panel.add(tfName, gbc);
        
        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Birth Year (AD):"), gbc);
        gbc.gridx = 1; panel.add(tfBirthYear, gbc);

        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1; panel.add(tfEmail, gbc);

        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1; panel.add(tfPhone, gbc);

        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(pfPass, gbc);

        gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 2; panel.add(btnSave, gbc);
        gbc.gridy = ++y; panel.add(btnBack, gbc);

        // Logic 
        btnSave.addActionListener(e -> {
            try {
                CardType type = (CardType) cbType.getSelectedItem();
                String id = tfId.getText();
                int bYear = Integer.parseInt(tfBirthYear.getText());

                // 1. Validate ID/Age Rules
                boolean isValid = false;
                if (type == CardType.STUDENT_CARD) {
                    isValid = ValidationService.validateStudentId(id);
                    if (!isValid) JOptionPane.showMessageDialog(this, "Invalid Student ID! Must start with 65-68.");
                } else {
                    isValid = ValidationService.validateAge(bYear);
                    if (!isValid) JOptionPane.showMessageDialog(this, "Invalid Age! Must be 18-22 years old.");
                }

                if (isValid) {
                    Student s = new Student(id, tfName.getText(), tfEmail.getText(), tfPhone.getText(), new String(pfPass.getPassword()), type, bYear);
                    DataStore.users.add(s);
                    JOptionPane.showMessageDialog(this, "Registration Successful!");
                    cardLayout.show(mainPanel, "LOGIN");
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please fill all fields correctly.");
            }
        });

        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        return panel;
    }

    // --- STUDENT PANEL ---
    private JPanel createStudentPanel() {
        JTabbedPane tabs = new JTabbedPane();
        
        // Tab 1: Browse Items
        JPanel browsePanel = new JPanel(new BorderLayout());
        DefaultTableModel itemModel = new DefaultTableModel(new String[]{"ID", "Name", "Category", "Status"}, 0);
        JTable itemTable = new JTable(itemModel);
        JButton btnBorrow = new JButton("Borrow Item");

        Runnable refreshItems = () -> {
            itemModel.setRowCount(0);
            for (Item i : DataStore.items) itemModel.addRow(new Object[]{i.getItemId(), i.getName(), i.getCategory(), i.getStatus()});
        };
        refreshItems.run();

        btnBorrow.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if (row != -1) {
                Item item = DataStore.items.get(row);
                if (item.getStatus() == ItemStatus.AVAILABLE) {
                    DataStore.requests.add(new BorrowRequest((Student) currentUser, item, RequestType.NEW_BORROW));
                    JOptionPane.showMessageDialog(this, "Borrow Request Sent!");
                } else {
                    JOptionPane.showMessageDialog(this, "Item is not available.");
                }
            }
        });
        
        browsePanel.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        browsePanel.add(btnBorrow, BorderLayout.SOUTH);

        // Tab 2: My Status & Renew 
        JPanel statusPanel = new JPanel(new BorderLayout());
        DefaultTableModel statusModel = new DefaultTableModel(new String[]{"Item", "Status", "Action"}, 0);
        JTable statusTable = new JTable(statusModel);
        JButton btnRenew = new JButton("Renew Selected Item");

        Runnable refreshStatus = () -> {
            statusModel.setRowCount(0);
            // Show Requests
            for (BorrowRequest req : DataStore.requests) {
                if (req.getStudent().equals(currentUser) && req.getStatus() != RequestStatus.COMPLETED) {
                    statusModel.addRow(new Object[]{req.getItem().getName(), "REQ: " + req.getStatus(), req.getType()});
                }
            }
            // Show Active Records
            for (BorrowRecord rec : DataStore.records) {
                if (rec.getStudent().equals(currentUser) && rec.getReturnDate() == null) {
                    statusModel.addRow(new Object[]{rec.getItem().getName(), "BORROWED (Due: " + rec.getDueDate() + ")", "Active"});
                }
            }
        };

        tabs.addChangeListener(e -> { refreshItems.run(); refreshStatus.run(); });

        btnRenew.addActionListener(e -> {
            int row = statusTable.getSelectedRow();
            if (row != -1) {
                String status = statusModel.getValueAt(row, 1).toString();
                if (status.startsWith("BORROWED")) {
                    // Find record
                    String itemName = statusModel.getValueAt(row, 0).toString();
                    BorrowRecord rec = DataStore.records.stream()
                            .filter(r -> r.getItem().getName().equals(itemName) && r.getStudent().equals(currentUser))
                            .findFirst().orElse(null);
                    
                    if (rec != null) {
                        DataStore.requests.add(new BorrowRequest((Student) currentUser, rec.getItem(), RequestType.RENEW));
                        JOptionPane.showMessageDialog(this, "Renew Request Sent!");
                        refreshStatus.run();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Can only renew currently borrowed items.");
                }
            }
        });

        statusPanel.add(new JScrollPane(statusTable), BorderLayout.CENTER);
        statusPanel.add(btnRenew, BorderLayout.SOUTH);

        tabs.addTab("Borrow Items", browsePanel);
        tabs.addTab("My Status", statusPanel);

        JPanel container = new JPanel(new BorderLayout());
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        container.add(logout, BorderLayout.NORTH);
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    // --- ADMIN PANEL ---
    private JPanel createAdminPanel() {
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Approvals
        JPanel approvePanel = new JPanel(new BorderLayout());
        DefaultTableModel reqModel = new DefaultTableModel(new String[]{"Student", "Item", "Type", "Status"}, 0);
        JTable reqTable = new JTable(reqModel);
        JPanel btnPanel = new JPanel();
        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        
        Runnable refreshReq = () -> {
            reqModel.setRowCount(0);
            for (BorrowRequest r : DataStore.requests) {
                if (r.getStatus() == RequestStatus.PENDING) {
                    reqModel.addRow(new Object[]{r.getStudent().getName(), r.getItem().getName(), r.getType(), r.getStatus()});
                }
            }
        };

        btnApprove.addActionListener(e -> {
            int row = reqTable.getSelectedRow();
            if (row != -1) {
                BorrowRequest req = findRequestAtRow(row, reqModel);
                if (req != null) {
                    req.setStatus(RequestStatus.APPROVED);
                    
                    if (req.getType() == RequestType.NEW_BORROW) {
                        req.getItem().setStatus(ItemStatus.BORROWED);
                        DataStore.records.add(new BorrowRecord(req.getStudent(), req.getItem()));
                    } else {
                        // Renew Case 
                        BorrowRecord rec = findRecord(req.getStudent(), req.getItem());
                        if (rec != null) rec.extendDueDate(7);
                    }
                    
                    refreshReq.run();
                }
            }
        });

        btnReject.addActionListener(e -> {
            int row = reqTable.getSelectedRow();
            if (row != -1) {
                BorrowRequest req = findRequestAtRow(row, reqModel);
                if (req != null) {
                    req.setStatus(RequestStatus.REJECTED);
                    refreshReq.run();
                }
            }
        });

        btnPanel.add(btnApprove);
        btnPanel.add(btnReject);
        approvePanel.add(new JScrollPane(reqTable), BorderLayout.CENTER);
        approvePanel.add(btnPanel, BorderLayout.SOUTH);

        // Tab 2: Returns & Fines 
        JPanel returnPanel = new JPanel(new BorderLayout());
        DefaultTableModel recModel = new DefaultTableModel(new String[]{"Student", "Item", "Due Date"}, 0);
        JTable recTable = new JTable(recModel);
        JButton btnReturn = new JButton("Process Return");

        Runnable refreshRec = () -> {
            recModel.setRowCount(0);
            for (BorrowRecord r : DataStore.records) {
                if (r.getReturnDate() == null) {
                    recModel.addRow(new Object[]{r.getStudent().getName(), r.getItem().getName(), r.getDueDate()});
                }
            }
        };

        btnReturn.addActionListener(e -> {
            int row = recTable.getSelectedRow();
            if (row != -1) {
                BorrowRecord rec = DataStore.records.stream()
                    .filter(r -> r.getStudent().getName().equals(recModel.getValueAt(row, 0)) && r.getReturnDate() == null)
                    .findFirst().orElse(null);
                
                if (rec != null) {
                    // Mock Return Date selection
                    String[] opts = {"Return Today", "Return Late (Test Fine)"};
                    int choice = JOptionPane.showOptionDialog(this, "Select Return Date", "Return", 0, 3, null, opts, opts[0]);
                    
                    LocalDate retDate = LocalDate.now();
                    if (choice == 1) retDate = rec.getDueDate().plusDays(5); // Mock Late

                    rec.markReturn(retDate);
                    rec.getItem().setStatus(ItemStatus.AVAILABLE); // 

                    int fine = FineCalculator.calculate(rec.getDueDate(), retDate);
                    String msg = "Item Returned.";
                    if (fine > 0) msg += "\n⚠️ LATE FEE: " + fine + " THB"; // 
                    
                    JOptionPane.showMessageDialog(this, msg);
                    refreshRec.run();
                }
            }
        });

        tabs.addChangeListener(e -> { refreshReq.run(); refreshRec.run(); });
        
        returnPanel.add(new JScrollPane(recTable), BorderLayout.CENTER);
        returnPanel.add(btnReturn, BorderLayout.SOUTH);

        tabs.addTab("Pending Requests", approvePanel);
        tabs.addTab("Active Returns", returnPanel);

        JPanel container = new JPanel(new BorderLayout());
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        container.add(logout, BorderLayout.NORTH);
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    // Helper to find object from table selection
    private BorrowRequest findRequestAtRow(int row, DefaultTableModel model) {
        String sName = model.getValueAt(row, 0).toString();
        String iName = model.getValueAt(row, 1).toString();
        return DataStore.requests.stream()
                .filter(r -> r.getStudent().getName().equals(sName) && r.getItem().getName().equals(iName) && r.getStatus() == RequestStatus.PENDING)
                .findFirst().orElse(null);
    }

    private BorrowRecord findRecord(Student s, Item i) {
        return DataStore.records.stream()
                .filter(r -> r.getStudent().equals(s) && r.getItem().equals(i) && r.getReturnDate() == null)
                .findFirst().orElse(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SmartBorrowSystem().setVisible(true));
    }
}