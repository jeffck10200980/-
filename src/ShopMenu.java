import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ShopMenu {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int choice;
        do {
            displayMenu();
            System.out.print("請輸入您的選擇：");
            choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character

            switch (choice) {
                case 1:
                    displayServicePricing();
                    break;
                case 2:
                    displayCustomersAndPets();
                    break;
                case 3:
                    addOrder();
                    break;
                case 4:
                    checkout();
                    break;
                case 5:
                    addCustomer();
                    break;
                case 6:
                    addPet();
                    break;
                case 0:
                    System.out.println("程式已結束。");
                    break;
                default:
                    System.out.println("無效的選擇，請重新輸入。");
            }
        } while (choice != 0);
    }

    private static void displayMenu() {
        System.out.println("\n=== 寵物美容院系統 ===");
        System.out.println("1. 顯示商品內容");
        System.out.println("2. 顯示客人與其寵物");
        System.out.println("3. 新增訂單");
        System.out.println("4. 結帳");
        System.out.println("5. 新增客人");
        System.out.println("6. 新增寵物");
        System.out.println("0. 離開");
    }

    private static void displayServicePricing() {
        System.out.println("\n=== 商品內容 ===");
        String query = "SELECT " +
                "sp.ServicePricingID, " +
                "pc.PetsCategories AS PetCategoryName, " +
                "pcf.PetsClassification AS PetClassificationName, " +
                "CASE " +
                "    WHEN pc.PetsCategoriesID = 1 THEN sfd.Services " +
                "    WHEN pc.PetsCategoriesID = 2 THEN sfc.Services " +
                "    ELSE '未知服務' " +
                "END AS ServiceName, " +
                "sp.ServicePrice " +
                "FROM servicepricing sp " +
                "JOIN petscategories pc ON sp.PetCategoriesID = pc.PetsCategoriesID " +
                "JOIN petsclassification pcf ON sp.PetClassificationID = pcf.PetsClassificationID " +
                "LEFT JOIN servicesfordogs sfd ON sp.ServicesForDogsID = sfd.ServicesForDogsID " +
                "LEFT JOIN servicesforcats sfc ON sp.ServicesForCatsID = sfc.ServicesForCatsID " +
                "ORDER BY sp.ServicePricingID";
        try (ResultSet rs = DBConnect.selectQuery(query)) {
            System.out.printf("%-5s %-10s %-20s %-20s %-10s\n", "ID", "類別", "分類", "服務", "價格");
            while (rs.next()) {
                int servicePricingID = rs.getInt("ServicePricingID");
                String petCategoryName = rs.getString("PetCategoryName");
                String petClassificationName = rs.getString("PetClassificationName");
                String serviceName = rs.getString("ServiceName");
                int servicePrice = rs.getInt("ServicePrice");
                System.out.printf("%-5d %-10s %-20s %-20s %-10d\n", servicePricingID, petCategoryName,
                        petClassificationName, serviceName, servicePrice);
            }
        } catch (SQLException e) {
            System.err.println("⚠️ 顯示商品內容失敗：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void displayCustomersAndPets() {
        System.out.println("\n=== 客人與其寵物 ===");
        String query = "SELECT * FROM view0002";
        try (ResultSet rs = DBConnect.selectQuery(query)) {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            // 1. 儲存欄位名稱
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(rsmd.getColumnName(i));
            }

            // 2. 儲存所有資料列
            List<List<String>> dataRows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i));
                }
                dataRows.add(row);
            }

            // 3. 計算每一欄的最大寬度
            int[] columnWidths = new int[columnCount];
            for (int i = 0; i < columnCount; i++) {
                // 先考慮標題的長度
                columnWidths[i] = columnNames.get(i).length();
                // 再考慮資料的長度
                for (List<String> row : dataRows) {
                    if (row.get(i) != null && row.get(i).length() > columnWidths[i]) {
                        columnWidths[i] = row.get(i).length();
                    }
                }
                // 增加一些邊距
                columnWidths[i] += 2;
            }

            // 4. 輸出標題列
            for (int i = 0; i < columnCount; i++) {
                System.out.printf("%-" + columnWidths[i] + "s", columnNames.get(i));
            }
            System.out.println();

            // 5. 輸出分隔線
            for (int width : columnWidths) {
                for (int i = 0; i < width; i++) {
                    System.out.print("-");
                }
            }
            System.out.println();

            // 6. 輸出資料列
            for (List<String> row : dataRows) {
                for (int i = 0; i < columnCount; i++) {
                    System.out.printf("%-" + columnWidths[i] + "s", row.get(i));
                }
                System.out.println();
            }
        } catch (SQLException e) {
            System.err.println("⚠️ 顯示客人與其寵物失敗：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addOrder() {
        try {
            // 步驟一：輸入寵物 ID
            int petsId = getValidPetsId();

            // 步驟二：輸入服務編號
            List<Integer> servicesIds = getValidServicesIds();

            // 步驟三：輸入經手員工編號
            int checkByWho = getValidEmployeeId();

            // 取得當前時間
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String createTime = now.format(formatter);

            // 準備 SQL 語句
            String sql = "INSERT INTO orders (CreateTime, PetsID, Services1, Services2, Services3, CheckTime, CheckByWho) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);

            // 設定參數
            preparedStatement.setString(1, createTime);
            preparedStatement.setInt(2, petsId);
            preparedStatement.setInt(3, servicesIds.get(0)); // Services1 必填
            if (servicesIds.size() > 1) {
                preparedStatement.setInt(4, servicesIds.get(1)); // Services2
            } else {
                preparedStatement.setNull(4, Types.INTEGER);
            }
            if (servicesIds.size() > 2) {
                preparedStatement.setInt(5, servicesIds.get(2)); // Services3
            } else {
                preparedStatement.setNull(5, Types.INTEGER);
            }
            preparedStatement.setNull(6, Types.TIMESTAMP); // CheckTime 先設為 null
            preparedStatement.setInt(7, checkByWho);

            // 執行 SQL 語句
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("訂單新增成功！");
            } else {
                System.out.println("訂單新增失敗！");
            }

            preparedStatement.close();

        } catch (SQLException e) {
            System.err.println("新增訂單時發生錯誤：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 驗證寵物 ID 是否存在
    private static int getValidPetsId() throws SQLException {
        int petsId;
        while (true) {
            System.out.print("請輸入寵物 ID：");
            try {
                petsId = Integer.parseInt(scanner.nextLine());
                if (isPetsIdExists(petsId)) {
                    return petsId;
                } else {
                    System.out.println("寵物 ID 不存在，請重新輸入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("輸入格式錯誤，請輸入數字。");
            }
        }
    }

    // 驗證服務編號是否存在
    private static List<Integer> getValidServicesIds() throws SQLException {
        List<Integer> servicesIds = new ArrayList<>();
        while (true) {
            System.out.print("請輸入服務編號 (最多三個，輸入完成請輸入0)：");
            try {
                int serviceId = Integer.parseInt(scanner.nextLine());
                if (serviceId == 0) {
                    if (servicesIds.size() > 0) {
                        return servicesIds;
                    } else {
                        System.out.println("至少需要輸入一個服務編號。");
                    }
                } else if (isServiceIdExists(serviceId)) {
                    if (servicesIds.size() < 3) {
                        servicesIds.add(serviceId);
                    } else {
                        System.out.println("最多只能輸入三個服務編號。");
                    }
                } else {
                    System.out.println("服務編號不存在，請重新輸入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("輸入格式錯誤，請輸入數字。");
            }
        }
    }

    // 驗證員工編號是否存在
    private static int getValidEmployeeId() throws SQLException {
        int employeeId;
        while (true) {
            System.out.print("請輸入經手員工編號：");
            try {
                employeeId = Integer.parseInt(scanner.nextLine());
                if (isEmployeeIdExists(employeeId)) {
                    return employeeId;
                } else {
                    System.out.println("員工編號不存在，請重新輸入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("輸入格式錯誤，請輸入數字。");
            }
        }
    }

    // 檢查寵物 ID 是否存在
    private static boolean isPetsIdExists(int petsId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pets WHERE PetsID = ?";
        PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);
        preparedStatement.setInt(1, petsId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        preparedStatement.close();
        return count > 0;
    }

    // 檢查服務編號是否存在
    private static boolean isServiceIdExists(int serviceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM servicepricing WHERE ServicePricingID = ?";
        PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);
        preparedStatement.setInt(1, serviceId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        preparedStatement.close();
        return count > 0;
    }

    // 檢查員工編號是否存在
    private static boolean isEmployeeIdExists(int employeeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM employees WHERE EmployeesID = ?";
        PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);
        preparedStatement.setInt(1, employeeId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        preparedStatement.close();
        return count > 0;
    }

    private static void checkout() {
        System.out.println("\n=== 結帳 ===");
        System.out.print("請輸入要結帳的訂單編號：");
        int orderId = scanner.nextInt();
        scanner.nextLine(); // Consume the newline character

        try {
            // 取得當前時間
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String checkTime = now.format(formatter);

            // 準備 SQL 語句
            String sql = "UPDATE orders SET CheckTime = ? WHERE OrdersID = ?";
            PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);

            // 設定參數
            preparedStatement.setString(1, checkTime);
            preparedStatement.setInt(2, orderId);

            // 執行 SQL 語句
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("訂單結帳成功！");
            } else {
                System.out.println("訂單結帳失敗，可能訂單編號不存在。");
            }

            preparedStatement.close();

        } catch (SQLException e) {
            System.err.println("結帳時發生錯誤：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addCustomer() {
        System.out.println("\n=== 新增客人 ===");
        try {
            // 步驟一：輸入客人姓名
            System.out.print("請輸入客人姓名：");
            String customerName = scanner.nextLine();

            // 步驟二：輸入客人電話號碼
            System.out.print("請輸入客人電話號碼：");
            String customerPhoneNumber = scanner.nextLine();

            // 步驟三：輸入客人電子郵件
            System.out.print("請輸入客人電子郵件：");
            String customerMail = scanner.nextLine();

            // 步驟四：自動填入註冊日期
            LocalDate now = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String registrationDate = now.format(formatter);

            // 準備 SQL 語句
            String sql = "INSERT INTO customers (CustomersName, CustomersPhoneNumbers, CustomersMail, CustomersRegistrationDate) VALUES (?, ?, ?, ?)";
            PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);

            // 設定參數
            preparedStatement.setString(1, customerName);
            preparedStatement.setString(2, customerPhoneNumber);
            preparedStatement.setString(3, customerMail);
            preparedStatement.setString(4, registrationDate);

            // 執行 SQL 語句
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("客人新增成功！");
            } else {
                System.out.println("客人新增失敗！");
            }

            preparedStatement.close();

        } catch (SQLException e) {
            System.err.println("新增客人時發生錯誤：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addPet() {
        System.out.println("\n=== 新增寵物 ===");
        try {
            // 步驟一：輸入寵物姓名
            System.out.print("請輸入寵物姓名：");
            String petName = scanner.nextLine();

            // 步驟二：輸入寵物年齡
            int petAge = getValidPetAge();

            // 步驟三：輸入寵物主人 ID
            int petOwnerId = getValidPetOwnerId();

            // 步驟四：輸入寵物類別 ID
            int petCategoryId1 = getValidPetCategoryId1();

            // 步驟五：輸入寵物分類 ID
            int petCategoryId2 = getValidPetCategoryId2();

            // 步驟六：輸入特殊疾病
            System.out.print("請輸入特殊疾病 (若無請留空)：");
            String specialDiseases = scanner.nextLine();

            // 步驟七：輸入備註
            System.out.print("請輸入備註 (若無請留空)：");
            String remark = scanner.nextLine();

            // 準備 SQL 語句
            String sql = "INSERT INTO pets (PetsName, PetsAge, PetsOwnersID, PetsCategory1, PetsCategory2, SpecialDiseases, Remark) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);

            // 設定參數
            preparedStatement.setString(1, petName);
            preparedStatement.setInt(2, petAge);
            preparedStatement.setInt(3, petOwnerId);
            preparedStatement.setInt(4, petCategoryId1);
            preparedStatement.setInt(5, petCategoryId2);
            preparedStatement.setString(6, specialDiseases);
            preparedStatement.setString(7, remark);

            // 執行 SQL 語句
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("寵物新增成功！");
            } else {
                System.out.println("寵物新增失敗！");
            }

            preparedStatement.close();

        } catch (SQLException e) {
            System.err.println("新增寵物時發生錯誤：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 驗證寵物年齡
    private static int getValidPetAge() {
        int petAge;
        while (true) {
            System.out.print("請輸入寵物年齡：");
            try {
                petAge = Integer.parseInt(scanner.nextLine());
                if (petAge >= 0) {
                    return petAge;
                } else {
                    System.out.println("寵物年齡不能為負數，請重新輸入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("輸入格式錯誤，請輸入數字。");
            }
        }
    }

    // 驗證寵物主人 ID 是否存在
    private static int getValidPetOwnerId() throws SQLException {
        int petOwnerId;
        while (true) {
            System.out.print("請輸入寵物主人 ID：");
            try {
                petOwnerId = Integer.parseInt(scanner.nextLine());
                if (isPetOwnerIdExists(petOwnerId)) {
                    return petOwnerId;
                } else {
                    System.out.println("寵物主人 ID 不存在，請重新輸入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("輸入格式錯誤，請輸入數字。");
            }
        }
    }

    // 驗證寵物類別 ID 是否存在
    private static int getValidPetCategoryId1() throws SQLException {
        int petCategoryId1;
        while (true) {
            System.out.print("請輸入寵物類別 ID：");
            try {
                petCategoryId1 = Integer.parseInt(scanner.nextLine());
                if (isPetCategoryId1Exists(petCategoryId1)) {
                    return petCategoryId1;
                } else {
                    System.out.println("寵物類別 ID 不存在，請重新輸入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("輸入格式錯誤，請輸入數字。");
            }
        }
    }

    // 驗證寵物分類 ID 是否存在
    private static int getValidPetCategoryId2() throws SQLException {
        int petCategoryId2;
        while (true) {
            System.out.print("請輸入寵物分類 ID：");
            try {
                petCategoryId2 = Integer.parseInt(scanner.nextLine());
                if (isPetCategoryId2Exists(petCategoryId2)) {
                    return petCategoryId2;
                } else {
                    System.out.println("寵物分類 ID 不存在，請重新輸入。");
                }
            } catch (NumberFormatException e) {
                System.out.println("輸入格式錯誤，請輸入數字。");
            }
        }
    }

    // 檢查寵物主人 ID 是否存在
    private static boolean isPetOwnerIdExists(int petOwnerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM customers WHERE CustomersID = ?";
        PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);
        preparedStatement.setInt(1, petOwnerId);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        preparedStatement.close();
        return count > 0;
    }

    // 檢查寵物類別 ID 是否存在
    private static boolean isPetCategoryId1Exists(int petCategoryId1) throws SQLException {
        String sql = "SELECT COUNT(*) FROM petscategories WHERE PetsCategoriesID = ?";
        PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);
        preparedStatement.setInt(1, petCategoryId1);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        preparedStatement.close();
        return count > 0;
    }

    // 檢查寵物分類 ID 是否存在
    private static boolean isPetCategoryId2Exists(int petCategoryId2) throws SQLException {
        String sql = "SELECT COUNT(*) FROM petsclassification WHERE PetsClassificationID = ?";
        PreparedStatement preparedStatement = DBConnect.getConnection().prepareStatement(sql);
        preparedStatement.setInt(1, petCategoryId2);
        ResultSet resultSet = preparedStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);
        preparedStatement.close();
        return count > 0;
    }
}
