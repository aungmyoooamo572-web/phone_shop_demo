package com.example.demo.catalog.config;


import com.example.demo.catalog.dao.BrandRepository;
import com.example.demo.catalog.dao.CategoryRepository;
import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.entity.*;
import com.example.demo.user.dao.UserRepository;
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final PhoneRepository phoneRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 0. Default Admin User Auto-Initialization (အခြားစက်များတွင် အလိုအလျောက် admin အကောင့်ဆောက်ပေးမည်)
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setPhone("09123456789");
            admin.setAddress("Headquarters");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            System.out.println("Default Admin Account initialized: admin / admin123");
        }

        // Data ရှိပြီးသားဆိုရင် duplicate မဖြစ်အောင် ရပ်မည်
        if (phoneRepository.count() > 0) {
            System.out.println("Data already exists. Skipping Data Initialization.");
            return;
        }

        //System.out.println("Starting Phone Store Data Initialization...");

        // 1. Categories
        Map<String, Category> categories = new HashMap<>();
        String[] catNames = {"Flagship", "Gaming", "Mid-Range", "Budget"};
        String[] catDescs = {
                "Top-tier performance & premium build",
                "High refresh rates & powerful chips",
                "Best value for daily use and camera",
                "Affordable smartphones with solid battery"
        };
        for (int i = 0; i < catNames.length; i++) {
            Category cat = new Category();
            cat.setName(catNames[i]);
            cat.setDescription(catDescs[i]);
            categories.put(catNames[i], categoryRepository.save(cat));
        }

        // 2. Brands (၁၅ မျိုး)
        Map<String, Brand> brands = new HashMap<>();
        String[] brandNames = {
                "Apple", "Samsung", "Xiaomi", "Redmi", "POCO", "Vivo", "OPPO",
                "Realme", "OnePlus", "Google", "Honor", "Sony",
                "Nothing", "Tecno", "Infinix"
        };
        for (String bName : brandNames) {
            Brand b = new Brand();
            b.setName(bName);
            b.setDescription(bName + " Official Smartphones");
            b.setLogo("https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=300");
            brands.put(bName, brandRepository.save(b));
        }

        // 3. Phones, Images & Variants ထည့်သွင်းခြင်း Helper Method
        // 1. Apple
        createPhone(
                brands.get("Apple"),
                categories.get("Flagship"),
                "iPhone 18 Pro",
                "A3472",
                "Apple A20 Pro",
                new BigDecimal("6.3"),
                4056,
                "48MP",
                "iOS 27",
                LocalDate.of(2026, 9, 18),
                "https://mkt.sellup.com.sg/a/files/iphone-18-pro-burgundy-front-01.jpg",
                new Object[][]{
                        {"12GB", "256GB", "Glacier", new BigDecimal("4850000"), 15, "IP18P-12-256-GL"},
                        {"12GB", "512GB", "Glacier", new BigDecimal("5450000"), 12, "IP18P-12-512-GL"},
                        {"12GB", "256GB", "Burgundy", new BigDecimal("4850000"), 15, "IP18P-12-256-BG"},
                        {"12GB", "512GB", "Burgundy", new BigDecimal("5450000"), 10, "IP18P-12-512-BG"}
                }
        );

        // 2. Samsung
        createPhone(
                brands.get("Samsung"), categories.get("Flagship"),
                "Samsung Galaxy S24 Ultra", "SM-S928B", "Snapdragon 8 Gen 3", new BigDecimal("6.8"), 5000, "200MP", "One UI 6.1", LocalDate.of(2024, 1, 24),
                "https://cdn.reebelo.com/pim/products/P-SAMSUNGGALAXYS24ULTRA/BLK-image-0.jpg",
                new Object[][]{
                        {"12GB", "256GB", "Titanium Black", new BigDecimal("2650000"), 15, "S24U-12-256-BLK"},
                        {"12GB", "512GB", "Titanium Black", new BigDecimal("2950000"), 12, "S24U-12-512-BLK"},
                        {"12GB", "256GB", "Titanium Gray", new BigDecimal("2650000"), 15, "S24U-12-256-GRY"},
                        {"12GB", "512GB", "Titanium Gray", new BigDecimal("2950000"), 10, "S24U-12-512-GRY"}
                }
        );

        // 3. Xiaomi
        createPhone(
                brands.get("Xiaomi"), categories.get("Flagship"),
                "Xiaomi 17 Pro Max 5G", "25091PNB7C", "Snapdragon 8 Elite Gen 5", new BigDecimal("6.9"), 7500, "50MP", "HyperOS 3", LocalDate.of(2025, 9, 25),
                "/images/phones/xiaomi-17-pro-max-black.jpg",
                new Object[][]{
                        {"12GB", "512GB", "Black", new BigDecimal("2850000"), 15, "MI17PM-12-512-BLK", "/images/phones/xiaomi-17-pro-max-black.jpg"},
                        {"16GB", "1TB", "Black", new BigDecimal("3350000"), 10, "MI17PM-16-1TB-BLK", "/images/phones/xiaomi-17-pro-max-black.jpg"},
                        {"12GB", "512GB", "Purple", new BigDecimal("2850000"), 15, "MI17PM-12-512-PUR", "/images/phones/xiaomi-17-pro-max-purple.jpg"},
                        {"16GB", "1TB", "Purple", new BigDecimal("3350000"), 10, "MI17PM-16-1TB-PUR", "/images/phones/xiaomi-17-pro-max-purple.jpg"}
                }
        );

        // 4. Redmi
        createPhone(
                brands.get("Redmi"), categories.get("Mid-Range"),
                "Redmi Note 17 Pro Max", "2609FRA74G", "Snapdragon 6 Gen 5", new BigDecimal("6.83"), 6200, "50MP", "Android 16, HyperOS 3", LocalDate.of(2026, 8, 27),
                "https://mkt.sellup.com.sg/a/files/xiaomi-redmi-note-17-pro-max-5g-black.jpg",
                new Object[][]{
                        {"8GB", "256GB", "Black", new BigDecimal("1390000"), 25, "RN17PM-8-256-BLK"},
                        {"12GB", "512GB", "Black", new BigDecimal("1650000"), 18, "RN17PM-12-512-BLK"}
                }
        );

        createPhone(
                brands.get("Redmi"), categories.get("Gaming"),
                "Redmi Turbo 5 Max", "26030RKB8C", "Dimensity 9500s (3 nm)", new BigDecimal("6.83"), 9000, "50MP Dual | 20MP Selfie", "Android 16, HyperOS 3", LocalDate.of(2026, 8, 15),
                "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-Orange.webp",
                new Object[][]{
                        {"12GB", "256GB", "Orange", new BigDecimal("2089000"), 20, "3101020373", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-Orange.webp"},
                        {"12GB", "256GB", "Black", new BigDecimal("2089000"), 20, "3101020370", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-Black.webp"},
                        {"12GB", "256GB", "White", new BigDecimal("2089000"), 15, "3101020371", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-White.webp"},
                        {"12GB", "256GB", "Blue", new BigDecimal("2089000"), 15, "3101020372", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-Blue.webp"},
                        {"12GB", "512GB", "Orange", new BigDecimal("2355000"), 15, "3101020377", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-Orange.webp"},
                        {"12GB", "512GB", "Black", new BigDecimal("2355000"), 15, "3101020374", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-Black.webp"},
                        {"12GB", "512GB", "White", new BigDecimal("2355000"), 12, "3101020375", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-White.webp"},
                        {"12GB", "512GB", "Blue", new BigDecimal("2355000"), 12, "3101020376", "https://anycallmobilemm.com/wp-content/uploads/2026/08/Redmi-Turbo-5-Max-5G-China-Version-Blue.webp"}
                }
        );


        // 5. POCO
        createPhone(
                brands.get("POCO"), categories.get("Gaming"),
                "POCO X8 Pro Max", "25030RKC8G", "MediaTek Dimensity 9500s", new BigDecimal("6.8"), 8500, "50MP OIS", "Xiaomi HyperOS 3", LocalDate.of(2026, 3, 11),
                "/images/phones/poco-x8-pro-max-black.png",
                new Object[][]{
                        {"12GB", "256GB", "Black", new BigDecimal("1650000"), 15, "POCO-X8PM-12-256-BLK", "/images/phones/poco-x8-pro-max-black.png"},
                        {"12GB", "512GB", "Black", new BigDecimal("1950000"), 12, "POCO-X8PM-12-512-BLK", "/images/phones/poco-x8-pro-max-black.png"},
                        {"12GB", "256GB", "White", new BigDecimal("1650000"), 15, "POCO-X8PM-12-256-WHT", "/images/phones/poco-x8-pro-max-white.jpg"},
                        {"12GB", "512GB", "White", new BigDecimal("1950000"), 12, "POCO-X8PM-12-512-WHT", "/images/phones/poco-x8-pro-max-white.jpg"},
                        {"12GB", "256GB", "Blue", new BigDecimal("1650000"), 15, "POCO-X8PM-12-256-BLU", "/images/phones/poco-x8-pro-max-blue.jpg"},
                        {"12GB", "512GB", "Blue", new BigDecimal("1950000"), 12, "POCO-X8PM-12-512-BLU", "/images/phones/poco-x8-pro-max-blue.jpg"}
                }
        );

        createPhone(
                brands.get("POCO"), categories.get("Flagship"),
                "POCO F9 Ultra", "26079PCE7G", "Snapdragon 8 Elite Gen 5", new BigDecimal("6.9"), 8050, "200MP Triple OIS | 32MP Front", "Android 16, HyperOS 3", LocalDate.of(2026, 9, 12),
                "https://fdn2.gsmarena.com/vv/bigpic/xiaomi-poco-f9-ultra.jpg",
                new Object[][]{
                        {"16GB", "512GB", "Black", new BigDecimal("3450000"), 15, "POCO-F9U-16-512-BLK", "https://fdn2.gsmarena.com/vv/bigpic/xiaomi-poco-f9-ultra.jpg"},
                        {"16GB", "512GB", "Dark Cherry", new BigDecimal("3450000"), 12, "POCO-F9U-16-512-CHY", "https://www.mobiledokan.com/media/xiaomi-poco-f9-ultra-dark-cherry-back-side-image.webp"},
                        {"16GB", "1TB", "Black", new BigDecimal("3950000"), 10, "POCO-F9U-16-1TB-BLK", "https://fdn2.gsmarena.com/vv/bigpic/xiaomi-poco-f9-ultra.jpg"},
                        {"16GB", "1TB", "Dark Cherry", new BigDecimal("3950000"), 8, "POCO-F9U-16-1TB-CHY", "https://www.mobiledokan.com/media/xiaomi-poco-f9-ultra-dark-cherry-back-side-image.webp"}
                }
        );


        // 6. Vivo
        createPhone(
                brands.get("Vivo"), categories.get("Mid-Range"),
                "vivo V70", "V2538", "Snapdragon 7 Gen 4", new BigDecimal("6.59"), 6500, "50MP", "OriginOS 6", LocalDate.of(2026, 2, 19),
                "https://mkt.sellup.com.sg/a/files/vivo-v70-alpine-gray-display.jpg",
                new Object[][]{
                        {"12GB", "256GB", "Alpine Gray", new BigDecimal("2150000"), 15, "V70-12-256-GRY"},
                        {"12GB", "512GB", "Alpine Gray", new BigDecimal("2450000"), 12, "V70-12-512-GRY"}
                }
        );

        // 7. OPPO
        createPhone(
                brands.get("OPPO"),
                categories.get("Mid-Range"), // Reno series သည် Mid-Range / Premium Mid-Range category တွင် အကျုံးဝင်ပါသည်
                "OPPO Reno 16 5G",
                "CPH2865",
                "Snapdragon 7 Gen 4",
                new BigDecimal("6.32"),
                6700,
                "50MP",
                "ColorOS 16",
                LocalDate.of(2026, 7, 9),
                "https://mkt.sellup.com.sg/a/files/oppo-reno-16-5g-pop-white.jpg",
                new Object[][]{
                        {"12GB", "256GB", "Pop White", new BigDecimal("1950000"), 15, "OPR16-12-256-WHT"},
                        {"12GB", "256GB", "Twilight Violet", new BigDecimal("1950000"), 12, "OPR16-12-256-VIO"}
                }
        );

        // 8. Realme
        createPhone(
                brands.get("Realme"), categories.get("Gaming"),
                "Realme Neo 8", "RMX8899", "Snapdragon 8 Gen 5", new BigDecimal("6.78"), 8000, "50MP OIS", "realme UI 7.0", LocalDate.of(2026, 1, 22),
                "/images/phones/realme-neo-8-mecha-gray.png",
                new Object[][]{
                        {"12GB", "256GB", "Mecha Gray", new BigDecimal("2150000"), 20, "RME-NEO8-12-256-GRY", "/images/phones/realme-neo-8-mecha-gray.png"},
                        {"16GB", "512GB", "Mecha Gray", new BigDecimal("2450000"), 15, "RME-NEO8-16-512-GRY", "/images/phones/realme-neo-8-mecha-gray.png"},
                        {"16GB", "1TB", "Mecha Gray", new BigDecimal("2750000"), 10, "RME-NEO8-16-1TB-GRY", "/images/phones/realme-neo-8-mecha-gray.png"},
                        {"12GB", "256GB", "Cyber Purple", new BigDecimal("2150000"), 20, "RME-NEO8-12-256-PUR", "/images/phones/realme-neo-8-cyber-purple.jpg"},
                        {"16GB", "512GB", "Cyber Purple", new BigDecimal("2450000"), 15, "RME-NEO8-16-512-PUR", "/images/phones/realme-neo-8-cyber-purple.jpg"},
                        {"16GB", "1TB", "Cyber Purple", new BigDecimal("2750000"), 10, "RME-NEO8-16-1TB-PUR", "/images/phones/realme-neo-8-cyber-purple.jpg"},
                        {"12GB", "256GB", "Origin White", new BigDecimal("2150000"), 20, "RME-NEO8-12-256-WHT", "/images/phones/realme-neo-8-origin-white.jpg"},
                        {"16GB", "512GB", "Origin White", new BigDecimal("2450000"), 15, "RME-NEO8-16-512-WHT", "/images/phones/realme-neo-8-origin-white.jpg"},
                        {"16GB", "1TB", "Origin White", new BigDecimal("2750000"), 10, "RME-NEO8-16-1TB-WHT", "/images/phones/realme-neo-8-origin-white.jpg"}
                }
        );

        // 9. OnePlus
        createPhone(
                brands.get("OnePlus"), categories.get("Flagship"),
                "OnePlus 15", "CPH2753", "Snapdragon 8 Elite Gen 5", new BigDecimal("6.78"), 7300, "50MP", "OxygenOS 16", LocalDate.of(2025, 10, 31),
                "https://mkt.sellup.com.sg/a/files/oneplus-15-infinite-black.jpg",
                new Object[][]{
                        {"12GB", "512GB", "Infinite Black", new BigDecimal("2850000"), 15, "OP15-12-512-BLK"},
                        {"16GB", "1TB", "Infinite Black", new BigDecimal("3450000"), 10, "OP15-16-1TB-BLK"},
                        {"12GB", "512GB", "Ultra Violet", new BigDecimal("2850000"), 12, "OP15-12-512-PUR"},
                        {"16GB", "1TB", "Ultra Violet", new BigDecimal("3450000"), 8, "OP15-16-1TB-PUR"},
                        {"12GB", "512GB", "Sand Storm", new BigDecimal("2850000"), 14, "OP15-12-512-SND"},
                        {"16GB", "1TB", "Sand Storm", new BigDecimal("3450000"), 9, "OP15-16-1TB-SND"}
                }
        );

        // 10. Google
        createPhone(
                brands.get("Google"), categories.get("Flagship"),
                "Google Pixel 11", "GUJ0N, GPQQ7", "Google Tensor G6", new BigDecimal("6.3"), 5000, "48MP Triple OIS", "Android 17", LocalDate.of(2026, 8, 12),
                "/images/phones/google-pixel-11-pistachio.jpg",
                new Object[][]{
                        {"12GB", "256GB", "Pistachio", new BigDecimal("2850000"), 20, "PX11-12-256-PST", "/images/phones/google-pixel-11-pistachio.jpg"},
                        {"12GB", "512GB", "Pistachio", new BigDecimal("3250000"), 15, "PX11-12-512-PST", "/images/phones/google-pixel-11-pistachio.jpg"},
                        {"12GB", "256GB", "Frost", new BigDecimal("2850000"), 20, "PX11-12-256-FRS", "/images/phones/google-pixel-11-frost.jpg"},
                        {"12GB", "512GB", "Frost", new BigDecimal("3250000"), 15, "PX11-12-512-FRS", "/images/phones/google-pixel-11-frost.jpg"},
                        {"12GB", "256GB", "Obsidian", new BigDecimal("2850000"), 20, "PX11-12-256-OBS", "/images/phones/google-pixel-11-obsidian.jpg"},
                        {"12GB", "512GB", "Obsidian", new BigDecimal("3250000"), 15, "PX11-12-512-OBS", "/images/phones/google-pixel-11-obsidian.jpg"},
                        {"12GB", "256GB", "Hibiscus", new BigDecimal("2850000"), 20, "PX11-12-256-HBC", "/images/phones/google-pixel-11-hibiscus.jpg"},
                        {"12GB", "512GB", "Hibiscus", new BigDecimal("3250000"), 15, "PX11-12-512-HBC", "/images/phones/google-pixel-11-hibiscus.jpg"}
                }
        );

        // 11. Honor
        createPhone(
                brands.get("Honor"), categories.get("Flagship"),
                "Honor 600 Pro", "VKP-NX9", "Snapdragon 8 Elite", new BigDecimal("6.57"), 7000, "200MP", "MagicOS 10", LocalDate.of(2026, 4, 22),
                "https://mkt.sellup.com.sg/a/files/honor-600-pro-orange.jpg",
                new Object[][]{
                        {"12GB", "256GB", "Orange", new BigDecimal("2450000"), 15, "HN600P-12-256-ORG"},
                        {"12GB", "512GB", "Orange", new BigDecimal("2750000"), 12, "HN600P-12-512-ORG"}
                }
        );


        // 13. Sony
        createPhone(
                brands.get("Sony"), categories.get("Flagship"),
                "Sony Xperia 5 III", "XQ-BQ72, XQ-BQ52", "Snapdragon 888 5G", new BigDecimal("6.1"), 4500, "12MP Triple ZEISS", "Android 11", LocalDate.of(2021, 10, 1),
                "/images/phones/sony-xperia-5-iii-green.jpg",
                new Object[][]{
                        {"8GB", "128GB", "Green", new BigDecimal("1350000"), 15, "SONY5III-8-128-GRN", "/images/phones/sony-xperia-5-iii-green.jpg"},
                        {"8GB", "256GB", "Green", new BigDecimal("1550000"), 12, "SONY5III-8-256-GRN", "/images/phones/sony-xperia-5-iii-green.jpg"},
                        {"8GB", "128GB", "Black", new BigDecimal("1350000"), 15, "SONY5III-8-128-BLK", "/images/phones/sony-xperia-5-iii-black.jpg"},
                        {"8GB", "256GB", "Black", new BigDecimal("1550000"), 12, "SONY5III-8-256-BLK", "/images/phones/sony-xperia-5-iii-black.jpg"},
                        {"8GB", "128GB", "Pink", new BigDecimal("1350000"), 15, "SONY5III-8-128-PNK", "/images/phones/sony-xperia-5-iii-pink.jpg"},
                        {"8GB", "256GB", "Pink", new BigDecimal("1550000"), 12, "SONY5III-8-256-PNK", "/images/phones/sony-xperia-5-iii-pink.jpg"}
                }
        );

        // 14. Nothing
        createPhone(
                brands.get("Nothing"), categories.get("Mid-Range"),
                "Nothing Phone (2)", "A065", "Snapdragon 8+ Gen 1", new BigDecimal("6.7"), 4700, "50MP", "Nothing OS 2.6", LocalDate.of(2023, 7, 15),
                "https://mkt.sellup.com.sg/a/files/nothing-phone-2-dark-gray.jpg",
                new Object[][]{
                        {"12GB", "256GB", "Dark Gray", new BigDecimal("1890000"), 15, "NOTH2-12-256-GRY", "https://mkt.sellup.com.sg/a/files/nothing-phone-2-dark-gray.jpg"},
                        {"12GB", "512GB", "Dark Gray", new BigDecimal("2190000"), 10, "NOTH2-12-512-GRY", "https://mkt.sellup.com.sg/a/files/nothing-phone-2-dark-gray.jpg"},
                        {"12GB", "256GB", "White", new BigDecimal("1890000"), 15, "NOTH2-12-256-WHT", "https://mkt.sellup.com.sg/a/files/nothing-phone-2-white.jpg"},
                        {"12GB", "512GB", "White", new BigDecimal("2190000"), 10, "NOTH2-12-512-WHT", "https://mkt.sellup.com.sg/a/files/nothing-phone-2-white.jpg"}
                }
        );

        // 15. Tecno
        createPhone(
                brands.get("Tecno"), categories.get("Mid-Range"),
                "Tecno Camon 50 Ultra 5G", "CL8", "Dimensity 7400 Ultimate", new BigDecimal("6.78"), 6500, "50MP", "HIOS 16", LocalDate.of(2026, 3, 3),
                "https://www.mobiledokan.com/media/tecno-camon-50-ultra-nebula-titanium-official-color-image.webp",
                new Object[][]{
                        {"8GB", "256GB", "Nebula Titanium", new BigDecimal("1150000"), 20, "TCN-CM50U-8-256-TI"},
                        {"12GB", "512GB", "Nebula Titanium", new BigDecimal("1380000"), 15, "TCN-CM50U-12-512-TI"},
                        {"8GB", "256GB", "Moonshadow Black", new BigDecimal("1150000"), 16, "TCN-CM50U-8-256-BLK"},
                        {"12GB", "512GB", "Moonshadow Black", new BigDecimal("1380000"), 12, "TCN-CM50U-12-512-BLK"}
                }
        );

        createPhone(
                brands.get("Tecno"), categories.get("Budget"),
                "Tecno Spark Go 3", "BG6", "Unisoc T7250", new BigDecimal("6.7"), 5000, "13MP Dual | 5MP Selfie", "Android 14 (HiOS 14)", LocalDate.of(2026, 2, 6),
                "https://innwa.com.mm/storage/product/feature_image/202602060934tecno-spark-go3.webp",
                new Object[][]{
                        {"4GB", "64GB", "Titanium", new BigDecimal("609900"), 15, "4894947105289", "https://innwa.com.mm/storage/product/2496/202602060934tecno-spark-go3-titanium.webp"},
                        {"4GB", "64GB", "Blue", new BigDecimal("609900"), 10, "4894947105300", "https://innwa.com.mm/storage/product/2496/202602060934tecno-spark-go3-blue.webp"},
                        {"4GB", "64GB", "Aurora Purple", new BigDecimal("609900"), 12, "4894947105326", "https://innwa.com.mm/storage/product/2496/202602060934tecno-spark-go3-purple.webp"},
                        {"4GB", "64GB", "Black", new BigDecimal("609900"), 10, "4894947105265", "https://innwa.com.mm/storage/product/2496/202602060934tecno-spark-go3-black.webp"}
                }
        );


        // 16. Infinix
        createPhone(
                brands.get("Infinix"), categories.get("Gaming"),
                "Infinix Note 60 Pro", "X6878", "Qualcomm Snapdragon 7s Gen 4", new BigDecimal("6.78"), 6500, "50MP OIS", "XOS 16", LocalDate.of(2026, 3, 11),
                "/images/phones/infinix-note-60-pro-solar-orange.jpg",
                new Object[][]{
                        {"8GB", "128GB", "Solar Orange", new BigDecimal("850000"), 25, "INF-N60P-8-128-ORG", "/images/phones/infinix-note-60-pro-solar-orange.jpg"},
                        {"8GB", "256GB", "Solar Orange", new BigDecimal("950000"), 20, "INF-N60P-8-256-ORG", "/images/phones/infinix-note-60-pro-solar-orange.jpg"},
                        {"12GB", "256GB", "Solar Orange", new BigDecimal("1150000"), 15, "INF-N60P-12-256-ORG", "/images/phones/infinix-note-60-pro-solar-orange.jpg"},
                        {"8GB", "128GB", "Deep Ocean Blue", new BigDecimal("850000"), 25, "INF-N60P-8-128-BLU", "/images/phones/infinix-note-60-pro-deep-ocean-blue.jpg"},
                        {"8GB", "256GB", "Deep Ocean Blue", new BigDecimal("950000"), 20, "INF-N60P-8-256-BLU", "/images/phones/infinix-note-60-pro-deep-ocean-blue.jpg"},
                        {"12GB", "256GB", "Deep Ocean Blue", new BigDecimal("1150000"), 15, "INF-N60P-12-256-BLU", "/images/phones/infinix-note-60-pro-deep-ocean-blue.jpg"},
                        {"8GB", "128GB", "Mist Titanium", new BigDecimal("850000"), 25, "INF-N60P-8-128-TI", "/images/phones/infinix-note-60-pro-mist-titanium.jpg"},
                        {"8GB", "256GB", "Mist Titanium", new BigDecimal("950000"), 20, "INF-N60P-8-256-TI", "/images/phones/infinix-note-60-pro-mist-titanium.jpg"},
                        {"12GB", "256GB", "Mist Titanium", new BigDecimal("1150000"), 15, "INF-N60P-12-256-TI", "/images/phones/infinix-note-60-pro-mist-titanium.jpg"}
                }
        );

        createPhone(
                brands.get("Infinix"), categories.get("Budget"),
                "Infinix Note Edge", "X6880", "Dimensity 7100", new BigDecimal("6.78"), 6500, "50MP", "Android 15 (XOS 15)", LocalDate.of(2026, 2, 27),
                "https://innwa.com.mm/storage/product/feature_image/202602270746infinix-note-edge.webp",
                new Object[][]{
                        {"8GB", "256GB", "Titanium", new BigDecimal("1399900"), 20, "4894947111242", "https://innwa.com.mm/storage/product/2581/202602270746infinix-note-edge-5g-titanium-01.webp"},
                        {"8GB", "256GB", "Blue", new BigDecimal("1399900"), 15, "4894947111243", "https://innwa.com.mm/storage/product/2581/202602270746infinix-note-edge-5g-blue-01.webp"},
                        {"8GB", "256GB", "Green", new BigDecimal("1399900"), 12, "4894947111244", "https://innwa.com.mm/storage/product/2581/202602270746infinix-note-edge-5g-green-01.webp"}
                }
        );



        //System.out.println("Data Initialization Completed Successfully!");
    }

    private void createPhone(
            Brand brand, Category category,
            String name, String model, String processor, BigDecimal screenSize,
            Integer battery, String camera, String os, LocalDate releaseDate,
            String imageUrl, Object[][] variantsData
    ) {
        Phone phone = new Phone();
        phone.setName(name);
        phone.setModel(model);
        phone.setProcessor(processor);
        phone.setScreenSize(screenSize);
        phone.setBattery(battery);
        phone.setCamera(camera);
        phone.setOperatingSystem(os);
        phone.setReleaseDate(releaseDate);
        phone.setBrand(brand);
        phone.setCategory(category);
        phone.setDeleted(false);

        // Image
        PhoneImage image = new PhoneImage();
        image.setImageUrl(imageUrl);
        image.setPrimary(true);
        image.setPhone(phone);
        phone.getImages().add(image);

        // Variants
        for (Object[] v : variantsData) {
            PhoneVariant variant = new PhoneVariant();
            variant.setRam((String) v[0]);
            variant.setStorage((String) v[1]);
            variant.setColor((String) v[2]);
            variant.setPrice((BigDecimal) v[3]);
            variant.setStock((Integer) v[4]);
            variant.setSku((String) v[5]);
            String vImg = v.length > 6 ? (String) v[6] : imageUrl;
            variant.setImageUrl(vImg);
            variant.setActive(true);
            variant.setDeleted(false);
            variant.setPhone(phone);
            phone.getVariants().add(variant);

            // Add unique image to phone images gallery
            boolean exists = phone.getImages().stream().anyMatch(pi -> pi.getImageUrl().equals(vImg));
            if (!exists) {
                PhoneImage extraImage = new PhoneImage();
                extraImage.setImageUrl(vImg);
                extraImage.setPrimary(false);
                extraImage.setPhone(phone);
                phone.getImages().add(extraImage);
            }
        }

        phoneRepository.save(phone);
    }
}