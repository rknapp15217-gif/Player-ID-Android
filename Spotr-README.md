# Spotr - AR Player Tracking & Highlight App

Spotr is a mobile app designed for parents of youth sports athletes to identify players in real-time using AR bubbles and record highlight clips with customizable overlays. The app emphasizes privacy, simplicity, and family-friendly sharing via text and email.

## 🎯 Core Purpose

- Help parents easily identify their child on the field using AR
- Record and share meaningful highlight clips with selected overlays  
- Enable private sharing via text/email with optional branding
- Build viral growth through trusted family networks

## 🚀 Key Features

### 🔵 Live AR Mode
- **Real-time Player Detection**: Jersey number and color recognition using ML Kit
- **Persistent AR Bubbles**: Player names stay visible until they leave screen
- **Smart Overlap Handling**: Maintains visibility even with overlapping players
- **Team Color Learning**: Tap any player to automatically learn team colors

### 📋 Privacy-First Roster Management
- **Firebase-Based Storage**: Secure cloud roster sync across team
- **QR Code Team Invites**: Easy team onboarding with scannable codes
- **OCR Roster Import**: Photo roster upload with automatic player extraction
- **Parent Opt-Out**: Parents can remove their child anytime
- **Manual Entry**: Full manual roster creation and editing

### 🎥 Video Recording & Export
- **Local Recording**: HD video with time-coded AR bubble metadata
- **Selective Export**: Parents choose which bubbles appear in final video
- **Custom Overlays**: Add celebration tags like "Winning Goal", "Great Save"
- **Optional Watermarking**: Spotr branding for viral growth

### 🏷️ Parent-Oriented Overlays
- **Pre-built Tags**: "Winning Goal", "Great Save", "Team Spirit", "First Game"
- **Custom Messages**: Add personalized celebration text
- **Timestamp Control**: Choose exactly when overlays appear
- **Professional Polish**: Smooth animations and positioning

### 📤 Smart Sharing Flow
- **Text/Email Integration**: Share directly from app with pre-filled messages
- **Branded Messaging**: "Created with Spotr — download here" with app link
- **AirDrop/USB Support**: Export for offline sharing
- **No Forced Social**: Privacy-focused sharing via trusted channels

### 🔐 Privacy & Safety
- **Parental Control**: Full opt-out capabilities for any parent
- **Local Storage**: All videos stored on device (no cloud upload in MVP)
- **Team-Only Access**: Rosters private to invited team members
- **Transparent Permissions**: Clear data usage and sharing policies

## 💰 Monetization Strategy

### Annual Subscription Model
- **$9.99/year per parent** (less than $1/month psychological pricing)
- **14-day Free Trial**: Full access to experience value
- **Family-Friendly**: Affordable for youth sports budgets

### Future Revenue Streams
- **Cloud Storage**: $1/month for highlight backup
- **AI Highlights**: $3-5/month for automatic best moment detection
- **Team Analytics**: Premium insights for coaches
- **Custom Branding**: White-label solutions for leagues

## 📈 Viral Growth Features

### Built-in Virality
- **QR Team Invites**: Seamless team onboarding
- **Branded Highlights**: Every shared video promotes the app
- **Push Notifications**: Alert parents when their child is tagged
- **Referral Program**: Free year for 5 successful referrals

### Network Effects
- **Team Collaboration**: More parents = better coverage
- **Cross-Pollination**: Teams play each other, spreading awareness
- **Trust-Based Growth**: Parents share with other parents they know

## 🧭 Strategic Focus

### Parent-First Experience
- **Emotional Connection**: Capture precious sports memories
- **Simple Interface**: Easy for non-technical parents
- **Time-Saving**: No more searching through long videos
- **Professional Quality**: Share-worthy highlight clips

### Anti-Social Media Approach  
- **No Public Feeds**: Keeps children's content private
- **Direct Sharing**: Text/email instead of social platforms
- **Family Control**: Parents decide what gets shared
- **Safety First**: No public profiles or commenting

## 🛠️ Technical Architecture

### Mobile Platforms
- **iOS**: Swift/SwiftUI with ARKit and Core ML
- **Android**: Kotlin/Compose with ARCore and ML Kit
- **Cross-Platform**: Shared business logic where possible

### Backend Infrastructure
- **Firebase**: Firestore for rosters, Auth for teams, Hosting for web
- **Real-time Sync**: Live roster updates across team members
- **Offline-First**: App works without internet for recording

### AI/Computer Vision
- **ML Kit/Core ML**: Jersey number detection and OCR
- **Custom Models**: Sports-specific training for better accuracy
- **Edge Processing**: All AI runs on-device for privacy

### Video Technology
- **Native Recording**: Platform camera APIs for HD capture
- **Metadata Tracking**: Time-coded bubble position data
- **Export Pipeline**: FFmpeg integration for overlay rendering

## 🚀 User Journey

### Onboarding Flow
1. **Download App** → Start 14-day free trial
2. **Create/Join Team** → QR code invite or manual setup
3. **Add Players** → OCR photo import or manual entry
4. **First Recording** → Tutorial for AR detection and recording

### Game Day Experience  
1. **Open AR Mode** → Point camera at field
2. **Learn Team Colors** → Tap any player to identify team
3. **Record Highlights** → Capture 15-60 second clips
4. **Add Celebrations** → Tag special moments with overlays
5. **Export & Share** → Select bubbles, add tags, share via text/email

### Post-Game Value
1. **Review Library** → Browse all captured highlights
2. **Create Compilations** → Combine multiple clips
3. **Share Memories** → Send to grandparents, friends
4. **Build Archive** → Season-long highlight collection

## 🎯 Target Market

### Primary Users
- **Parents of Youth Athletes** (ages 6-18)
- **Income Level**: $50K+ household (can afford youth sports)
- **Tech Comfort**: Smartphone users, social sharers
- **Geographic**: Suburban communities with youth sports programs

### Use Cases
- **Soccer/Football**: Large fields, hard to track players
- **Lacrosse**: Fast-paced action, multiple players
- **Baseball/Softball**: Tournament play, multiple games
- **Basketball**: Indoor courts, close-action highlights

### Market Size
- **US Youth Sports**: 45M+ participants
- **Parent Reach**: 90M+ parents of youth athletes  
- **Addressable Market**: $2B+ spent on youth sports annually
- **Target Penetration**: 1% = 450K families = $4.5M ARR

## 🚦 Development Roadmap

### MVP Features (Month 1-3)
- ✅ Basic AR player detection
- ✅ Simple video recording
- ✅ Manual roster management  
- ✅ Text/email sharing
- ✅ Subscription system

### V1.1 Features (Month 4-6)
- QR code team invites
- OCR roster import
- Custom overlay system
- Improved AI detection
- Analytics dashboard

### V1.2 Features (Month 7-9)
- Cloud highlight backup
- Advanced video editing
- Team collaboration features
- Referral program
- Coach analytics

### V2.0 Vision (Month 10-12)
- AI-generated highlights
- Multi-sport support
- League integration
- White-label solutions
- Advanced sharing features

## 🏆 Competitive Advantages

### Technical Moats
- **AR Expertise**: Advanced sports-specific computer vision
- **Parent Focus**: Purpose-built for family use cases
- **Privacy Design**: Trust-first architecture
- **Mobile-Native**: Optimized for smartphone capture

### Business Moats  
- **Network Effects**: Teams create lock-in
- **Data Advantage**: Sports-specific training data
- **Brand Trust**: Safety and privacy reputation
- **Viral Mechanics**: Built-in growth loops

## 📊 Success Metrics

### Product Metrics
- **Daily Active Users**: Recording sessions per day
- **Session Length**: Time spent in AR mode
- **Export Rate**: % of recordings that get exported
- **Sharing Rate**: % of exports that get shared

### Business Metrics
- **Subscription Rate**: Trial → paid conversion
- **Retention**: Month 2, 6, 12 retention rates
- **Viral Coefficient**: Invites sent per active user
- **Revenue per User**: LTV and payback period

### Growth Metrics
- **Team Adoption**: % of team rosters completed
- **Cross-Team Spread**: Teams discovering through games
- **Referral Success**: Friends recruited per user
- **App Store Rankings**: Organic discovery growth

---

**Built with ❤️ for sports families** - Capturing memories that matter, one highlight at a time.