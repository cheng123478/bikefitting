package uts.starrysky.dynamicpose.android

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import io.dcloud.uts.UTSAndroid
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.Executors

object MediaPipePoseNative {
    private const val DEFAULT_MODEL_ASSET = "pose_landmarker_lite.task"
    private const val DEFAULT_PROVIDER = "MediaPipe Pose Landmarker"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getPluginInfoAsJson(): String {
        val info = JSONObject()
        info.put("ready", false)
        info.put("provider", DEFAULT_PROVIDER)
        info.put("platform", "android")
        info.put("modelAssetPath", DEFAULT_MODEL_ASSET)
        info.put("supportedModes", JSONArray().put("video"))
        val context = UTSAndroid.getAppContext()
        if (context == null) {
            info.put("reason", "Android context is unavailable")
            return info.toString()
        }
        return try {
            context.assets.open(DEFAULT_MODEL_ASSET).close()
            info.put("ready", true)
            info.put("reason", JSONObject.NULL)
            info.toString()
        } catch (t: Throwable) {
            info.put("reason", t.message ?: "Model asset is missing")
            info.toString()
        }
    }

    fun analyzeVideoAsJsonAsync(optionsJson: String, callback: (String?, String?) -> Unit) {
        executor.execute {
            try {
                val result = analyzeVideoInternal(parseAnalyzeOptions(optionsJson))
                mainHandler.post {
                    callback(result, null)
                }
            } catch (t: Throwable) {
                val message = t.message ?: t.javaClass.simpleName
                mainHandler.post {
                    callback(null, message)
                }
            }
        }
    }

    fun captureFrameAsBase64(optionsJson: String): String? {
        val options = parseSnapshotOptions(optionsJson)
        val retriever = MediaMetadataRetriever()
        return try {
            val context = requireContext()
            retriever.setDataSource(context, buildVideoUri(options.videoPath))
            val frame = retriever.getFrameAtTime(
                options.timestampMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: return null
            val normalized = ensureArgb8888(applyRotationIfNeeded(retriever, frame))
            val scaled = scaleBitmapIfNeeded(normalized, options.maxWidth)
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, options.quality, output)
            val encoded = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        } finally {
            retriever.release()
        }
    }

    private fun analyzeVideoInternal(options: AnalyzeOptions): String {
        val context = requireContext()
        val videoUri = buildVideoUri(options.videoPath)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: throw IllegalArgumentException("Unable to read video duration")
        val intervalMs = options.inferenceIntervalMs.coerceAtLeast(33L)
        val expectedFrameCount = (durationMs / intervalMs).toInt() + 1
        val firstFrameRaw = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
            ?: throw IllegalArgumentException("Unable to decode first video frame")
        val firstFrame = ensureArgb8888(applyRotationIfNeeded(retriever, firstFrameRaw))
        val width = firstFrame.width
        val height = firstFrame.height

        val poseLandmarker = createPoseLandmarker(context, options)
        val result = JSONObject()
        val framesJson = JSONArray()
        var detectedFrameCount = 0

        try {
            for (index in 0 until expectedFrameCount) {
                val timestampMs = (index * intervalMs).coerceAtMost(durationMs)
                val rawFrame = retriever.getFrameAtTime(
                    timestampMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST
                ) ?: continue
                val frame = ensureArgb8888(applyRotationIfNeeded(retriever, rawFrame))
                val mpImage = BitmapImageBuilder(frame).build()
                val detectionResult = poseLandmarker.detectForVideo(mpImage, timestampMs)
                val poseLandmarks = detectionResult.landmarks().firstOrNull() ?: continue

                val frameJson = JSONObject()
                frameJson.put("frameIndex", detectedFrameCount)
                frameJson.put("timestampMs", timestampMs)
                frameJson.put("landmarks", toLandmarkJson(poseLandmarks))
                framesJson.put(frameJson)
                detectedFrameCount++
            }
        } finally {
            poseLandmarker.close()
            retriever.release()
        }

        if (detectedFrameCount == 0) {
            throw IllegalStateException("No pose was detected from the selected video")
        }

        result.put("provider", DEFAULT_PROVIDER)
        result.put("platform", "android")
        result.put("modelAssetPath", options.modelAssetPath)
        result.put("viewType", options.viewType ?: JSONObject.NULL)
        result.put("width", width)
        result.put("height", height)
        result.put("durationMs", durationMs)
        result.put("intervalMs", intervalMs)
        result.put("expectedFrameCount", expectedFrameCount)
        result.put("detectedFrameCount", detectedFrameCount)
        result.put("frames", framesJson)
        return result.toString()
    }

    private fun createPoseLandmarker(
        context: android.content.Context,
        options: AnalyzeOptions
    ): PoseLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(options.modelAssetPath)
            .build()

        val landmarkerOptions = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(options.numPoses)
            .setMinPoseDetectionConfidence(options.minPoseDetectionConfidence)
            .setMinPosePresenceConfidence(options.minPosePresenceConfidence)
            .setMinTrackingConfidence(options.minTrackingConfidence)
            .build()

        return PoseLandmarker.createFromOptions(context, landmarkerOptions)
    }

    private fun toLandmarkJson(landmarks: List<NormalizedLandmark>): JSONArray {
        val jsonArray = JSONArray()
        landmarks.forEach { landmark ->
            val landmarkJson = JSONObject()
            landmarkJson.put("x", landmark.x().toDouble())
            landmarkJson.put("y", landmark.y().toDouble())
            landmarkJson.put("z", landmark.z().toDouble())
            landmarkJson.put("visibility", landmark.visibility().orElse(0f).toDouble())
            landmarkJson.put("presence", landmark.presence().orElse(0f).toDouble())
            jsonArray.put(landmarkJson)
        }
        return jsonArray
    }

    private fun parseAnalyzeOptions(optionsJson: String): AnalyzeOptions {
        val json = JSONObject(optionsJson)
        return AnalyzeOptions(
            videoPath = json.getString("videoPath"),
            viewType = json.optString("viewType", null),
            inferenceIntervalMs = json.optLong("inferenceIntervalMs", 83L),
            modelAssetPath = json.optString("modelAssetPath", DEFAULT_MODEL_ASSET).ifBlank { DEFAULT_MODEL_ASSET },
            numPoses = json.optInt("numPoses", 1).coerceAtLeast(1),
            minPoseDetectionConfidence = json.optDouble("minPoseDetectionConfidence", 0.6).toFloat(),
            minPosePresenceConfidence = json.optDouble("minPosePresenceConfidence", 0.6).toFloat(),
            minTrackingConfidence = json.optDouble("minTrackingConfidence", 0.6).toFloat()
        )
    }

    private fun parseSnapshotOptions(optionsJson: String): SnapshotOptions {
        val json = JSONObject(optionsJson)
        return SnapshotOptions(
            videoPath = json.getString("videoPath"),
            timestampMs = json.optLong("timestampMs", 0L).coerceAtLeast(0L),
            quality = json.optInt("quality", 72).coerceIn(40, 95),
            maxWidth = json.optInt("maxWidth", 720).coerceAtLeast(320)
        )
    }

    private fun requireContext(): android.content.Context {
        return UTSAndroid.getUniActivity() ?: UTSAndroid.getAppContext()
            ?: throw IllegalStateException("Android context is unavailable")
    }

    private fun buildVideoUri(videoPath: String): Uri {
        if (videoPath.startsWith("content://")) {
            return Uri.parse(videoPath)
        }
        if (videoPath.startsWith("file://")) {
            return Uri.parse(videoPath)
        }
        return Uri.fromFile(File(videoPath))
    }

    private fun ensureArgb8888(bitmap: Bitmap): Bitmap {
        return if (bitmap.config == Bitmap.Config.ARGB_8888) {
            bitmap
        } else {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
    }

    private fun applyRotationIfNeeded(retriever: MediaMetadataRetriever, bitmap: Bitmap): Bitmap {
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toFloatOrNull()
            ?: 0f
        if (rotation == 0f) {
            return bitmap
        }
        val matrix = Matrix()
        matrix.postRotate(rotation)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth || maxWidth <= 0) {
            return bitmap
        }
        val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, targetHeight, true)
    }
}

private data class AnalyzeOptions(
    val videoPath: String,
    val viewType: String?,
    val inferenceIntervalMs: Long,
    val modelAssetPath: String,
    val numPoses: Int,
    val minPoseDetectionConfidence: Float,
    val minPosePresenceConfidence: Float,
    val minTrackingConfidence: Float
)

private data class SnapshotOptions(
    val videoPath: String,
    val timestampMs: Long,
    val quality: Int,
    val maxWidth: Int
)
