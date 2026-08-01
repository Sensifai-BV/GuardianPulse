"""
Sensifai Guardian Pulse — AER Training Script (Pure NumPy/SciPy)
=================================================================
Zero dependency on librosa/numba. Uses pure scipy for audio processing.
Trains a CRNN on ESC-50 to classify: CRYING | IMPACT | AMBIENT
Output: sensifai_aer.tflite → Android app/src/main/assets/
"""

import os, csv, warnings

# Fix threading issues on macOS with TensorFlow
os.environ['OMP_NUM_THREADS']           = '1'
os.environ['TF_NUM_INTEROP_THREADS']    = '1'
os.environ['TF_NUM_INTRAOP_THREADS']    = '1'
os.environ['TF_CPP_MIN_LOG_LEVEL']      = '3'
os.environ['OPENBLAS_NUM_THREADS']      = '1'
os.environ['MKL_NUM_THREADS']           = '1'

warnings.filterwarnings('ignore')

import numpy as np
from scipy.io import wavfile
from scipy.signal import resample_poly
from scipy.fft import rfft
from math import gcd

# ─────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────
DATASET_DIR    = "./ESC-50-master"
AUDIO_DIR      = os.path.join(DATASET_DIR, "audio")
META_CSV       = os.path.join(DATASET_DIR, "meta/esc50.csv")
MODEL_OUTPUT   = "./sensifai_aer.tflite"
ANDROID_ASSETS = "./app/src/main/assets/"

TARGET_SR  = 22050
DURATION   = 5       # seconds
N_MFCC     = 40
N_MELS     = 128
MAX_FRAMES = 128
EPOCHS     = 40
BATCH_SIZE = 32

# Labels
CATEGORY_MAP = {
    "crying_baby":    0,  # CRYING
    "glass_breaking": 1,  # IMPACT
    "door_knock":     1,
    "fireworks":      1,
    "clock_alarm":    1,
    "can_opening":    1,
}
LABEL_NAMES = ["CRYING", "IMPACT", "AMBIENT"]
NUM_CLASSES = 3

def get_label(category):
    return CATEGORY_MAP.get(category, 2)

# ─────────────────────────────────────────────
# PURE SCIPY MFCC (No numba/librosa)
# ─────────────────────────────────────────────
def hz_to_mel(hz):
    return 2595.0 * np.log10(1.0 + hz / 700.0)

def mel_to_hz(mel):
    return 700.0 * (10.0 ** (mel / 2595.0) - 1.0)

def mel_filterbank(sr, n_fft, n_mels):
    low_hz, high_hz = 0.0, sr / 2.0
    mel_points = np.linspace(hz_to_mel(low_hz), hz_to_mel(high_hz), n_mels + 2)
    hz_points  = mel_to_hz(mel_points)
    bin_points = np.floor((n_fft + 1) * hz_points / sr).astype(int)

    fbank = np.zeros((n_mels, n_fft // 2 + 1))
    for m in range(1, n_mels + 1):
        f_left, f_center, f_right = bin_points[m-1], bin_points[m], bin_points[m+1]
        for k in range(f_left, f_center):
            fbank[m-1, k] = (k - f_left) / max(f_center - f_left, 1)
        for k in range(f_center, f_right):
            fbank[m-1, k] = (f_right - k) / max(f_right - f_center, 1)
    return fbank

def dct_matrix(n_input, n_output):
    n = np.arange(n_input)
    k = np.arange(n_output).reshape(-1, 1)
    dct = np.cos(np.pi / n_input * (n + 0.5) * k)
    dct[0] /= np.sqrt(n_input)
    dct[1:] /= np.sqrt(n_input / 2)
    return dct

def compute_mfcc(y, sr, n_mfcc=40, n_mels=128, n_fft=2048, hop=512):
    # Pre-emphasis
    y = np.append(y[0], y[1:] - 0.97 * y[:-1])
    
    # Frame & window
    frames = []
    window = np.hamming(n_fft)
    for i in range(0, len(y) - n_fft, hop):
        frame = y[i:i + n_fft] * window
        frames.append(frame)
    if not frames:
        return None
    frames = np.array(frames)
    
    # Power spectrum
    mag_spec = np.abs(rfft(frames, n=n_fft, axis=1)) ** 2
    
    # Mel filterbank
    fbank = mel_filterbank(sr, n_fft, n_mels)
    mel_spec = np.dot(mag_spec, fbank.T)
    mel_spec = np.maximum(mel_spec, 1e-10)
    log_mel  = 10.0 * np.log10(mel_spec)
    
    # DCT → MFCC
    dct = dct_matrix(n_mels, n_mfcc)
    mfcc = np.dot(log_mel, dct.T)  # (frames, n_mfcc)
    return mfcc

def load_and_resample(filepath):
    sr, data = wavfile.read(filepath)
    # Convert to float32 mono
    if data.ndim > 1:
        data = data.mean(axis=1)
    data = data.astype(np.float32)
    if data.max() > 1.0:
        data /= 32768.0
    # Resample to TARGET_SR if needed
    if sr != TARGET_SR:
        g = gcd(TARGET_SR, sr)
        up, down = TARGET_SR // g, sr // g
        data = resample_poly(data, up, down).astype(np.float32)
    # Trim/pad to DURATION
    target_len = TARGET_SR * DURATION
    if len(data) > target_len:
        data = data[:target_len]
    else:
        data = np.pad(data, (0, target_len - len(data)))
    return data

def extract_features(filepath):
    try:
        y = load_and_resample(filepath)
        mfcc = compute_mfcc(y, TARGET_SR, n_mfcc=N_MFCC, n_mels=N_MELS)
        if mfcc is None:
            return None
        # Pad/truncate to MAX_FRAMES
        if mfcc.shape[0] < MAX_FRAMES:
            mfcc = np.pad(mfcc, ((0, MAX_FRAMES - mfcc.shape[0]), (0, 0)))
        else:
            mfcc = mfcc[:MAX_FRAMES]
        # Normalize
        mean = mfcc.mean(axis=0, keepdims=True)
        std  = mfcc.std(axis=0, keepdims=True) + 1e-8
        return ((mfcc - mean) / std).astype(np.float32)
    except Exception as e:
        print(f"  ⚠️  {os.path.basename(filepath)}: {e}")
        return None

# ─────────────────────────────────────────────
# LOAD DATASET
# ─────────────────────────────────────────────
print("=" * 55)
print("  Sensifai AER — CRNN Training on ESC-50")
print("  (Pure NumPy/SciPy — Zero librosa/numba)")
print("=" * 55)

X, y_labels = [], []
class_counts = {i: 0 for i in range(NUM_CLASSES)}

with open(META_CSV, newline='') as f:
    rows = list(csv.DictReader(f))

print(f"\n📂 Processing {len(rows)} audio files...")
for idx, row in enumerate(rows):
    features = extract_features(os.path.join(AUDIO_DIR, row["filename"]))
    if features is not None:
        label = get_label(row["category"])
        X.append(features)
        y_labels.append(label)
        class_counts[label] += 1
    if (idx + 1) % 400 == 0:
        print(f"  ✓ {idx+1}/{len(rows)} processed...")

X = np.array(X)[..., np.newaxis]  # (N, MAX_FRAMES, N_MFCC, 1)
y_arr = np.array(y_labels)

print(f"\n✅ Dataset ready: {X.shape[0]} samples | Shape: {X.shape}")
for i, name in enumerate(LABEL_NAMES):
    print(f"   {name}: {class_counts[i]} samples")

# ─────────────────────────────────────────────
# BUILD CRNN MODEL
# ─────────────────────────────────────────────
import tensorflow as tf
from tensorflow import keras
from sklearn.model_selection import train_test_split

print(f"\n🧠 Building Sensifai CRNN (TF {tf.__version__})...")

y_cat = keras.utils.to_categorical(y_arr, NUM_CLASSES)
X_train, X_val, y_train, y_val = train_test_split(
    X, y_cat, test_size=0.2, random_state=42, stratify=y_arr)

inputs = keras.Input(shape=(MAX_FRAMES, N_MFCC, 1), name="mfcc_input")

x = keras.layers.Conv2D(32, (3,3), padding='same', activation='relu')(inputs)
x = keras.layers.BatchNormalization()(x)
x = keras.layers.MaxPooling2D((2,2))(x)
x = keras.layers.Dropout(0.25)(x)

x = keras.layers.Conv2D(64, (3,3), padding='same', activation='relu')(x)
x = keras.layers.BatchNormalization()(x)
x = keras.layers.MaxPooling2D((2,2))(x)
x = keras.layers.Dropout(0.25)(x)

x = keras.layers.Conv2D(128, (3,3), padding='same', activation='relu')(x)
x = keras.layers.BatchNormalization()(x)
x = keras.layers.MaxPooling2D((2,2))(x)
x = keras.layers.Dropout(0.3)(x)

# Reshape for LSTM
_, h, w, c = x.shape
x = keras.layers.Reshape((h, w * c))(x)
x = keras.layers.Bidirectional(keras.layers.LSTM(64))(x)
x = keras.layers.Dropout(0.4)(x)
x = keras.layers.Dense(64, activation='relu')(x)
x = keras.layers.Dropout(0.3)(x)
outputs = keras.layers.Dense(NUM_CLASSES, activation='softmax', name="event_label")(x)

model = keras.Model(inputs, outputs, name="SensifaiAER_CRNN")
model.summary()

model.compile(optimizer=keras.optimizers.Adam(1e-3),
              loss='categorical_crossentropy', metrics=['accuracy'])

callbacks = [
    keras.callbacks.EarlyStopping(patience=8, restore_best_weights=True, verbose=1),
    keras.callbacks.ReduceLROnPlateau(factor=0.5, patience=4, verbose=1),
]

print(f"\n🚀 Training ({EPOCHS} epochs max)...")
model.fit(X_train, y_train, validation_data=(X_val, y_val),
          epochs=EPOCHS, batch_size=BATCH_SIZE, callbacks=callbacks, verbose=1)

val_loss, val_acc = model.evaluate(X_val, y_val, verbose=0)
print(f"\n📊 Validation Accuracy: {val_acc*100:.1f}%")

from sklearn.metrics import classification_report, confusion_matrix
y_pred = np.argmax(model.predict(X_val, verbose=0), axis=1)
y_true = np.argmax(y_val, axis=1)
print("\n📋 Confusion Matrix:")
print(confusion_matrix(y_true, y_pred))
print("\n📋 Classification Report:")
print(classification_report(y_true, y_pred, target_names=LABEL_NAMES))

# ─────────────────────────────────────────────
# EXPORT TO TFLITE
# ─────────────────────────────────────────────
print("\n⚙️  Converting to TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

def representative_dataset():
    for i in range(min(200, len(X_train))):
        yield [X_train[i:i+1].astype(np.float32)]

converter.representative_dataset = representative_dataset
tflite_model = converter.convert()

with open(MODEL_OUTPUT, 'wb') as f:
    f.write(tflite_model)

size_kb = len(tflite_model) / 1024
os.makedirs(ANDROID_ASSETS, exist_ok=True)
import shutil
shutil.copy(MODEL_OUTPUT, os.path.join(ANDROID_ASSETS, "sensifai_aer.tflite"))

print(f"\n🎉 Done!")
print(f"   Model: {MODEL_OUTPUT} ({size_kb:.0f} KB)")
print(f"   Android: {ANDROID_ASSETS}sensifai_aer.tflite")
print(f"   Accuracy: {val_acc*100:.1f}%")
print(f"   Classes: {LABEL_NAMES}")
