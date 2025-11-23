import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// Imports for JavaMail (ต้องมี JAR Files ถึงจะ Compile ผ่าน)
import javax.mail.*;
import javax.mail.internet.*;

// ==========================================
// 1. ENUMS & CONSTANTS
// ==========================================
enum UserRole { STUDENT, ADMIN }
enum RequestType { NEW_BORROW, RENEW, EXTEND } 
enum RequestStatus { PENDING, APPROVED, REJECTED, COMPLETED }
enum CardType { STUDENT_CARD, NATIONAL_ID }

// ==========================================
// 2. MODEL LAYER
// ==========================================

abstract class User {
    protected String id; 
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
    public String getEmail() { return email; } 
    public boolean login(String inputId, String inputPass) {
        return this.id.equals(inputId) && this.password.equals(inputPass);
    }
}

class Student extends User {
    private CardType cardType;
    private int birthYear; 

    public Student(String id, String name, String email, String phone, String password, CardType cardType, int birthYear) {
        super(id, name, email, phone, password);
        this.cardType = cardType;
        this.birthYear = birthYear;
    }
}

class Admin extends User {
    public Admin(String username, String password) {
        super(username, "Administrator", "admin@sys.com", "-", password);
    }
}

class Item {
    private String itemId;
    private String name;
    private String category;
    private int totalQty;
    private int currentQty;

    public Item(String itemId, String name, String category, int totalQty) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.totalQty = totalQty;
        this.currentQty = totalQty;
    }

    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public int getTotalQty() { return totalQty; }
    public int getCurrentQty() { return currentQty; }

    public void decreaseQty() { if (currentQty > 0) currentQty--; }
    public void increaseQty() { if (currentQty < totalQty) currentQty++; }
    public String getCategory() { return category; }
    
    @Override
    public String toString() { return name; }
}

class BorrowRequest {
    private Student student;
    private Item item;
    private RequestType type;
    private RequestStatus status;
    private LocalDate requestDate;
    private int daysRequested; 

    public BorrowRequest(Student student, Item item, RequestType type, int daysRequested) {
        this.student = student;
        this.item = item;
        this.type = type;
        this.status = RequestStatus.PENDING;
        this.requestDate = LocalDate.now();
        this.daysRequested = daysRequested;
    }

    public Student getStudent() { return student; }
    public Item getItem() { return item; }
    public RequestType getType() { return type; }
    public RequestStatus getStatus() { return status; }
    public int getDaysRequested() { return daysRequested; }
    public void setStatus(RequestStatus status) { this.status = status; }
}

class BorrowRecord {
    private Student student;
    private Item item;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private boolean isExtended;

    public BorrowRecord(Student student, Item item) {
        this.student = student;
        this.item = item;
        this.borrowDate = LocalDate.now();
        this.dueDate = LocalDate.now().plusDays(7); 
        this.isExtended = false;
    }

    // Constructor for Mock Data
    public BorrowRecord(Student student, Item item, LocalDate customDueDate) {
        this.student = student;
        this.item = item;
        this.borrowDate = customDueDate.minusDays(7);
        this.dueDate = customDueDate;
        this.isExtended = false;
    }

    public Student getStudent() { return student; }
    public Item getItem() { return item; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public boolean isExtended() { return isExtended; }
    
    public void extendDueDate(int days) {
        this.dueDate = this.dueDate.plusDays(days);
        this.isExtended = true; 
    }

    public void markReturn(LocalDate date) {
        this.returnDate = date;
    }
}

// ==========================================
// 3. SERVICE LAYER (Business Logic)
// ==========================================

// [REAL IMPLEMENTATION] Email Service using javax.mail
class EmailService {
    // !!! แก้ไขข้อมูลตรงนี้ !!!
    private static final String SENDER_EMAIL = "chanutsunatho@gmail.com"; 
    private static final String APP_PASSWORD = "dcjj putl zrkz qmxw"; // รหัส 16 หลักจาก Google App Password

    public static void send(String recipientEmail, String subject, String body) {
        // Run in background thread to prevent GUI freezing
        new Thread(() -> {
            System.out.println("⏳ Sending email to " + recipientEmail + "...");
            
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);

                System.out.println("✅ Email Sent Successfully to: " + recipientEmail);

            } catch (MessagingException e) {
                System.err.println("❌ Failed to send email: " + e.getMessage());
                // e.printStackTrace(); // Uncomment to debug
            }
        }).start();
    }
}

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

class DataStore {
    public static List<User> users = new ArrayList<>();
    public static List<Item> items = new ArrayList<>();
    public static List<BorrowRequest> requests = new ArrayList<>();
    public static List<BorrowRecord> records = new ArrayList<>();

    static {
        // Admin
        users.add(new Admin("admin", "admin")); 
        
        // Items
        items.add(new Item("I01", "Projector Sony", "AV", 5));
        items.add(new Item("I02", "MacBook Pro M2", "IT", 2));
        items.add(new Item("I03", "Canon Camera", "AV", 3));
        items.add(new Item("I04", "Microphone Shure", "Audio", 10));
        
        // Mock Data
        Student s1 = new Student("66001", "Good Student", "test_student@gmail.com", "081", "1234", CardType.STUDENT_CARD, 2002);
        users.add(s1);
        
        Student s2 = new Student("66999", "Late Student", "late_student@gmail.com", "089", "1234", CardType.STUDENT_CARD, 2001);
        users.add(s2);

        Item i2 = items.get(1); // MacBook
        i2.decreaseQty(); 
        BorrowRecord lateRec = new BorrowRecord(s2, i2, LocalDate.now().minusDays(3)); 
        records.add(lateRec);
    }
}

// ==========================================
// 4. VIEW LAYER (Swing GUI)
// ==========================================

public class SmartBorrowSystem extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private User currentUser;

    private JPanel adminPanel;
    private JPanel studentPanel;

    public SmartBorrowSystem() {
        setTitle("University Borrowing System v2.0 (Real Email)");
        setSize(1100, 700); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createRegisterPanel(), "REGISTER");
        
        studentPanel = createStudentPanel();
        adminPanel = createAdminPanel();
        
        mainPanel.add(studentPanel, "STUDENT");
        mainPanel.add(adminPanel, "ADMIN");
        
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

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; panel.add(title, gbc);
        gbc.gridy = 1; gbc.gridwidth = 1; panel.add(new JLabel("ID / Card ID:"), gbc);
        gbc.gridx = 1; panel.add(tfUser, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; panel.add(pfPass, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; panel.add(btnLogin, gbc);
        gbc.gridy = 4; panel.add(btnReg, gbc);

        btnLogin.addActionListener(e -> {
            String id = tfUser.getText();
            String pass = new String(pfPass.getPassword());

            User foundUser = DataStore.users.stream()
                    .filter(u -> u.login(id, pass))
                    .findFirst().orElse(null);

            if (foundUser != null) {
                currentUser = foundUser;
                if (currentUser instanceof Admin) {
                    refreshAdminData();
                    cardLayout.show(mainPanel, "ADMIN");
                } else {
                    refreshStudentData();
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
        JTextField tfId = new JTextField(15);
        JTextField tfName = new JTextField(15);
        JTextField tfEmail = new JTextField(15);
        JTextField tfPhone = new JTextField(15);
        JTextField tfBirthYear = new JTextField(15);
        JPasswordField pfPass = new JPasswordField(15);
        JButton btnSave = new JButton("Confirm Registration");
        JButton btnBack = new JButton("Back");

        int y = 0;
        gbc.gridx = 0; gbc.gridy = y++; gbc.gridwidth = 2; panel.add(new JLabel("Register New Student"), gbc);
        gbc.gridwidth = 1; gbc.gridy = y; panel.add(new JLabel("Card Type:"), gbc); gbc.gridx = 1; panel.add(cbType, gbc);
        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("ID Number:"), gbc); gbc.gridx = 1; panel.add(tfId, gbc);
        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Full Name:"), gbc); gbc.gridx = 1; panel.add(tfName, gbc);
        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Birth Year (AD):"), gbc); gbc.gridx = 1; panel.add(tfBirthYear, gbc);
        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Email:"), gbc); gbc.gridx = 1; panel.add(tfEmail, gbc);
        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Phone:"), gbc); gbc.gridx = 1; panel.add(tfPhone, gbc);
        gbc.gridx = 0; gbc.gridy = ++y; panel.add(new JLabel("Password:"), gbc); gbc.gridx = 1; panel.add(pfPass, gbc);
        gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 2; panel.add(btnSave, gbc);
        gbc.gridy = ++y; panel.add(btnBack, gbc);

        btnSave.addActionListener(e -> {
            try {
                CardType type = (CardType) cbType.getSelectedItem();
                String id = tfId.getText();
                String name = tfName.getText();
                String email = tfEmail.getText();
                String pass = new String(pfPass.getPassword());
                int bYear = Integer.parseInt(tfBirthYear.getText());

                if (id.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                     JOptionPane.showMessageDialog(this, "Please fill all fields.");
                     return;
                }

                Student s = new Student(id, name, email, tfPhone.getText(), pass, type, bYear);
                DataStore.users.add(s);
                
                // [EMAIL] Welcome
                EmailService.send(email, "Welcome to Smart Borrow System", 
                    "Registration successful!\nUsername: " + id + "\n\nYou can now login to the system.");
                
                JOptionPane.showMessageDialog(this, "Registration Successful! Email sent.");
                cardLayout.show(mainPanel, "LOGIN");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: Check inputs.");
            }
        });

        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        return panel;
    }

    // --- STUDENT PANEL ---
    private DefaultTableModel stItemModel, stStatusModel;

    private JPanel createStudentPanel() {
        JTabbedPane tabs = new JTabbedPane();
        
        // Tab 1: Browse Items
        JPanel browsePanel = new JPanel(new BorderLayout());
        stItemModel = new DefaultTableModel(new String[]{"ID", "Name", "Category", "Stock (Avail/Total)"}, 0);
        JTable itemTable = new JTable(stItemModel);
        JButton btnBorrow = new JButton("Borrow Item");

        btnBorrow.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if (row != -1) {
                Item item = DataStore.items.get(row);
                
                boolean alreadyBorrowed = DataStore.records.stream()
                    .anyMatch(r -> r.getStudent().equals(currentUser) && r.getItem().equals(item) && r.getReturnDate() == null);
                
                boolean pendingRequest = DataStore.requests.stream()
                    .anyMatch(r -> r.getStudent().equals(currentUser) && r.getItem().equals(item) && r.getStatus() == RequestStatus.PENDING);

                if (alreadyBorrowed) {
                    JOptionPane.showMessageDialog(this, "You are already borrowing this item!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (pendingRequest) {
                    JOptionPane.showMessageDialog(this, "You have a pending request for this item!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (item.getCurrentQty() > 0) {
                    DataStore.requests.add(new BorrowRequest((Student) currentUser, item, RequestType.NEW_BORROW, 7));
                    JOptionPane.showMessageDialog(this, "Borrow Request Sent!");
                    refreshStudentData();
                } else {
                    JOptionPane.showMessageDialog(this, "Item Out of Stock!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        browsePanel.add(new JScrollPane(itemTable), BorderLayout.CENTER);
        browsePanel.add(btnBorrow, BorderLayout.SOUTH);

        // Tab 2: My Status
        JPanel statusPanel = new JPanel(new BorderLayout());
        stStatusModel = new DefaultTableModel(new String[]{"Item", "Status", "Type"}, 0);
        JTable statusTable = new JTable(stStatusModel);
        JButton btnAction = new JButton("Request Extension / Renew");

        btnAction.addActionListener(e -> {
            int row = statusTable.getSelectedRow();
            if (row != -1) {
                String status = stStatusModel.getValueAt(row, 1).toString();
                
                if (status.startsWith("BORROWED")) {
                    String itemName = stStatusModel.getValueAt(row, 0).toString();
                    BorrowRecord rec = DataStore.records.stream()
                            .filter(r -> r.getItem().getName().equals(itemName) && r.getStudent().equals(currentUser) && r.getReturnDate() == null)
                            .findFirst().orElse(null);
                    
                    if (rec != null) {
                        String[] options = {"Renew (Start New Cycle)", "Extend Due Date (Delay Return)"};
                        int choice = JOptionPane.showOptionDialog(this, "Choose request type:", "Request Option",
                                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

                        if (choice == 0) {
                            DataStore.requests.add(new BorrowRequest((Student) currentUser, rec.getItem(), RequestType.RENEW, 7));
                            JOptionPane.showMessageDialog(this, "Renew Request Sent!");
                        } else if (choice == 1) {
                            String[] dayOptions = {"1 Day", "3 Days"};
                            int dayChoice = JOptionPane.showOptionDialog(this, "Select extension duration:", "Extend Due Date",
                                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, dayOptions, dayOptions[0]);
                            
                            int daysToAdd = (dayChoice == 0) ? 1 : 3; 
                            DataStore.requests.add(new BorrowRequest((Student) currentUser, rec.getItem(), RequestType.EXTEND, daysToAdd));
                            JOptionPane.showMessageDialog(this, "Extension Request Sent (" + daysToAdd + " days)!");
                        }
                        refreshStudentData();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Can only request on currently BORROWED items.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select an item.");
            }
        });
        statusPanel.add(new JScrollPane(statusTable), BorderLayout.CENTER);
        statusPanel.add(btnAction, BorderLayout.SOUTH);

        tabs.addTab("Borrow Items", browsePanel);
        tabs.addTab("My Status", statusPanel);
        tabs.addChangeListener(e -> refreshStudentData());

        JPanel container = new JPanel(new BorderLayout());
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        container.add(logout, BorderLayout.NORTH);
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    // --- ADMIN PANEL ---
    private DefaultTableModel adReqModel, adRecModel;

    private JPanel createAdminPanel() {
        JTabbedPane tabs = new JTabbedPane();

        // Tab 1: Approvals
        JPanel approvePanel = new JPanel(new BorderLayout());
        String[] reqCols = {"Student ID", "Name", "Item", "Type", "Details", "Status"};
        adReqModel = new DefaultTableModel(reqCols, 0);
        JTable reqTable = new JTable(adReqModel);
        JPanel btnPanel = new JPanel();
        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        
        btnApprove.setBackground(new Color(144, 238, 144));
        btnReject.setBackground(new Color(255, 99, 71));

        btnApprove.addActionListener(e -> handleAdminAction(reqTable, true));
        btnReject.addActionListener(e -> handleAdminAction(reqTable, false));

        btnPanel.add(btnApprove);
        btnPanel.add(btnReject);
        approvePanel.add(new JScrollPane(reqTable), BorderLayout.CENTER);
        approvePanel.add(btnPanel, BorderLayout.SOUTH);

        // Tab 2: Returns
        JPanel returnPanel = new JPanel(new BorderLayout());
        adRecModel = new DefaultTableModel(new String[]{"Student", "Item", "Due Date", "Status", "Current Fine"}, 0);
        JTable recTable = new JTable(adRecModel);
        JButton btnReturn = new JButton("Process Return (Today)");

        btnReturn.addActionListener(e -> {
            int row = recTable.getSelectedRow();
            if (row != -1) {
                String sName = adRecModel.getValueAt(row, 0).toString();
                String iName = adRecModel.getValueAt(row, 1).toString();

                BorrowRecord rec = DataStore.records.stream()
                    .filter(r -> r.getStudent().getName().equals(sName) && r.getItem().getName().equals(iName) && r.getReturnDate() == null)
                    .findFirst().orElse(null);
                
                if (rec != null) {
                    LocalDate returnDate = LocalDate.now(); 
                    int fine = FineCalculator.calculate(rec.getDueDate(), returnDate);

                    if (fine > 0) {
                        int confirm = JOptionPane.showConfirmDialog(this, 
                            "⚠️ ITEM IS OVERDUE!\nFine Amount: " + fine + " THB\n\nConfirm Return?", 
                            "Overdue Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        
                        if (confirm != JOptionPane.YES_OPTION) return; 
                    }

                    rec.markReturn(returnDate);
                    rec.getItem().increaseQty();
                    
                    String msg = "Item Returned.";
                    if (fine > 0) msg += " Fine Paid: " + fine + " THB";
                    JOptionPane.showMessageDialog(this, msg);
                    
                    // [EMAIL] Return & Fine
                    EmailService.send(rec.getStudent().getEmail(), "Item Returned: " + rec.getItem().getName(), 
                        "You have returned '" + rec.getItem().getName() + "'.\n" +
                        "Date: " + returnDate + "\n" +
                        "Fine: " + fine + " THB.");
                    
                    refreshAdminData();
                }
            }
        });
        
        returnPanel.add(new JScrollPane(recTable), BorderLayout.CENTER);
        returnPanel.add(btnReturn, BorderLayout.SOUTH);

        tabs.addChangeListener(e -> refreshAdminData());
        tabs.addTab("Pending Requests", approvePanel);
        tabs.addTab("Active Returns", returnPanel);

        JPanel container = new JPanel(new BorderLayout());
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));
        container.add(logout, BorderLayout.NORTH);
        container.add(tabs, BorderLayout.CENTER);
        return container;
    }

    private void handleAdminAction(JTable table, boolean isApprove) {
        int row = table.getSelectedRow();
        if (row != -1) {
            String sId = adReqModel.getValueAt(row, 0).toString();
            String iName = adReqModel.getValueAt(row, 2).toString();

            BorrowRequest req = DataStore.requests.stream()
                    .filter(r -> r.getStudent().getId().equals(sId) && r.getItem().getName().equals(iName) && r.getStatus() == RequestStatus.PENDING)
                    .findFirst().orElse(null);

            if (req != null) {
                String email = req.getStudent().getEmail();
                if (isApprove) {
                    if (req.getType() == RequestType.NEW_BORROW) {
                        if (req.getItem().getCurrentQty() > 0) {
                            req.getItem().decreaseQty();
                            req.setStatus(RequestStatus.APPROVED);
                            DataStore.records.add(new BorrowRecord(req.getStudent(), req.getItem()));
                            
                            // [EMAIL] Approve Borrow
                            EmailService.send(email, "Borrow Approved", "Your request to borrow '" + req.getItem().getName() + "' has been approved.");
                            JOptionPane.showMessageDialog(this, "Request APPROVED. Stock: " + req.getItem().getCurrentQty());
                        } else {
                            JOptionPane.showMessageDialog(this, "Cannot Approve: Item Out of Stock!");
                            return; 
                        }
                    } else if (req.getType() == RequestType.RENEW) {
                        req.setStatus(RequestStatus.APPROVED);
                        BorrowRecord rec = findRecord(req);
                        if (rec != null) rec.extendDueDate(7);
                        
                        // [EMAIL] Approve Renew
                        EmailService.send(email, "Renew Approved", "Your item '" + req.getItem().getName() + "' has been renewed for 7 days.");
                        JOptionPane.showMessageDialog(this, "Renew Approved.");
                        
                    } else if (req.getType() == RequestType.EXTEND) {
                        req.setStatus(RequestStatus.APPROVED);
                        BorrowRecord rec = findRecord(req);
                        if (rec != null) rec.extendDueDate(req.getDaysRequested());
                        
                        // [EMAIL] Approve Extend
                        EmailService.send(email, "Extension Approved", "Your due date for '" + req.getItem().getName() + "' has been extended by " + req.getDaysRequested() + " days.");
                        JOptionPane.showMessageDialog(this, "Extension Approved.");
                    }
                    
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    // [EMAIL] Reject
                    EmailService.send(email, "Request Rejected", "Your request for '" + req.getItem().getName() + "' was rejected by Admin.");
                    JOptionPane.showMessageDialog(this, "Request REJECTED.");
                }
                refreshAdminData();
            }
        }
    }
    
    private BorrowRecord findRecord(BorrowRequest req) {
        return DataStore.records.stream()
            .filter(r -> r.getStudent().equals(req.getStudent()) && r.getItem().equals(req.getItem()) && r.getReturnDate() == null)
            .findFirst().orElse(null);
    }

    private void refreshStudentData() {
        if (stItemModel == null || currentUser == null) return;
        stItemModel.setRowCount(0);
        for (Item i : DataStore.items) {
            String stockInfo = i.getCurrentQty() + " / " + i.getTotalQty();
            stItemModel.addRow(new Object[]{i.getItemId(), i.getName(), i.getCategory(), stockInfo});
        }
        stStatusModel.setRowCount(0);
        for (BorrowRequest req : DataStore.requests) {
            if (req.getStudent().equals(currentUser) && req.getStatus() != RequestStatus.COMPLETED) {
                String detail = req.getType().toString();
                if (req.getType() == RequestType.EXTEND) detail += " (+" + req.getDaysRequested() + " days)";
                stStatusModel.addRow(new Object[]{req.getItem().getName(), "REQ: " + req.getStatus(), detail});
            }
        }
        for (BorrowRecord rec : DataStore.records) {
            if (rec.getStudent().equals(currentUser)) {
                if (rec.getReturnDate() == null) {
                    stStatusModel.addRow(new Object[]{rec.getItem().getName(), "BORROWED (Due: " + rec.getDueDate() + ")", "Active"});
                } else {
                    String status = "RETURNED (Normal)";
                    if (rec.getReturnDate().isAfter(rec.getDueDate())) status = "RETURNED LATE";
                    stStatusModel.addRow(new Object[]{rec.getItem().getName(), status, "History"});
                }
            }
        }
    }

    private void refreshAdminData() {
        if (adReqModel == null) return;
        adReqModel.setRowCount(0);
        for (BorrowRequest r : DataStore.requests) {
            if (r.getStatus() == RequestStatus.PENDING) {
                String detail = "-";
                if (r.getType() == RequestType.EXTEND) detail = "+" + r.getDaysRequested() + " Days";
                adReqModel.addRow(new Object[]{r.getStudent().getId(), r.getStudent().getName(), r.getItem().getName(), r.getType(), detail, r.getStatus()});
            }
        }
        adRecModel.setRowCount(0);
        LocalDate today = LocalDate.now();
        for (BorrowRecord r : DataStore.records) {
            if (r.getReturnDate() == null) {
                String status = "BORROWED";
                int currentFine = 0;
                if (today.isAfter(r.getDueDate())) {
                    status = "OVERDUE (LATE)";
                    currentFine = FineCalculator.calculate(r.getDueDate(), today);
                } else if (r.isExtended()) {
                    status = "EXTENDED";
                }
                adRecModel.addRow(new Object[]{r.getStudent().getName(), r.getItem().getName(), r.getDueDate(), status, currentFine + " THB"});
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SmartBorrowSystem().setVisible(true));
    }
}