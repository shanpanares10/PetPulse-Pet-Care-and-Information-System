package petpulse.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;


public final class Db {

    public static final File CONFIG_FILE = new File("db.properties");

    private static String host = "localhost";
    private static String port = "3306";
    private static String name = "petpulse_db";
    private static String user = "root";
    private static String password = "";

    private static volatile String currentUser;
    private static volatile String initWarning;

    private static final String OPTIONS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            + "&useUnicode=true&characterEncoding=UTF-8";

    private static final String[] SCHEMA = {
        "CREATE TABLE IF NOT EXISTS users ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "username VARCHAR(50) NOT NULL UNIQUE,"
            + "password VARCHAR(100) NOT NULL,"
            + "full_name VARCHAR(100) NOT NULL DEFAULT 'Admin') ENGINE=InnoDB",

        "CREATE TABLE IF NOT EXISTS pets ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "pet_name VARCHAR(100) NOT NULL,"
            + "species VARCHAR(50) NOT NULL,"
            + "breed VARCHAR(100),"
            + "gender VARCHAR(20),"
            + "age VARCHAR(30),"
            + "owner_name VARCHAR(100) NOT NULL,"
            + "contact_no VARCHAR(30),"
            + "address VARCHAR(200),"
            + "archived TINYINT(1) NOT NULL DEFAULT 0,"
            + "archived_at DATETIME NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB",

        "CREATE TABLE IF NOT EXISTS appointments ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "pet_id INT NOT NULL,"
            + "appt_date DATE NOT NULL,"
            + "appt_time VARCHAR(15),"
            + "service VARCHAR(100) NOT NULL,"
            + "veterinarian VARCHAR(100),"
            + "status VARCHAR(20) NOT NULL DEFAULT 'Scheduled',"
            + "notes VARCHAR(255),"
            + "archived TINYINT(1) NOT NULL DEFAULT 0,"
            + "archived_at DATETIME NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "CONSTRAINT fk_appt_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE) ENGINE=InnoDB",

        "CREATE TABLE IF NOT EXISTS vaccinations ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "pet_id INT NOT NULL,"
            + "vaccine_name VARCHAR(100) NOT NULL,"
            + "date_given DATE NULL,"
            + "next_due DATE NULL,"
            + "status VARCHAR(20) NOT NULL DEFAULT 'Pending',"
            + "remarks VARCHAR(255),"
            + "archived TINYINT(1) NOT NULL DEFAULT 0,"
            + "archived_at DATETIME NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "CONSTRAINT fk_vacc_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE) ENGINE=InnoDB",

        "CREATE TABLE IF NOT EXISTS payments ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "pet_id INT NOT NULL,"
            + "description VARCHAR(150) NOT NULL,"
            + "amount DECIMAL(10,2) NOT NULL,"
            + "payment_date DATE NOT NULL,"
            + "method VARCHAR(30) NOT NULL DEFAULT 'Cash',"
            + "archived TINYINT(1) NOT NULL DEFAULT 0,"
            + "archived_at DATETIME NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "CONSTRAINT fk_pay_pet FOREIGN KEY (pet_id) REFERENCES pets(id) ON DELETE CASCADE) ENGINE=InnoDB",

        "INSERT INTO users (username, password, full_name) "
            + "SELECT 'admin', SHA2('admin123', 256), 'Admin' "
            + "WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')"
    };

    private static final String[] HISTORY_TABLES = {
            "CREATE TABLE IF NOT EXISTS transaction_history ( "
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "payment_id INT NULL, "
            + "receipt_no VARCHAR(10) NOT NULL, "
            + "pet_id INT NULL, "
            + "pet_name VARCHAR(100), "
            + "owner_name VARCHAR(100), "
            + "description VARCHAR(150), "
            + "amount DECIMAL(10,2) NOT NULL, "
            + "method VARCHAR(30), "
            + "payment_date DATE NULL, "
            + "action VARCHAR(20) NOT NULL, "
            + "performed_by VARCHAR(100) NOT NULL DEFAULT 'system', "
            + "logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "INDEX idx_th_payment (payment_id), "
            + "INDEX idx_th_pet (pet_id), "
            + "INDEX idx_th_logged (logged_at) "
            + ") ENGINE=InnoDB",

            "CREATE TABLE IF NOT EXISTS archive_log ( "
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "record_type VARCHAR(20) NOT NULL, "
            + "record_id INT NOT NULL, "
            + "details VARCHAR(255), "
            + "action VARCHAR(20) NOT NULL, "
            + "performed_by VARCHAR(100) NOT NULL DEFAULT 'system', "
            + "logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "INDEX idx_al_record (record_type, record_id), "
            + "INDEX idx_al_logged (logged_at) "
            + ") ENGINE=InnoDB"
    };

    /** One-time fill so records that existed before this update also appear in the logs. */
    private static final String[] HISTORY_BACKFILL = {
            "INSERT INTO transaction_history "
            + "(payment_id, receipt_no, pet_id, pet_name, owner_name, description, amount, method, payment_date, "
            + "action, performed_by, logged_at) "
            + "SELECT y.id, LPAD(y.id, 5, '0'), p.id, p.pet_name, p.owner_name, y.description, y.amount, y.method, "
            + "y.payment_date, 'PAID', 'system', y.created_at "
            + "FROM payments y JOIN pets p ON p.id = y.pet_id "
            + "WHERE NOT EXISTS (SELECT 1 FROM transaction_history h WHERE h.payment_id = y.id)",

            "INSERT INTO archive_log (record_type, record_id, details, action, performed_by, logged_at) "
            + "SELECT 'Pet', p.id, CONCAT_WS(' | ', p.pet_name, p.species, p.owner_name), 'ARCHIVED', 'system', "
            + "COALESCE(p.archived_at, p.created_at) "
            + "FROM pets p WHERE p.archived = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM archive_log l WHERE l.record_type = 'Pet' AND l.record_id = p.id)",

            "INSERT INTO archive_log (record_type, record_id, details, action, performed_by, logged_at) "
            + "SELECT 'Appointment', a.id, CONCAT_WS(' | ', p.pet_name, a.service, a.appt_date), 'ARCHIVED', 'system', "
            + "COALESCE(a.archived_at, a.created_at) "
            + "FROM appointments a JOIN pets p ON p.id = a.pet_id WHERE a.archived = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM archive_log l WHERE l.record_type = 'Appointment' AND l.record_id = a.id)",

            "INSERT INTO archive_log (record_type, record_id, details, action, performed_by, logged_at) "
            + "SELECT 'Vaccination', v.id, CONCAT_WS(' | ', p.pet_name, v.vaccine_name, v.status), 'ARCHIVED', 'system', "
            + "COALESCE(v.archived_at, v.created_at) "
            + "FROM vaccinations v JOIN pets p ON p.id = v.pet_id WHERE v.archived = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM archive_log l WHERE l.record_type = 'Vaccination' AND l.record_id = v.id)",

            "INSERT INTO archive_log (record_type, record_id, details, action, performed_by, logged_at) "
            + "SELECT 'Payment', y.id, CONCAT_WS(' | ', p.pet_name, y.description, y.amount), 'ARCHIVED', 'system', "
            + "COALESCE(y.archived_at, y.created_at) "
            + "FROM payments y JOIN pets p ON p.id = y.pet_id WHERE y.archived = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM archive_log l WHERE l.record_type = 'Payment' AND l.record_id = y.id)"
    };

    private static final String[] HISTORY_TRIGGERS = {
            "DROP TRIGGER IF EXISTS trg_payments_ai",

            "CREATE TRIGGER trg_payments_ai AFTER INSERT ON payments FOR EACH ROW "
            + "BEGIN "
            + "INSERT INTO transaction_history "
            + "(payment_id, receipt_no, pet_id, pet_name, owner_name, description, amount, method, payment_date, action, performed_by) "
            + "VALUES (NEW.id, LPAD(NEW.id, 5, '0'), NEW.pet_id, (SELECT pet_name FROM pets WHERE id = NEW.pet_id), (SELECT owner_name FROM pets WHERE id = NEW.pet_id), "
            + "NEW.description, NEW.amount, NEW.method, NEW.payment_date, 'PAID', COALESCE(@pp_user, 'system')); "
            + "END",

            "DROP TRIGGER IF EXISTS trg_payments_au",

            "CREATE TRIGGER trg_payments_au AFTER UPDATE ON payments FOR EACH ROW "
            + "BEGIN "
            + "DECLARE act VARCHAR(20) DEFAULT NULL; "
            + "IF NEW.archived = 1 AND OLD.archived = 0 THEN "
            + "SET act = 'ARCHIVED'; "
            + "ELSEIF NEW.archived = 0 AND OLD.archived = 1 THEN "
            + "SET act = 'RESTORED'; "
            + "ELSEIF NOT (NEW.pet_id <=> OLD.pet_id AND NEW.description <=> OLD.description "
            + "AND NEW.amount <=> OLD.amount AND NEW.payment_date <=> OLD.payment_date "
            + "AND NEW.method <=> OLD.method) THEN "
            + "SET act = 'EDITED'; "
            + "END IF; "
            + "IF act IS NOT NULL THEN "
            + "INSERT INTO transaction_history "
            + "(payment_id, receipt_no, pet_id, pet_name, owner_name, description, amount, method, payment_date, action, performed_by) "
            + "VALUES (NEW.id, LPAD(NEW.id, 5, '0'), NEW.pet_id, (SELECT pet_name FROM pets WHERE id = NEW.pet_id), (SELECT owner_name FROM pets WHERE id = NEW.pet_id), "
            + "NEW.description, NEW.amount, NEW.method, NEW.payment_date, act, COALESCE(@pp_user, 'system')); "
            + "IF act <> 'EDITED' THEN "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Payment', NEW.id, CONCAT_WS(' | ', (SELECT pet_name FROM pets WHERE id = NEW.pet_id), NEW.description, NEW.amount), act, COALESCE(@pp_user, 'system')); "
            + "END IF; "
            + "END IF; "
            + "END",

            "DROP TRIGGER IF EXISTS trg_payments_ad",

            "CREATE TRIGGER trg_payments_ad AFTER DELETE ON payments FOR EACH ROW "
            + "BEGIN "
            + "INSERT INTO transaction_history "
            + "(payment_id, receipt_no, pet_id, pet_name, owner_name, description, amount, method, payment_date, action, performed_by) "
            + "VALUES (OLD.id, LPAD(OLD.id, 5, '0'), OLD.pet_id, (SELECT pet_name FROM pets WHERE id = OLD.pet_id), (SELECT owner_name FROM pets WHERE id = OLD.pet_id), "
            + "OLD.description, OLD.amount, OLD.method, OLD.payment_date, 'DELETED', COALESCE(@pp_user, 'system')); "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Payment', OLD.id, CONCAT_WS(' | ', (SELECT pet_name FROM pets WHERE id = OLD.pet_id), OLD.description, OLD.amount), 'DELETED', COALESCE(@pp_user, 'system')); "
            + "END",

            "DROP TRIGGER IF EXISTS trg_appointments_au",

            "CREATE TRIGGER trg_appointments_au AFTER UPDATE ON appointments FOR EACH ROW "
            + "BEGIN "
            + "IF NEW.archived <> OLD.archived THEN "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Appointment', NEW.id, CONCAT_WS(' | ', (SELECT pet_name FROM pets WHERE id = NEW.pet_id), NEW.service, NEW.appt_date), IF(NEW.archived = 1, 'ARCHIVED', 'RESTORED'), COALESCE(@pp_user, 'system')); "
            + "END IF; "
            + "END",

            "DROP TRIGGER IF EXISTS trg_appointments_ad",

            "CREATE TRIGGER trg_appointments_ad AFTER DELETE ON appointments FOR EACH ROW "
            + "BEGIN "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Appointment', OLD.id, CONCAT_WS(' | ', (SELECT pet_name FROM pets WHERE id = OLD.pet_id), OLD.service, OLD.appt_date), 'DELETED', COALESCE(@pp_user, 'system')); "
            + "END",

            "DROP TRIGGER IF EXISTS trg_vaccinations_au",

            "CREATE TRIGGER trg_vaccinations_au AFTER UPDATE ON vaccinations FOR EACH ROW "
            + "BEGIN "
            + "IF NEW.archived <> OLD.archived THEN "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Vaccination', NEW.id, CONCAT_WS(' | ', (SELECT pet_name FROM pets WHERE id = NEW.pet_id), NEW.vaccine_name, NEW.status), IF(NEW.archived = 1, 'ARCHIVED', 'RESTORED'), COALESCE(@pp_user, 'system')); "
            + "END IF; "
            + "END",

            "DROP TRIGGER IF EXISTS trg_vaccinations_ad",

            "CREATE TRIGGER trg_vaccinations_ad AFTER DELETE ON vaccinations FOR EACH ROW "
            + "BEGIN "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Vaccination', OLD.id, CONCAT_WS(' | ', (SELECT pet_name FROM pets WHERE id = OLD.pet_id), OLD.vaccine_name, OLD.status), 'DELETED', COALESCE(@pp_user, 'system')); "
            + "END",

            "DROP TRIGGER IF EXISTS trg_pets_au",

            "CREATE TRIGGER trg_pets_au AFTER UPDATE ON pets FOR EACH ROW "
            + "BEGIN "
            + "IF NEW.archived <> OLD.archived THEN "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Pet', NEW.id, CONCAT_WS(' | ', NEW.pet_name, NEW.species, NEW.owner_name), IF(NEW.archived = 1, 'ARCHIVED', 'RESTORED'), COALESCE(@pp_user, 'system')); "
            + "END IF; "
            + "END",

            "DROP TRIGGER IF EXISTS trg_pets_bd",

            "CREATE TRIGGER trg_pets_bd BEFORE DELETE ON pets FOR EACH ROW "
            + "BEGIN "
            + "INSERT INTO transaction_history "
            + "(payment_id, receipt_no, pet_id, pet_name, owner_name, description, amount, method, payment_date, action, performed_by) "
            + "SELECT y.id, LPAD(y.id, 5, '0'), OLD.id, OLD.pet_name, OLD.owner_name, y.description, y.amount, y.method, "
            + "y.payment_date, 'DELETED', COALESCE(@pp_user, 'system') "
            + "FROM payments y WHERE y.pet_id = OLD.id; "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "SELECT 'Payment', y.id, CONCAT_WS(' | ', OLD.pet_name, y.description, y.amount), 'DELETED', COALESCE(@pp_user, 'system') "
            + "FROM payments y WHERE y.pet_id = OLD.id; "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "SELECT 'Appointment', a.id, CONCAT_WS(' | ', OLD.pet_name, a.service, a.appt_date), 'DELETED', COALESCE(@pp_user, 'system') "
            + "FROM appointments a WHERE a.pet_id = OLD.id; "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "SELECT 'Vaccination', v.id, CONCAT_WS(' | ', OLD.pet_name, v.vaccine_name, v.status), 'DELETED', COALESCE(@pp_user, 'system') "
            + "FROM vaccinations v WHERE v.pet_id = OLD.id; "
            + "INSERT INTO archive_log (record_type, record_id, details, action, performed_by) "
            + "VALUES ('Pet', OLD.id, CONCAT_WS(' | ', OLD.pet_name, OLD.species, OLD.owner_name), 'DELETED', COALESCE(@pp_user, 'system')); "
            + "END"
    };

    static {
        load();
    }

    private Db() {
    }


    public static synchronized void load() {
        Properties p = new Properties();
        try (InputStream in = CONFIG_FILE.exists()
                ? new FileInputStream(CONFIG_FILE)
                : Db.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException ignored) {
        }
        host = p.getProperty("db.host", host).trim();
        port = p.getProperty("db.port", port).trim();
        name = p.getProperty("db.name", name).trim();
        user = p.getProperty("db.user", user).trim();
        password = p.getProperty("db.password", password);
    }

    public static synchronized void configure(String h, String p, String n, String u, String pw) {
        host = h.trim();
        port = p.trim();
        name = n.trim();
        user = u.trim();
        password = pw;
    }

    public static synchronized void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("db.host", host);
        p.setProperty("db.port", port);
        p.setProperty("db.name", name);
        p.setProperty("db.user", user);
        p.setProperty("db.password", password);
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            p.store(out, "PetPulse database settings");
        }
    }

    public static String getHost() { return host; }
    public static String getPort() { return port; }
    public static String getName() { return name; }
    public static String getUser() { return user; }
    public static String getPassword() { return password; }

    private static void loadDriver() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("MySQL JDBC driver not found. In NetBeans: right-click the project > "
                    + "Build (so Maven downloads mysql-connector-j), or add the MySQL JDBC Driver library.", ex);
        }
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();
        Connection c = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + name + OPTIONS, user, password);
        String who = currentUser;
        if (who != null) {
            // The history triggers read @pp_user to record who made each change.
            try (PreparedStatement ps = c.prepareStatement("SET @pp_user = ?")) {
                ps.setString(1, who);
                ps.executeUpdate();
            } catch (SQLException ex) {
                c.close();
                throw ex;
            }
        }
        return c;
    }

    /** Sets who is logged in, so history rows say who did what. Pass null on logout. */
    public static void setCurrentUser(String fullName) {
        currentUser = fullName;
    }

    /** Warning from the last initialize() (null = everything installed). */
    public static String getInitWarning() {
        return initWarning;
    }

    /** Creates the database + tables if they do not exist yet and adds the default admin login. */
    public static void initialize() throws SQLException {
        loadDriver();
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + OPTIONS, user, password);
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + name
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            for (String sql : SCHEMA) {
                st.executeUpdate(sql);
            }
            for (String sql : HISTORY_TABLES) {
                st.executeUpdate(sql);
            }
            for (String sql : HISTORY_BACKFILL) {
                st.executeUpdate(sql);
            }
            // Triggers need the TRIGGER privilege. If they cannot be created, keep the app
            // usable but tell the user (shown on the login screen) instead of failing silently.
            initWarning = null;
            try {
                for (String sql : HISTORY_TRIGGERS) {
                    st.executeUpdate(sql);
                }
            } catch (SQLException ex) {
                initWarning = "History triggers not installed: " + ex.getMessage();
            }
        }
    }

    public static String authenticate(String username, String pass) throws SQLException {
        String sql = "SELECT full_name FROM users WHERE username = ? AND password = SHA2(?, 256)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public static String scalar(String sql) throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
