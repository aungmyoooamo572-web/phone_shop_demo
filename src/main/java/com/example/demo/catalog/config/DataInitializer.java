package com.example.demo.catalog.config;


import com.example.demo.catalog.dao.BrandRepository;
import com.example.demo.catalog.dao.CategoryRepository;
import com.example.demo.catalog.dao.PhoneRepository;
import com.example.demo.catalog.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
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

    @Override
    @Transactional
    public void run(String... args) {
        // Data ရှိပြီးသားဆိုရင် duplicate မဖြစ်အောင် ရပ်မည်
        if (phoneRepository.count() > 0) {
            System.out.println("Data already exists. Skipping Data Initialization.");
            return;
        }

        System.out.println("Starting Phone Store Data Initialization...");

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

        // 2. Brands (၁၇ မျိုး)
        Map<String, Brand> brands = new HashMap<>();
        String[] brandNames = {
                "Apple", "Samsung", "Xiaomi", "Redmi", "POCO", "Vivo", "OPPO",
                "Realme", "OnePlus", "Google", "Honor", "Huawei", "Sony",
                "Nothing", "Tecno", "Infinix", "Itel"
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
                brands.get("Apple"), categories.get("Flagship"),
                "iPhone 16 Pro Max", "A3296", "Apple A18 Pro", new BigDecimal("6.9"), 4685, "48MP", "iOS 18", LocalDate.of(2024, 9, 20),
                "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600",
                new Object[][]{
                        {"8GB", "256GB", "Natural Titanium", new BigDecimal("4850000"), 15, "IP16PM-8-256-NT"},
                        {"8GB", "512GB", "Desert Titanium", new BigDecimal("5450000"), 10, "IP16PM-8-512-DT"}
                }
        );

        // 2. Samsung
        createPhone(
                brands.get("Samsung"), categories.get("Flagship"),
                "Samsung Galaxy S25 Ultra", "SM-S938B", "Snapdragon 8 Elite", new BigDecimal("6.9"), 5000, "200MP", "Android 15", LocalDate.of(2025, 1, 22),
                "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=600",
                new Object[][]{
                        {"12GB", "256GB", "Titanium Silverblue", new BigDecimal("4299000"), 20, "S25U-12-256-BLU"},
                        {"16GB", "512GB", "Titanium Black", new BigDecimal("4899000"), 12, "S25U-16-512-BLK"}
                }
        );

        // 3. Xiaomi
        createPhone(
                brands.get("Xiaomi"), categories.get("Flagship"),
                "Xiaomi 15 Pro", "24101PNB7C", "Snapdragon 8 Elite", new BigDecimal("6.7"), 6100, "50MP", "HyperOS 2.0", LocalDate.of(2024, 10, 29),
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600",
                new Object[][]{
                        {"16GB", "512GB", "Spruce Green", new BigDecimal("3350000"), 10, "MI15P-16-512-GRN"},
                        {"16GB", "1TB", "Liquid Silver", new BigDecimal("3850000"), 6, "MI15P-16-1TB-SLV"}
                }
        );

        // 4. Redmi
        createPhone(
                brands.get("Redmi"), categories.get("Mid-Range"),
                "Redmi Note 14 Pro+", "24090RA29C", "Snapdragon 7s Gen 3", new BigDecimal("6.7"), 6200, "50MP", "HyperOS", LocalDate.of(2024, 9, 26),
                "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=600",
                new Object[][]{
                        {"12GB", "256GB", "Mirror Porcelain White", new BigDecimal("1390000"), 25, "RN14PP-12-256-WHT"},
                        {"16GB", "512GB", "Midnight Black", new BigDecimal("1650000"), 18, "RN14PP-16-512-BLK"}
                }
        );

        // 5. POCO
        createPhone(
                brands.get("POCO"), categories.get("Gaming"),
                "POCO F6 Pro", "23113RKC6G", "Snapdragon 8 Gen 2", new BigDecimal("6.7"), 5000, "50MP", "HyperOS", LocalDate.of(2024, 5, 23),
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
                new Object[][]{
                        {"12GB", "512GB", "Black", new BigDecimal("1850000"), 14, "POCOF6P-12-512-BLK"},
                        {"16GB", "1TB", "White", new BigDecimal("2150000"), 8, "POCOF6P-16-1TB-WHT"}
                }
        );

        // 6. Vivo
        createPhone(
                brands.get("Vivo"), categories.get("Flagship"),
                "Vivo X200 Pro", "V2419", "Dimensity 9400", new BigDecimal("6.8"), 6000, "200MP", "OriginOS 5", LocalDate.of(2024, 10, 14),
                "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600",
                new Object[][]{
                        {"16GB", "512GB", "Titanium Gray", new BigDecimal("3199000"), 12, "X200P-16-512-GRY"},
                        {"16GB", "1TB", "Sapphire Blue", new BigDecimal("3699000"), 7, "X200P-16-1TB-BLU"}
                }
        );

        // 7. OPPO
        createPhone(
                brands.get("OPPO"), categories.get("Flagship"),
                "OPPO Find X8 Pro", "PKC110", "Dimensity 9400", new BigDecimal("6.8"), 5910, "50MP", "ColorOS 15", LocalDate.of(2024, 10, 24),
                "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600",
                new Object[][]{
                        {"16GB", "512GB", "Hoshino Black", new BigDecimal("3399000"), 10, "OPX8P-16-512-BLK"},
                        {"16GB", "1TB", "Moonlight White", new BigDecimal("3899000"), 5, "OPX8P-16-1TB-WHT"}
                }
        );

        // 8. Realme
        createPhone(
                brands.get("Realme"), categories.get("Gaming"),
                "Realme GT 7 Pro", "RMX5010", "Snapdragon 8 Elite", new BigDecimal("6.8"), 6500, "50MP", "realme UI 6.0", LocalDate.of(2024, 11, 4),
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
                new Object[][]{
                        {"12GB", "256GB", "Mars Orange", new BigDecimal("2450000"), 15, "RMEGT7-12-256-ORG"},
                        {"16GB", "512GB", "Star Trail Titanium", new BigDecimal("2850000"), 10, "RMEGT7-16-512-TI"}
                }
        );

        // 9. OnePlus
        createPhone(
                brands.get("OnePlus"), categories.get("Flagship"),
                "OnePlus 13", "CPH2653", "Snapdragon 8 Elite", new BigDecimal("6.8"), 6000, "50MP", "OxygenOS 15", LocalDate.of(2024, 10, 31),
                "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=600",
                new Object[][]{
                        {"16GB", "512GB", "Midnight Ocean", new BigDecimal("2950000"), 14, "OP13-16-512-BLU"},
                        {"24GB", "1TB", "Black Eclipse", new BigDecimal("3550000"), 8, "OP13-24-1TB-BLK"}
                }
        );

        // 10. Google
        createPhone(
                brands.get("Google"), categories.get("Flagship"),
                "Google Pixel 9 Pro XL", "GEC77", "Google Tensor G4", new BigDecimal("6.8"), 5060, "50MP", "Android 15", LocalDate.of(2024, 8, 22),
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600",
                new Object[][]{
                        {"16GB", "256GB", "Hazel", new BigDecimal("3450000"), 10, "PX9PXL-16-256-HZL"},
                        {"16GB", "512GB", "Obsidian", new BigDecimal("3950000"), 8, "PX9PXL-16-512-OBS"}
                }
        );

        // 11. Honor
        createPhone(
                brands.get("Honor"), categories.get("Flagship"),
                "Honor Magic7 Pro", "PTP-AN10", "Snapdragon 8 Elite", new BigDecimal("6.8"), 5850, "200MP", "MagicOS 9.0", LocalDate.of(2024, 10, 30),
                "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600",
                new Object[][]{
                        {"16GB", "512GB", "Moon Shadow Gray", new BigDecimal("3150000"), 9, "HNM7P-16-512-GRY"},
                        {"16GB", "1TB", "Snow White", new BigDecimal("3650000"), 5, "HNM7P-16-1TB-WHT"}
                }
        );

        // 12. Huawei
        createPhone(
                brands.get("Huawei"), categories.get("Flagship"),
                "Huawei Mate 70 Pro", "HBN-AL10", "Kirin 9100", new BigDecimal("6.9"), 5500, "50MP", "HarmonyOS NEXT", LocalDate.of(2024, 11, 26),
                "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=600",
                new Object[][]{
                        {"12GB", "512GB", "Spruce Green", new BigDecimal("3890000"), 8, "HWM70-12-512-GRN"},
                        {"16GB", "1TB", "Hyacinth Purple", new BigDecimal("4490000"), 4, "HWM70-16-1TB-PUR"}
                }
        );

        // 13. Sony
        createPhone(
                brands.get("Sony"), categories.get("Flagship"),
                "Sony Xperia 1 VI", "XQ-EC54", "Snapdragon 8 Gen 3", new BigDecimal("6.5"), 5000, "48MP", "Android 14", LocalDate.of(2024, 6, 7),
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
                new Object[][]{
                        {"12GB", "256GB", "Platinum Silver", new BigDecimal("3499000"), 7, "SONY1VI-12-256-SLV"},
                        {"12GB", "512GB", "Black", new BigDecimal("3999000"), 5, "SONY1VI-12-512-BLK"}
                }
        );

        // 14. Nothing
        createPhone(
                brands.get("Nothing"), categories.get("Mid-Range"),
                "Nothing Phone (2)", "A065", "Snapdragon 8+ Gen 1", new BigDecimal("6.7"), 4700, "50MP", "Nothing OS 2.6", LocalDate.of(2023, 7, 15),
                "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600",
                new Object[][]{
                        {"12GB", "256GB", "Dark Grey", new BigDecimal("1890000"), 15, "NOTH2-12-256-GRY"},
                        {"12GB", "512GB", "White", new BigDecimal("2190000"), 10, "NOTH2-12-512-WHT"}
                }
        );

        // 15. Tecno
        createPhone(
                brands.get("Tecno"), categories.get("Mid-Range"),
                "Tecno Camon 30 Premier", "CL9", "Dimensity 8200 Ultimate", new BigDecimal("6.8"), 5000, "50MP", "HIOS 14", LocalDate.of(2024, 5, 9),
                "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600",
                new Object[][]{
                        {"12GB", "512GB", "Alps Snowy Silver", new BigDecimal("1250000"), 20, "TCN-CM30P-12-512-SLV"}
                }
        );

        // 16. Infinix
        createPhone(
                brands.get("Infinix"), categories.get("Gaming"),
                "Infinix GT 20 Pro", "X6871", "Dimensity 8200 Ultimate", new BigDecimal("6.8"), 5000, "108MP", "XOS 14", LocalDate.of(2024, 5, 21),
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
                new Object[][]{
                        {"12GB", "256GB", "Mecha Silver", new BigDecimal("980000"), 30, "INF-GT20P-12-256-SLV"},
                        {"12GB", "256GB", "Mecha Blue", new BigDecimal("980000"), 25, "INF-GT20P-12-256-BLU"}
                }
        );

        // 17. Itel
        createPhone(
                brands.get("Itel"), categories.get("Budget"),
                "Itel S24", "S666LN", "MediaTek Helio G91", new BigDecimal("6.6"), 5000, "108MP", "itel OS 13", LocalDate.of(2024, 3, 29),
                "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600",
                new Object[][]{
                        {"8GB", "128GB", "Dawn White", new BigDecimal("420000"), 40, "ITL-S24-8-128-WHT"},
                        {"8GB", "256GB", "Coastline Blue", new BigDecimal("490000"), 35, "ITL-S24-8-256-BLU"}
                }
        );

        System.out.println("Data Initialization Completed Successfully!");
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
            variant.setImageUrl(imageUrl);
            variant.setActive(true);
            variant.setDeleted(false);
            variant.setPhone(phone);
            phone.getVariants().add(variant);
        }

        phoneRepository.save(phone);
    }
}