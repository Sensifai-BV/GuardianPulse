"""
Sensifai Guardian Pulse — AER Training Script (Scikit-Learn MLP)
=================================================================
Trains a lightweight Neural Network on ESC-50 and RAVDESS to classify:
  0=AMBIENT | 1=SHOUTING | 2=CRYING | 3=IMPACT
Output: sensifai_aer_weights.json → Android app/src/main/assets/
"""

import os, csv, json, warnings, glob
warnings.filterwarnings('ignore')

import numpy as np
from scipy.io import wavfile
from scipy.signal import resample_poly
from scipy.fft import rfft
from math import gcd
from sklearn.neural_network import MLPClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

# ─────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────
DATASET_DIR    = "./ESC-50-master"
AUDIO_DIR      = os.path.join(DATASET_DIR, "audio")
META_CSV       = os.path.join(DATASET_DIR, "meta/esc50.csv")
RAVDESS_DIR    = "./RAVDESS Emotional speech audio"

MODEL_OUTPUT   = "./sensifai_aer_weights.json"
ANDROID_ASSETS = "./app/src/main/assets/"

TARGET_SR  = 16000
DURATION   = 5       # seconds
N_MFCC     = 20
N_MELS     = 64
N_FFT      = 1024
HOP        = 512

# Labels
LABEL_NAMES = ["AMBIENT", "SHOUTING", "CRYING", "IMPACT"]
NUM_CLASSES = 4

L_AMBIENT  = 0
L_SHOUTING = 1
L_CRYING   = 2
L_IMPACT   = 3

CATEGORY_MAP = {
    "crying_baby":    L_CRYING,
    "glass_breaking": L_IMPACT,
    "door_knock":     L_IMPACT,
    "fireworks":      L_IMPACT,
    "clock_alarm":    L_IMPACT,
    "can_opening":    L_IMPACT,
}

# ─────────────────────────────────────────────
# PURE SCIPY MFCC
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

def load_and_resample(filepath):
    sr, data = wavfile.read(filepath)
    if data.ndim > 1: data = data.mean(axis=1)
    data = data.astype(np.float32)
    if data.max() > 1.0: data /= 32768.0
    if sr != TARGET_SR:
        g = gcd(TARGET_SR, sr)
        data = resample_poly(data, TARGET_SR // g, sr // g).astype(np.float32)
    target_len = TARGET_SR * DURATION
    if len(data) > target_len: data = data[:target_len]
    else: data = np.pad(data, (0, target_len - len(data)))
    return data

def extract_features(filepath):
    try:
        y = load_and_resample(filepath)
        y = np.append(y[0], y[1:] - 0.97 * y[:-1])
        window = np.hamming(N_FFT)
        frames = []
        for i in range(0, len(y) - N_FFT, HOP):
            frames.append(y[i:i + N_FFT] * window)
        frames = np.array(frames)
        mag_spec = np.abs(rfft(frames, n=N_FFT, axis=1)) ** 2
        fbank = mel_filterbank(TARGET_SR, N_FFT, N_MELS)
        mel_spec = np.maximum(np.dot(mag_spec, fbank.T), 1e-10)
        log_mel  = 10.0 * np.log10(mel_spec)
        dct = dct_matrix(N_MELS, N_MFCC)
        mfcc = np.dot(log_mel, dct.T)
        mean = mfcc.mean(axis=0)
        std = mfcc.std(axis=0)
        return np.concatenate([mean, std]).astype(np.float32)
    except Exception as e:
        return None

# ─────────────────────────────────────────────
# LOAD DATASETS
# ─────────────────────────────────────────────
print("====================================================")
print("  Sensifai AER — Fusion Training (ESC-50 + RAVDESS)")
print("====================================================")

X, y = [], []
class_counts = {i: 0 for i in range(NUM_CLASSES)}

# 1. ESC-50
print(f"\n📂 Processing ESC-50...")
with open(META_CSV, newline='') as f:
    rows = list(csv.DictReader(f))

for idx, row in enumerate(rows):
    feats = extract_features(os.path.join(AUDIO_DIR, row["filename"]))
    if feats is not None:
        label = CATEGORY_MAP.get(row["category"], L_AMBIENT)
        X.append(feats)
        y.append(label)
        class_counts[label] += 1
print(f"   Done! (Total ESC-50: {len(rows)})")

# 2. RAVDESS
print(f"\n📂 Processing RAVDESS...")
ravdess_files = glob.glob(os.path.join(RAVDESS_DIR, "**/*.wav"), recursive=True)
for idx, filepath in enumerate(ravdess_files):
    # RAVDESS filename format: 03-01-05-01-01-01-01.wav
    # Identifiers: Modality-VocalChannel-Emotion-Intensity-Statement-Repetition-Actor
    parts = os.path.basename(filepath).replace(".wav", "").split("-")
    if len(parts) == 7:
        emotion = int(parts[2])
        intensity = int(parts[3])
        
        # 05=Angry, 06=Fearful
        if (emotion == 5 or emotion == 6) and intensity == 2:
            label = L_SHOUTING
        else:
            # We don't want to flood the AMBIENT class with human speech,
            # but we can add some normal speech as AMBIENT
            if emotion == 1 or emotion == 2: # neutral or calm
                label = L_AMBIENT
            else:
                continue # Skip other emotions to balance
                
        feats = extract_features(filepath)
        if feats is not None:
            X.append(feats)
            y.append(label)
            class_counts[label] += 1

print(f"   Done! (Total RAVDESS files added: {len(ravdess_files)})")

X = np.array(X)
y = np.array(y)

print(f"\n✅ Data ready. Shape: {X.shape}")
for i, name in enumerate(LABEL_NAMES):
    print(f"   {name}: {class_counts[i]} samples")

# ─────────────────────────────────────────────
# TRAIN MODEL
# ─────────────────────────────────────────────
print("\n🧠 Training 4-Class MLP Classifier...")
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

mlp = MLPClassifier(hidden_layer_sizes=(64, 32), max_iter=500, random_state=42)
mlp.fit(X_train, y_train)

y_pred = mlp.predict(X_test)
print(f"✅ Training complete. Accuracy: {mlp.score(X_test, y_test)*100:.1f}%")
print("\n📋 Classification Report:")
print(classification_report(y_test, y_pred, target_names=LABEL_NAMES))

# ─────────────────────────────────────────────
# EXPORT WEIGHTS + CLASS CENTROIDS TO JSON
# ─────────────────────────────────────────────
model_data = {
    "layers": [],
    "class_centroids": {},
    "label_names": LABEL_NAMES
}

for idx, (w, b) in enumerate(zip(mlp.coefs_, mlp.intercepts_)):
    model_data["layers"].append({
        "weights": w.tolist(),
        "biases": b.tolist(),
        "activation": "relu" if idx < len(mlp.coefs_)-1 else "softmax"
    })

# Compute class centroids (mean feature vector per class from training data)
# These are used in FusionEngine as realistic input prototypes when real PCM is not available
X_full = np.array(X)
y_full = np.array(y)
for class_idx in range(NUM_CLASSES):
    mask = y_full == class_idx
    if mask.sum() > 0:
        centroid = X_full[mask].mean(axis=0).tolist()
    else:
        centroid = [0.0] * 40
    model_data["class_centroids"][str(class_idx)] = centroid
    print(f"   Centroid for {LABEL_NAMES[class_idx]}: {mask.sum()} samples averaged")

with open(MODEL_OUTPUT, "w") as f:
    json.dump(model_data, f)

os.makedirs(ANDROID_ASSETS, exist_ok=True)
import shutil
shutil.copy(MODEL_OUTPUT, os.path.join(ANDROID_ASSETS, "sensifai_aer_weights.json"))
print(f"\n✅ Exported {MODEL_OUTPUT} (with centroids) to Android assets!")

