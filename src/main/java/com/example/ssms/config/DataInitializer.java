package com.example.ssms.config;

import com.example.ssms.admin.entity.AdminProfile;
import com.example.ssms.admin.repository.AdminProfileRepository;
import com.example.ssms.auth.entity.User;
import com.example.ssms.auth.repository.UserRepository;
import com.example.ssms.security.AccountStatus;
import com.example.ssms.security.ProfileType;
import com.example.ssms.security.Role;
import com.example.ssms.security.crypto.Argon2PasswordEncoder;
import com.example.ssms.system.entity.SystemSetting;
import com.example.ssms.system.repository.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final Argon2PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AdminProfileRepository adminProfileRepository,
                           SystemSettingRepository systemSettingRepository,
                           Argon2PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedDefaultSettings();
        seedSuperAdmin();
    }

    private void seedDefaultSettings() {
        Map<String, String> defaultSettings = Map.ofEntries(
                Map.entry("school.name", "Standard Secondary School"),
                Map.entry("school.timezone", "Africa/Addis_Ababa"),
                Map.entry("school.workingDays", "[1,2,3,4,5]"),
                Map.entry("auth.cookieMode", "false"),
                Map.entry("auth.passwordMinLength", "12"),
                Map.entry("auth.maxFailedLogins", "5"),
                Map.entry("auth.lockoutMinutes", "15"),
                Map.entry("class.maxCapacity", "50"),
                Map.entry("course.allowCrossGradeElectives", "false"),
                Map.entry("teacher.defaultMaxWeeklyPeriods", "30"),
                Map.entry("teacher.enforceQualifications", "true"),
                Map.entry("attendance.teacherEditWindowHours", "24"),
                Map.entry("attendance.lateCountsAsPresent", "true"),
                Map.entry("attendance.excusedCountsAsPresent", "true"),
                Map.entry("attendance.dailyPresentPercent", "50"),
                Map.entry("attendance.excuseDeadlineDays", "7"),
                Map.entry("attendance.lowThresholdPercent", "85"),
                Map.entry("attendance.teacherCanReviewExcuses", "false"),
                Map.entry("student.suspendBlocksLogin", "false")
        );

        for (Map.Entry<String, String> entry : defaultSettings.entrySet()) {
            if (!systemSettingRepository.existsBySettingKey(entry.getKey())) {
                SystemSetting setting = new SystemSetting();
                setting.setSettingKey(entry.getKey());
                setting.setSettingValue(entry.getValue());
                setting.setDescription("Default system setting: " + entry.getKey());
                systemSettingRepository.save(setting);
            }
        }
        log.info("System settings seeded or verified.");
    }

    private void seedSuperAdmin() {
        String adminEmail = "admin@school.example.com";
        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            User user = new User();
            user.setEmail(adminEmail);
            user.setPasswordHash(passwordEncoder.encode("Admin123456!"));
            user.setRole(Role.SUPER_ADMIN);
            user.setStatus(AccountStatus.ACTIVE);
            user.setMustChangePassword(false);
            user.setProfileType(ProfileType.ADMIN);
            User savedUser = userRepository.save(user);

            AdminProfile profile = new AdminProfile();
            profile.setUserId(savedUser.getId());
            profile.setFirstName("Super");
            profile.setLastName("Admin");
            profile.setDepartment("System Administration");
            profile.setJobTitle("Chief Administrator");
            AdminProfile savedProfile = adminProfileRepository.save(profile);

            savedUser.setProfileId(savedProfile.getId());
            userRepository.save(savedUser);
            log.info("Default SUPER_ADMIN created: {}", adminEmail);
        }
    }
}
