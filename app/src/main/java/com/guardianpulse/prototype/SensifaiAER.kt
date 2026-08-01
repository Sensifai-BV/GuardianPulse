package com.guardianpulse.prototype

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader
import kotlin.math.max

/**
 * Sensifai Custom Neural Network Engine (Pure Kotlin)
 * 
 * This class implements a lightweight Multi-Layer Perceptron (MLP) inference engine
 * without requiring any external ML libraries (like TensorFlow or PyTorch).
 * It loads weights exported from our custom Scikit-Learn training script.
 * 
 * Zero dependencies, zero binary bloat, 100% on-device privacy.
 */
class SensifaiAER(private val context: Context) {

    private val layers = mutableListOf<Layer>()
    // Class prototype MFCC vectors (mean of training samples per class)
    // Used when real-time PCM extraction is not available (prototype mode)
    private val classCentroids = mutableMapOf<Int, FloatArray>()

    init {
        loadWeights()
    }

    private fun loadWeights() {
        try {
            val inputStream = context.assets.open("sensifai_aer_weights.json")
            val jsonString = InputStreamReader(inputStream).readText()
            val jsonObject = JSONObject(jsonString)
            
            val jsonLayers = jsonObject.getJSONArray("layers")
            for (i in 0 until jsonLayers.length()) {
                val layerObj = jsonLayers.getJSONObject(i)
                
                val weightsJson = layerObj.getJSONArray("weights")
                val biasesJson = layerObj.getJSONArray("biases")
                val activation = layerObj.getString("activation")
                
                val rows = weightsJson.length()
                val cols = weightsJson.getJSONArray(0).length()
                
                val weights = Array(rows) { FloatArray(cols) }
                for (r in 0 until rows) {
                    val rowJson = weightsJson.getJSONArray(r)
                    for (c in 0 until cols) {
                        weights[r][c] = rowJson.getDouble(c).toFloat()
                    }
                }
                
                val biases = FloatArray(biasesJson.length())
                for (b in 0 until biasesJson.length()) {
                    biases[b] = biasesJson.getDouble(b).toFloat()
                }
                
                layers.add(Layer(weights, biases, activation))
            }

            // Load class centroids (real MFCC prototype vectors from training data)
            if (jsonObject.has("class_centroids")) {
                val centroidsJson = jsonObject.getJSONObject("class_centroids")
                for (key in centroidsJson.keys()) {
                    val idx = key.toInt()
                    val arr = centroidsJson.getJSONArray(key)
                    val vec = FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
                    classCentroids[idx] = vec
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the trained MFCC centroid for a given class index.
     * Fallback: zero vector if centroid not loaded.
     */
    fun getCentroid(classIdx: Int): FloatArray {
        return classCentroids[classIdx] ?: FloatArray(40)
    }

    /**
     * Performs forward propagation of the Neural Network.
     * @param mfccFeatures FloatArray of size 40 (extracted MFCC features)
     * @return AudioEventLabel
     */
    fun classify(mfccFeatures: FloatArray): AudioEventLabel {
        if (layers.isEmpty()) return AudioEventLabel.UNKNOWN

        var currentInput = mfccFeatures

        for (layer in layers) {
            currentInput = layer.forward(currentInput)
        }

        // Output layer is Softmax, so we just find the argmax
        var maxIdx = 0
        var maxVal = currentInput[0]
        for (i in 1 until currentInput.size) {
            if (currentInput[i] > maxVal) {
                maxVal = currentInput[i]
                maxIdx = i
            }
        }

        return when (maxIdx) {
            0 -> AudioEventLabel.AMBIENT
            1 -> AudioEventLabel.SHOUTING
            2 -> AudioEventLabel.CRYING
            3 -> AudioEventLabel.IMPACT
            else -> AudioEventLabel.UNKNOWN
        }
    }

    private class Layer(
        val weights: Array<FloatArray>, // Shape: [input_size][output_size]
        val biases: FloatArray,         // Shape: [output_size]
        val activation: String
    ) {
        fun forward(input: FloatArray): FloatArray {
            val outputSize = biases.size
            val inputSize = input.size
            val output = FloatArray(outputSize)

            for (j in 0 until outputSize) {
                var sum = biases[j]
                for (i in 0 until inputSize) {
                    sum += input[i] * weights[i][j]
                }
                output[j] = sum
            }

            // Apply activation function
            when (activation) {
                "relu" -> {
                    for (j in 0 until outputSize) {
                        output[j] = max(0f, output[j])
                    }
                }
                "softmax" -> {
                    var maxLogit = Float.NEGATIVE_INFINITY
                    for (j in 0 until outputSize) {
                        if (output[j] > maxLogit) maxLogit = output[j]
                    }
                    var sumExp = 0f
                    for (j in 0 until outputSize) {
                        output[j] = kotlin.math.exp((output[j] - maxLogit).toDouble()).toFloat()
                        sumExp += output[j]
                    }
                    for (j in 0 until outputSize) {
                        output[j] /= sumExp
                    }
                }
            }

            return output
        }
    }
}
