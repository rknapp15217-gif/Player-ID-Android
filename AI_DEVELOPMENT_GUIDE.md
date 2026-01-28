# Spotr (PlayerID) - Comprehensive AI Development Guide

## 🏆 Project Overview

**Spotr** (evolved from PlayerID) is a parent-focused sports video recording and sharing platform with advanced AR capabilities, viral referral systems, and subscription monetization. The app transforms basic player identification into a comprehensive sports content creation ecosystem.

### **Core Value Proposition**
- **For Parents**: Record, edit, and share personalized sports highlights with automatic player name overlays
- **For Teams**: Create professional-looking content with manual bubble placement and customization
- **For Growth**: Built-in viral referral system offering free year subscriptions for bringing 5 friends

---

## 🎯 App Architecture & Navigation

### **Application Structure**
```
PlayerIDApp (Main Compose Navigation)
├── CameraScreen (Tab 0) - Real-time AR + Video Recording
├── TeamScreen (Tab 1) - Team Management (CRUD)
├── ReferralScreen (Tab 2) - Viral Growth + QR Sharing
└── SettingsScreen (Tab 3) - App Configuration
```

### **Core Navigation Flow**
1. **Camera → Video Import → Video Editor → Video Export**
2. **Camera → Live Recording → Auto-processing**  
3. **Referral → QR Code → Friend Signup → Reward Tracking**

### **Key Entry Points**
- `MainActivity.kt` - Android lifecycle management
- `PlayerIDApp.kt` - Main Compose navigation hub
- **4-tab bottom navigation** - Primary user interface

---

## 🎬 Video System Architecture

### **Video Recording Pipeline**
```kotlin
// Real-time Recording with AR Overlays
VideoRecordingManager
├── CameraX Integration (HD quality)
├── Real-time bubble overlay rendering
├── Metadata tracking (timestamps, positions)
└── Auto-export with embedded names
```

### **Video Import & Manual Editing**
```kotlin
// Manual Video Enhancement Workflow
VideoImportScreen → VideoEditorScreen → VideoExportScreen
├── Gallery picker (any video file)
├── ExoPlayer video playback
├── Draggable name bubble placement
├── Player roster integration
└── Export with custom overlays
```

### **Core Video Components**
- **`VideoImportScreen.kt`** - Gallery selection with processing options
- **`VideoEditorScreen.kt`** - Interactive bubble placement with ExoPlayer
- **`VideoProcessingManager.kt`** - ML Kit + manual overlay rendering
- **`VideoExportScreen.kt`** - Final customization and sharing

### **Video Processing Modes**
1. **Auto-detect**: ML Kit jersey number recognition
2. **Manual**: Drag-and-drop bubble placement anywhere
3. **Hybrid**: Auto-detect + manual refinement

---

## 🤖 AR & Computer Vision System

### **Real-time AR Pipeline**
```kotlin
// AR Detection Architecture
CameraScreen 
├── CameraX live preview
├── NumberDetectionAnalyzer (ML Kit)
├── AROverlayView (custom rendering)
└── Real-time player database lookup
```

### **Core AR Components**
- **`NumberDetectionAnalyzer.kt`** - ML Kit text recognition for jersey numbers
- **`AROverlayView.kt`** - Custom view for AR bubble rendering
- **`CameraScreen.kt`** - Unified camera interface with AR overlays

### **Detection Workflow**
1. **Frame Capture**: CameraX provides continuous image analysis
2. **Text Detection**: ML Kit OCR extracts potential numbers
3. **Number Validation**: Filter for valid jersey numbers (0-99)
4. **Database Lookup**: Match numbers to player roster
5. **AR Rendering**: Display floating name bubbles in camera view

### **Performance Features**
- 60fps target performance
- Efficient memory management
- Thermal throttling protection
- Battery optimization

---

## 📊 Database Architecture

### **Core Data Models**
```kotlin
// Player Management
Player.kt - Jersey numbers, names, positions, teams
PlayerDao.kt - Room database operations
PlayerDatabase.kt - Database configuration

// Referral System  
ReferralData.kt - User codes, counts, rewards
ReferralDatabase.kt - Tracking and analytics

// Video System
VideoModels.kt - Clips, metadata, overlays
BubbleMetadata.kt - Position tracking, timestamps
```

### **Database Schema**
- **Players Table**: Core roster with jersey numbers, names, positions, contributor tracking (`addedBy`)
- **Teams Table**: Crowd-sourced team management (`createdBy`, timestamps, team metadata)
- **Referrals Table**: Tracking codes, counts, milestone rewards  
- **Videos Table**: Recorded clips with metadata
- **Bubbles Table**: Name overlay positions and timing

### **Crowd-Sourcing Features**
- **Team Creation**: Any user can create teams visible to all users
- **Contributor Tracking**: Every player addition tracked by user identifier
- **Community Statistics**: Real-time team and player counts across all users
- **Team Discovery**: Browse and contribute to teams created by others
- **Collaborative Rosters**: Multiple users can contribute to the same team

---

## 🔄 Viral Referral System

### **5-Friend Free Year Mechanics**
```kotlin
// Referral Growth Engine
ReferralManager.kt
├── Unique 6-character code generation
├── QR code sharing with deep links  
├── Progress tracking (5 friends = free year)
├── Milestone rewards and notifications
└── Subscription integration for rewards
```

### **Viral Components**
- **`ReferralScreen.kt`** - Progress tracking, QR codes, social sharing
- **`ReferralCodeEntryScreen.kt`** - New user referral code input
- **`ReferralManager.kt`** - Core referral logic and reward processing

### **Growth Mechanics**
1. **User gets unique 6-character code** (e.g., "SPORT1")
2. **QR code generation** for easy sharing
3. **Friend signup tracking** with code validation
4. **Milestone rewards**: 5 friends = 1 free year subscription
5. **Automatic subscription activation** when milestone reached

### **Viral Features**
- QR code sharing (auto-generated)
- Progress visualization (X/5 friends)
- Social media integration
- Reward notifications
- Deep linking for seamless onboarding

---

## 💰 Subscription & Monetization

### **Subscription Tiers**
```kotlin
// Freemium Model
SubscriptionManager.kt
├── Free Trial (14 days)
├── Premium ($9.99/year)  
├── Referral Free Year (5 friends)
└── Feature gating and paywall
```

### **Feature Restrictions**
- **Free**: Basic AR viewing, limited recording
- **Premium**: Unlimited recording, video editing, export
- **Referral Reward**: Full premium features for 1 year

### **Subscription Components**
- **`SubscriptionManager.kt`** - Core billing and feature management
- **`PaywallScreen.kt`** - Conversion interface
- **Integration with referral rewards** - Free year activation

---

## 🛠️ Technical Stack

### **Core Technologies**
```gradle
// Android Modern Stack
- Kotlin + Jetpack Compose (UI)
- CameraX + ARCore (AR functionality)
- ML Kit (text recognition)
- Room Database (local storage)
- ExoPlayer/Media3 (video playback)
- Coroutines + StateFlow (reactive programming)
```

### **Key Dependencies**
```gradle
// Computer Vision & AR
implementation 'com.google.ar:core:1.41.0'
implementation 'com.google.mlkit:text-recognition:16.0.0'
implementation 'androidx.camera:camera-camera2:1.3.0'

// Video Processing
implementation 'androidx.media3:media3-exoplayer:1.2.0'
implementation 'androidx.media3:media3-ui:1.2.0'

// Database & Storage
implementation 'androidx.room:room-runtime:2.6.0'
implementation 'androidx.room:room-ktx:2.6.0'

// QR Codes & Sharing
implementation 'com.google.zxing:core:3.5.2'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'

// Subscriptions
implementation 'com.android.billingclient:billing-ktx:6.1.0'
```

### **Build Configuration**
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Java Version**: 17
- **AGP**: 8.1.2
- **Kotlin**: 1.9.10

---

## 📱 Screen-by-Screen Breakdown

### **1. CameraScreen.kt**
**Purpose**: Primary AR experience with live video recording
**Features**:
- Real-time jersey number detection
- Floating name bubbles in AR
- HD video recording with overlays
- Team color learning
- Import video button

### **2. VideoImportScreen.kt** 
**Purpose**: Import existing videos for manual enhancement
**Features**:
- Gallery video picker
- Processing mode selection (auto/manual/hybrid)
- Video preview before editing
- Batch processing capabilities

### **3. VideoEditorScreen.kt**
**Purpose**: Interactive video editing with draggable bubbles
**Features**:
- ExoPlayer video playback controls
- Draggable name bubble placement
- Player roster integration
- Real-time preview updates
- Timeline scrubbing

### **4. VideoExportScreen.kt**
**Purpose**: Final video customization and sharing
**Features**:
- Export options (resolution, watermarks)
- Social media sharing integration
- Custom overlay selection
- Progress tracking during export

### **5. ReferralScreen.kt**
**Purpose**: Viral growth mechanics and friend invitations
**Features**:
- Personal referral code display
- QR code generation
- Progress tracking (X/5 friends)
- Social sharing integration
- Milestone celebration

### **6. TeamScreen.kt** (Crowd-Sourced Team Management)
**Purpose**: Community-driven team and player management
**Features**:
- Crowd-sourced team creation and discovery
- Collaborative player roster building
- Team statistics and contributor tracking
- "Browse All Teams" community feature
- Real-time team and player counts
- Cross-user collaboration on team rosters

### **7. CrowdSourcedTeamsScreen.kt** (Team Discovery)
**Purpose**: Community team browser and statistics
**Features**:
- Browse all teams created by users
- Team statistics (player count, contributors)
- Team creation and selection
- Contributor transparency
- Real-time community insights

### **8. SettingsScreen.kt**
**Purpose**: App configuration and preferences
**Features**:
- Camera settings and detection sensitivity
- AR calibration and debug info
- Database export/import (TODO)
- Cache management (TODO)
- App version and privacy controls

---

## 🔧 Key Development Patterns

### **State Management**
```kotlin
// Reactive UI with StateFlow
class PlayerViewModel : ViewModel() {
    private val _detectedPlayers = MutableStateFlow<List<Player>>(emptyList())
    val detectedPlayers: StateFlow<List<Player>> = _detectedPlayers.asStateFlow()
}

// Compose integration
val detectedPlayers by viewModel.detectedPlayers.collectAsState()
```

### **Video Processing**
```kotlin
// ExoPlayer setup for video editing
val exoPlayer = remember {
    ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(videoUri))
        prepare()
    }
}
```

### **AR Overlay Rendering**
```kotlin
// Custom view for AR bubbles
class AROverlayView : View {
    override fun onDraw(canvas: Canvas) {
        detectedPlayers.forEach { player ->
            drawNameBubble(canvas, player.name, player.boundingBox)
        }
    }
}
```

---

## 🎨 Design System

### **Material 3 Theme**
- **Primary Color**: Blue (#1976D2) - Professional sports theme
- **Surface Colors**: Clean whites and light grays
- **Accent Colors**: Success green, warning orange
- **Typography**: Modern, readable fonts optimized for video overlays

### **Component Library**
- Consistent button styles
- Custom video player controls
- AR overlay bubble designs
- Progress indicators
- QR code styling

---

## 🚀 Performance Considerations

### **Camera & AR Optimization**
- 60fps target for smooth AR experience
- Efficient frame processing with background threads
- Memory management for continuous video analysis
- Battery optimization strategies

### **Video Processing**
- Hardware acceleration where possible
- Progressive loading for large video files
- Background processing for exports
- Thumbnail generation and caching

### **Database Performance**
- Room database with proper indexing
- Coroutine-based async operations
- Efficient player lookup algorithms
- Caching strategies for frequent queries

---

## 🧪 Testing Strategy

### **Unit Testing**
- ViewModel logic testing
- Database operations
- Referral calculation algorithms
- Video processing functions

### **UI Testing** 
- Compose UI testing
- Navigation flow validation
- Camera permission handling
- Video playback controls

### **Integration Testing**
- End-to-end referral flow
- Video recording → editing → export
- AR detection accuracy
- Subscription integration

---

## 🔮 Future Enhancements

### **Planned Features**
- **Advanced ML**: Custom jersey number recognition models
- **Cloud Sync**: Cross-device player rosters and videos
- **Team Analytics**: Performance tracking and statistics
- **Live Streaming**: Real-time AR overlays in live broadcasts
- **Multi-sport Support**: Expand beyond traditional jersey sports

### **Technical Improvements**
- **ARCore Improvements**: Better tracking and stability
- **Video Quality**: 4K recording and processing
- **Cloud Processing**: Server-side video enhancement
- **Advanced Sharing**: Direct social platform integration

---

## 📋 Development Workflow

### **Getting Started**
1. **Clone repository** and open in Android Studio
2. **Sync Gradle** dependencies 
3. **Configure ARCore** for your test device
4. **Set up player database** with sample data
5. **Test camera permissions** and AR functionality

### **Key Development Areas**
- **AR/ML Features**: Focus on `ar/` package for detection improvements
- **Video System**: Work in `video/` package for recording/editing features  
- **Referral Growth**: Enhance `referral/` package for viral mechanics
- **UI/UX**: Modify `ui/screens/` for interface improvements
- **Data Layer**: Update `data/` package for database enhancements

### **Build & Deploy**
- **Development**: Use debug builds with test data
- **Testing**: Beta releases with analytics integration
- **Production**: Release builds with proper signing and optimization

---

## 💡 AI Assistant Guidelines

### **When Helping with Development**
1. **Always consider the complete user flow** - from camera to sharing
2. **Prioritize performance** - especially for AR and video processing
3. **Maintain viral growth focus** - referral system is key to success
4. **Keep UI consistency** - follow Material 3 design patterns
5. **Consider subscription impact** - feature gating and monetization

### **Key Files to Understand**
- **`PlayerIDApp.kt`** - Navigation and app structure
- **`CameraScreen.kt`** - Core AR experience  
- **`VideoEditorScreen.kt`** - Manual editing capabilities
- **`ReferralManager.kt`** - Growth mechanics
- **`SubscriptionManager.kt`** - Monetization logic

### **Development Priorities**
1. **User Experience**: Smooth, intuitive AR interactions
2. **Viral Growth**: Effective referral system implementation
3. **Content Creation**: Professional video editing capabilities
4. **Performance**: 60fps AR, fast video processing
5. **Monetization**: Clear value proposition for subscriptions

---

This comprehensive guide provides the Android Studio AI with complete context about Spotr's architecture, features, and development patterns to assist with effective code development, debugging, and feature enhancement.