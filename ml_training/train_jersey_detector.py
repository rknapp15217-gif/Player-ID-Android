"""
🏆 Jersey Number Detection Model Training Pipeline

This script sets up training for a custom TensorFlow Lite model specifically 
optimized for detecting jersey numbers in sports scenarios.

Target: >80% detection reliability with:
- Motion blur tolerance
- Variable lighting conditions  
- Multiple jersey fonts/styles
- Different camera angles
- Distance variations
"""

import tensorflow as tf
import tensorflow_datasets as tfds
import numpy as np
import cv2
import json
import os
from pathlib import Path
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split

# 🎯 Model Configuration
MODEL_CONFIG = {
    "input_size": 416,  # YOLO-style square input
    "num_classes": 100,  # Jersey numbers 0-99
    "max_detections": 10,
    "confidence_threshold": 0.6,
    "nms_threshold": 0.4,
    "batch_size": 16,
    "epochs": 100,
    "learning_rate": 0.001
}

# 📁 Directory Structure
BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data"
ANNOTATIONS_DIR = DATA_DIR / "annotations"  
IMAGES_DIR = DATA_DIR / "images"
MODELS_DIR = BASE_DIR / "models"
AUGMENTED_DIR = DATA_DIR / "augmented"

def setup_directories():
    """Create training directory structure"""
    for dir_path in [DATA_DIR, ANNOTATIONS_DIR, IMAGES_DIR, MODELS_DIR, AUGMENTED_DIR]:
        dir_path.mkdir(exist_ok=True)
    print("✅ Training directories created")

def create_yolo_model():
    """
    🧠 Create YOLO-style model for jersey number detection
    Optimized for sports scenarios with motion and lighting variations
    """
    
    input_layer = tf.keras.Input(shape=(MODEL_CONFIG["input_size"], MODEL_CONFIG["input_size"], 3))
    
    # 🔥 Feature extraction backbone (MobileNetV3 for efficiency)
    backbone = tf.keras.applications.MobileNetV3Large(
        input_shape=(MODEL_CONFIG["input_size"], MODEL_CONFIG["input_size"], 3),
        include_top=False,
        weights='imagenet'
    )(input_layer)
    
    # 🎯 Detection head for jersey numbers
    x = tf.keras.layers.GlobalAveragePooling2D()(backbone)
    x = tf.keras.layers.Dense(512, activation='relu')(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    
    # Outputs: [boxes, confidence, classes]
    boxes = tf.keras.layers.Dense(MODEL_CONFIG["max_detections"] * 4, activation='sigmoid', name='boxes')(x)
    confidence = tf.keras.layers.Dense(MODEL_CONFIG["max_detections"], activation='sigmoid', name='confidence')(x)
    classes = tf.keras.layers.Dense(MODEL_CONFIG["max_detections"] * MODEL_CONFIG["num_classes"], activation='softmax', name='classes')(x)
    
    # Reshape outputs
    boxes = tf.keras.layers.Reshape((MODEL_CONFIG["max_detections"], 4))(boxes)
    classes = tf.keras.layers.Reshape((MODEL_CONFIG["max_detections"], MODEL_CONFIG["num_classes"]))(classes)
    
    model = tf.keras.Model(inputs=input_layer, outputs=[boxes, confidence, classes])
    
    return model

def create_data_augmentation():
    """
    🎨 Data augmentation pipeline for sports scenarios
    Simulates real-world conditions: motion blur, lighting, angles
    """
    
    return tf.keras.Sequential([
        # Geometric transformations
        tf.keras.layers.RandomRotation(0.1),  # Camera angle variations
        tf.keras.layers.RandomZoom(0.2),      # Distance variations
        tf.keras.layers.RandomTranslation(0.1, 0.1),  # Player movement
        
        # Color/lighting augmentations  
        tf.keras.layers.RandomBrightness(0.3),  # Stadium lighting
        tf.keras.layers.RandomContrast(0.2),    # Shadow/sun conditions
        
        # Motion blur simulation (custom layer needed)
        # CustomMotionBlur(),
        
        # Normalize for model input
        tf.keras.layers.Rescaling(1./255)
    ])

def compile_model(model):
    """
    ⚡ Compile model with appropriate loss functions for detection
    """
    
    # Custom loss functions for detection
    def box_loss(y_true, y_pred):
        return tf.reduce_mean(tf.square(y_true - y_pred))
    
    def confidence_loss(y_true, y_pred):
        return tf.keras.losses.binary_crossentropy(y_true, y_pred)
    
    def class_loss(y_true, y_pred):
        return tf.keras.losses.categorical_crossentropy(y_true, y_pred)
    
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=MODEL_CONFIG["learning_rate"]),
        loss={
            'boxes': box_loss,
            'confidence': confidence_loss, 
            'classes': class_loss
        },
        loss_weights={
            'boxes': 1.0,
            'confidence': 1.0,
            'classes': 1.0
        },
        metrics=['accuracy']
    )
    
    return model

def create_dataset_collection_guide():
    """
    📋 Generate guide for collecting jersey number training data
    """
    
    guide = """
    🏆 JERSEY NUMBER DATASET COLLECTION GUIDE
    
    📸 DATA COLLECTION REQUIREMENTS:
    
    1. JERSEY NUMBER VARIETY:
       - All numbers 0-99 (focus on common 1-50)
       - Different fonts (block, script, outlined)
       - Various team colors and contrasts
       - Home vs away jersey styles
    
    2. SPORTS SCENARIOS:
       - Soccer, basketball, football, baseball, hockey
       - Youth, high school, college, professional levels
       - Indoor vs outdoor lighting conditions
    
    3. CAMERA CONDITIONS:
       - Multiple distances (close-up to field-wide shots)
       - Various angles (straight-on, side, diagonal) 
       - Different lighting (bright sun, shadows, indoor)
       - Motion blur (moving players)
       - Partial occlusion (arms covering numbers)
    
    4. ANNOTATION FORMAT:
       - Bounding boxes around jersey numbers
       - Class labels (0-99 for jersey numbers)
       - JSON format: {"image": "path", "boxes": [{"class": 10, "bbox": [x,y,w,h]}]}
    
    📊 TARGET DATASET SIZE:
       - Minimum: 10,000 annotated jersey instances
       - Optimal: 50,000+ instances  
       - Balance: ~500 examples per number (0-99)
    
    🎯 QUALITY REQUIREMENTS:
       - Clear number visibility
       - Accurate bounding box annotations
       - Diverse lighting and angle conditions
       - Real-world motion and blur scenarios
    
    🔧 ANNOTATION TOOLS:
       - LabelImg: https://github.com/tzutalin/labelImg
       - Roboflow: https://roboflow.com (automated assistance)
       - CVAT: https://github.com/opencv/cvat (team collaboration)
    """
    
    with open(BASE_DIR / "dataset_collection_guide.md", "w") as f:
        f.write(guide)
    
    print("📋 Dataset collection guide created")

def convert_to_tflite(model, model_name="jersey_detector"):
    """
    📱 Convert trained model to TensorFlow Lite for Android deployment
    """
    
    # Convert to TensorFlow Lite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Optimization for mobile deployment
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    
    # Quantization for smaller model size and faster inference
    converter.representative_dataset = lambda: representative_dataset_generator()
    converter.target_spec.supported_types = [tf.int8]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8
    
    tflite_model = converter.convert()
    
    # Save TensorFlow Lite model
    model_path = MODELS_DIR / f"{model_name}.tflite"
    with open(model_path, "wb") as f:
        f.write(tflite_model)
    
    print(f"📱 TensorFlow Lite model saved: {model_path}")
    return model_path

def representative_dataset_generator():
    """Generate representative dataset for quantization"""
    # This would use actual training data samples
    # For now, generate random samples
    for _ in range(100):
        yield [np.random.random((1, MODEL_CONFIG["input_size"], MODEL_CONFIG["input_size"], 3)).astype(np.float32)]

def main():
    """
    🚀 Main training pipeline setup
    """
    
    print("🏆 Setting up Jersey Number Detection Model Training")
    
    # Setup
    setup_directories()
    create_dataset_collection_guide()
    
    # Create model architecture
    print("🧠 Creating model architecture...")
    model = create_yolo_model()
    model = compile_model(model)
    
    # Print model summary
    model.summary()
    
    # Save model architecture diagram
    tf.keras.utils.plot_model(
        model, 
        to_file=MODELS_DIR / "model_architecture.png",
        show_shapes=True,
        show_layer_names=True
    )
    
    print("✅ Model architecture created")
    print(f"📁 Training setup complete at: {BASE_DIR}")
    print("📋 Next steps:")
    print("   1. Collect jersey number dataset (see dataset_collection_guide.md)")
    print("   2. Annotate images with bounding boxes")  
    print("   3. Run training with collected data")
    print("   4. Convert to TensorFlow Lite")
    print("   5. Deploy to Android app")

if __name__ == "__main__":
    main()