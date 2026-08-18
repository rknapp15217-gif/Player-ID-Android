# PlayerID Android App

PlayerID is an Android application that uses **real-time augmented reality** and computer vision to identify players on a field by their jersey numbers and display their names in **3D text overlays** that persist and track in the live camera view.

## 🚀 Real-Time AR Features

- **Live AR Camera Feed**: Continuous real-time video processing using ARCore
- **3D Text Overlays**: Player names appear as floating 3D text in world space  
- **Spatial Tracking**: Text labels stay positioned relative to players as you move
- **World-Scale AR**: Text appears at proper depth and distance in the real world
- **Motion Tracking**: Labels persist and track smoothly as the camera moves
- **60fps Performance**: Optimized for real-time AR at full frame rate

## Core Features

- **Jersey Number Recognition**: Uses ML Kit for real-time text detection
- **Player Database**: Instant lookup of player information with Room database
- **3D AR Text Bubbles**: Custom AR overlays with player names
- **Team Management**: Tap-to-learn team colors and collaborative editing
- **Admin Panel**: Complete player database management
- **Academic Year Tracking**: Color-coded academic years (Freshman: Green, Sophomore: Blue, Junior: Orange, Senior: Red)

## Technologies Used

- **ARCore**: Full world tracking and spatial mapping for true 3D AR
- **ML Kit**: Google's machine learning for text recognition and OCR
- **Jetpack Compose**: Modern Android UI toolkit
- **CameraX**: High-resolution camera capture and analysis
- **Room Database**: Local SQLite database with Kotlin coroutines
- **Kotlin Coroutines & Flow**: Reactive programming for real-time data
- **Material 3**: Google's latest design system
- **OpenAI Chat Completions API**: AI-assisted Compose UI generation and design validation

## AI UI Designer Integration

The app now includes a ChatGPT-backed design module in `app/src/main/java/com/playerid/app/ui/ai/`:
- `OpenAIClient.kt` - secure API client wrapper
- `DesignSystemAnalyzer.kt` - extracts Spotr design tokens/components
- `UIDesignGenerator.kt` - generates Compose screen code from requirements
- `DesignValidator.kt` - checks generated code for design-system compliance
- `DesignSystemPrompts.kt` - shared system + instruction prompts

Use the **AI Design** route (`design_system`) to open `DesignSystemExplorer` and run:
- design system analysis
- AI screen generation requests
- design consistency checks

### OpenAI Key Setup (Local Development)

1. Copy `.env.example` to `.env`
2. Set `OPENAI_API_KEY`
3. Expose the key to Gradle (environment variable or Gradle property)

Do not commit real API keys.

## Requirements

- **Android 7.0+ (API 24+)**
- **Android Studio**: Latest version with Kotlin support
- **ARCore Support**: Device must support ARCore
- **Camera**: Rear-facing camera required
- **RAM**: 4GB+ recommended for optimal AR performance

## Development Setup

### Prerequisites

1. **Android Studio** (free from Google)
2. **Android SDK** with API 24+ 
3. **ARCore supported device** for testing
4. **Kotlin plugin** (included in Android Studio)

### Installation

1. **Clone or Download** this project
2. **Open in Android Studio**:
   ```bash
   # Navigate to the PlayerID-Android folder
   cd PlayerID-Android
   # Open in Android Studio
   studio .
   ```
3. **Sync Gradle** dependencies
4. **Connect Android device** or start emulator
5. **Build and Run** (Ctrl+F9, then Ctrl+F10)

### Project Structure

```
PlayerID-Android/
├── app/
│   ├── src/main/java/com/playerid/app/
│   │   ├── MainActivity.kt              # Main app entry point
│   │   ├── PlayerIDApp.kt               # Main Compose app with navigation
│   │   ├── data/                        # Data models and database
│   │   │   ├── Player.kt                # Player data class and enums
│   │   │   ├── PlayerDao.kt             # Room database interface
│   │   │   └── PlayerDatabase.kt        # Room database setup
│   │   ├── viewmodels/                  # ViewModels for state management
│   │   │   ├── PlayerViewModel.kt       # Player data management
│   │   │   ├── TeamViewModel.kt         # Team selection and management
│   │   │   └── AuthViewModel.kt         # Admin authentication
│   │   ├── ui/screens/                  # Compose UI screens
│   │   │   ├── CameraScreen.kt          # AR camera with detection
│   │   │   ├── PlayersScreen.kt         # All players list (read-only)
│   │   │   ├── TeamScreen.kt            # Team management
│   │   │   ├── AdminScreen.kt           # Admin panel with full CRUD
│   │   │   └── SettingsScreen.kt        # App settings
│   │   ├── ar/                          # AR and detection components
│   │   │   ├── NumberDetectionAnalyzer.kt  # ML Kit text detection
│   │   │   └── AROverlayView.kt         # Custom AR overlay rendering
│   │   └── ui/theme/                    # Material 3 theming
│   ├── src/main/res/                    # Android resources
│   └── build.gradle                     # App dependencies
├── build.gradle                         # Project configuration
└── README.md                            # This file
```

## How It Works (Real-Time AR)

1. **ARCore Session**: Initializes world-tracking session with high-quality camera feed
2. **Frame Analysis**: CameraX captures frames, ML Kit processes for text detection
3. **Number Recognition**: ML Kit OCR identifies jersey numbers from detected text regions
4. **Database Lookup**: Numbers are instantly matched against Room database
5. **AR Overlay Rendering**: Custom View renders floating text bubbles in camera space
6. **Real-time Tracking**: Bubbles follow detected players using bounding box coordinates

## Usage

### Basic AR Camera

1. **Launch App**: Opens directly to camera view
2. **Grant Permissions**: Allow camera access for AR functionality
3. **Point at Players**: Aim camera at players wearing jerseys with visible numbers
4. **Automatic Detection**: Player names appear in bubbles above detected numbers
5. **Team Learning**: Tap on a player to learn your team colors

### Managing Players

#### **For Regular Users:**
1. **Tap "Players" tab** to view the player database
2. **Search for players** using the search bar
3. **View player details** including name, position, and academic year

#### **For Team Members:**
1. **Tap "My Team" tab** to access team features
2. **Select your team** from the available options
3. **Collaborative editing** with team-specific player management:

**🤝 Team Features:**
- **Add Team Players**: Complete form with number, name, position, and academic year
- **Edit Team Players**: Modify player information
- **Remove Team Players**: Delete with confirmation
- **Team-only View**: See only your team's players in AR camera
- **Color Learning**: Tap any player to automatically learn team colors

#### **For Administrators:**
1. **Tap "Admin" tab** to access administrator features
2. **Enter admin password**: `PlayerID2025!`
3. **Full database management** with the following capabilities:

**🔧 Admin Features:**
- **Add New Players**: Complete form with number, name, position, team, and academic year
- **Edit Existing Players**: Modify any player information
- **Delete Players**: Remove players with confirmation dialog
- **Search & Filter**: Find players by name, number, team, or academic year
- **Statistics View**: See total players and team counts
- **Bulk Operations**: Manage multiple players efficiently

**📊 Admin Interface:**
- **Real-time search** across all player fields
- **Statistics cards** showing total players and teams
- **Color-coded academic years** (Freshman: Green, Sophomore: Blue, Junior: Orange, Senior: Red)
- **Instant updates** with reactive UI

## Android-Specific Features

### ARCore Integration
- **World Tracking**: Full 6DOF tracking for precise AR placement
- **Plane Detection**: Automatic ground plane detection for stability
- **Light Estimation**: Realistic lighting for AR text rendering
- **Occlusion Handling**: Basic depth understanding

### ML Kit Advantages  
- **On-Device Processing**: No internet required for text detection
- **Optimized Performance**: Hardware-accelerated text recognition
- **Multi-Language Support**: Supports various character sets
- **Real-time Processing**: Designed for live camera feeds

### Material 3 Design
- **Dynamic Color**: Adapts to system theme on Android 12+
- **Responsive Layout**: Works on phones and tablets
- **Accessibility**: Full screen reader and keyboard navigation support
- **Dark Theme**: Automatic dark/light theme switching

## Performance Considerations

- **Real-time Processing**: Optimized for 60fps camera performance
- **Battery Efficiency**: Efficient camera and ML processing
- **Memory Management**: Proper lifecycle management for AR resources
- **Thermal Management**: Prevents overheating during extended use

## Customization

### Adding Players

Use the Admin panel or modify the sample data in `PlayerViewModel.kt`:

```kotlin
Player(
    number = 42,
    name = "Your Player",
    position = "Position",
    team = "Team Name",
    academicYear = "Senior"
)
```

### Improving Detection

- **Better Lighting**: Ensure good lighting conditions for optimal text detection
- **Camera Distance**: Maintain appropriate distance from players (2-10 meters)
- **Jersey Contrast**: High contrast jerseys work better for number detection
- **Stable Positioning**: Keep camera steady for better tracking

## Free Development

This app uses only **free Google services** and can be developed without any paid software:

✅ **Android Studio** - Free IDE from Google  
✅ **Kotlin** - Open source programming language  
✅ **Android SDK** - All frameworks included with Android Studio  
✅ **ARCore** - Free AR framework from Google  
✅ **ML Kit** - Free machine learning APIs  
✅ **Device Testing** - Free with any Android device  
✅ **Emulator Testing** - Included with Android Studio  

**Note**: Google Play Store publishing requires a one-time $25 registration fee, but development and personal testing are completely free.

## Future Enhancements

- [ ] **Custom ML Model**: Train specialized model for sports jersey numbers
- [ ] **Multiple Sport Support**: Basketball, football, soccer jersey formats
- [ ] **Cloud Sync**: Firebase integration for team collaboration
- [ ] **Video Recording**: Record AR sessions with overlays
- [ ] **Statistics**: Player appearance tracking and analytics
- [ ] **Offline Maps**: ARCore Cloud Anchors for persistent placement

## Troubleshooting

### ARCore Issues
- Ensure device supports ARCore (check [ARCore supported devices](https://developers.google.com/ar/devices))
- Update Google Play Services for AR
- Restart app if AR tracking fails

### Camera Not Working
- Grant camera permissions in Android Settings > Apps > PlayerID > Permissions
- Ensure camera is not being used by another app
- Restart device if camera preview appears black

### No Player Names Appearing
- Verify players exist in the database
- Ensure jersey numbers are clearly visible and well-lit
- Check that numbers are not obscured or at extreme angles
- Try different camera distances (2-10 meters optimal)

### App Performance Issues
- Close other apps to free up memory
- Ensure device meets minimum requirements (Android 7.0+, 4GB RAM)
- Lower camera quality in settings if performance is poor

## Contributing

1. **Fork the project**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)  
3. **Commit changes** (`git commit -m 'Add amazing feature'`)
4. **Push to branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

## License

This project is available under the MIT License. See the LICENSE file for more details.

## Support

For questions or support:
- **Create an issue** on GitHub
- **Check the troubleshooting section** above  
- **Review Google's documentation** for Android development, ARCore, and ML Kit

---

**📱 Built with Android** - Using ARCore, ML Kit, CameraX, Room, and Jetpack Compose