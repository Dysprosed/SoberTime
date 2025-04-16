SoberTime: Sobriety Tracking App
A calming, motivational Android app that tracks your sobriety journey, celebrates milestones, and provides encouragement along the way.
Features

Sobriety Counter: Tracks days sober from your specified start date
Daily Check-in System: Confirm your sobriety each day to build streaks
Milestone Tracking: Automatically identifies and celebrates meaningful milestones
Health Benefits: Visualize physical, mental, and financial benefits of sobriety
Achievement System: Earn badges for accomplishments in your sobriety journey
Journal: Record thoughts, feelings, and experiences with mood and craving tracking
Accountability Buddy: Add trusted contacts to support your sobriety journey
Emergency Help: Quick access to resources during difficult moments
Inspirational Content: Browse and favorite motivational quotes
Community Support: Connect with resources and support groups
Progress Reports: Visualize your journey with charts and statistics
Data Backup/Restore: Protect your progress data
Custom Notifications: Configurable reminders to help you stay motivated
Home Screen Widget: Keep your sobriety count visible right on your home screen

Screenshots
[Screenshots will be added here]
Running the App
Prerequisites

Android Studio (latest version recommended)
Android SDK installed
An Android device or emulator running Android 7.0 (API level 24) or higher

Setup Instructions

Clone the repository:
git clone https://github.com/Dysprosed/SoberTime.git

Open the project in Android Studio:

Launch Android Studio
Select "Open an existing Android Studio project"
Navigate to the cloned repository folder and click "OK"


Build and run the app:

Connect an Android device to your computer via USB with developer options and USB debugging enabled
OR set up an Android Virtual Device (emulator) in Android Studio
Click the green "Run" button in Android Studio
Select your target device
Wait for the app to build and install



Project Structure

Activities:

MainActivity: Main interface showing day count and core features
HealthBenefitsActivity: Tracking of physical, mental, and financial benefits
CheckInActivity: Daily confirmation of sobriety maintenance
JournalActivity: Manage journal entries with mood and craving tracking
JournalEntryActivity: Create and edit journal entries
AccountabilityBuddyActivity: Manage support contacts who can receive updates
AchievementsActivity: Track earned achievements and badges
EmergencyHelpActivity: Quick access to coping tools and support contacts
InspirationActivity: Browse and save motivational quotes
CommunitySupportActivity: Find meetings and resources
ProgressReportActivity: Visualize your progress with charts and statistics
SettingsActivity: Configure app preferences and notifications
BackupRestoreActivity: Manage data backup and restore
WelcomeActivity: Initial onboarding process for new users
AboutActivity: App information and open source details


Managers and Helpers:

SobrietyTracker: Core tracking of sobriety duration and status
AchievementManager: Handles achievement tracking and notifications
NotificationHelper: Manages scheduled reminders and alerts
QuoteManager: Provides inspirational content
MilestoneCelebration: Handles milestone celebration displays
SobrietyWidgetProvider: Manages home screen widget


Models:

Achievement: Represents achievements and milestones
AccountabilityBuddy: Represents a support contact
JournalEntry: Stores journal entry data with mood and cravings
Quote: Stores inspirational quotes
SupportResource: Represents external support resources


Adapters:

JournalAdapter: Displays journal entries
AchievementsAdapter: Displays achievements
BuddyAdapter: Manages accountability buddies
QuoteAdapter: Displays inspirational quotes
SupportResourceAdapter: Displays community resources


Data:

DatabaseHelper: SQLite database management for all app data
Local storage for user preferences, achievements, and journal entries



Customization

Appearance: Day/night mode support for comfortable viewing
Notifications: Configure timing and content in settings, including custom times
Health Benefits: Personalize calculations based on your consumption patterns
Milestones: Track important days in your recovery journey

Permissions
The app requires the following permissions:

RECEIVE_BOOT_COMPLETED: To reschedule notifications after device restart
SCHEDULE_EXACT_ALARM: For precise milestone notifications
POST_NOTIFICATIONS: To display reminders and celebrations
SEND_SMS: Optional - for accountability buddy notifications

Privacy and Security

All data is stored locally on your device
No personal information is transmitted or shared
Optional backup files are stored in your device's storage
No analytics or tracking is implemented

Contributing
Contributions are welcome! Please feel free to submit a Pull Request.
License
This project is licensed under the GNU General Public License v3.0 (GPL-3.0) - see the LICENSE file for details.
This license ensures that the software remains free and open source, requiring that modifications and derivative works are also released under the same license.
Acknowledgments

Created with assistance from Claude by Anthropic
Special thanks to the recovery community for inspiration