# 🏆 Jersey Number Detection - Custom ML Model Setup Guide

## 🎯 Overview

This guide sets up training for a custom TensorFlow Lite model specifically optimized for detecting jersey numbers in sports scenarios, targeting >80% detection reliability.

## 📋 Prerequisites

- Python 3.8+ 
- CUDA-capable GPU (recommended for training)
- 16GB+ RAM
- 50GB+ storage for dataset
- Android Studio for model deployment

## 🚀 Setup Instructions

### 1. Environment Setup

```bash
# Create virtual environment
python -m venv jersey_ml_env
source jersey_ml_env/bin/activate  # On Windows: jersey_ml_env\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Verify TensorFlow GPU (if available)
python -c "import tensorflow as tf; print(tf.config.list_physical_devices('GPU'))"
```

### 2. Data Collection Strategy

#### 📸 **Priority Data Types:**
- **High Priority**: Numbers 1-50 (most common in sports)
- **Medium Priority**: Numbers 51-99
- **Special Cases**: Numbers 0, 00 (goalkeepers, special positions)

#### 🏟️ **Sports Coverage:**
- Soccer (football): Youth, high school, college, professional
- Basketball: Indoor lighting, different jersey materials
- American Football: Outdoor conditions, various fonts
- Baseball: Different angles, distance shots
- Hockey: Fast movement, ice rink lighting

#### 📊 **Target Dataset Composition:**
```
Total Target: 50,000+ annotated instances
├── Close-up shots (0-10 feet): 40%
├── Medium distance (10-30 feet): 35% 
├── Far shots (30+ feet): 25%
└── Motion blur samples: 15% of each category
```

### 3. Data Collection Methods

#### Method 1: Manual Collection with App
1. Use the PlayerID app's data collection mode
2. Tap to annotate jersey numbers in real-time
3. Auto-export annotations in YOLO format

#### Method 2: Video Frame Extraction
```python
# Extract frames from game videos
python extract_frames.py --video game_footage.mp4 --fps 2
```

#### Method 3: Synthetic Data Generation
```python
# Generate synthetic jersey numbers with various conditions
python generate_synthetic.py --count 10000 --variations lighting,angle,blur
```

### 4. Model Training Pipeline

#### Phase 1: Initial Training (Basic Detection)
```bash
# Train base model with collected data
python train_jersey_detector.py --epochs 50 --batch-size 16 --data-path ./data

# Convert to TensorFlow Lite
python convert_to_tflite.py --model jersey_detector.h5
```

#### Phase 2: Fine-tuning (Sports-Specific Optimization)
```bash
# Fine-tune on sports-specific scenarios
python fine_tune_model.py --base-model jersey_detector.tflite --sports-data ./sports_dataset
```

#### Phase 3: Optimization (Mobile Deployment)
```bash
# Optimize for mobile inference
python optimize_for_mobile.py --model jersey_detector.tflite --target android
```

### 5. Model Evaluation

#### Performance Metrics:
- **Detection Accuracy**: >80% (primary goal)
- **Inference Speed**: <100ms per frame on mobile
- **Model Size**: <50MB for TensorFlow Lite
- **False Positive Rate**: <10%

#### Evaluation Script:
```bash
python evaluate_model.py --model jersey_detector.tflite --test-data ./test_set
```

### 6. Android Integration

#### Deploy Model to App:
1. Copy `jersey_detector.tflite` to `app/src/main/assets/`
2. Update `CustomJerseyDetectionAnalyzer.kt` model path
3. Test on device with various jerseys
4. Monitor performance metrics in app

## 📊 Training Progress Tracking

### Data Collection Progress:
- [ ] Collect 1,000 samples (MVP)
- [ ] Collect 5,000 samples (Basic training)
- [ ] Collect 20,000 samples (Good coverage)  
- [ ] Collect 50,000+ samples (Production ready)

### Model Development Milestones:
- [ ] Base model architecture complete
- [ ] Initial training pipeline working
- [ ] >60% accuracy achieved
- [ ] >80% accuracy achieved (GOAL)
- [ ] Mobile optimization complete
- [ ] Android integration complete

## 🎯 Alternative Approaches (If Needed)

### Option 1: Transfer Learning from YOLO
```bash
# Use pre-trained YOLOv8 and fine-tune for jersey numbers
yolo train model=yolov8n.pt data=jersey_dataset.yaml epochs=100
```

### Option 2: Template Matching Hybrid
```python
# Combine ML detection with template matching
python train_hybrid_detector.py --ml-confidence 0.6 --template-fallback
```

### Option 3: Optical Character Recognition (OCR) Enhancement
```python
# Train specialized OCR for jersey fonts
python train_jersey_ocr.py --focus sports-fonts
```

## 🚨 Troubleshooting

### Common Issues:
1. **Low accuracy**: Increase dataset diversity, check annotation quality
2. **Slow inference**: Reduce model size, use quantization
3. **High false positives**: Improve negative sampling, adjust thresholds
4. **Poor mobile performance**: Optimize model architecture, use GPU acceleration

### Performance Optimization:
- Use mixed precision training for speed
- Implement progressive resizing during training
- Apply knowledge distillation for model compression
- Use TensorRT for NVIDIA GPU optimization

## 📞 Next Steps

1. **Start Data Collection**: Begin with Method 1 using the app
2. **Baseline Training**: Train initial model with 1,000+ samples
3. **Iterative Improvement**: Continuously add data and retrain
4. **Production Deployment**: Deploy when >80% accuracy achieved

## 📚 Resources

- **TensorFlow Lite Guide**: https://www.tensorflow.org/lite
- **YOLOv8 Documentation**: https://docs.ultralytics.com/
- **Data Annotation Tools**: LabelImg, Roboflow, CVAT
- **Sports Dataset Examples**: Sport-1M, UCF Sports Action

---

**Goal**: Achieve >80% jersey number detection reliability in real-world sports scenarios through custom ML model training and deployment.