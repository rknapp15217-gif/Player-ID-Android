@echo off
REM 🏆 Jersey Number Detection - Quick Setup Script (Windows)

echo 🏆 Setting up Jersey Number Detection Training Environment...

REM Create virtual environment
echo 📦 Creating Python virtual environment...
python -m venv jersey_ml_env

REM Activate virtual environment
echo ⚡ Activating virtual environment...
call jersey_ml_env\Scripts\activate.bat

REM Install dependencies
echo ⬇️ Installing Python dependencies...
python -m pip install --upgrade pip
pip install -r requirements.txt

REM Initialize training directories
echo 📁 Creating training directories...
python train_jersey_detector.py

echo ✅ Setup complete!
echo.
echo 🚀 Next Steps:
echo 1. Connect your Android device
echo 2. Install the updated app: adb install -r app-debug.apk
echo 3. Switch to '📸 Collect' mode in the app
echo 4. Tap on jersey numbers to collect training samples
echo 5. Enable 'Auto-collect' to capture high-confidence ML Kit detections
echo.
echo 📊 Target: Collect 1,000+ samples across different:
echo    - Jersey numbers (0-99)
echo    - Lighting conditions (bright/normal/dark)
echo    - Distances (close/medium/far)
echo    - Camera angles (front/side/angled)
echo.
echo 💡 Tip: Focus on common numbers first (1-50) for faster training
echo.
pause