# Sobriety Tracker App

A calming, motivational Android app that tracks your sobriety journey, celebrates milestones, and provides encouragement along the way.

## Features

- **Sobriety Counter**: Tracks days sober from your specified start date
- **Milestone Tracking**: Automatically identifies and celebrates meaningful milestones
- **Health Benefits**: Visual representation of physical, mental, and financial benefits
- **Custom Notifications**: Configurable reminders to help you stay motivated
- **Achievement System**: Earn badges for accomplishments in your sobriety journey
- **Journal**: Record thoughts, feelings, and experiences with mood and craving tracking
- **Emergency Help**: Quick access to resources during difficult moments
- **Inspirational Content**: Browse and favorite motivational quotes
- **Community Support**: Connect with resources and support groups
- **Data Backup/Restore**: Protect your progress data

## Screenshots

[Screenshots will be added here]

## Running the App

### Prerequisites

- Android Studio (latest version recommended)
- Android SDK installed
- An Android device or emulator running Android 7.0 (API level 24) or higher

### Setup Instructions

1. **Clone the repository**:
   ```
   git clone https://github.com/Dysprosed/SoberTime.git
   ```

2. **Open the project in Android Studio**:
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned repository folder and click "OK"

3. **Build and run the app**:
   - Connect an Android device to your computer via USB with developer options and USB debugging enabled
   - OR set up an Android Virtual Device (emulator) in Android Studio
   - Click the green "Run" button in Android Studio
   - Select your target device
   - Wait for the app to build and install

## Project Structure

- **Activities**:
  - `MainActivity`: Main interface showing day count and core features
  - `HealthBenefitsActivity`: Tracking of physical, mental, and financial benefits
  - `MilestonesActivity`: Displays achieved and upcoming milestones
  - `JournalActivity`: Manage journal entries with mood and craving tracking
  - `AchievementsActivity`: Track earned achievements and badges
  - `EmergencyHelpActivity`: Quick access to coping tools and support contacts
  - `InspirationActivity`: Browse and save motivational quotes
  - `CommunitySupportActivity`: Find meetings and resources
  - `SettingsActivity`: Configure app preferences and notifications
  - `BackupRestoreActivity`: Manage data backup and restore

- **Managers**:
  - `AchievementManager`: Handles achievement tracking and notifications
  - `NotificationHelper`: Manages scheduled reminders and alerts
  - `QuoteManager`: Provides inspirational content

- **Data**:
  - `DatabaseHelper`: SQLite database management
  - Local storage for user preferences and journal entries

## Customization

- **Appearance**: Modify colors.xml to change the app's color scheme
- **Notifications**: Configure timing and content in settings
- **Health Benefits**: Personalize calculations based on your consumption patterns
- **Milestones**: Track important days in your recovery journey

## Permissions

The app requires the following permissions:
- `RECEIVE_BOOT_COMPLETED`: To reschedule notifications after device restart
- `SCHEDULE_EXACT_ALARM`: For precise milestone notifications
- `POST_NOTIFICATIONS`: To display reminders and celebrations

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0) - see the [LICENSE](LICENSE) file for details.

This license ensures that the software remains free and open source, requiring that modifications and derivative works are also released under the same license.

## Acknowledgments

- Created with assistance from Claude by Anthropic
- Special thanks to the recovery community for inspiration